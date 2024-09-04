package mega.privacy.android.app.presentation.cancelaccountplan.model

import mega.privacy.android.shared.resources.R

/**
 * Enum class to define the answers for the cancellation survey and their IDs
 *
 * @property answerValue the string resource ID of the answer
 * @property answerId the ID of the answer
 */
enum class UICancellationSurveyAnswer(
    val answerValue: Int,
    val answerId: Int,
) {
    /**
     * I needed this plan temporarily and no longer use it
     */
    Answer1(
        R.string.account_cancel_subscription_survey_option_temporary_use,
        1
    ),

    /**
     * I found my plan too expensive
     */
    Answer2(
        R.string.account_cancel_subscription_survey_option_expensive,
        2
    ),

    /**
     * I don’t need all the storage in my plan
     */
    Answer3(
        R.string.account_cancel_subscription_survey_option_no_storage_need,
        3
    ),

    /**
     * I need features MEGA doesn’t have
     */
    Answer4(
        R.string.account_cancel_subscription_survey_option_missing_features,
        4
    ),

    /**
     * I’m switching to a provider I like better
     */
    Answer5(
        R.string.account_cancel_subscription_survey_option_switch_provider,
        5
    ),

    /**
     * I find MEGA confusing and difficult to use
     */
    Answer6(
        R.string.account_cancel_subscription_survey_option_confusing,
        6
    ),

    /**
     * I’m not happy with the support and customer service
     */
    Answer7(
        R.string.account_cancel_subscription_survey_option_dissatisfied_support,
        7
    ),

    /**
     * Other (please provide details)
     */
    Answer8(
        R.string.account_cancel_subscription_survey_option_other,
        8
    ),

    /**
     * I can no longer afford it
     */
    Answer9(
        R.string.account_cancel_subscription_survey_option_cannot_afford,
        9
    ),

    /**
     * I don't want a subscription or auto-billing
     */
    Answer10(
        R.string.account_cancel_subscription_survey_option_no_subscription,
        10
    ),
}