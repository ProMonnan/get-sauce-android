package app.sahal.getsauce.ui.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sahal.getsauce.app.App
import app.sahal.getsauce.bridge.ExtractedData
import app.sahal.getsauce.data.db.DownloadJob
import app.sahal.getsauce.data.db.JobStatus
import app.sahal.getsauce.download.DownloadService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InfoUiState(
    val loading: Boolean = true,
    val results: List<ExtractedData> = emptyList(),
    /** Map from data.sourceUrl → selected stream ID. */
    val selectedStreamId: Map<String, String> = emptyMap(),
    val error: String? = null,
    val snack: String? = null,
)

class InfoViewModel(private val app: App, private val url: String) : ViewModel() {

    private val _state = MutableStateFlow(InfoUiState())
    val state: StateFlow<InfoUiState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val snap = app.container.settings.flow.first()
            // Apply user settings that affect extraction (headers/proxy — extraction uses HTTP too).
            app.container.bridge.setUserHeaders(snap.userHeaders)
            app.container.bridge.setProxy(snap.proxy)
            app.container.bridge.setTimeoutMinutes(snap.timeoutMinutes)

            runCatching { app.container.bridge.extract(url) }
                .onSuccess { list ->
                    // Default-select the first stream for each result so a user can just tap Download.
                    val sel = list.associate { d -> d.sourceUrl to (d.streams.firstOrNull()?.id ?: "0") }
                    _state.update { InfoUiState(loading = false, results = list, selectedStreamId = sel) }
                }
                .onFailure { t ->
                    _state.update { it.copy(loading = false, error = t.message ?: t::class.java.simpleName) }
                }
        }
    }

    fun selectStream(dataSourceUrl: String, streamId: String) {
        _state.update { it.copy(selectedStreamId = it.selectedStreamId + (dataSourceUrl to streamId)) }
    }

    fun enqueue(data: ExtractedData) {
        viewModelScope.launch {
            val snap = app.container.settings.flow.first()
            if (snap.outputTreeUri.isNullOrBlank()) {
                _state.update { it.copy(snack = "Set an output folder in Settings first.") }
                return@launch
            }
            val streamId = _state.value.selectedStreamId[data.sourceUrl] ?: "0"
            val stream = data.streams.firstOrNull { it.id == streamId } ?: data.streams.first()

            val staging = java.io.File(app.cacheDir, "dl-pending").apply { mkdirs() }
            val job = DownloadJob(
                sourceUrl = data.sourceUrl,
                title = data.title,
                site = data.site,
                payload = data.payload,
                streamId = streamId,
                streamLabel = "${stream.quality} ${stream.info}".trim(),
                estimatedBytes = stream.size,
                stagingDir = staging.absolutePath,
                destTreeUri = snap.outputTreeUri,
                status = JobStatus.QUEUED,
                updatedAt = System.currentTimeMillis(),
            )
            app.container.repo.enqueue(job)
            DownloadService.kick(app)
            _state.update { it.copy(snack = "Queued: ${data.title}") }
        }
    }

    fun clearSnack() { _state.update { it.copy(snack = null) } }
}
