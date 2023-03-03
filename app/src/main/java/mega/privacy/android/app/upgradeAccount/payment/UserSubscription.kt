package mega.privacy.android.app.upgradeAccount.payment

/**
 * User subscription
 *
 * MONTHLY_SUBSCRIBED if already subscribed to the monthly plan
 * YEARLY_SUBSCRIBED if already subscribed to the yearly plan
 * NOT_SUBSCRIBED if not subscribed
 */
internal enum class UserSubscription {
    NOT_SUBSCRIBED, MONTHLY_SUBSCRIBED, YEARLY_SUBSCRIBED
}