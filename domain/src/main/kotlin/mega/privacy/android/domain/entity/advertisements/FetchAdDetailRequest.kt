package mega.privacy.android.domain.entity.advertisements

/**
 * Class containing slot id for each screen to fetch [AdDetails]
 *
 * @param slotId  The ad slot id to fetch ad
 * @param linkHandle  The public handle for file/folder link if user visits Share Link screen, this parameter is optional
 */
data class FetchAdDetailRequest(
    val slotId: String,
    val linkHandle: Long?
)
