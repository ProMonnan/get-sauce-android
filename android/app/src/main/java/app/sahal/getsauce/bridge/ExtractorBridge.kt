package app.sahal.getsauce.bridge

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Kotlin-facing wrapper around the gomobile-generated `mobile.Mobile` class.
 *
 * The Go bridge lives in ../../../../../../../mobile/mobile.go and is compiled
 * to an .aar by scripts/build-aar.sh (or CI). Every method here is safe to
 * call from a coroutine; heavy calls (extract, download) are dispatched on
 * [Dispatchers.IO].
 */
class ExtractorBridge(context: Context) {

    private val cacheDir: String = context.cacheDir.absolutePath

    init {
        // Init is idempotent (protected by sync.Once on the Go side).
        // `init` is a soft keyword in Kotlin so we backtick the Java call.
        try {
            mobile.Mobile.`init`(cacheDir)
        } catch (t: Throwable) {
            Log.e(TAG, "mobile.Mobile.init failed", t)
            throw t
        }
    }

    val version: String get() = mobile.Mobile.version()

    // Gomobile maps Go `int` → Java `long` because Go's int is 64-bit on 64-bit
    // Android. We take Int in the Kotlin API for ergonomics and widen here.
    fun setWorkers(n: Int) = mobile.Mobile.setWorkers(n.toLong())
    fun setTimeoutMinutes(n: Int) = mobile.Mobile.setTimeoutMinutes(n.toLong())
    fun setUserHeaders(headers: String) = mobile.Mobile.setUserHeaders(headers)
    fun setProxy(url: String) = mobile.Mobile.setProxy(url)
    fun setTruncate(v: Boolean) = mobile.Mobile.setTruncate(v)
    fun setOutputPath(p: String) = mobile.Mobile.setOutputPath(p)
    fun cancel() = mobile.Mobile.cancel()

    /**
     * Runs the appropriate site extractor. Suspending; safe to call from any
     * dispatcher — the actual HTTP work happens on IO.
     */
    suspend fun extract(url: String): List<ExtractedData> = withContext(Dispatchers.IO) {
        val json = mobile.Mobile.extractInfo(url)
        parseJson.decodeFromString<List<ExtractedData>>(json)
    }

    /**
     * Downloads the parts for [streamId] under [outputDir]. Blocks the coroutine
     * for the duration of the download; wire cancellation through [cancel] +
     * listener.isCancelled if you need mid-flight abort.
     */
    suspend fun download(
        payloadJson: String,
        streamId: String,
        outputDir: String,
        listener: BridgeProgressListener,
    ): DownloadResult = withContext(Dispatchers.IO) {
        val json = mobile.Mobile.downloadStreamParts(payloadJson, streamId, outputDir, listener)
        parseJson.decodeFromString(json)
    }

    /** Static supported-sites list, sync-only, cheap. */
    val supportedSites: List<String>
        get() = parseJson.decodeFromString(mobile.Mobile.supportedSites())

    companion object {
        private const val TAG = "ExtractorBridge"
        internal val parseJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }
}

/**
 * Kotlin implementation of the gomobile-generated `mobile.ProgressListener`
 * Java interface. The Go side calls these methods from a background goroutine
 * — bounce to the main thread yourself if you want to touch the UI.
 */
open class BridgeProgressListener(
    private val onBytesDelta: (Long) -> Unit = {},
    private val onLog: (String) -> Unit = {},
) : mobile.ProgressListener {
    override fun onBytes(delta: Long) = onBytesDelta(delta)
    override fun onLog(msg: String) = onLog.invoke(msg)
}
