package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.preference.ListViewType

/**
 * Mapper to convert the integer value received from DataStore into [ListViewType]
 */
typealias ListViewTypeMapper = (@JvmSuppressWildcards Int?) -> @JvmSuppressWildcards ListViewType?