package app.sahal.moegrab.updater

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches the latest release from GitHub and decides whether an in-app update
 * should be offered. Purposefully lightweight — no third-party HTTP client, no
 * OkHttp; a single JSON call over HttpsURLConnection with 10s timeouts.
 *
 * Configure the target repo via [repoSlug]. The default matches the fork
 * where releases live (`owner/repo` — no leading `https://`).
 */
class UpdateChecker(
    private val currentVersion: String,
    private val repoSlug: String = DEFAULT_REPO,
    private val abiPreference: List<String> = listOf("arm64-v8a", "universal"),
) {

    /**
     * Returns [UpdateInfo] iff a strictly-newer, non-prerelease, non-draft
     * release exists and it publishes an APK asset we know how to install.
     * Returns null on: same/older version, no APK asset, network/parse error,
     * or repo not found.
     */
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        val release = fetchLatestRelease() ?: return@withContext null
        if (release.draft || release.prerelease) return@withContext null

        val latest = release.tagName.removePrefix("v").trim()
        if (!isNewer(currentVersion, latest)) return@withContext null

        val asset = pickBestAsset(release.assets) ?: return@withContext null

        UpdateInfo(
            version = latest,
            releaseNotes = release.body,
            downloadUrl = asset.browserDownloadUrl,
            sizeBytes = asset.size,
            htmlUrl = release.htmlUrl,
        )
    }

    private fun fetchLatestRelease(): GitHubReleaseDto? {
        val url = URL("https://api.github.com/repos/$repoSlug/releases/latest")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "MoeGrab-Android/$currentVersion")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            instanceFollowRedirects = true
        }
        return try {
            if (conn.responseCode !in 200..299) {
                Log.w(TAG, "GitHub /releases/latest returned ${conn.responseCode}")
                return null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            JSON.decodeFromString(GitHubReleaseDto.serializer(), body)
        } catch (t: Throwable) {
            Log.w(TAG, "update check failed", t)
            null
        } finally {
            runCatching { conn.disconnect() }
        }
    }

    /**
     * Pick the APK asset that best matches this device.
     *   arm64-v8a > universal   (order controlled by [abiPreference])
     * We fall back to *any* .apk asset if nothing in the preference list hits,
     * so an unusual release naming still surfaces an install.
     */
    private fun pickBestAsset(assets: List<GitHubAssetDto>): GitHubAssetDto? {
        val apks = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        if (apks.isEmpty()) return null
        for (pref in abiPreference) {
            apks.firstOrNull { it.name.contains(pref, ignoreCase = true) }?.let { return it }
        }
        return apks.first()
    }

    companion object {
        private const val TAG = "UpdateChecker"

        /** Owner/repo pair to check. Kept as a const so it's easy to grep. */
        const val DEFAULT_REPO = "ProMonnan/get-sauce-android"

        private val JSON = Json {
            ignoreUnknownKeys = true    // GitHub adds new fields all the time
            isLenient = true
        }

        /**
         * Semver-ish comparison. Splits on '.' and compares part-by-part
         * numerically. Missing trailing parts are treated as 0
         * (`1.1` == `1.1.0`). Non-numeric parts (`1.1.0-rc1`) are ignored
         * from the point of a mismatch and treated as equal → prevents
         * downgrading users to a pre-release from a "latest" tag.
         *
         * Returns true iff `latest` is strictly greater than `current`.
         */
        fun isNewer(current: String, latest: String): Boolean {
            val cur = current.split('.').mapNotNull { it.takeWhile(Char::isDigit).toIntOrNull() }
            val lat = latest.split('.').mapNotNull { it.takeWhile(Char::isDigit).toIntOrNull() }
            val n = maxOf(cur.size, lat.size)
            for (i in 0 until n) {
                val c = cur.getOrElse(i) { 0 }
                val l = lat.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            return false
        }
    }
}
