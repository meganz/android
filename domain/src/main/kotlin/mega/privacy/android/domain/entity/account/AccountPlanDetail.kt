package mega.privacy.android.domain.entity.account

import mega.privacy.android.domain.entity.AccountType

/**
 * Account Plan Detail
 *
 * @property accountType
 * @property isProPlan             True if the plan is Pro (including Flexi), Low-tier or Business, False if plan is VPN or PWM standalone/trial
 * @property expirationTime        Timestamp when the plan will expire
 * @property subscriptionId        String ID for active recurring subscription if there is one for this plan
 * @property featuresList          List of features available for this plan (e.g. "vpn", "pwm")
 * @property isFreeTrial           True if plan is a trial one
 */
class AccountPlanDetail(
    val accountType: AccountType?,
    val isProPlan: Boolean,
    val expirationTime: Long?,
    val subscriptionId: String?,
    val featuresList: List<String>,
    val isFreeTrial: Boolean,
)