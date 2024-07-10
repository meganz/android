package mega.privacy.android.app.presentation.cancelaccountplan

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.cancelaccountplan.model.CancellationInstructionsType
import mega.privacy.android.app.presentation.cancelaccountplan.model.UIAccountDetails
import mega.privacy.android.app.presentation.cancelaccountplan.view.CancelAccountPlanView
import mega.privacy.android.app.presentation.cancelaccountplan.view.CancellationInstructionsView
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.mobile.analytics.event.CancelSubscriptionContinueCancellationButtonPressedEvent
import mega.privacy.mobile.analytics.event.CancelSubscriptionKeepPlanButtonPressedEvent
import org.jetbrains.anko.toast
import javax.inject.Inject

/**
 * Activity to cancel the current account plan
 */
@AndroidEntryPoint
class CancelAccountPlanActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ACCOUNT_TYPE = "EXTRA_ACCOUNT_TYPE"
        const val EXTRA_TRANSFER_QUOTA = "EXTRA_TRANSFER_QUOTA"
        const val EXTRA_STORAGE_QUOTA = "EXTRA_STORAGE_QUOTA"
        const val EXTRA_USED_STORAGE = "EXTRA_USED_STORAGE"

    }

    @Inject
    lateinit var getThemeMode: GetThemeMode
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val accountType = intent.getIntExtra(EXTRA_ACCOUNT_TYPE, 0)
        val usedStorage = intent.getStringExtra(EXTRA_USED_STORAGE) ?: ""
        val transferQuota = intent.getStringExtra(EXTRA_TRANSFER_QUOTA) ?: ""
        val storageQuota = intent.getStringExtra(EXTRA_STORAGE_QUOTA) ?: ""

        val cancelAccountPlanRoute = "cancelAccount/plan"
        val cancellationInstructionsRoute = "cancelAccount/cancellationInstructions"

        setContent {
            val navController = rememberNavController()
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                NavHost(
                    navController = navController,
                    startDestination = if (accountType == Constants.PRO_FLEXI) {
                        cancellationInstructionsRoute
                    } else {
                        cancelAccountPlanRoute
                    },
                ) {
                    composable(cancelAccountPlanRoute) {
                        CancelAccountPlanView(accountDetailsUI = UIAccountDetails(
                            accountType = getString(getAccountNameRes(accountType)),
                            storageQuotaSize = storageQuota,
                            usedStorageSize = usedStorage,
                            transferQuotaSize = transferQuota,
                        ),
                            onKeepPlanButtonClicked = {
                                Analytics.tracker.trackEvent(
                                    CancelSubscriptionKeepPlanButtonPressedEvent
                                )
                                finish()
                            },
                            onContinueCancellationButtonClicked = {
                                Analytics.tracker.trackEvent(
                                    CancelSubscriptionContinueCancellationButtonPressedEvent
                                )
                                toast("Cancel button clicked")
                                navController.navigate(cancellationInstructionsRoute)
                            })
                    }
                    composable(cancellationInstructionsRoute) {
                        CancellationInstructionsView(
                            instructionsType = CancellationInstructionsType.AppStore,
                            onMegaUrlClicked = {},
                            onCancelSubsFromOtherDeviceClicked = {},
                            onBackPressed = {
                                navController.popBackStack()
                            },
                            isAccountExpired = false
                        )
                        toast("Show the correct cancellation instruction steps")
                    }
                }
            }
        }
    }

    private fun getAccountNameRes(accountType: Int): Int {
        return when (accountType) {
            Constants.PRO_LITE -> R.string.prolite_account
            Constants.PRO_I -> R.string.pro1_account
            Constants.PRO_II -> R.string.pro2_account
            Constants.PRO_III -> R.string.pro3_account
            Constants.PRO_FLEXI -> R.string.pro_flexi_account
            else -> R.string.free_account
        }
    }
}