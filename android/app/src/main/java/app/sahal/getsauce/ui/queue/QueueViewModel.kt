package app.sahal.getsauce.ui.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sahal.getsauce.app.App
import app.sahal.getsauce.data.db.JobStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QueueViewModel(private val app: App) : ViewModel() {

    val jobs = app.container.repo.observeJobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cancel(id: Long) {
        viewModelScope.launch {
            val job = app.container.repo.byId(id) ?: return@launch
            // If it's the one actively running, signal Go to abort. Otherwise
            // flip status so the pump skips it.
            app.container.bridge.cancel()
            app.container.repo.setStatus(id, JobStatus.CANCELLED, err = "Cancelled by user")
            // Cleanup any leftover staging files best-effort.
            runCatching { java.io.File(job.stagingDir).deleteRecursively() }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { app.container.repo.delete(id) }
    }
}
