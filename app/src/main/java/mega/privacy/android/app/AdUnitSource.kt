package mega.privacy.android.app

import android.util.Log
import mega.privacy.android.app.utils.TimeUtils
import nz.mega.sdk.*
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaError.*

object AdUnitSource : MegaRequestListenerInterface {
    const val INVALID_UNIT_ID = "invalid_unit_id"

    private var fetching = false
    private var adUnitMap: MegaStringMap? = null
    private val megaApi = MegaApplication.getInstance().megaApi
    private var lastFetchTime: Long = 0
    private var lastQueryTime: Long = 0
    private var isAdsUser = true

    private var callbacks = mutableSetOf<FetchCallback>()
    private var queryCallback: QueryCallback? = null

    private val handleQueryCache = mutableMapOf<Long, Int>()

    interface FetchCallback {
        fun adUnitsFetched()
    }

    interface QueryCallback {
        fun queryShowAdsDone(result: Int)
    }

    fun setQueryCallback(cb: QueryCallback?) {
        queryCallback = cb
    }

    fun addCallback(cb: FetchCallback) {
        callbacks.add(cb)
    }

    fun removeCallback(cb: FetchCallback) {
        callbacks.remove(cb)
    }

    fun isAdsUser(): Boolean {
        if (needRequery(lastFetchTime)) isAdsUser = true
        return isAdsUser  // true to trigger the query
    }

    fun getAdUnitBySlot(slotId: String): String {
        if (needRequery(lastFetchTime)) return INVALID_UNIT_ID
        return adUnitMap?.get(slotId) ?: INVALID_UNIT_ID
    }

    fun fetchAdUnits() {
        if (fetching) {
            Log.i("Alex", "fetching in progress, return")
            return
        }

        fetching = true

        val adSlotIds = MegaStringList.createInstance()
        adSlotIds.add("and0")
        adSlotIds.add("and1")
        adSlotIds.add("and2")
        adSlotIds.add("and3")
        adSlotIds.add("and4")
        adSlotIds.add("and5")

        Log.i("Alex", "startcall")
        megaApi.fetchGoogleAds(512, adSlotIds, INVALID_HANDLE, this)
        Log.i("Alex", "endcall")
    }

    fun showAdOrNot(publicHandle: Long) {
        if (publicHandle == INVALID_HANDLE) return

        val cacheResult = handleQueryCache[publicHandle]
        if (!needRequery(lastQueryTime) && cacheResult != null) {
            queryCallback?.queryShowAdsDone(cacheResult)
        } else {
            megaApi.queryGoogleAds(512, publicHandle, this)
        }
    }

    private fun needRequery(lastTime: Long) = System.currentTimeMillis() - lastTime > TimeUtils.DAY

    override fun onRequestStart(api: MegaApiJava?, request: MegaRequest?) {
        Log.i("Alex", "onRequestStart")
    }

    override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {
        Log.i("Alex", "onRequestUpdate")
    }

    override fun onRequestFinish(api: MegaApiJava?, request: MegaRequest?, e: MegaError?) {
        when (request?.type) {
            MegaRequest.TYPE_FETCH_GOOGLE_ADS -> {
                fetching = false
                e?.let {
                    lastFetchTime = System.currentTimeMillis()

                    when (e.errorCode) {
                        API_OK -> {
                        adUnitMap = request.megaStringMap
//                            adUnitMap = MegaStringMap.createInstance()
//                            adUnitMap!!.set("and0", "/30497360/adaptive_banner_test_iu/backfill")
//                            adUnitMap!!.set("and1", "/30497360/adaptive_banner_test_iu/backfill")
//                            adUnitMap!!.set("and2", "/30497360/adaptive_banner_test_iu/backfill")
//                            adUnitMap!!.set("and3", "/30497360/adaptive_banner_test_iu/backfill")
//                            adUnitMap!!.set("and4", "/30497360/adaptive_banner_test_iu/backfill")
//                            adUnitMap!!.set("and5", "/30497360/adaptive_banner_test_iu/backfill")

                            for (cb in callbacks) {
                                cb.adUnitsFetched()
                            }
//                        Log.i("Alex", "size=" + a?.size())
//                        val b = a?.keys
//                        for (i in 0..7) {
////
//                            Log.i("Alex", "key=" + (b?.get(i) ?: -1))
//                            Log.i("Alex", "unit id=" + a?.get(b?.get(i)))
//                        }
                        }

                        API_ENOENT -> {
                            isAdsUser = false
                        }

                        else -> {

                        }
                    }
                }
            }

            MegaRequest.TYPE_QUERY_GOOGLE_ADS -> {
                lastQueryTime = System.currentTimeMillis()
                e?.let {
                    if (e.errorCode == API_OK) {
                        val result = request.numDetails
                        handleQueryCache[request.nodeHandle] = result
                        queryCallback?.queryShowAdsDone(result)
                        Log.i("Alex", "showAdOrNot:${result}")
//                            cb.queryShowAds(request.numDetails)
                    }
                }
            }
        }
    }

    override fun onRequestTemporaryError(
        api: MegaApiJava?,
        request: MegaRequest?,
        e: MegaError?
    ) {
        Log.i("Alex", "onRequestTemporaryError")
    }
}