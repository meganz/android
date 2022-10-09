package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.AccountType

/**
 * Map [AccountType], [Int] to [String]
 */
typealias SkuMapper = (@JvmSuppressWildcards AccountType?, @JvmSuppressWildcards Int) -> @JvmSuppressWildcards String?

// TODO move toSkuMapper function here once the BillingManager implementation is moved to Data module