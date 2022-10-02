package mega.privacy.android.app.data.mapper

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.SubscriptionPlan

/**
 *  Mapper for Subscription plan
 */
typealias SubscriptionPlanMapper = (
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards AccountType?,
    @JvmSuppressWildcards Int,
    @JvmSuppressWildcards Int,
    @JvmSuppressWildcards Int,
    @JvmSuppressWildcards Int,
    @JvmSuppressWildcards Currency?,
    @JvmSuppressWildcards String,
) -> @JvmSuppressWildcards SubscriptionPlan