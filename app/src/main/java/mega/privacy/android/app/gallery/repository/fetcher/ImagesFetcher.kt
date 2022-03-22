package mega.privacy.android.app.gallery.repository.fetcher

import android.content.Context
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.gallery.data.GalleryItem
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.*
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import java.util.*

class ImagesFetcher(
    context: Context,
    megaApi: MegaApiAndroid,
    selectedNodesMap: LinkedHashMap<Any, GalleryItem>,
    zoom: Int,
) : GalleryBaseFetcher(
    context = context,
    megaApi = megaApi,
    selectedNodesMap = selectedNodesMap,
    zoom = zoom
) {
    /**
     * Gets photos.
     *
     * @param cancelToken       MegaCancelToken to cancel the search at any time.
     *                          Must not be null.
     */
    override fun getNodes(cancelToken: MegaCancelToken?): List<MegaNode> =
        megaApi.searchByType(
            cancelToken!!,
            ORDER_MODIFICATION_DESC,
            FILE_TYPE_PHOTO,
            SEARCH_TARGET_ROOTNODE
        ).filter { MimeTypeList.typeForName(it.name).isImage }
}