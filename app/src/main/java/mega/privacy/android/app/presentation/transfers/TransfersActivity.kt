package mega.privacy.android.app.presentation.transfers

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.components.chatsession.ChatSessionContainer
import mega.privacy.android.app.components.session.SessionContainer
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.psa.PsaContainer
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.presentation.settings.model.StorageTargetPreference
import mega.privacy.android.app.presentation.transfers.view.ACTIVE_TAB_INDEX
import mega.privacy.android.app.presentation.transfers.view.navigation.TransfersInfo
import mega.privacy.android.app.presentation.transfers.view.navigation.transfersScreen
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import javax.inject.Inject

/**
 * Extra for the tab to show in the transfers screen.
 */
const val EXTRA_TAB = "TAB"

/**
 * Activity to show transfers.
 */

@AndroidEntryPoint
class TransfersActivity : AppCompatActivity() {

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    @Inject
    lateinit var megaNavigator: MegaNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val mode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            SessionContainer {
                ChatSessionContainer {
                    OriginalTheme(isDark = mode.isDarkMode()) {
                        PasscodeContainer(
                            passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                            content = {
                                PsaContainer {
                                    val tabIndex = intent?.getIntExtra(EXTRA_TAB, ACTIVE_TAB_INDEX)
                                        ?: ACTIVE_TAB_INDEX

                                    NavHost(
                                        navController = rememberNavController(),
                                        startDestination = TransfersInfo(tabIndex = tabIndex),
                                        modifier = Modifier.navigationBarsPadding()
                                    ) {
                                        transfersScreen(
                                            onBackPress = { supportFinishAfterTransition() },
                                            onNavigateToStorageSettings = {
                                                megaNavigator.openSettings(
                                                    this@TransfersActivity,
                                                    StorageTargetPreference
                                                )
                                            },
                                            onNavigateToUpgradeAccount = {
                                                megaNavigator.openUpgradeAccount(this@TransfersActivity)
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}