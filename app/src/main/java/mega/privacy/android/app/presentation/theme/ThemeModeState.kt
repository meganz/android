package mega.privacy.android.app.presentation.theme

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ThemeModeState - Monitors the theme mode preference and updates the ui accordingly
 *
 * @property getThemeMode use case to monitor theme mode preference
 * @property coroutineScope
 * @property ioDispatcher
 */
@Singleton
class ThemeModeState @Inject constructor(
    private val getThemeMode: GetThemeMode,
    @ApplicationScope private val coroutineScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Current theme mode preference
     */
    val themeMode = mutableStateOf(ThemeMode.System)

    /**
     * Initialise
     *
     */
    fun initialise() = coroutineScope.launch(ioDispatcher) {
        getThemeMode()
            .collect {
                Timber.d("Theme mode updated to $it")
                themeMode.value = it
                setThemeMode(it)
            }
    }

    private suspend fun setThemeMode(mode: ThemeMode) = withContext(Dispatchers.Main) {
        when (mode) {
            ThemeMode.Light -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            ThemeMode.Dark -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            ThemeMode.System -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                }
            }
        }
    }
}