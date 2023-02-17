package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.verification.Country

/**
 * Map properties to [Country]
 */
typealias CountryMapper = (
    @JvmSuppressWildcards String,
    @JvmSuppressWildcards String,
    @JvmSuppressWildcards String,
) -> @JvmSuppressWildcards Country


internal fun toCountry(
    name: String,
    code: String,
    callingCode: String,
) = Country(name, code, callingCode)
