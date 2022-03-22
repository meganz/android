package mega.privacy.android.app.myAccount

/**
 * The visible state of subscription dialog
 */
sealed class SubscriptionDialogState {
    /**
     * The visible state of dialog
     * @param result SubscriptionCheckResult
     * @return SubscriptionDialogState
     */
    data class Visible(val result: SubscriptionCheckResult): SubscriptionDialogState()

    /**
     * The invisible state of dialog
     * @return SubscriptionDialogState
     */
    object Invisible : SubscriptionDialogState()
}

/**
 * The visible state of cancel account dialog
 */
sealed class CancelAccountDialogState {
    /**
     * The current subscription is from ECP/Stripe, dialog is visible and uses corresponding message.
     * @return CancelAccountDialogState
     */
    object VisibleWithSubscription: CancelAccountDialogState()

    /**
     * The dialog is visible and uses default message.
     * @return CancelAccountDialogState
     */
    object VisibleDefault: CancelAccountDialogState()

    /**
     * The dialog is invisible
     * @return CancelAccountDialogState
     */
    object Invisible: CancelAccountDialogState()
}
