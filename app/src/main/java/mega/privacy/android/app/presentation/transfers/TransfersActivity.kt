package mega.privacy.android.app.presentation.transfers

import android.app.PendingIntent
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
import mega.privacy.android.app.presentation.transfers.navigation.TransferDeepLinkHandler
import mega.privacy.android.app.presentation.transfers.view.navigation.transfersScreen
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.destination.TransfersNavKey
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import javax.inject.Inject

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
                                    val tab = intent?.getStringExtra(EXTRA_TAB)
                                        ?.let { TransfersNavKey.Tab.valueOf(it) }

                                    NavHost(
                                        navController = rememberNavController(),
                                        startDestination = TransfersNavKey(tab = tab),
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
         * Extra for the tab to show in the transfers screen.
         */
        private const val EXTRA_TAB = "TAB"

        /**
         * Get the Intent to open the [TransfersActivity] in the active tab
         */
        private fun getActiveTabIntent(context: Context): Intent =
            Intent(context, TransfersActivity::class.java).apply {
                putExtra(EXTRA_TAB, TransfersNavKey.Tab.Active.name)
            }

        /**
         * Get the Intent to open the [TransfersActivity] in the completed tab
         */
        private fun getCompletedTabIntent(context: Context): Intent =
            Intent(context, TransfersActivity::class.java).apply {
                putExtra(EXTRA_TAB, TransfersNavKey.Tab.Completed.name)
            }

        /**
         * Get the Intent to open the [TransfersActivity] in the default tab (failed tab if the transfer are in error status, active tab otherwise)
         */
        fun getIntent(context: Context): Intent =
            Intent(context, TransfersActivity::class.java)

        /**
         * Helper method to create a pending intent for transfers section with optional selected tab
         * that works with both values of single activity feature flag
         * Once the [SingleActivity] feature flag is removed this activity will be removed as well and
         * we can replace calls to this method with calls to similar method:
         * [TransferDeepLinkHandler.getPendingIntentForTransfersSection]
         */
        fun getPendingIntentForTransfersSection(
            singleActivity: Boolean,
            context: Context,
            tab: TransfersNavKey.Tab? = null,
            requestCode: Int = 0,
        ): PendingIntent = if (singleActivity) {
            TransferDeepLinkHandler.getPendingIntentForTransfersSection(
                context, tab, requestCode
            )
        } else {
            PendingIntent.getActivity(
                context,
                requestCode,
                when (tab) {
                    TransfersNavKey.Tab.Active -> getActiveTabIntent(context)
                    TransfersNavKey.Tab.Completed -> getCompletedTabIntent(context)
                    else -> getIntent(context)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}