package mega.privacy.android.data.mapper

import mega.privacy.android.data.constant.SortOrderSource
import mega.privacy.android.domain.entity.SortOrder
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * Mapper to convert [SortOrder] to SortOrder related Integer values in MegaApiJava
 */
class SortOrderIntMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param sortOrder The [SortOrder] representing order
     *
     * @return The corresponding [Int]
     */
    operator fun invoke(
        sortOrder: SortOrder,
        source: SortOrderSource = SortOrderSource.Default,
    ) = when (sortOrder) {
        SortOrder.ORDER_NONE -> MegaApiJava.ORDER_NONE
        SortOrder.ORDER_DEFAULT_ASC -> MegaApiJava.ORDER_DEFAULT_ASC
        SortOrder.ORDER_DEFAULT_DESC -> MegaApiJava.ORDER_DEFAULT_DESC
        SortOrder.ORDER_SIZE_ASC -> MegaApiJava.ORDER_SIZE_ASC
        SortOrder.ORDER_SIZE_DESC -> MegaApiJava.ORDER_SIZE_DESC
        SortOrder.ORDER_CREATION_ASC -> MegaApiJava.ORDER_CREATION_ASC
        SortOrder.ORDER_CREATION_DESC -> MegaApiJava.ORDER_CREATION_DESC
        SortOrder.ORDER_MODIFICATION_ASC -> if (source == SortOrderSource.OutgoingShares) MegaApiJava.ORDER_SHARE_CREATION_ASC else MegaApiJava.ORDER_MODIFICATION_ASC
        SortOrder.ORDER_MODIFICATION_DESC -> if (source == SortOrderSource.OutgoingShares) MegaApiJava.ORDER_SHARE_CREATION_DESC else MegaApiJava.ORDER_MODIFICATION_DESC
        SortOrder.ORDER_LINK_CREATION_ASC -> MegaApiJava.ORDER_LINK_CREATION_ASC
        SortOrder.ORDER_LINK_CREATION_DESC -> MegaApiJava.ORDER_LINK_CREATION_DESC
        SortOrder.ORDER_LABEL_ASC -> MegaApiJava.ORDER_LABEL_ASC
        SortOrder.ORDER_LABEL_DESC -> MegaApiJava.ORDER_LABEL_DESC
        SortOrder.ORDER_FAV_ASC -> MegaApiJava.ORDER_FAV_ASC
        SortOrder.ORDER_FAV_DESC -> MegaApiJava.ORDER_FAV_DESC
    }
}
        

