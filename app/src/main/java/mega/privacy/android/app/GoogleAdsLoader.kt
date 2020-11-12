package mega.privacy.android.app

import android.util.DisplayMetrics
import android.util.Log
import android.view.ViewGroup
import androidx.lifecycle.*
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.doubleclick.PublisherAdRequest
import com.google.android.gms.ads.doubleclick.PublisherAdView

class GoogleAdsLoader(private val slotId: String, private var loadImmediate: Boolean = true) :
    DefaultLifecycleObserver, AdUnitSource.FetchCallback, AdUnitSource.QueryCallback {

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
        adView?.adUnitId = unitId
        adView?.setAdSizes(adSize)
        Log.i("Alex", "adSize$adSize")
        // Create an ad request.
        val adRequest = PublisherAdRequest.Builder().build()

        // Start loading the ad in the background.
        adView?.loadAd(adRequest)
        Log.i("Alex", "loadbanner end")
    }

    private fun setUpBanner() {
        Log.i("Alex", "setupbanner begin")
        if (!AdUnitSource.isAdsUser() || !loadImmediate) return

        val unitId = AdUnitSource.getAdUnitBySlot(slotId)
        if (unitId == AdUnitSource.INVALID_UNIT_ID) {
            AdUnitSource.addCallback(this)
            AdUnitSource.fetchAdUnits()
            return
        }

        if (this.unitId == unitId) return  // The same ad has been loaded
        this.unitId = unitId

        Log.i("Alex", "new PublisherAdView")
        adViewContainer.removeAllViews()
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
        Log.i("Alex", "onCreate")
    }

    override fun onStart(owner: LifecycleOwner) {
        Log.i("Alex", "onStart")
        setUpBanner()
    }

    override fun onStop(owner: LifecycleOwner) {
        Log.i("Alex", "onStop")
        AdUnitSource.removeCallback(this)
        AdUnitSource.setQueryCallback(null)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        adView?.destroy()
    }

    override fun onPause(owner: LifecycleOwner) {
        adView?.pause()
    }

    override fun onResume(owner: LifecycleOwner) {
        Log.i("Alex", "onResume")
        adView?.resume()
    }

    fun setAdViewContainer(adViewContainer: ViewGroup, displayMetrics: DisplayMetrics) {
        this.adViewContainer = adViewContainer
        this.displayMetrics = displayMetrics
    }

    override fun adUnitsFetched() {
        setUpBanner()
    }

    fun queryShowOrNotByHandle(handle: Long) {
        Log.i("Alex", "query handle:$handle")
        AdUnitSource.setQueryCallback(this)
        AdUnitSource.queryShowOrNot(handle)
    }

    override fun queryShowOrNotDone(result: Int) {
        if (result == 0) {
            loadImmediate = true
            setUpBanner()
        }
    }
}