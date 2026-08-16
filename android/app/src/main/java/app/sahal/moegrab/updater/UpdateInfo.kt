package app.sahal.moegrab.updater

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Structured summary of a newer release available upstream, ready to display in
 * the update prompt. Populated by [UpdateChecker.check].
 */
data class UpdateInfo(
    val version: String,        // e.g. "1.1.0" (no leading v)
    val releaseNotes: String,   // markdown body from the GitHub release
    val downloadUrl: String,    // browser_download_url for the arm64 APK
    val sizeBytes: Long,        // asset size for progress display
    val htmlUrl: String,        // release page URL, for "view release" fallback
)

// ---- GitHub API payload (subset we care about) ----------------------------
// Kept deliberately narrow so the parse doesn't break if GitHub tacks on fields.

@Serializable
internal data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    @SerialName("name") val name: String? = null,
    @SerialName("body") val body: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("prerelease") val prerelease: Boolean = false,
    @SerialName("draft") val draft: Boolean = false,
    @SerialName("assets") val assets: List<GitHubAssetDto> = emptyList(),
)

@Serializable
internal data class GitHubAssetDto(
    @SerialName("name") val name: String,
    @SerialName("size") val size: Long,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
)
