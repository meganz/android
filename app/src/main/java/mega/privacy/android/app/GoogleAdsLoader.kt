package mega.privacy.android.app

import android.util.DisplayMetrics
import android.util.Log
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.doubleclick.PublisherAdRequest
import com.google.android.gms.ads.doubleclick.PublisherAdView

class GoogleAdsLoader(
    lifecycleOwner: LifecycleOwner,
    private val adViewContainer: ViewGroup,
    private val displayMetrics: DisplayMetrics
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
        adView = PublisherAdView(MegaApplication.getInstance())
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.i("Alex", "ad is loaded")
            }
        }

        adViewContainer.addView(adView)
        adViewContainer.viewTreeObserver.addOnGlobalLayoutListener {
            if (!initialLayoutComplete) {
                initialLayoutComplete = true
                loadBanner(adSize)
            }
        }
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