package mega.privacy.android.app.presentation.cancelaccountplan.model


/**
 * UI state for Cancel Account Plan Activity
 * @param cancellationInstructionsType type of cancellation instruction to be shown to user if they proceed with cancel subscription flow
 * @param isMonthlySubscription check if user's current subscription is yearly or monthly
 */
data class CancelAccountPlanUiState(
    val cancellationInstructionsType: CancellationInstructionsType? = null,
    val isMonthlySubscription: Boolean? = null,
)
