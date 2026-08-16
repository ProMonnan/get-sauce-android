package app.sahal.moegrab.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Handles the tail end of the update flow: stream the APK from [UpdateInfo]
 * into a private cache subdir, hand it to the system installer via a
 * FileProvider content:// URI, and (if needed) surface the "unknown sources"
 * permission screen so the user can grant it once.
 *
 * All state lives inside the singleton — the caller only needs to invoke
 * [download] (suspending, with progress callback) then [triggerInstall].
 */
class UpdateInstaller(private val ctx: Context) {

    /**
     * Download [info]'s APK to `cacheDir/updates/<version>.apk`.
     * Emits progress via [onProgress]. Throws on network failure so the VM can
     * surface an error snackbar; deletes the partial file on failure.
     *
     * Returns the local File on success.
     */
    suspend fun download(
        info: UpdateInfo,
        onProgress: (bytesDone: Long, bytesTotal: Long) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
        // Clean any prior downloads before starting a new one — we only ever
        // keep the currently-fetching APK, not a history.
        dir.listFiles()?.forEach { runCatching { it.delete() } }

        val out = File(dir, "moegrab-${info.version}.apk")
        val conn = (URL(info.downloadUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "MoeGrab-Android/${info.version}")
        }
        try {
            if (conn.responseCode !in 200..299) {
                throw java.io.IOException("HTTP ${conn.responseCode} from ${info.downloadUrl}")
            }
            val total = if (conn.contentLengthLong > 0) conn.contentLengthLong else info.sizeBytes
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        done += n
                        onProgress(done, total)
                    }
                }
            }
            out
        } catch (t: Throwable) {
            runCatching { out.delete() }
            throw t
        } finally {
            runCatching { conn.disconnect() }
        }
    }

    /**
     * Hand [apk] to the system PackageInstaller. If the user hasn't yet
     * granted "install from unknown sources" for this app, first bounce them
     * into the settings screen where they can toggle it — otherwise
     * ACTION_VIEW just does nothing on API 26+.
     *
     * Returns true if an intent was fired (either the installer or the perm
     * screen). False only if we somehow can't launch anything (shouldn't
     * happen — the manifest declares the queries + permission).
     */
    fun triggerInstall(apk: File): Boolean {
        // API 26+ requires per-app permission to install from unknown sources.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!ctx.packageManager.canRequestPackageInstalls()) {
                val i = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${ctx.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return runCatching { ctx.startActivity(i); true }.getOrElse {
                    Log.w(TAG, "cannot open unknown-sources settings", it); false
                }
            }
        }
        val uri = try {
            FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.updater.fileprovider",
                apk,
            )
        } catch (t: Throwable) {
            Log.e(TAG, "FileProvider.getUriForFile failed for $apk", t)
            return false
        }
        val install = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { ctx.startActivity(install); true }.getOrElse {
            Log.e(TAG, "cannot launch installer intent", it); false
        }
    }

    companion object { private const val TAG = "UpdateInstaller" }
}
