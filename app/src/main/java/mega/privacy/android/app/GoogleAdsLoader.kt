package mega.privacy.android.app

import android.app.Activity
import android.util.DisplayMetrics
import android.util.Log
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.doubleclick.PublisherAdRequest
import com.google.android.gms.ads.doubleclick.PublisherAdView

class GoogleAdsLoader(private val slotId: String, private var showImmediate: Boolean = true) :
    DefaultLifecycleObserver, AdUnitSource.Callback {
    private lateinit var adViewContainer: ViewGroup
    private lateinit var displayMetrics: DisplayMetrics
    private var initialLayoutComplete = false
    private var adView: PublisherAdView? = null
    private var unitId = AdUnitSource.INVALID_UNIT_ID

    private val adSize: AdSize
        get() {
            val density = displayMetrics.density

            var adWidthPixels = adViewContainer.width.toFloat()
            if (adWidthPixels == 0f) {
                adWidthPixels = displayMetrics.widthPixels.toFloat()
            }

            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationBannerAdSizeWithWidth(
                MegaApplication.getInstance(),
                adWidth
            )
        }

    private fun loadBanner(adSize: AdSize) {
        Log.i("Alex", "loadBanner, unitId=$unitId")
//        adView.adUnitId = BACKFILL_AD_UNIT_ID
        adView?.adUnitId = unitId
        adView?.setAdSizes(adSize)
        Log.i("Alex", "adSize$adSize")
        // Create an ad request.
        val adRequest = PublisherAdRequest.Builder().build()

        // Start loading the ad in the background.
        adView?.loadAd(adRequest)
    }

    private fun setUpBanner() {
        if (!showImmediate) return

        unitId = AdUnitSource.getAdUnitBySlot(slotId)
        if (unitId == AdUnitSource.INVALID_UNIT_ID) {
            AdUnitSource.addCallback(this)
            AdUnitSource.fetchAdUnits()
            return
        }

        adView = PublisherAdView(MegaApplication.getInstance())
        adView?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.i("Alex", "ad is loaded")
            }
        }

        adViewContainer.addView(adView)
        adViewContainer.viewTreeObserver.addOnGlobalLayoutListener {
            if (!initialLayoutComplete) {
                Log.i("Alex", "object:$this")
                initialLayoutComplete = true
                loadBanner(adSize)
            }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        if (owner is Activity) setUpBanner()
    }

    override fun onStart(owner: LifecycleOwner) {
        if (owner is Fragment) {
            setUpBanner()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        AdUnitSource.removeCallback(this)
        adView?.destroy()
    }

    override fun onPause(owner: LifecycleOwner) {
        adView?.pause()
    }

    override fun onResume(owner: LifecycleOwner) {
        adView?.resume()
    }

    fun setAdViewContainer(adViewContainer: ViewGroup, displayMetrics: DisplayMetrics) {
        this.adViewContainer = adViewContainer
        this.displayMetrics = displayMetrics
    }

    companion object {
        internal val BACKFILL_AD_UNIT_ID = "/30497360/adaptive_banner_test_iu/backfill"
    }

    override fun adUnitsFetched() {
        setUpBanner()
    }

    override fun queryShowAds(result: Int) {
        if (result == 1) {
            showImmediate = true
            setUpBanner()
        }
    }

    fun queryPublicHandle(handle: Long) {
        Log.i("Alex", "query handle:$handle")
        AdUnitSource.addCallback(this)
        AdUnitSource.showAdOrNot(handle)
    }
}