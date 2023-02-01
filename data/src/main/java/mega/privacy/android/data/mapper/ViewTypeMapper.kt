package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.preference.ViewType

/**
 * Mapper to convert the integer value received from DataStore into [ViewType]
 */
typealias ViewTypeMapper = (@JvmSuppressWildcards Int?) -> @JvmSuppressWildcards ViewType?