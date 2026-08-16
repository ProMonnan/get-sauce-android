package app.sahal.moegrab.bridge

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Kotlin mirrors of the JSON emitted by the Go bridge. Keep these fields
 * in sync with mobile/mobile.go's jsonStream / jsonData / jsonCaption and
 * downloader/mobile.go's MobileResult / AdditionalFile.
 */
@Serializable
data class ExtractedStream(
    val id: String,
    val type: String,
    val quality: String = "",
    val info: String = "",
    val parts: Int = 0,
    val size: Long = 0L,
    val ext: String = "",
    val bitrate: String = "",
    val language: String = "",
)

@Serializable
data class ExtractedCaption(
    val language: String,
    val ext: String,
    val url: String,
)

@Serializable
data class ExtractedData(
    val site: String,
    val title: String,
    val type: String,
    @SerialName("sourceUrl") val sourceUrl: String,
    val streams: List<ExtractedStream> = emptyList(),
    val captions: List<ExtractedCaption> = emptyList(),
    /** Opaque token to pass back into [ExtractorBridge.download]. */
    val payload: String,
)

@Serializable
data class AdditionalFile(
    val path: String,
    @SerialName("dataType") val dataType: String,
    val language: String = "",
)

@Serializable
data class DownloadResult(
    val mainFiles: List<String>,
    val additional: List<AdditionalFile> = emptyList(),
    val needsFinalMerge: Boolean,
    val finalExt: String = "",
    val title: String,
    val mainType: String = "",
)
