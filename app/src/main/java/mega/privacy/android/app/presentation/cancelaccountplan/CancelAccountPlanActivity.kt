package mega.privacy.android.app.presentation.cancelaccountplan

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import mega.privacy.android.app.presentation.cancelaccountplan.view.instructionscreens.CancellationInstructionsView
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.upgradeAccount.UpgradeAccountViewModel.Companion.getProductId
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MANAGE_PLAY_STORE_SUBSCRIPTION_URL
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.mobile.analytics.event.CancelSubscriptionContinueCancellationButtonPressedEvent
import mega.privacy.mobile.analytics.event.CancelSubscriptionKeepPlanButtonPressedEvent
import timber.log.Timber
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
    private val viewModel: CancelAccountPlanViewModel by viewModels()

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
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
                        CancelAccountPlanView(
                            accountDetailsUI = UIAccountDetails(
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
                                uiState.cancellationInstructionsType?.let { cancellationInstructionsType ->
                                    when (cancellationInstructionsType) {
                                        CancellationInstructionsType.AppStore,
                                        CancellationInstructionsType.WebClient,
                                        -> navController.navigate(
                                            cancellationInstructionsRoute
                                        )

                                        CancellationInstructionsType.PlayStore ->
                                            uiState.isMonthlySubscription?.let { isMonthlySubscription ->
                                                redirectToCancelPlayStoreSubscription(
                                                    accountType,
                                                    isMonthlySubscription
                                                )
                                            }
                                    }
                                }
                            })
                    }
                    composable(cancellationInstructionsRoute) {
                        CancellationInstructionsView(
                            instructionsType = uiState.cancellationInstructionsType,
                            onMegaUrlClicked = { url ->
                                navigateToBrowser(url)
                            },
                            onCancelSubsFromOtherDeviceClicked = { url ->
                                navigateToBrowser(url)
                            },
                            onBackPressed = {
                                if (accountType == Constants.PRO_FLEXI) {
                                    finish()
                                } else {
                                    navController.popBackStack()
                                }
                            },
                            isAccountReactivationNeeded = false
                        )
                    }
                }
            }
        }
    }

    private fun navigateToBrowser(url: String) {
        startActivity(
            Intent(
                ACTION_VIEW,
                Uri.parse(url)
            )
        )
    }

    private fun getAccountNameRes(accountTypeInt: Int): Int {
        return when (accountTypeInt) {
            Constants.PRO_LITE -> R.string.prolite_account
            Constants.PRO_I -> R.string.pro1_account
            Constants.PRO_II -> R.string.pro2_account
            Constants.PRO_III -> R.string.pro3_account
            Constants.PRO_FLEXI -> R.string.pro_flexi_account
            else -> R.string.free_account
        }
    }

    private fun redirectToCancelPlayStoreSubscription(
        accountType: Int,
        isMonthly: Boolean,
    ) {
        val appPackage = applicationContext.packageName
        val productID = getProductId(isMonthly, accountType)
        val link = "${MANAGE_PLAY_STORE_SUBSCRIPTION_URL}${productID}&package=${appPackage}"
        val uriUrl = Uri.parse(link)
        val launchBrowser = Intent(ACTION_VIEW, uriUrl)
        runCatching {
            startActivity(launchBrowser)
        }.onFailure {
            Timber.e("Failed to open play store subscription page with error: ${it.message}")
        }
    }
}
