package mega.privacy.android.data.mapper.search

import mega.privacy.android.domain.entity.search.SearchTarget
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

/**
 * Search target mapper to map [SearchTarget] to an Int
 */
class SearchTargetIntMapper @Inject constructor() {

    /**
     * Invoke
     * @param searchTarget search target
     *
     * @return Int value of search target
     */
    operator fun invoke(searchTarget: SearchTarget): Int = when (searchTarget) {
        SearchTarget.INCOMING_SHARE -> MegaApiAndroid.SEARCH_TARGET_INSHARE
        SearchTarget.OUTGOING_SHARE -> MegaApiAndroid.SEARCH_TARGET_OUTSHARE
        SearchTarget.LINKS_SHARE -> MegaApiAndroid.SEARCH_TARGET_PUBLICLINK
        SearchTarget.ROOT_NODES -> MegaApiAndroid.SEARCH_TARGET_ROOTNODE
        SearchTarget.ALL -> MegaApiAndroid.SEARCH_TARGET_ALL
    }
}