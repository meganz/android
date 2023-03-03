package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.SortOrder

/**
 * Mapper to convert Sort Order related Integer values in MegaApiJava to [SortOrder]
 */
internal fun interface SortOrderMapper {

    /**
     * Invocation function
     *
     * @param order The [Int] representing order
     *
     * @return The corresponding [SortOrder]
     */
    operator fun invoke(order: Int): SortOrder?
}

