package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.SortOrder

/**
 * Mapper to convert [SortOrder] to SortOrder related Integer values in MegaApiJava
 */
fun interface SortOrderIntMapper {

    /**
     * Invocation function
     *
     * @param sortOrder The [SortOrder] representing order
     *
     * @return The corresponding [Int]
     */
    operator fun invoke(sortOrder: SortOrder): Int
}
        

