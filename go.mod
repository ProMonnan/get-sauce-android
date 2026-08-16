module github.com/gan-of-culture/get-sauce

go 1.24.0

toolchain go1.24.7

require (
	github.com/andybalholm/brotli v1.2.0
	github.com/schollz/progressbar/v3 v3.19.0
	golang.org/x/sync v0.19.0
)

require (
	github.com/mattn/go-runewidth v0.0.20 // indirect
	github.com/mitchellh/colorstring v0.0.0-20190213212951-d06e56a500db // indirect
	github.com/pkg/errors v0.9.1
	github.com/rivo/uniseg v0.4.7 // indirect
	golang.org/x/sys v0.41.0 // indirect
	golang.org/x/term v0.40.0 // indirect
)

replace (
	golang.org/x/sync => github.com/golang/sync v0.19.0
	golang.org/x/sys => github.com/golang/sys v0.41.0
	golang.org/x/term => github.com/golang/term v0.40.0
)
