package downloader

import (
	"cmp"
	"context"
	"crypto/sha1"
	"fmt"
	"io"
	"log"
	"maps"
	"net/http"
	"os"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
	"sync"
	"sync/atomic"
	"time"

	"github.com/gan-of-culture/get-sauce/config"
	"github.com/gan-of-culture/get-sauce/merger"
	"github.com/gan-of-culture/get-sauce/request"
	"github.com/gan-of-culture/get-sauce/static"
	"github.com/gan-of-culture/get-sauce/utils"
	"github.com/pkg/errors"
	"github.com/schollz/progressbar/v3"
	"golang.org/x/sync/errgroup"
)

type downloadInfo struct {
	URL     static.URL
	FileURI string
	Headers map[string]string
}

// downloaderStruct instance
type downloaderStruct struct {
	stream      *static.Stream
	client      *http.Client
	filePath    string
	tmpFilePath string
	filename    string
	progressBar *progressbar.ProgressBar
	bar         bool
}

func init() {
	runtime.GOMAXPROCS(runtime.NumCPU())
}

// New instance of Downloader
func New(bar bool) *downloaderStruct {
	return &downloaderStruct{
		client:   request.DefaultClient(),
		filePath: config.OutputPath,
		bar:      bar,
	}
}

// Download extracted data
func (downloader *downloaderStruct) Download(data *static.Data) error {
	if config.ShowInfo {
		printInfo(data)
		return nil
	}

	data.Title = cmp.Or(config.OutputName, data.Title)
	// sanitize filename here
	downloader.filename = sanitizeTitle(data.Title)

	if config.Subdirectory {
		downloader.filePath = config.OutputPath
		downloader.filePath = filepath.Join(downloader.filePath, downloader.filename)
	}

	if downloader.filePath != "" {
		err := os.MkdirAll(downloader.filePath, os.ModePerm)
		if err != nil {
			return err
		}
	}

	fileURIs, err := downloader.downloadStream(data)
	if err != nil {
		return err
	}

	additionalFiles, err := downloader.downloadAdditionalStreams(data)
	if err != nil {
		return err
	}

	if config.Merge == config.MergeOptNone {
		return nil
	}

	mergeFiles := make([]*merger.MergeFile, len(fileURIs))
	for i, f := range fileURIs {
		mergeFiles[i] = &merger.MergeFile{Path: f, DataType: static.DataTypeImage}
	}

	switch config.Merge {
	case config.MergeOptDefault:
		if len(additionalFiles) < 1 {
			break
		}
		downloader.stream.Ext = cmp.Or(downloader.stream.Ext, downloader.stream.URLs[0].Ext)
		mergeFiles = append(mergeFiles, additionalFiles...)
		return merger.NewDataMerger().Merge(mergeFiles, filepath.Join(downloader.filePath, fmt.Sprintf("%s_merged.%s", downloader.filename, data.Streams[config.SelectStream].Ext)))
	case config.MergeOptCBZ:
		return merger.NewArchiveMerger(downloader.bar, data).Merge(mergeFiles, filepath.Join(downloader.filePath, fmt.Sprintf("%s.cbz", downloader.filename)))
	}

	return nil
}

func (downloader *downloaderStruct) downloadStream(data *static.Data) ([]string, error) {
	// select stream to download
	var ok bool
	if downloader.stream, ok = data.Streams[config.SelectStream]; !ok {
		log.Println(data.Streams)
		return nil, fmt.Errorf("stream %s not found", config.SelectStream)
	}

	if !config.Quiet {
		printStreamInfo(data, config.SelectStream)
	}

	streamNeedsMerge := (downloader.stream.Ext != "")
	if streamNeedsMerge {
		// ensure a different tmpDir for each download so concurrent processes won't colide
		h := sha1.New()
		h.Write([]byte(data.Title + config.SelectStream))
		downloader.tmpFilePath = filepath.Join(downloader.filePath, fmt.Sprintf("%x/", h.Sum(nil)[15:]))
		err := os.MkdirAll(downloader.tmpFilePath, os.ModePerm)
		if err != nil {
			return nil, err
		}
	}

	headers := config.FakeHeaders
	headers["Referer"] = data.URL
	maps.Copy(headers, downloader.stream.Headers)

	lenOfUrls := len(downloader.stream.URLs)
	appendEnum := false
	if lenOfUrls > 1 || config.Pages != "" {
		appendEnum = true
	}

	URLchan := make(chan downloadInfo, lenOfUrls)
	workers := min(config.Workers, lenOfUrls)
	errs, _ := errgroup.WithContext(context.TODO())

	for range workers {
		errs.Go(func() error {
			for {
				dlInfo, ok := <-URLchan
				if !ok {
					return nil
				}
				err := downloader.save(dlInfo.URL, dlInfo.FileURI, dlInfo.Headers)
				if err != nil {
					return err
				}
			}
		})
	}

	// get page numbers if -p is supplied to name files correctly
	pageNumbers := utils.NeedDownloadList(lenOfUrls)

	var fileURIs []string
	var fileURI string
	for idx, URL := range downloader.stream.URLs {
		if appendEnum {
			if config.Merge == config.MergeOptCBZ {
				fileURI = fmt.Sprint(pageNumbers[idx])
			} else {
				fileURI = fmt.Sprintf("%s_%d", downloader.filename, pageNumbers[idx])
			}
		} else {
			fileURI = downloader.filename
		}

		// build final file URI
		fileURI = filepath.Join(downloader.filePath, fileURI+"."+URL.Ext)
		if streamNeedsMerge {
			fileURI = filepath.Join(downloader.tmpFilePath, fmt.Sprintf("%d.%s", pageNumbers[idx], URL.Ext))
		}
		fileURIs = append(fileURIs, fileURI)

		URLchan <- downloadInfo{*URL, fileURI, headers}
	}
	close(URLchan)
	if err := errs.Wait(); err != nil {
		return nil, err
	}

	if streamNeedsMerge {
		// build final file URI
		fileURI = filepath.Join(downloader.filePath, downloader.filename+"."+downloader.stream.Ext)

		tmpFiles := []*merger.MergeFile{}
		for i, u := range downloader.stream.URLs {
			tmpFiles = append(tmpFiles, &merger.MergeFile{Path: filepath.Join(downloader.tmpFilePath, fmt.Sprintf("%d.%s", i, u.Ext)), DataType: downloader.stream.Type})
		}

		err := merger.NewFragmentMerger(downloader.stream.Key, downloader.bar).Merge(tmpFiles, fileURI)
		if err != nil {
			return nil, err
		}

		err = os.RemoveAll(downloader.tmpFilePath)
		if err != nil {
			return nil, errors.WithStack(err)
		}

		return []string{fileURI}, nil
	}

	return fileURIs, nil
}

func (downloader *downloaderStruct) save(URL static.URL, fileURI string, headers map[string]string) error {

	openOpts := os.O_RDWR | os.O_CREATE
	if config.Truncate {
		openOpts |= os.O_TRUNC
	}

	file, err := os.OpenFile(fileURI, openOpts, 0644)
	if err != nil {
		return err
	}

	stat, err := file.Stat()
	if err != nil {
		return err
	}

	if stat.Size() > 0 {
		if !config.Quiet {
			log.Printf(`file "%s" already exists and will be skipped`, fileURI)
		}
		return nil
	}

	// if stream size bigger than 10MB then use concurWrite
	if downloader.stream.Size > 10_000_000 && config.Workers > 1 && downloader.stream.Ext == "" {
		return downloader.concurWriteFile(URL.URL, file, headers)
	}

	if err = downloader.writeFile(URL.URL, file, headers); err != nil {
		file.Close()
		os.Remove(fileURI)
		return err
	}

	file.Close()
	return nil
}

// concurWriteFile splits a single-blob download into N parallel Range requests
// and writes each range directly to its byte offset with file.WriteAt (safe
// for concurrent non-overlapping writes on both Linux and Android).
//
// Rewritten in v1.2:
//   - Adaptive chunk size (~fileSize / (2*workers), clamped to 1–8 MB). Small
//     chunks == more parallel connections in flight, more chances to bypass
//     per-connection CDN throttling.
//   - Aborts if the server returns 200 to a Range request instead of 206.
//     Previously the code would then treat the FULL body as if it were the
//     requested range and WriteAt at the range's offset — silently corrupting
//     the file with N overlapping full-file copies.
//   - Per-chunk retry with linear backoff (up to 3 retries per chunk) so a
//     single stalled connection doesn't kill the whole download.
//   - Error propagation via atomic first-error — no shared mutex, no missed
//     early exit when one worker fails.
//   - Progress counter is now called with per-chunk bytes as they arrive, so
//     the mobile UI actually reflects concurrent progress.
func (downloader *downloaderStruct) concurWriteFile(URL string, file *os.File, headers map[string]string) error {
	// Probe the server for the AUTHORITATIVE file size before chunking.
	// stream.Size from the extractor can be wrong (e.g. HLS-fragment total
	// vs single-file endpoint size — oppai.stream returns 416 partway
	// through if we trust the extractor's inflated number). A one-byte
	// Range request is basically free and gives us the real Content-Range
	// total, so we always use that when it's available.
	realSize, sizeErr := probeContentSize(URL, headers, downloader.client)
	var fileSize int64
	switch {
	case sizeErr == nil && realSize > 0:
		fileSize = realSize
	case downloader.stream.Size > 0:
		fileSize = downloader.stream.Size
	default:
		// Unknown size and probe failed => can't chunk. Fall back to the
		// streaming writer.
		return downloader.writeFile(URL, file, headers)
	}

	workers := config.Workers
	if workers < 1 {
		workers = 1
	}
	pieceSize := fileSize / int64(workers*2)
	switch {
	case pieceSize < 1_000_000:
		pieceSize = 1_000_000 // 1 MB floor keeps HTTP overhead in check
	case pieceSize > 8_000_000:
		pieceSize = 8_000_000 // 8 MB ceiling caps memory + per-chunk time
	}

	// Enumerate pieces up front.
	type piece struct{ offset, endIncl int64 }
	pieces := make([]piece, 0, (fileSize/pieceSize)+1)
	for off := int64(0); off < fileSize; off += pieceSize {
		end := off + pieceSize - 1
		if end >= fileSize {
			end = fileSize - 1
		}
		pieces = append(pieces, piece{off, end})
	}

	downloader.progressBar = utils.InitPB(utils.ProgressBarConfig{
		Length:      fileSize,
		Description: fmt.Sprintf("Downloading %s using %d workers (%d chunks × %s)...", file.Name(), workers, len(pieces), humanChunk(pieceSize)),
		AsBytes:     true,
	})

	ch := make(chan piece, len(pieces))
	for _, p := range pieces {
		ch <- p
	}
	close(ch)

	var firstErr atomic.Value // holds error, set exactly once
	var wg sync.WaitGroup
	for i := 0; i < workers; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for p := range ch {
				if v := firstErr.Load(); v != nil {
					return // another worker already failed; abort cleanly
				}
				if err := downloader.downloadRange(URL, file, headers, p.offset, p.endIncl, 3); err != nil {
					firstErr.CompareAndSwap(nil, err)
					return
				}
			}
		}()
	}
	wg.Wait()

	if v := firstErr.Load(); v != nil {
		if err, _ := v.(error); err != nil {
			return err
		}
	}
	return nil
}

// downloadRange fetches [offset,endIncl] into `file` at position `offset`,
// retrying up to `retries` additional attempts on transient failure.
// Retries use linear backoff (500ms, 1s, 1.5s). Returns nil on success.
func (downloader *downloaderStruct) downloadRange(URL string, file *os.File, headers map[string]string, offset, endIncl int64, retries int) error {
	var lastErr error
	expected := endIncl - offset + 1

	for attempt := 0; attempt <= retries; attempt++ {
		if attempt > 0 {
			time.Sleep(time.Duration(attempt) * 500 * time.Millisecond)
		}

		req, err := http.NewRequest(http.MethodGet, URL, nil)
		if err != nil {
			lastErr = err
			continue
		}
		for k, v := range headers {
			req.Header.Set(k, v)
		}
		req.Header.Set("Range", fmt.Sprintf("bytes=%d-%d", offset, endIncl))

		res, err := downloader.client.Do(req)
		if err != nil {
			lastErr = err
			continue
		}

		// The server MUST honour Range with 206 Partial Content. A 200 means
		// it's sending the full body — we cannot safely stitch that into a
		// multi-chunk layout, so treat it as a fatal error for this chunk and
		// let the retry loop back off. If every retry returns 200, we surface
		// the error and the whole download fails cleanly.
		if res.StatusCode != http.StatusPartialContent {
			io.Copy(io.Discard, res.Body) // drain so the connection can be pooled
			res.Body.Close()
			lastErr = fmt.Errorf("range request for bytes %d-%d returned status %d", offset, endIncl, res.StatusCode)
			continue
		}

		buf := make([]byte, expected)
		n, err := io.ReadFull(res.Body, buf)
		res.Body.Close()
		if err != nil && err != io.ErrUnexpectedEOF {
			lastErr = err
			continue
		}
		if int64(n) != expected {
			lastErr = fmt.Errorf("short read: got %d bytes, want %d", n, expected)
			continue
		}

		written, err := file.WriteAt(buf, offset)
		if err != nil {
			lastErr = err
			continue
		}
		if downloader.bar {
			downloader.progressBar.Add(written)
		}
		// Mobile progress hook.
		fireProgress(int64(written))
		return nil // success
	}
	return lastErr
}

// humanChunk renders a piece size like "2.0 MB" for the progress-bar label.
// Local helper — the util.humanBytes is int64→string for total bytes and
// includes B/KB; here we always want "N MB" grain.
func humanChunk(b int64) string {
	return fmt.Sprintf("%.1f MB", float64(b)/1_000_000.0)
}

// probeContentSize asks the server for byte 0 with a Range request and parses
// the total from the Content-Range header. Returns the size on success, an
// error on any failure. Cheap (one byte transferred) so it's fine to call
// before every chunked download.
//
// Content-Range format: "bytes 0-0/12345678" — the trailing number after
// the slash is the total resource size.
func probeContentSize(URL string, headers map[string]string, client *http.Client) (int64, error) {
	req, err := http.NewRequest(http.MethodGet, URL, nil)
	if err != nil {
		return 0, err
	}
	for k, v := range headers {
		req.Header.Set(k, v)
	}
	req.Header.Set("Range", "bytes=0-0")

	res, err := client.Do(req)
	if err != nil {
		return 0, err
	}
	defer func() {
		io.Copy(io.Discard, res.Body) // drain so the connection can be pooled
		res.Body.Close()
	}()

	// If the server honoured Range, we get 206 and a Content-Range header.
	// Some servers still send full body with 200 — accept Content-Length in
	// that case as the total (single-request would have downloaded the whole
	// thing anyway).
	if cr := res.Header.Get("Content-Range"); cr != "" {
		parts := strings.SplitN(cr, "/", 2)
		if len(parts) == 2 && parts[1] != "*" {
			if n, err := strconv.ParseInt(strings.TrimSpace(parts[1]), 10, 64); err == nil && n > 0 {
				return n, nil
			}
		}
	}
	if res.StatusCode == http.StatusOK && res.ContentLength > 0 {
		return res.ContentLength, nil
	}
	return 0, fmt.Errorf("cannot determine size: status=%d, Content-Range=%q, Content-Length=%d",
		res.StatusCode, res.Header.Get("Content-Range"), res.ContentLength)
}

func (downloader *downloaderStruct) writeFile(URL string, file *os.File, headers map[string]string) error {
	// Supply http request with headers to ensure a higher possibility of success
	req, err := http.NewRequest(http.MethodGet, URL, nil)
	if err != nil {
		return err
	}

	for k, v := range headers {
		req.Header.Set(k, v)
	}

	res, err := downloader.client.Do(req)
	if err != nil {
		return err
	}
	if res.StatusCode != http.StatusOK {
		time.Sleep(1 * time.Second)
		res, err = downloader.client.Get(URL)
		if err != nil {
			return err
		}
		if res.StatusCode != http.StatusOK {
			res.Body.Close()
			return fmt.Errorf("downloading URL: '%s' returned status %d even after retrying", URL, res.StatusCode)
		}
	}
	defer res.Body.Close()

	var writer io.Writer
	writer = file
	// some sites do not return "content-type" or "content-length" in http header
	// it will render a spinner progressbar
	downloader.progressBar = utils.InitPB(utils.ProgressBarConfig{
		Length:      res.ContentLength,
		Description: fmt.Sprintf("Downloading %s ...", file.Name()),
		AsBytes:     true,
	})
	if downloader.bar {
		writer = io.MultiWriter(file, downloader.progressBar)
	}
	// Mobile progress hook: mirror bytes to the mobile listener if one is installed.
	if v := progressCallback.Load(); v != nil {
		if cb, ok := v.(func(int64)); ok && cb != nil {
			writer = io.MultiWriter(writer, partProgress{})
		}
	}

	// Note that io.Copy reads 32kb(maximum) from input and writes them to output, then repeats.
	// So don't worry about memory.
	_, copyErr := io.Copy(writer, res.Body)
	if copyErr != nil && copyErr != io.EOF {
		return fmt.Errorf("file copy error: %s", copyErr)
	}
	return nil
}

// downloadAdditionalStreams needed for e.g. a video file to be complete. Downloads audio and captions if separate and requested
func (downloader *downloaderStruct) downloadAdditionalStreams(data *static.Data) ([]*merger.MergeFile, error) {
	// everything besides of video streams doesn't need the following logic to merge using FFmpeg
	if downloader.stream.Type != static.DataTypeVideo {
		return nil, nil
	}

	var files []*merger.MergeFile
	audioFileURIs, err := downloader.downloadExtraAudio(data)
	if err != nil {
		return nil, err
	}

	captionFileURI, err := downloader.downloadCaption(data)
	if err != nil {
		return nil, err
	}

	if len(audioFileURIs) > 0 {
		for _, a := range audioFileURIs {
			files = append(files, &merger.MergeFile{Path: a, DataType: static.DataTypeAudio})
		}
	}
	if captionFileURI != "" {
		files = append(files, &merger.MergeFile{Path: captionFileURI, DataType: static.DataTypeText})
	}

	return files, nil
}

func (downloader *downloaderStruct) downloadExtraAudio(data *static.Data) ([]string, error) {
	// if audio is in separate stream -> download it with the video stream.
	// normally audio is included in the video streams. With this only special cases where this is not
	// the case are handled.

	streamID := ""
	for k, v := range data.Streams {
		if v.Type != static.DataTypeAudio {
			continue
		}
		streamID = k
	}
	if streamID == "" {
		return nil, nil
	}
	selectStreamOld := config.SelectStream
	config.SelectStream = streamID

	fileURIs, err := downloader.downloadStream(data)
	if err != nil {
		return nil, err
	}
	config.SelectStream = selectStreamOld

	return fileURIs, nil
}

func (downloader *downloaderStruct) downloadCaption(data *static.Data) (string, error) {
	if len(data.Captions) <= config.Caption || config.Caption <= -1 {
		return "", nil
	}

	headers := config.FakeHeaders
	headers["Referer"] = data.URL

	fileURI := filepath.Join(downloader.filePath, fmt.Sprintf("%s_caption_%s.%s", downloader.filename, data.Captions[config.Caption].Language, data.Captions[config.Caption].URL.Ext))
	err := downloader.save(data.Captions[config.Caption].URL, fileURI, headers)
	if err != nil {
		return "", err
	}
	if data.Captions[config.Caption].URL.Ext == "vtt" {
		err = sanitizeVTT(fileURI)
		if err != nil {
			return "", err
		}
	}

	return fileURI, nil
}
