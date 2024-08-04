package mega.privacy.android.domain.entity.account

import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.PaymentMethodType
import mega.privacy.android.domain.entity.SubscriptionStatus

/**
 * AccountSubscriptionDetail
 *
 * @property subscriptionId
 * @property subscriptionStatus    Status of subscription
 * @property subscriptionCycle
 * @property paymentMethodType
 * @property renewalTime
 * @property subscriptionLevel     Level of subscription (e.g. Pro I, II, III, Flexi or Business or any low-tier plan or standalone plan)
 * @property featuresList          List of features available for this subscription (e.g. "vpn", "pwm")
 * @property isFreeTrial           True if plan is a trial one
 */
class AccountSubscriptionDetail(
    val subscriptionId: String,
    val subscriptionStatus: SubscriptionStatus?,
    val subscriptionCycle: AccountSubscriptionCycle,
    val paymentMethodType: PaymentMethodType?,
    val renewalTime: Long,
    val subscriptionLevel: AccountType,
    val featuresList: List<String>,
    val isFreeTrial: Boolean,
)