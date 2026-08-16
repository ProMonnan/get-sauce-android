package app.sahal.moegrab.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import app.sahal.moegrab.R
import app.sahal.moegrab.bridge.ExtractorBridge
import app.sahal.moegrab.data.db.AppDb
import app.sahal.moegrab.data.prefs.SettingsStore
import app.sahal.moegrab.data.repo.DownloadRepository

/**
 * Manual DI container — Hilt/Koin are overkill for a project this size.
 *
 * All singletons live here and are reached via ((application as App).container).
 * ViewModels get the container passed in through their factories.
 */
class App : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.build(this)
        createNotifChannel()
    }

    private fun createNotifChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(
            CHANNEL_DOWNLOADS,
            getString(R.string.notif_channel_downloads),
            NotificationManager.IMPORTANCE_LOW,
        )
        mgr.createNotificationChannel(ch)
    }

    companion object {
        const val CHANNEL_DOWNLOADS = "downloads"
    }
}

/** Everything long-lived. Constructed exactly once in [App.onCreate]. */
class AppContainer(
    val bridge: ExtractorBridge,
    val db: AppDb,
    val settings: SettingsStore,
    val repo: DownloadRepository,
) {
    companion object {
        fun build(app: Application): AppContainer {
            val bridge = ExtractorBridge(app)
            val db = AppDb.get(app)
            val settings = SettingsStore(app)
            val repo = DownloadRepository(db.downloadDao(), db.historyDao())
            return AppContainer(bridge, db, settings, repo)
        }
    }
}
