package app.sahal.getsauce.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sahal.getsauce.app.App
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val app: App) : ViewModel() {
    val history = app.container.repo.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clear() {
        viewModelScope.launch { app.container.repo.clearHistory() }
    }
}
