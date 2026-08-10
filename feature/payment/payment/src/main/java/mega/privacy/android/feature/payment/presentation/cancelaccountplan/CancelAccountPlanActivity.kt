package mega.privacy.android.feature.payment.presentation.cancelaccountplan

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.sharedcomponents.extension.isDarkMode
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.feature.payment.presentation.cancelaccountplan.model.CancellationInstructionsType
import mega.privacy.android.feature.payment.presentation.cancelaccountplan.view.CancelAccountPlanView
import mega.privacy.android.feature.payment.presentation.cancelaccountplan.view.CancelSubscriptionSurveyView
import mega.privacy.android.feature.payment.presentation.cancelaccountplan.view.instructionscreens.CancellationInstructionsView
import mega.privacy.android.feature.payment.presentation.storage.AccountStorageViewModel
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
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

        /**
         * Navigates to the Cancel Account Plan screen.
         *
         * @param context The context to use for navigation.
         * @param usedStorage The formatted used storage string to display.
         */
        fun navigateToCancelAccountPlan(context: Context, usedStorage: String) {
            context.startActivity(
                Intent(context, CancelAccountPlanActivity::class.java)
                    .putExtra(EXTRA_USED_STORAGE, usedStorage)
            )
        }
    }

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

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
            val themeMode by monitorThemeModeUseCase()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val accountUiState by accountStorageViewModel.state.collectAsStateWithLifecycle()

            val accountType = uiState.accountType
            val formattedBaseStorage = remember(accountUiState.baseStorage) {
                accountUiState.baseStorage?.let {
                    formatFileSize(it, this@CancelAccountPlanActivity)
                }.orEmpty()
            }
            OriginalTheme(isDark = themeMode.isDarkMode()) {
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
                            formattedBaseStorage = formattedBaseStorage,
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
                                            -> navController.navigate(cancellationInstructionsRoute)

                                        CancellationInstructionsType.PlayStore,
                                            -> navController.navigate(cancellationSurveyRoute)
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
                                uiState.sku?.let { sku ->
                                    redirectToCancelPlayStoreSubscription(sku)
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

    private fun redirectToCancelPlayStoreSubscription(sku: String) {
        val appPackage = applicationContext.packageName
        val link = "${MANAGE_PLAY_STORE_SUBSCRIPTION_URL}${sku}&package=${appPackage}"
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

/**
 * Manage Subscription page URL in Google Play Store for user to manage their subscriptions (the link need the sku and app package added)
 */
private const val MANAGE_PLAY_STORE_SUBSCRIPTION_URL =
    "https://play.google.com/store/account/subscriptions?sku="
