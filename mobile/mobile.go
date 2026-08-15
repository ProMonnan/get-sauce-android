// Package mobile is the gomobile-friendly entry point for the Android app.
//
// Everything exported here has a signature gomobile can bind: basic types,
// strings, byte slices, or interfaces defined in this package. Complex domain
// types cross the boundary as JSON strings so the Kotlin side has one thing
// to parse and we avoid gomobile's struct-binding sharp edges.
//
// The Kotlin caller sequence is:
//
//	Mobile.init("/data/data/app.sahal.getsauce/cache")
//	Mobile.setWorkers(4)
//	val json = Mobile.extractInfo("https://...")
//	val result = Mobile.downloadStreamParts(entryJson, "0", "/storage/.../Sauce", listener)
//	// then ffmpeg-kit merges MainFiles[0] with Additional[]
package mobile

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"sync"

	"github.com/gan-of-culture/get-sauce/config"
	"github.com/gan-of-culture/get-sauce/downloader"
	"github.com/gan-of-culture/get-sauce/extractors"
	"github.com/gan-of-culture/get-sauce/static"
)

// Version reported to the Android side. Overridden at build time via
// -ldflags "-X github.com/gan-of-culture/get-sauce/mobile.version=..." if wanted.
var version = "dev"

// Version returns the bridge version string.
func Version() string { return version }

// initOnce guards Init.
var initOnce sync.Once

// Init prepares runtime state. Call once from Application.onCreate().
// cacheDir is a writable directory the bridge can use for temporary files
// (typically Context.cacheDir on Android).
func Init(cacheDir string) error {
	var initErr error
	initOnce.Do(func() {
		if cacheDir == "" {
			initErr = fmt.Errorf("cacheDir must not be empty")
			return
		}
		if err := os.MkdirAll(cacheDir, 0o755); err != nil {
			initErr = err
			return
		}
		// TMPDIR is used by os.TempDir(); a few sites' extractors write cookie
		// caches under it.
		if err := os.Setenv("TMPDIR", cacheDir); err != nil {
			initErr = err
			return
		}
		// Sensible defaults, all overridable via the setters below.
		config.Workers = 4
		config.Timeout = 15
		config.SelectStream = "0"
		config.Merge = config.MergeOptDefault
	})
	return initErr
}

// SetWorkers overrides the concurrent download worker count.
func SetWorkers(n int) {
	if n < 1 {
		n = 1
	}
	config.Workers = n
}

// SetTimeoutMinutes overrides the HTTP client timeout (in minutes).
func SetTimeoutMinutes(n int) {
	if n < 1 {
		n = 1
	}
	config.Timeout = n
}

// SetUserHeaders installs custom headers (same format as the -h CLI flag:
// "Key1: value1\nKey2: value2"). Used to bypass Cloudflare / DDOS-Guard by
// pasting a real browser's cookie + user-agent.
func SetUserHeaders(headers string) {
	config.UserHeaders = headers
}

// SetTruncate mirrors the CLI -t flag: overwrite existing files instead of skipping.
func SetTruncate(t bool) { config.Truncate = t }

// SetOutputPath sets the base output directory. On Android this will typically
// be a per-app scratch directory; the Kotlin side then copies finished files
// into the user-chosen SAF tree.
func SetOutputPath(p string) { config.OutputPath = p }

// SetProxy sets the HTTP(S)_PROXY environment variables used by the default
// http.Client. Pass an empty string to clear.
func SetProxy(p string) {
	if p == "" {
		os.Unsetenv("HTTP_PROXY")
		os.Unsetenv("HTTPS_PROXY")
		os.Unsetenv("http_proxy")
		os.Unsetenv("https_proxy")
		return
	}
	os.Setenv("HTTP_PROXY", p)
	os.Setenv("HTTPS_PROXY", p)
	os.Setenv("http_proxy", p)
	os.Setenv("https_proxy", p)
}

// jsonStream is a JSON-safe projection of static.Stream — we strip the raw
// []*URL slice (kept internally) and expose only what the UI needs.
type jsonStream struct {
	ID       string `json:"id"`
	Type     string `json:"type"`
	Quality  string `json:"quality"`
	Info     string `json:"info"`
	Parts    int    `json:"parts"`
	Size     int64  `json:"size"`
	Ext      string `json:"ext"`
	Bitrate  string `json:"bitrate,omitempty"`
	Language string `json:"language,omitempty"`
}

type jsonCaption struct {
	Language string `json:"language"`
	Ext      string `json:"ext"`
	URL      string `json:"url"`
}

type jsonData struct {
	Site     string        `json:"site"`
	Title    string        `json:"title"`
	Type     string        `json:"type"`
	URL      string        `json:"sourceUrl"`
	Streams  []jsonStream  `json:"streams"`
	Captions []jsonCaption `json:"captions,omitempty"`
	// Opaque token the Android side gives back to downloadStreamParts. It is the
	// full static.Data as JSON so we can rehydrate without re-extracting.
	Payload string `json:"payload"`
}

// ExtractInfo runs the appropriate extractor for url and returns the resulting
// entries as JSON. Extractors that return more than one entry (playlists,
// category pages) show up as multiple items in the returned array.
func ExtractInfo(url string) (string, error) {
	if strings.TrimSpace(url) == "" {
		return "", fmt.Errorf("empty url")
	}
	items, err := extractors.Extract(url)
	if err != nil {
		return "", err
	}

	out := make([]jsonData, 0, len(items))
	for _, d := range items {
		if d == nil {
			continue
		}
		streams := make([]jsonStream, 0, len(d.Streams))
		for id, s := range d.Streams {
			if s == nil {
				continue
			}
			streams = append(streams, jsonStream{
				ID:      id,
				Type:    string(s.Type),
				Quality: s.Quality,
				Info:    s.Info,
				Parts:   len(s.URLs),
				Size:    s.Size,
				Ext:     s.Ext,
			})
		}
		// Sort so the UI has a stable order (best quality first — extractors
		// generally emit "0" as the top pick, so numeric-ish ID sort works).
		sort.Slice(streams, func(i, j int) bool { return streams[i].ID < streams[j].ID })

		caps := make([]jsonCaption, 0, len(d.Captions))
		for _, c := range d.Captions {
			if c == nil {
				continue
			}
			caps = append(caps, jsonCaption{
				Language: c.Language,
				Ext:      c.URL.Ext,
				URL:      c.URL.URL,
			})
		}

		payload, err := json.Marshal(d)
		if err != nil {
			return "", err
		}

		out = append(out, jsonData{
			Site:     d.Site,
			Title:    d.Title,
			Type:     string(d.Type),
			URL:      d.URL,
			Streams:  streams,
			Captions: caps,
			Payload:  string(payload),
		})
	}

	b, err := json.Marshal(out)
	if err != nil {
		return "", err
	}
	return string(b), nil
}

// ProgressListener is implemented on the Kotlin side and passed into
// DownloadStreamParts. Gomobile turns this into a Java interface.
type ProgressListener interface {
	// OnBytes is called during file downloads with the number of bytes just
	// written (delta, not total). Called on Go's goroutine — implementations
	// should hop to the main thread themselves if needed.
	OnBytes(delta int64)
	// OnLog surfaces informational / warning messages ("skipping existing
	// file X", "retrying …") that the CLI would have printed.
	OnLog(msg string)
}

// DownloadStreamParts downloads the stream identified by streamID from the
// data payload obtained via ExtractInfo. All parts land under outputDir. The
// returned JSON matches the shape of downloader.MobileResult and tells the
// Kotlin side whether ffmpeg-kit muxing is still needed.
//
// This is a blocking call — Kotlin should run it on a background dispatcher.
// Cancellation: call Cancel() from any thread; between-part boundaries return
// an error, in-flight bodies finish first.
func DownloadStreamParts(payloadJSON string, streamID string, outputDir string, listener ProgressListener) (string, error) {
	if payloadJSON == "" {
		return "", fmt.Errorf("empty payload")
	}
	if streamID == "" {
		streamID = "0"
	}
	if outputDir == "" {
		return "", fmt.Errorf("empty outputDir")
	}
	if err := os.MkdirAll(outputDir, 0o755); err != nil {
		return "", err
	}

	var data static.Data
	if err := json.Unmarshal([]byte(payloadJSON), &data); err != nil {
		return "", fmt.Errorf("decode payload: %w", err)
	}
	if data.Streams == nil {
		return "", fmt.Errorf("payload has no streams")
	}
	if _, ok := data.Streams[streamID]; !ok {
		// Fall back to the first available stream ID if the requested one is missing.
		for k := range data.Streams {
			streamID = k
			break
		}
	}

	// Wire config for this call. These are process-global; concurrent downloads
	// on Android would clobber each other, so the Kotlin download service must
	// serialize calls to this function (which it does via a single-worker
	// dispatcher).
	config.SelectStream = streamID
	config.OutputPath = outputDir
	config.Subdirectory = false
	// We want fragment-concat but NOT the final ffmpeg data merge (Kotlin does that).
	config.Merge = config.MergeOptDefault

	// Install the progress callback for the duration of this call.
	downloader.SetCancelled(false)
	if listener != nil {
		downloader.SetProgressCallback(func(n int64) { listener.OnBytes(n) })
		defer downloader.SetProgressCallback(nil)
	}

	d := downloader.New(false)
	result, err := d.DownloadParts(&data)
	if err != nil {
		return "", err
	}

	// Rewrite absolute paths to be under outputDir for the Kotlin side (they
	// already are, but normalize).
	for i, p := range result.MainFiles {
		if !filepath.IsAbs(p) {
			result.MainFiles[i] = filepath.Join(outputDir, p)
		}
	}

	b, err := json.Marshal(result)
	if err != nil {
		return "", err
	}
	if listener != nil {
		listener.OnLog(fmt.Sprintf("Downloaded %d main file(s), %d extra track(s)", len(result.MainFiles), len(result.Additional)))
	}
	return string(b), nil
}

// Cancel signals an in-flight DownloadStreamParts to stop at the next
// between-part boundary.
func Cancel() { downloader.SetCancelled(true) }

// SupportedSites returns the JSON array of hostnames the extractors handle.
// The Kotlin side uses this to render the "supported sites" screen and to
// validate URLs before sending them into ExtractInfo.
func SupportedSites() string {
	// extractors.Extract's map is unexported; enumerate via a probe would be
	// fragile. The list below is kept in sync manually with extractors.init()
	// — see /extractors/extractors.go. When you add a site there, add it here.
	sites := []string{
		"asmhentai.com",
		"comicporn.xxx",
		"danbooru.donmai.us",
		"eahentai.com",
		"haho.moe",
		"hanime.tv",
		"hentai-moon.com",
		"hentai2read.com",
		"hentai2w.com",
		"hentaicloud.com",
		"hentaiera.com",
		"hentaienvy.com",
		"hentai-foundry.com",
		"hentaifox.com",
		"hentaimama.io",
		"hentainexus.com",
		"hentaiplay.net",
		"hentaipulse.com",
		"hentairox.com",
		"hentaivideos.net",
		"hentaiworld.tv",
		"hentaizap.com",
		"hitomi.la",
		"hstream.moe",
		"imhentai.xxx",
		"iwara.tv",
		"miohentai.com",
		"muchohentai.com",
		"nhentai.net",
		"ohentai.org",
		"oppai.stream",
		"rule34.paheal.net",
		"rule34video.com",
		"simply-hentai.com",
		"thehentaiworld.com",
	}
	sort.Strings(sites)
	b, _ := json.Marshal(sites)
	return string(b)
}
