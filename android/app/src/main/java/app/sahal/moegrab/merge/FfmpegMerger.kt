package app.sahal.moegrab.merge

import android.util.Log
import app.sahal.moegrab.bridge.AdditionalFile
import app.sahal.moegrab.bridge.DownloadResult
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File

/**
 * Wraps ffmpeg-kit for the final mux step. Downloader gives us:
 *   MainFiles[0] = video (possibly already fragment-concat'd)
 *   Additional[] = separate audio and/or caption tracks
 *
 * We mirror what the CLI's dataMerger does:
 *   -c copy               (no re-encode)
 *   -map 0:v -map N:a ... (pull video from index 0, audio from wherever)
 *   -c:s mov_text | webvtt depending on container
 */
class FfmpegMerger(private val stagingDir: File) {

    /**
     * @return absolute path of the merged output on success, or null on failure.
     *         The caller is responsible for copying it into the SAF destination.
     */
    fun mergeIfNeeded(res: DownloadResult, onLog: (String) -> Unit = {}): File? {
        if (!res.needsFinalMerge) {
            // Nothing to do — Go already produced usable file(s).
            return res.mainFiles.firstOrNull()?.let(::File)
        }
        if (res.mainFiles.isEmpty()) return null

        val outExt = res.finalExt.ifBlank { "mp4" }
        val output = File(stagingDir, "${sanitize(res.title)}.$outExt")

        val inputs = buildList {
            add(res.mainFiles[0])
            addAll(res.additional.map(AdditionalFile::path))
        }
        val cmd = buildCommand(inputs, res.additional, output.absolutePath, outExt)
        onLog("ffmpeg $cmd")

        val session = FFmpegKit.execute(cmd)
        if (!ReturnCode.isSuccess(session.returnCode)) {
            Log.e(TAG, "ffmpeg failed rc=${session.returnCode} logs=${session.allLogsAsString}")
            onLog("ffmpeg failed: rc=${session.returnCode}")
            return null
        }

        // Best-effort cleanup of the intermediate parts.
        (inputs.map(::File)).forEach { runCatching { it.delete() } }
        return output
    }

    /** Space-separated ffmpeg CLI. ffmpeg-kit accepts a single string with the same parsing rules. */
    private fun buildCommand(
        inputs: List<String>,
        additional: List<AdditionalFile>,
        output: String,
        outExt: String,
    ): String {
        val parts = mutableListOf("-y")
        inputs.forEach {
            parts += "-i"
            parts += quote(it)
        }
        parts += "-map"; parts += "0:v"

        // Audio: if we have any separate audio streams, map from them.
        // Otherwise take audio from input 0 (in case it's baked in).
        val extraAudio = additional.withIndex()
            .filter { it.value.dataType == "audio" }
            .map { it.index + 1 } // +1 because mainFile is input 0
        if (extraAudio.isNotEmpty()) {
            extraAudio.forEach { parts += "-map"; parts += "$it:a" }
        } else {
            parts += "-map"; parts += "0:a?"   // "?" makes it optional
        }

        // Captions: map every extra caption stream. Adjust codec for container.
        val extraCap = additional.withIndex()
            .filter { it.value.dataType == "text" }
            .map { it.index + 1 }
        extraCap.forEach { parts += "-map"; parts += "$it:s" }

        parts += "-c"; parts += "copy"
        if (extraCap.isNotEmpty()) {
            when (outExt.lowercase()) {
                "mp4", "mov", "m4v" -> { parts += "-c:s"; parts += "mov_text" }
                else -> {
                    parts += "-c:s"; parts += "webvtt"
                    parts += "-disposition:s:0"; parts += "default"
                }
            }
        }
        parts += quote(output)
        return parts.joinToString(" ")
    }

    /** Cheap shell-safe quoting; ffmpeg-kit's tokenizer respects single quotes. */
    private fun quote(s: String): String =
        if (s.any { it.isWhitespace() || it == '\'' }) "'${s.replace("'", "'\\''")}'" else s

    private fun sanitize(t: String): String =
        t.replace(Regex("""[\\/:*?"<>|]"""), "_").take(120).ifBlank { "download" }

    companion object { private const val TAG = "FfmpegMerger" }
}
