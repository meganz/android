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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.presentation.cancelaccountplan.model.CancellationInstructionsType
import mega.privacy.android.app.presentation.cancelaccountplan.model.UIAccountDetails
import mega.privacy.android.app.presentation.cancelaccountplan.view.CancelAccountPlanView
import mega.privacy.android.app.presentation.cancelaccountplan.view.CancelSubscriptionSurveyView
import mega.privacy.android.app.presentation.cancelaccountplan.view.instructionscreens.CancellationInstructionsView
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.myaccount.mapper.AccountNameMapper
import mega.privacy.android.app.upgradeAccount.UpgradeAccountViewModel.Companion.getProductId
import mega.privacy.android.app.utils.MANAGE_PLAY_STORE_SUBSCRIPTION_URL
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.resources.R
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

    private val REWIND_DAYS_QUOTA_PRO_LITE = "90"
    private val REWIND_DAYS_QUOTA_OTHERS = "180"
    private val FREE_STORAGE_QUOTA = "20"

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var accountNameMapper: AccountNameMapper

    private val viewModel: CancelAccountPlanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val accountType = intent.getStringExtra(EXTRA_ACCOUNT_TYPE)?.let { AccountType.valueOf(it) }
            ?: AccountType.UNKNOWN
        val usedStorage = intent.getStringExtra(EXTRA_USED_STORAGE) ?: ""
        val transferQuota = intent.getStringExtra(EXTRA_TRANSFER_QUOTA) ?: ""
        val storageQuota = intent.getStringExtra(EXTRA_STORAGE_QUOTA) ?: ""

        val cancelAccountPlanRoute = "cancelAccount/plan"
        val cancellationInstructionsRoute = "cancelAccount/cancellationInstructions"
        val cancellationSurveyRoute = "cancelAccount/cancellationSurvey"

        val possibleCancellationReasons = listOf(
            R.string.account_cancel_subscription_survey_option_expensive,
            R.string.account_cancel_subscription_survey_option_cannot_afford,
            R.string.account_cancel_subscription_survey_option_no_subscription,
            R.string.account_cancel_subscription_survey_option_no_storage_need,
            R.string.account_cancel_subscription_survey_option_missing_features,
            R.string.account_cancel_subscription_survey_option_switch_provider,
            R.string.account_cancel_subscription_survey_option_confusing,
            R.string.account_cancel_subscription_survey_option_dissatisfied_support,
            R.string.account_cancel_subscription_survey_option_temporary_use,
        ).shuffled() + R.string.account_cancel_subscription_survey_option_other

        setContent {
            val navController = rememberNavController()
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
                            accountDetailsUI = UIAccountDetails(
                                accountType = getString(accountNameMapper(accountType)),
                                storageQuotaSize = storageQuota,
                                usedStorageSize = usedStorage,
                                transferQuotaSize = transferQuota,
                                rewindDaysQuota = if (accountType == AccountType.PRO_LITE) {
                                    REWIND_DAYS_QUOTA_PRO_LITE
                                } else {
                                    REWIND_DAYS_QUOTA_OTHERS
                                },
                                freeStorageQuota = FREE_STORAGE_QUOTA
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
                        CancelSubscriptionSurveyView(
                            possibleCancellationReasons = possibleCancellationReasons,
                            onCancelSubscriptionButtonClicked = {
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
                            },
                            onDoNotCancelButtonClicked = {
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
