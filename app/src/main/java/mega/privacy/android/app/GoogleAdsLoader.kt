package mega.privacy.android.app

import android.app.Activity
import android.util.DisplayMetrics
import android.util.Log
import android.util.LogPrinter
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.doubleclick.PublisherAdRequest
import com.google.android.gms.ads.doubleclick.PublisherAdView

class GoogleAdsLoader(
    private val lifecycleOwner: LifecycleOwner,
    private val adViewContainer: ViewGroup,
    private val displayMetrics: DisplayMetrics,
) : LifecycleObserver {

    private var initialLayoutComplete = false
    private lateinit var adView: PublisherAdView

    private val adSize : AdSize
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

    init {
        Log.i("Alex", "addObserver")
        lifecycleOwner.lifecycle.addObserver(this)
    }

    private fun loadBanner(adSize: AdSize) {
        Log.i("Alex", "loadBanner")
        adView.adUnitId = BACKFILL_AD_UNIT_ID
        adView.setAdSizes(adSize)
        Log.i("Alex", "adSize$adSize")
        // Create an ad request.
        val adRequest = PublisherAdRequest.Builder().build()

        // Start loading the ad in the background.
        adView.loadAd(adRequest)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
        if (lifecycleOwner is Fragment) setUpBanner()
    }

    private fun setUpBanner() {
        adView = PublisherAdView(MegaApplication.getInstance())
        adView.adListener = object : AdListener() {
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

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun create() {
        if (lifecycleOwner is Activity) setUpBanner()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroy() {
        adView.destroy()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun pause() {
        adView.pause()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun resume() {
        adView.resume()
    }

    companion object {
        internal val BACKFILL_AD_UNIT_ID = "/30497360/adaptive_banner_test_iu/backfill"

        fun bindGoogleAdsLoader(
            lifecycleOwner: LifecycleOwner,
            adViewContainer: ViewGroup,
            displayMetrics: DisplayMetrics
        ) {
            GoogleAdsLoader(lifecycleOwner, adViewContainer, displayMetrics)
        }
    }
}