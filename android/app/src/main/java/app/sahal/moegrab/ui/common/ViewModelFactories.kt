package app.sahal.moegrab.ui.common

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import app.sahal.moegrab.app.App

/**
 * Kotlin-only DI: every ViewModel we ship in this app takes ((application as App).container)
 * and its own args in the constructor. rememberVm bridges Compose's viewModel()
 * to that constructor. Not fancy, but keeps startup snappy.
 */
@Composable
inline fun <reified VM : ViewModel> rememberVm(
    key: String? = null,
    crossinline factory: (App) -> VM,
): VM {
    val app = LocalContext.current.applicationContext as App
    return viewModel(
        key = key,
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return factory(app) as T
            }
        },
    )
}
