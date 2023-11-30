package mega.privacy.android.app.presentation.achievements

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.listeners.GetAchievementsListener
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import timber.log.Timber
import javax.inject.Inject

/**
 * Achievements activity
 *
 */
@AndroidEntryPoint
class AchievementsFeatureActivity : PasscodeActivity() {
    /**
     * Get system's default theme mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel: AchievementsOverviewViewModel by viewModels()

    /**
     * fetcher
     */
    @Inject
    @Deprecated("This field will be removed in future")
    lateinit var fetcher: GetAchievementsListener

    /**
     * On create
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            return
        }
        setContent {
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            /**
             * AndroidTheme will be removed once the AchievementsActivity is removed in the future
             */
            MegaAppTheme(isDark = themeMode.isDarkMode()) {
                AchievementsFeatureScreen(viewModel)
            }
        }
    }

    /**
     * On options item selected
     *
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected")
        when (item.itemId) {
            android.R.id.home -> onBackPressedDispatcher.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Deprecated showing SnackBar method from ContactController
     */
    @Deprecated("This method is only used to support showing snackbar from ContactController, remove it once it's refactored")
    fun showSnackbar(@StringRes message: Int) {
        viewModel.showErrorMessage(message)
    }
}