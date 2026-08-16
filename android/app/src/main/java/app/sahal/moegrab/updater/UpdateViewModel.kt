package app.sahal.moegrab.updater

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sahal.moegrab.app.App
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State machine for the updater flow:
 *
 *   IDLE           → after app launch, before any check ran
 *   CHECKING       → hitting GitHub /releases/latest
 *   UP_TO_DATE     → checked; no newer release. Kept briefly for manual-check UX.
 *   AVAILABLE      → newer release; prompt is shown
 *   DOWNLOADING    → user tapped update; APK streaming into cache
 *   READY_TO_INSTALL → APK on disk; installer intent is about to fire
 *   FAILED         → network/parse/install error; message in state.error
 *
 * A single flow drives the bottom sheet + settings row + snackbar.
 */
class UpdateViewModel(app: App) : ViewModel() {

    private val checker = UpdateChecker(currentVersion = versionOf(app))
    private val installer = UpdateInstaller(app)

    private val _state = MutableStateFlow(UpdateState())
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /**
     * Kick off a background check. If [showUpToDate] is true (manual "Check
     * for updates" tap), we surface an UP_TO_DATE state so the user gets
     * feedback. Silent auto-checks pass false so quiet cases stay quiet.
     */
    fun checkForUpdates(showUpToDate: Boolean = false) {
        if (_state.value.phase == Phase.CHECKING || _state.value.phase == Phase.DOWNLOADING) return
        _state.value = _state.value.copy(phase = Phase.CHECKING, error = null)
        viewModelScope.launch {
            val info = runCatching { checker.check() }
                .onFailure { Log.w(TAG, "check failed", it) }
                .getOrNull()
            _state.value = when {
                info != null -> _state.value.copy(phase = Phase.AVAILABLE, info = info)
                showUpToDate -> _state.value.copy(phase = Phase.UP_TO_DATE)
                else         -> _state.value.copy(phase = Phase.IDLE)
            }
        }
    }

    /** User tapped "Later" in the prompt — dismiss without downloading. */
    fun dismissPrompt() {
        _state.value = _state.value.copy(phase = Phase.IDLE)
    }

    /** User tapped "Update now" — begin download, then fire installer intent. */
    fun downloadAndInstall() {
        val info = _state.value.info ?: return
        if (_state.value.phase == Phase.DOWNLOADING) return
        _state.value = _state.value.copy(phase = Phase.DOWNLOADING, bytesDone = 0, bytesTotal = info.sizeBytes)
        viewModelScope.launch {
            try {
                val apk = installer.download(info) { done, total ->
                    _state.value = _state.value.copy(bytesDone = done, bytesTotal = total)
                }
                _state.value = _state.value.copy(phase = Phase.READY_TO_INSTALL)
                installer.triggerInstall(apk)
                // Leave the state at READY_TO_INSTALL — the user is now in the
                // system installer UI. When they come back to the app it'll
                // reset on next check.
            } catch (t: Throwable) {
                Log.w(TAG, "update download failed", t)
                _state.value = _state.value.copy(
                    phase = Phase.FAILED,
                    error = t.message ?: "Download failed",
                )
            }
        }
    }

    /** Clear FAILED / UP_TO_DATE terminal states so the sheet closes. */
    fun clearTerminal() {
        val cur = _state.value.phase
        if (cur == Phase.FAILED || cur == Phase.UP_TO_DATE) {
            _state.value = _state.value.copy(phase = Phase.IDLE, error = null)
        }
    }

    companion object {
        private const val TAG = "UpdateVM"

        private fun versionOf(app: App): String = try {
            val pm = app.packageManager
            val name = pm.getPackageInfo(app.packageName, 0).versionName ?: "0.0.0"
            name
        } catch (_: Throwable) { "0.0.0" }
    }
}

enum class Phase { IDLE, CHECKING, UP_TO_DATE, AVAILABLE, DOWNLOADING, READY_TO_INSTALL, FAILED }

data class UpdateState(
    val phase: Phase = Phase.IDLE,
    val info: UpdateInfo? = null,
    val bytesDone: Long = 0,
    val bytesTotal: Long = 0,
    val error: String? = null,
)
