package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.Currency

/**
 * Map [String] to [Currency]
 */
typealias CurrencyMapper = (@JvmSuppressWildcards String) -> @JvmSuppressWildcards Currency