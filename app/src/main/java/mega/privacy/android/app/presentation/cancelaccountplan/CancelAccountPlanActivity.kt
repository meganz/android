package mega.privacy.android.app.presentation.cancelaccountplan

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.presentation.account.AccountStorageViewModel
import mega.privacy.android.app.presentation.cancelaccountplan.model.CancellationInstructionsType
import mega.privacy.android.app.presentation.cancelaccountplan.view.CancelAccountPlanView
import mega.privacy.android.app.presentation.cancelaccountplan.view.CancelSubscriptionSurveyView
import mega.privacy.android.app.presentation.cancelaccountplan.view.instructionscreens.CancellationInstructionsView
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.upgradeAccount.UpgradeAccountViewModel.Companion.getProductId
import mega.privacy.android.app.utils.MANAGE_PLAY_STORE_SUBSCRIPTION_URL
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.mobile.analytics.event.CancelSubscriptionContinueCancellationButtonPressedEvent
import mega.privacy.mobile.analytics.event.CancelSubscriptionKeepPlanButtonPressedEvent
import mega.privacy.mobile.analytics.event.SubscriptionCancellationSurveyCancelSubscriptionButtonEvent
import mega.privacy.mobile.analytics.event.SubscriptionCancellationSurveyDontCancelButtonEvent
import mega.privacy.mobile.analytics.event.SubscriptionCancellationSurveyScreenEvent
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity to cancel the current account plan
 */
@AndroidEntryPoint
class CancelAccountPlanActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_USED_STORAGE = "EXTRA_USED_STORAGE"
    }

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel: CancelAccountPlanViewModel by viewModels()
    private val accountStorageViewModel: AccountStorageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val usedStorage = intent.getStringExtra(EXTRA_USED_STORAGE) ?: ""
        val cancelAccountPlanRoute = "cancelAccount/plan"
        val cancellationInstructionsRoute = "cancelAccount/cancellationInstructions"
        val cancellationSurveyRoute = "cancelAccount/cancellationSurvey"

        setContent {
            val navController = rememberNavController()
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val accountUiState by accountStorageViewModel.state.collectAsStateWithLifecycle()

            val accountType = uiState.accountType
            OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                NavHost(
                    navController = navController,
                    startDestination = if (accountType == AccountType.PRO_FLEXI) {
                        cancellationInstructionsRoute
                    } else {
                        cancelAccountPlanRoute
                    },
                ) {
                    composable(cancelAccountPlanRoute) {
                        CancelAccountPlanView(
                            uiState = uiState,
                            accountUiState = accountUiState,
                            formattedUsedStorage = usedStorage,
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

                                        CancellationInstructionsType.PlayStore,
                                        -> navController.navigate(
                                            cancellationSurveyRoute
                                        )
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
                                if (accountType == AccountType.PRO_FLEXI) {
                                    finish()
                                } else {
                                    navController.popBackStack()
                                }
                            },
                            isAccountReactivationNeeded = false
                        )
                    }
                    composable(cancellationSurveyRoute) {
                        Analytics.tracker.trackEvent(SubscriptionCancellationSurveyScreenEvent)
                        CancelSubscriptionSurveyView(
                            possibleCancellationReasons = uiState.cancellationReasons,
                            onCancelSubscriptionButtonClicked = { reason, canContact ->
                                Analytics.tracker.trackEvent(
                                    SubscriptionCancellationSurveyCancelSubscriptionButtonEvent
                                )
                                viewModel.cancelSubscription(reason, canContact)
                                uiState.isMonthlySubscription?.let { isMonthlySubscription ->
                                    redirectToCancelPlayStoreSubscription(
                                        accountType,
                                        isMonthlySubscription
                                    )
                                }
                            },
                            onDoNotCancelButtonClicked = {
                                Analytics.tracker.trackEvent(
                                    SubscriptionCancellationSurveyDontCancelButtonEvent
                                )
                                finish()
                            })
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

    private fun redirectToCancelPlayStoreSubscription(
        accountType: AccountType,
        isMonthly: Boolean,
    ) {
        val appPackage = applicationContext.packageName
        val productID = getProductId(isMonthly, accountType)
        val link = "${MANAGE_PLAY_STORE_SUBSCRIPTION_URL}${productID}&package=${appPackage}"
        val uriUrl = Uri.parse(link)
        val launchBrowser = Intent(ACTION_VIEW, uriUrl)
        runCatching {
            startActivity(launchBrowser)
            finish()
        }.onFailure {
            Timber.e("Failed to open play store subscription page with error: ${it.message}")
        }
    }
}
