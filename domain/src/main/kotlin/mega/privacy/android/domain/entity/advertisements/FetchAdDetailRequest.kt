package mega.privacy.android.domain.entity.advertisements

/**
 * Class containing slot id for each screen to fetch [AdDetail]
 *
 * @param slotIdList  The list of ad slot ids to fetch ads
 * @param linkHandle  The public handle for file/folder link if user visits Share Link screen, this parameter is optional
 */
data class FetchAdDetailRequest(
    val slotIdList: List<String>,
    val linkHandle: Long?
)
