package mega.privacy.android.app.repo

import android.content.Context
import android.text.TextUtils
import android.util.Pair
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.MimeTypeThumbnail
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants.SEARCH_BY_DATE_FILTER_LAST_MONTH
import mega.privacy.android.app.utils.Constants.SEARCH_BY_DATE_FILTER_LAST_YEAR
import mega.privacy.android.app.utils.Constants.SEARCH_BY_DATE_FILTER_POS_END_DAY
import mega.privacy.android.app.utils.Constants.SEARCH_BY_DATE_FILTER_POS_MONTH_OR_YEAR
import mega.privacy.android.app.utils.Constants.SEARCH_BY_DATE_FILTER_POS_START_DAY
import mega.privacy.android.app.utils.Constants.SEARCH_BY_DATE_FILTER_POS_THE_DAY
import mega.privacy.android.app.utils.Constants.SEARCH_BY_DATE_FILTER_POS_TYPE
import mega.privacy.android.app.utils.Constants.SEARCH_BY_DATE_FILTER_TYPE_BETWEEN_TWO_DAYS
import mega.privacy.android.app.utils.Constants.SEARCH_BY_DATE_FILTER_TYPE_LAST_MONTH_OR_YEAR
import mega.privacy.android.app.utils.Constants.SEARCH_BY_DATE_FILTER_TYPE_ONE_DAY
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFile
import mega.privacy.android.app.utils.SortUtil.sortOfflineByModificationDateAscending
import mega.privacy.android.app.utils.SortUtil.sortOfflineByModificationDateDescending
import mega.privacy.android.app.utils.SortUtil.sortOfflineByNameAscending
import mega.privacy.android.app.utils.SortUtil.sortOfflineByNameDescending
import mega.privacy.android.app.utils.SortUtil.sortOfflineBySizeAscending
import mega.privacy.android.app.utils.SortUtil.sortOfflineBySizeDescending
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_DESC
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_ASC
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_DESC
import nz.mega.sdk.MegaApiJava.ORDER_SIZE_ASC
import nz.mega.sdk.MegaApiJava.ORDER_SIZE_DESC
import nz.mega.sdk.MegaNode
import java.io.File
import java.time.YearMonth
import java.util.Locale
import java.util.function.Function
import javax.inject.Inject

class MegaNodeRepo @Inject constructor(
    @ApplicationContext private val context: Context,
    private val megaApi: MegaApiAndroid,
    private val dbHandler: DatabaseHandler
) {

    /**
     * Get children of CU/MU, with the given order, and filter nodes by date (optional).
     *
     * @param type CU_TYPE_CAMERA or CU_TYPE_MEDIA
     * @param orderBy order
     * @param filter search filter
     * filter[0] is the search type:
     * 1 means search for nodes in one day, then filter[1] is the day in millis.
     * 2 means search for nodes in last month (filter[2] is 1), or in last year (filter[2] is 2).
     * 3 means search for nodes between two days, filter[3] and filter[4] are start and end day in
     * millis.
     * @return list of pairs, whose first value is index used for
     * FullscreenImageViewer/AudioVideoPlayer, and second value is the node
     */
    fun getCuChildren(
        type: Int,
        orderBy: Int,
        filter: LongArray?
    ): List<Pair<Int, MegaNode>> {
        var cuHandle: Long = INVALID_HANDLE
        val pref = dbHandler.preferences

        if (type == CU_TYPE_CAMERA) {
            if (pref != null && pref.camSyncHandle != null) {
                try {
                    cuHandle = pref.camSyncHandle.toLong()
                } catch (e: NumberFormatException) {
                    logError("parse getCamSyncHandle error $e")
                }

                if (megaApi.getNodeByHandle(cuHandle) == null) {
                    cuHandle = INVALID_HANDLE
                }
            }
            if (cuHandle == INVALID_HANDLE) {
                for (node in megaApi.getChildren(megaApi.rootNode)) {
                    if (node.isFolder && TextUtils.equals(
                            context.getString(R.string.section_photo_sync),
                            node.name
                        )
                    ) {
                        cuHandle = node.handle
                        dbHandler.setCamSyncHandle(cuHandle)
                        break
                    }
                }
            }
        } else {
            if (pref != null && pref.megaHandleSecondaryFolder != null) {
                try {
                    cuHandle = pref.megaHandleSecondaryFolder.toLong()
                } catch (e: NumberFormatException) {
                    logError("parse MegaHandleSecondaryFolder error $e")
                }

                if (megaApi.getNodeByHandle(cuHandle) == null) {
                    cuHandle = INVALID_HANDLE
                }
            }
        }

        if (cuHandle == INVALID_HANDLE) {
            return emptyList()
        }

        val children: List<MegaNode> =
            megaApi.getChildren(megaApi.getNodeByHandle(cuHandle), orderBy)
        val nodes = ArrayList<Pair<Int, MegaNode>>()

        for ((index, node) in children.withIndex()) {
            if (node.isFolder) {
                continue
            }

            val mime = MimeTypeThumbnail.typeForName(node.name)
            if (mime.isImage || mime.isVideoReproducible) {
                // when not in search mode, index used by viewer is index in all siblings,
                // including non image/video nodes
                nodes.add(Pair.create(index, node))
            }
        }

        if (filter == null) {
            return nodes
        }

        val result = ArrayList<Pair<Int, MegaNode>>()
        var filterFunction: Function<MegaNode, Boolean>? = null

        when (filter[SEARCH_BY_DATE_FILTER_POS_TYPE]) {
            SEARCH_BY_DATE_FILTER_TYPE_ONE_DAY -> {
                val date = Util.fromEpoch(filter[SEARCH_BY_DATE_FILTER_POS_THE_DAY] / 1000)
                filterFunction = Function { node: MegaNode ->
                    date == Util.fromEpoch(node.modificationTime)
                }
            }
            SEARCH_BY_DATE_FILTER_TYPE_LAST_MONTH_OR_YEAR -> {
                when (filter[SEARCH_BY_DATE_FILTER_POS_MONTH_OR_YEAR]) {
                    SEARCH_BY_DATE_FILTER_LAST_MONTH -> {
                        val lastMonth = YearMonth.now().minusMonths(1)
                        filterFunction = Function { node: MegaNode ->
                            lastMonth == YearMonth.from(Util.fromEpoch(node.modificationTime))
                        }
                    }
                    SEARCH_BY_DATE_FILTER_LAST_YEAR -> {
                        val lastYear = YearMonth.now().year - 1
                        filterFunction = Function { node: MegaNode ->
                            Util.fromEpoch(node.modificationTime).year == lastYear
                        }
                    }
                }
            }
            SEARCH_BY_DATE_FILTER_TYPE_BETWEEN_TWO_DAYS -> {
                val from = Util.fromEpoch(filter[SEARCH_BY_DATE_FILTER_POS_START_DAY] / 1000)
                val to = Util.fromEpoch(filter[SEARCH_BY_DATE_FILTER_POS_END_DAY] / 1000)
                filterFunction = Function { node: MegaNode ->
                    val modifyDate = Util.fromEpoch(node.modificationTime)
                    !modifyDate.isBefore(from) && !modifyDate.isAfter(to)
                }
            }
        }

        if (filterFunction == null) {
            return result
        }

        // when in search mode, index used by viewer is also index in all siblings,
        // but all siblings are image/video, non image/video nodes are filtered by previous step
        var indexInSiblings = 0
        for (node in nodes) {
            if (filterFunction.apply(node.second)) {
                result.add(Pair.create(indexInSiblings, node.second))
                indexInSiblings++
            }
        }
        return result
    }

    fun findOfflineNode(handle: String): MegaOffline? {
        return dbHandler.findByHandle(handle)
    }

    fun loadOfflineNodes(path: String, order: Int, searchQuery: String?): List<MegaOffline> {
        val nodes = if (searchQuery != null && searchQuery.isNotEmpty()) {
            searchOfflineNodes(path, searchQuery)
        } else {
            dbHandler.findByPath(path)
        }

        when (order) {
            ORDER_DEFAULT_DESC -> {
                sortOfflineByNameDescending(nodes)
            }
            ORDER_DEFAULT_ASC -> {
                sortOfflineByNameAscending(nodes)
            }
            ORDER_MODIFICATION_ASC -> {
                sortOfflineByModificationDateAscending(nodes)
            }
            ORDER_MODIFICATION_DESC -> {
                sortOfflineByModificationDateDescending(nodes)
            }
            ORDER_SIZE_ASC -> {
                sortOfflineBySizeAscending(nodes)
            }
            ORDER_SIZE_DESC -> {
                sortOfflineBySizeDescending(nodes)
            }
            else -> {
            }
        }
        return nodes
    }

    private fun searchOfflineNodes(path: String, query: String): ArrayList<MegaOffline> {
        val result = ArrayList<MegaOffline>()

        val nodes = dbHandler.findByPath(path)
        for (node in nodes) {
            if (node.isFolder) {
                result.addAll(searchOfflineNodes(getChildPath(node), query))
            }

            if (node.name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)) &&
                FileUtil.isFileAvailable(getOfflineFile(context, node))
            ) {
                result.add(node)
            }
        }

        return result
    }

    private fun getChildPath(offline: MegaOffline): String {
        return if (offline.path.endsWith(File.separator)) {
            offline.path + offline.name + File.separator
        } else {
            offline.path + File.separator + offline.name + File.separator
        }
    }

    companion object {
        const val CU_TYPE_CAMERA = 0
        const val CU_TYPE_MEDIA = 1
    }
}
