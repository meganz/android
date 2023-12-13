package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.SortOrder
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * Default implementation of [SortOrderIntMapper]
 */
class SortOrderIntMapperImpl @Inject constructor() : SortOrderIntMapper {
    override fun invoke(sortOrder: SortOrder): Int = sortOrderToInt(sortOrder)

    /**
     * Map [SortOrder] to [Int]
     */
    private fun sortOrderToInt(sortOrder: SortOrder) = when (sortOrder) {
        SortOrder.ORDER_NONE -> MegaApiJava.ORDER_NONE
        SortOrder.ORDER_DEFAULT_ASC -> MegaApiJava.ORDER_DEFAULT_ASC
        SortOrder.ORDER_DEFAULT_DESC -> MegaApiJava.ORDER_DEFAULT_DESC
        SortOrder.ORDER_SIZE_ASC -> MegaApiJava.ORDER_SIZE_ASC
        SortOrder.ORDER_SIZE_DESC -> MegaApiJava.ORDER_SIZE_DESC
        SortOrder.ORDER_CREATION_ASC -> MegaApiJava.ORDER_CREATION_ASC
        SortOrder.ORDER_CREATION_DESC -> MegaApiJava.ORDER_CREATION_DESC
        SortOrder.ORDER_MODIFICATION_ASC -> MegaApiJava.ORDER_MODIFICATION_ASC
        SortOrder.ORDER_MODIFICATION_DESC -> MegaApiJava.ORDER_MODIFICATION_DESC
        SortOrder.ORDER_LINK_CREATION_ASC -> MegaApiJava.ORDER_LINK_CREATION_ASC
        SortOrder.ORDER_LINK_CREATION_DESC -> MegaApiJava.ORDER_LINK_CREATION_DESC
        SortOrder.ORDER_LABEL_ASC -> MegaApiJava.ORDER_LABEL_ASC
        SortOrder.ORDER_LABEL_DESC -> MegaApiJava.ORDER_LABEL_DESC
        SortOrder.ORDER_FAV_ASC -> MegaApiJava.ORDER_FAV_ASC
        SortOrder.ORDER_FAV_DESC -> MegaApiJava.ORDER_FAV_DESC
    }
}