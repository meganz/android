package mega.privacy.android.app.presentation.cancelaccountplan.model

import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.FormattedSize
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.shared.resources.R.string


/**
 * UI state for Cancel Account Plan Activity
 * @param cancellationInstructionsType type of cancellation instruction to be shown to user if they proceed with cancel subscription flow
 * @param isMonthlySubscription check if user's current subscription is yearly or monthly
 * @param formattedPlanTransfer transfer quota of the current subscription plan
 * @param formattedPlanStorage storage quota of the current subscription plan
 * @param freePlanStorageQuota free storage quota of the free plan
 * @param rewindDaysQuota rewind days quota of the current subscription plan
 * @param accountType type of the account
 * @param accountNameRes name of the account
 * @param isLoading UI state to show the loading state
 * @property cancellationReasons list of cancellation reasons
 */
data class CancelAccountPlanUiState(
    val cancellationInstructionsType: CancellationInstructionsType? = null,
    val isMonthlySubscription: Boolean? = null,
    val formattedPlanStorage: FormattedSize? = null,
    val formattedPlanTransfer: FormattedSize? = null,
    val freePlanStorageQuota: String = "20",
    val rewindDaysQuota: String = "",
    val accountType: AccountType = AccountType.UNKNOWN,
    val accountNameRes: Int = R.string.recovering_info,
    val isLoading: Boolean = true,
    val cancellationReasons: List<Int> = listOf(
        string.account_cancel_subscription_survey_option_expensive,
        string.account_cancel_subscription_survey_option_cannot_afford,
        string.account_cancel_subscription_survey_option_no_subscription,
        string.account_cancel_subscription_survey_option_no_storage_need,
        string.account_cancel_subscription_survey_option_missing_features,
        string.account_cancel_subscription_survey_option_switch_provider,
        string.account_cancel_subscription_survey_option_confusing,
        string.account_cancel_subscription_survey_option_dissatisfied_support,
        string.account_cancel_subscription_survey_option_temporary_use,
        string.account_cancel_subscription_survey_option_other
    ),
)
