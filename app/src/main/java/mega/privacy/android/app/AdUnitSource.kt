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

    private var callbacks = mutableSetOf<Callback>()

    interface Callback {
        fun adUnitsFetched()
        fun queryShowAds(result: Int)
    }

    fun addCallback(cb: Callback) {
        callbacks.add(cb)
    }

    fun removeCallback(cb: Callback) {
        callbacks.remove(cb)
    }
    fun getAdUnitBySlot(slotId: String): String {
        if (System.currentTimeMillis() - lastFetchTime > TimeUtils.DAY) return INVALID_UNIT_ID
        return adUnitMap?.get(slotId) ?: INVALID_UNIT_ID
    }

    fun fetchAdUnits() {
        if (fetching)  {
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

        megaApi.queryGoogleAds(512, publicHandle, this)
    }

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
                    if (e.errorCode == API_OK) {
                        lastFetchTime = System.currentTimeMillis()
//                        adUnitMap = request.megaStringMap
                        adUnitMap = MegaStringMap.createInstance()
                        adUnitMap!!.set("and0", "/30497360/adaptive_banner_test_iu/backfill")
                        adUnitMap!!.set("and1", "/30497360/adaptive_banner_test_iu/backfill")
                        adUnitMap!!.set("and2", "/30497360/adaptive_banner_test_iu/backfill")
                        adUnitMap!!.set("and3", "/30497360/adaptive_banner_test_iu/backfill")
                        adUnitMap!!.set("and4", "/30497360/adaptive_banner_test_iu/backfill")
                        adUnitMap!!.set("and5", "/30497360/adaptive_banner_test_iu/backfill")

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
                }
            }

            MegaRequest.TYPE_QUERY_GOOGLE_ADS -> {
                e?.let {
                    if (e.errorCode == API_OK) {
                        for (cb in callbacks) {
                            Log.i("Alex", "showAdOrNot:${request.numDetails}")
//                            cb.queryShowAds(request.numDetails)
                            cb.queryShowAds(1)
                        }
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