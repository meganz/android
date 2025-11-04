package mega.privacy.android.domain.entity.payment

import mega.privacy.android.domain.entity.Subscription

data class Subscriptions(
    val monthlySubscriptions: List<Subscription>,
    val yearlySubscriptions: List<Subscription>
)