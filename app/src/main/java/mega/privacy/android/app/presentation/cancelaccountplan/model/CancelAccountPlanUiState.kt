package mega.privacy.android.app.presentation.cancelaccountplan.model

import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.FormattedSize
import mega.privacy.android.domain.entity.AccountType


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
 * @param subscriptionId subscription ID if user has one associated with their account
 * @property cancellationReasons list of cancellation reasons
 * @param showCancellationSurvey true to show the cancellation survey
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
    val subscriptionId: String? = null,
    val cancellationReasons: List<UICancellationSurveyAnswer> = listOf(
        UICancellationSurveyAnswer.Answer1,
        UICancellationSurveyAnswer.Answer2,
        UICancellationSurveyAnswer.Answer3,
        UICancellationSurveyAnswer.Answer4,
        UICancellationSurveyAnswer.Answer5,
        UICancellationSurveyAnswer.Answer6,
        UICancellationSurveyAnswer.Answer7,
        UICancellationSurveyAnswer.Answer9,
        UICancellationSurveyAnswer.Answer10,
        UICancellationSurveyAnswer.Answer8,
    ),
    val showCancellationSurvey: Boolean = false,
)
