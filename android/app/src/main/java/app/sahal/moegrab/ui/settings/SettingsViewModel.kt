package app.sahal.moegrab.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sahal.moegrab.app.App
import app.sahal.moegrab.data.prefs.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val app: App) : ViewModel() {

    private val store: SettingsStore = app.container.settings

    val snapshot = store.flow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsStore.Snapshot(),
    )

    fun setOutputTree(uri: String?) = viewModelScope.launch { store.setOutputTree(uri) }
    fun setWorkers(n: Int) = viewModelScope.launch { store.setWorkers(n) }
    fun setTimeout(n: Int) = viewModelScope.launch { store.setTimeout(n) }
    fun setProxy(v: String) = viewModelScope.launch { store.setProxy(v) }
    fun setUserHeaders(v: String) = viewModelScope.launch { store.setUserHeaders(v) }
    fun setTruncate(v: Boolean) = viewModelScope.launch { store.setTruncate(v) }
}
