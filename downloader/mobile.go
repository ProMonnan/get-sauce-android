package downloader

import (
	"cmp"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"sync/atomic"

	"github.com/gan-of-culture/get-sauce/config"
	"github.com/gan-of-culture/get-sauce/merger"
	"github.com/gan-of-culture/get-sauce/static"
)

// AdditionalFile describes an extra file (audio/caption) that Kotlin/ffmpeg-kit
// should mux with the main video during the final merge on the Android side.
type AdditionalFile struct {
	Path     string          `json:"path"`
	DataType static.DataType `json:"dataType"`
	Language string          `json:"language,omitempty"`
}

// MobileResult is what the mobile bridge returns to Kotlin after a Download call.
type MobileResult struct {
	// Files that make up the main stream. If NeedsFinalMerge is false these are
	// the finished outputs (one per URL, e.g. image sets, or a single fragment-merged file).
	MainFiles []string `json:"mainFiles"`
	// Additional audio / caption files to mux with MainFiles[0] on the Android side.
	Additional []AdditionalFile `json:"additional,omitempty"`
	// If true the caller should run ffmpeg-kit to combine MainFiles[0] with Additional.
	NeedsFinalMerge bool `json:"needsFinalMerge"`
	// Suggested final extension (mp4, webm, mkv...) based on stream metadata.
	FinalExt string `json:"finalExt,omitempty"`
	// The cleaned title used for filenames.
	Title string `json:"title"`
	// DataType of the main stream (video/image/audio).
	MainType static.DataType `json:"mainType"`
}

// progressCallback, if set, is invoked repeatedly with the running total of bytes
// written for the currently downloading file. Cleared automatically at end of
// DownloadParts. Not thread-safe against reassignment; use only through the mobile bridge.
var progressCallback atomic.Value // holds func(int64) or nil

// SetProgressCallback installs a callback fired with byte-counts during Download.
// Pass nil to clear.
func SetProgressCallback(cb func(int64)) {
	if cb == nil {
		progressCallback.Store((func(int64))(nil))
		return
	}
	progressCallback.Store(cb)
}

func fireProgress(n int64) {
	if v := progressCallback.Load(); v != nil {
		if cb, ok := v.(func(int64)); ok && cb != nil {
			cb(n)
		}
	}
}

// partProgress is io.Writer that forwards byte counts to the mobile listener.
type partProgress struct{}

func (partProgress) Write(p []byte) (int, error) {
	n := len(p)
	fireProgress(int64(n))
	return n, nil
}

// cancelToken is checked between file downloads. Set from the mobile bridge.
var cancelToken atomic.Bool

// SetCancelled marks the current download as cancelled. Between-file boundaries
// return an error; the currently in-flight HTTP body has to finish.
func SetCancelled(v bool) { cancelToken.Store(v) }

// IsCancelled reports whether Cancel has been requested.
func IsCancelled() bool { return cancelToken.Load() }

// DownloadParts is the mobile-friendly entry point. It performs everything
// downloader.Download does *except* the final ffmpeg data-merge (video+audio+caption
// muxing) which is delegated to ffmpeg-kit on the Android side.
//
// Fragment-based streams (HLS parts, split image sets) ARE assembled here because
// merger.NewFragmentMerger is pure-Go concat and works fine on Android.
func (d *downloaderStruct) DownloadParts(data *static.Data) (*MobileResult, error) {
	if IsCancelled() {
		return nil, fmt.Errorf("cancelled")
	}

	data.Title = cmp.Or(config.OutputName, data.Title)
	d.filename = sanitizeTitle(data.Title)

	if config.Subdirectory {
		d.filePath = filepath.Join(config.OutputPath, d.filename)
	} else {
		d.filePath = config.OutputPath
	}

	if d.filePath != "" {
		if err := os.MkdirAll(d.filePath, os.ModePerm); err != nil {
			return nil, err
		}
	}

	mainFiles, err := d.downloadStream(data)
	if err != nil {
		return nil, err
	}
	if IsCancelled() {
		return nil, fmt.Errorf("cancelled")
	}

	extras, err := d.downloadAdditionalStreams(data)
	if err != nil {
		return nil, err
	}

	var additional []AdditionalFile
	for _, f := range extras {
		additional = append(additional, AdditionalFile{
			Path:     f.Path,
			DataType: f.DataType,
		})
	}

	res := &MobileResult{
		MainFiles:  mainFiles,
		Additional: additional,
		MainType:   d.stream.Type,
		Title:      d.filename,
	}

	// A final merge is only meaningful for video streams that got extras
	// (separate audio or caption tracks).
	if d.stream.Type == static.DataTypeVideo && len(additional) > 0 {
		res.NeedsFinalMerge = true
		res.FinalExt = cmp.Or(d.stream.Ext, "mp4")
	} else {
		// no extras => nothing to mux. Result is already usable.
		res.FinalExt = d.stream.Ext
	}

	return res, nil
}

// unusedMerger keeps merger import alive for potential future decrypt-only usage.
var _ = merger.MergeFile{}

// tapWriter is io.Writer that mirrors bytes through partProgress.
// (kept if we later wrap the internal writers; currently unused because
// downloader.writeFile uses io.Copy directly and progress can be inferred
// per-part from stream.Size when known.)
type tapWriter struct{ w io.Writer }

func (t tapWriter) Write(p []byte) (int, error) {
	n, err := t.w.Write(p)
	if n > 0 {
		fireProgress(int64(n))
	}
	return n, err
}
