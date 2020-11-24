package mega.privacy.android.app

import android.util.Log
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.utils.TimeUtils
import nz.mega.sdk.*
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaError.*

/**
 * Provide interfaces for fetching/querying Ad unit ids and user status
 */
object AdUnitSource : BaseListener(MegaApplication.getInstance()) {

    // The value indicates the unit ID hasn't been fetched or outdated
    const val INVALID_UNIT_ID = "invalid_unit_id"

    // Currently, only 5 screens has 1 Ad banner view for each
    private const val SLOT_NUM = 5

    // Ad Flag (see API doc)
    private const val AD_FLAG = 512

    // The time threshold for requiring a new fetch/query
    private const val TIME_THRESHOLD = TimeUtils.HOUR * 6

    private val megaApi = MegaApplication.getInstance().megaApi

    // True if the async fetch is in progress
    private var fetching = false

    // True if the async querying is in progress
    private var querying = false

    // The map of ad slot id to ad unit id. Given by the server
    private var adUnitMap = mutableMapOf<String, String>()

    // True if need showing Ads for the user, false otherwise
    private var isAdsUser = true

    // Last fetch/query time in ms
    private var lastFetchTime: Long = 0
    private var lastQueryTime: Long = 0

    // The set of FetchCallbacks for fetching ad unit ids
    private var callbacks = mutableSetOf<FetchCallback>()

    // The QueryCallback for querying whether showing the Ad or not
    private var queryShowOrNotCallback: QueryShowOrNotCallback? = null

    // The memory cache to save the query result, for reducing the API request times
    // and shortening the delay of showing the Ad. A map of public handle to result value.
    private val handleQueryCache = mutableMapOf<Long, Int>()

    /**
     * The callback of fetching ad units
     */
    interface FetchCallback {
        fun adUnitsFetched()
    }

    /**
     * The callback of querying whether show the Ad or not
     */
    interface QueryShowOrNotCallback {
        fun queryShowOrNotDone(result: Int)
    }

    fun setQueryShowOrNotCallback(cb: QueryShowOrNotCallback?) {
        queryShowOrNotCallback = cb
    }

    fun addFetchCallback(cb: FetchCallback) {
        callbacks.add(cb)
    }

    fun removeFetchCallback(cb: FetchCallback) {
        callbacks.remove(cb)
    }

    /**
     * Get whether should show Ad or not for current user.
     *
     * @return false for fast-fail, true indicates that Ad should be shown, or
     * the client should re-fetch the user status
     */
    fun isAdsUser(): Boolean {
        if (needRequery(lastFetchTime)) isAdsUser = true
        return isAdsUser
    }

    /**
     * Get the ad unit id
     *
     * @param slotId the ad slot id
     * @return the ad unit id
     */
    fun getAdUnitBySlot(slotId: String): String {
        if (needRequery(lastFetchTime)) return INVALID_UNIT_ID
        return adUnitMap[slotId] ?: ""
    }

    /**
     * Fetch from the server about the ad unit ids and whether should show Ads for the user
     */
    fun fetchAdUnits() {
        // Return immediately if the fetch is already in progress
        if (fetching) {
            Log.i("Alex", "fetching in progress, return")
            return
        }

        Log.i("Alex", "Start to fetch")
        fetching = true

        // Android ad slot ids for each screen has the form of "and1", "and2", and so forth
        val adSlotIds = MegaStringList.createInstance()
        for (i in 0..SLOT_NUM) {
            adSlotIds.add("and".plus(i))
        }

        megaApi.fetchGoogleAds(AD_FLAG, adSlotIds, INVALID_HANDLE, this)
    }

    /**
     * Query the server by the public handle if should show Ads for the shared file
     * Typically, the file/folder shared by Pro user should not show any Ads even if
     * the receiver is an Ads user
     *
     * @param publicHandle the public handle of the shared file/folder
     */
    fun queryShowOrNotByHandle(publicHandle: Long) {
        if (publicHandle == INVALID_HANDLE || querying) return

        val cachedResult = handleQueryCache[publicHandle]

        if (!needRequery(lastQueryTime) && cachedResult != null) {
            queryShowOrNotCallback?.queryShowOrNotDone(cachedResult)
        } else {
            querying = true
            megaApi.queryGoogleAds(AD_FLAG, publicHandle, this)
        }
    }

    /**
     * Return if need a re-query of Ads info from server
     * This is for throttling the requests to the server
     *
     * @param lastTime The time of the last request in milliseconds
     */
    private fun needRequery(lastTime: Long) =
        System.currentTimeMillis() - lastTime > TIME_THRESHOLD


    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        when (request.type) {
            MegaRequest.TYPE_FETCH_GOOGLE_ADS -> {
                fetching = false
                lastFetchTime = System.currentTimeMillis()
                Log.i("Alex", "onRequestFinish:$e")
                when (e.errorCode) {
                    API_OK -> {
                        copyAdUnitsMap(request.megaStringMap)
//                        adUnitMap = MegaStringMap.createInstance()
//                        adUnitMap!!.set("and0", "/30497360/adaptive_banner_test_iu/backfill")
//                        adUnitMap!!.set("and1", "/30497360/adaptive_banner_test_iu/backfill")
//                        adUnitMap!!.set("and2", "/30497360/adaptive_banner_test_iu/backfill")
//                        adUnitMap!!.set("and3", "/30497360/adaptive_banner_test_iu/backfill")
//                        adUnitMap!!.set("and4", "/30497360/adaptive_banner_test_iu/backfill")
//                        adUnitMap!!.set("and5", "/30497360/adaptive_banner_test_iu/backfill")

                        if (adUnitMap.isEmpty()) return
                        callBackForFetch()
                    }
                    // -9 for the user is a non-Ad user (here SDK returns -9 for API -9)
                    API_ENOENT -> {
                        Log.i("Alex", "not ads user")
                        adUnitMap.clear()
                        isAdsUser = false
                        callBackForFetch()
                    }
                }
            }

            MegaRequest.TYPE_QUERY_GOOGLE_ADS -> {
                querying = false
                lastQueryTime = System.currentTimeMillis()

                if (e.errorCode == API_OK) {
                    val result = request.numDetails
                    handleQueryCache[request.nodeHandle] = result
                    queryShowOrNotCallback?.queryShowOrNotDone(result)
                    Log.i("Alex", "showAdOrNot:${result}")
                }
            }
        }
    }

    /**
     * Call callbacks when the fetching operation has done
     */
    private fun callBackForFetch() {
        for (cb in callbacks) {
            cb.adUnitsFetched()
        }
    }

    /**
     * Copy the Ad slot/unit Id map from SDK returned MegaStringMap to a local
     * map. Because directly using the SDK MegaStringMap may cause some crash.
     */
    private fun copyAdUnitsMap(stringMap: MegaStringMap) {
        adUnitMap.clear()
        val keys = stringMap.keys

        for (i in 0..keys.size()) {
            if (keys[i] != null) {
                adUnitMap[keys[i]] = stringMap.get(keys[i])
            }
        }
    }
}