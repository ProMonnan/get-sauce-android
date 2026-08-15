package app.sahal.getsauce.merge

import android.util.Log
import app.sahal.getsauce.bridge.DownloadResult
import java.io.File

/**
 * Phase-1 stub. ffmpeg-kit was sunset in April 2025 and its artifacts pulled
 * from Maven Central. Until we swap in a maintained fork, we skip the final
 * mux step entirely: the user gets the main video file, and any separate
 * audio/caption tracks are left as sidecar files next to it.
 */
class FfmpegMerger(private val stagingDir: File) {
    fun mergeIfNeeded(res: DownloadResult, onLog: (String) -> Unit = {}): File? {
        if (res.needsFinalMerge) {
            Log.w(TAG, "final mux skipped (ffmpeg-kit unavailable). Sidecars: ${res.additional.map { it.path }}")
            onLog("Skipping mux — ${res.additional.size} sidecar track(s) left alongside the video.")
        }
        return res.mainFiles.firstOrNull()?.let(::File)
    }
    companion object { private const val TAG = "FfmpegMerger" }
}
