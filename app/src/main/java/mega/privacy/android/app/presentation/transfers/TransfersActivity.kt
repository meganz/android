package mega.privacy.android.app.presentation.transfers

import android.content.Context
import android.content.Intent
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
import mega.privacy.android.app.presentation.transfers.view.ACTIVE_TAB_INDEX
import mega.privacy.android.app.presentation.transfers.view.COMPLETED_TAB_INDEX
import mega.privacy.android.app.presentation.transfers.view.navigation.transfersScreen
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.destination.TransfersNavKey
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

    /**
     * monitorThemeModeUseCase
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    /**
     * passcodeCryptObjectFactory
     */
    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    /**
     * megaNavigator
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    /**
     * On create
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val mode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            OriginalTheme(isDark = mode.isDarkMode()) {
                SessionContainer(optimistic = true) {
                    ChatSessionContainer(optimistic = true) {
                        PasscodeContainer(
                            passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                            content = {
                                PsaContainer {
                                    val tabIndex =
                                        intent?.getIntExtra(EXTRA_TAB, -1)?.takeIf { it >= 0 }

                                    NavHost(
                                        navController = rememberNavController(),
                                        startDestination = TransfersNavKey(tabIndex = tabIndex),
                                        modifier = Modifier.navigationBarsPadding()
                                    ) {
                                        transfersScreen(
                                            onBackPress = { supportFinishAfterTransition() },
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

    companion object {

        /**
         * Get the Intent to open the [TransfersActivity] in the active tab
         */
        @JvmStatic
        fun getActiveTabIntent(context: Context): Intent =
            Intent(context, TransfersActivity::class.java).apply {
                putExtra(EXTRA_TAB, ACTIVE_TAB_INDEX)
            }

        /**
         * Get the Intent to open the [TransfersActivity] in the completed tab
         */
        @JvmStatic
        fun getCompletedTabIntent(context: Context): Intent =
            Intent(context, TransfersActivity::class.java).apply {
                putExtra(EXTRA_TAB, COMPLETED_TAB_INDEX)
            }

        /**
         * Get the Intent to open the [TransfersActivity] in the default tab (failed tab if the transfer are in error status, active tab otherwise)
         */
        @JvmStatic
        fun getIntent(context: Context): Intent =
            Intent(context, TransfersActivity::class.java)
    }
}