package mega.privacy.android.app.presentation.transfers.preview

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.psa.PsaContainer
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.presentation.settings.model.StorageTargetPreference
import mega.privacy.android.app.presentation.transfers.preview.model.LoadingPreviewViewModel
import mega.privacy.android.app.presentation.transfers.preview.view.LoadingPreviewInfo
import mega.privacy.android.app.presentation.transfers.preview.view.loadingPreviewScreen
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import javax.inject.Inject

/**
 * Activity to show a loading preview of a file being downloaded. Only for preview purposes.
 */
@AndroidEntryPoint
class LoadingPreviewActivity : AppCompatActivity() {

    /**
     * Use case to monitor the theme mode
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    /**
     * Factory to create PasscodeCryptObject instances
     */
    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    /**
     * The centralized navigator in the :app module
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    private val viewModel by viewModels<LoadingPreviewViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val mode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            OriginalTheme(isDark = mode.isDarkMode()) {
                PasscodeContainer(
                    passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                    content = {
                        PsaContainer {
                            val navHostController = rememberNavController()
                            val transferPath =
                                intent?.getStringExtra(EXTRA_FILE_PATH)
                                    .takeUnless { it.isNullOrEmpty() }
                            val transferUniqueId =
                                intent?.getLongExtra(EXTRA_TRANSFER_UNIQUE_ID, -1)
                                    .takeUnless { it == -1L }
                            val transferTag =
                                intent.getIntExtra(EXTRA_TRANSFER_TAG, -1)
                                    .takeUnless { it == -1 }

                            NavHost(
                                navController = navHostController,
                                startDestination = LoadingPreviewInfo(
                                    transferPath = transferPath,
                                    transferUniqueId = transferUniqueId,
                                    transferTag = transferTag,
                                ),
                                modifier = Modifier.navigationBarsPadding(),
                            ) {
                                loadingPreviewScreen(
                                    onBackPress = { supportFinishAfterTransition() },
                                    navigateToStorageSettings = {
                                        megaNavigator.openSettings(
                                            this@LoadingPreviewActivity,
                                            StorageTargetPreference
                                        )
                                    },
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        intent.getIntExtra(EXTRA_TRANSFER_TAG, -1).takeIf { it != -1 }?.let { tag ->
            viewModel.onNewIntent(tag)
        }
    }

    companion object {
        /**
         * Intent extra for transfer unique ID
         */
        const val EXTRA_TRANSFER_UNIQUE_ID = "TRANSFER_UNIQUE_ID"

        /**
         * Intent extra for file path
         */
        const val EXTRA_FILE_PATH = "FILE_PATH"

        /**
         * Intent extra for error
         */
        const val EXTRA_ERROR = "ERROR"

        /**
         * Intent extra for transfer tag
         */
        const val EXTRA_TRANSFER_TAG = "TRANSFER_TAG"
    }
}