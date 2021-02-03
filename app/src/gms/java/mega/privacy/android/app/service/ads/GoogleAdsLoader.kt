package mega.privacy.android.app.service.ads

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.DisplayMetrics
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerAdView
import mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.TextUtil

/**
 * Load Google advertisement adaptive banners to an Activity/Fragment.
 * Create a GoogleAdsLoader and associate it with a Ads Slot ID(predefined by Mega API)
 * Then set the Ads view container to this Ads loader
 * Act as a Activity/Fragment LifecycleObserver to pause/resume/destroy the AdManagerAdView in time
 * https://developers.google.com/ad-manager/mobile-ads-sdk/android/banner/adaptive
 * https://github.com/googleads/googleads-mobile-android-examples/tree/master/kotlin/admanager/AdaptiveBannerExample
 */
class GoogleAdsLoader(
    private val context: Context,
    // The ad slot id, e.g. "and1", "and2"
    private val slotId: String,
    // Load the Ad immediately if true, false indicates some conditions need to be checked in advance
    // regarding whether should load the ad or not, e.g. Public Handle of link file/folder
    // see queryShowOrNotByHandle()
    private var loadImmediate: Boolean = true
) : DefaultLifecycleObserver, AdUnitSource.FetchCallback, AdUnitSource.QueryShowOrNotCallback {

    // The view container of the Ad banner view
    private lateinit var adViewContainer: ViewGroup

    // Display metrics for calculating the Ad size
    private lateinit var displayMetrics: DisplayMetrics

    private var initialLayoutComplete = false

    // The Ad banner view
    private var adView: AdManagerAdView? = null

    // The unit Id of the Ad going to be loaded
    private var adUnitId = AdUnitSource.INVALID_UNIT_ID

    // The unit id of the current successfully showed AD
    private var showedAdUnitId = AdUnitSource.INVALID_UNIT_ID

    // Whether the ads view container was set
    private var isContainerSet = false

    private val adSize: AdSize
        get() {
            val density = displayMetrics.density

            var adWidthPixels = adViewContainer.width.toFloat()
            if (adWidthPixels == 0f) {
                adWidthPixels = displayMetrics.widthPixels.toFloat()
            }

            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                context,
                adWidth
            )
        }

    /**
     * Load the Ad into the banner view
     *
     * @param adSize the size of the banner view
     */
    private fun loadBanner(adSize: AdSize) {
        adView?.adUnitId = adUnitId
        adView?.setAdSizes(adSize)
        // Create an ad request.
        val adRequest = AdManagerAdRequest.Builder().build()

        // Start loading the ad in the background.
        adView?.loadAd(adRequest)
    }

    private fun setUpBanner() {
        if (!isContainerSet) return

        // If the user is not an Ad user any more, remove the Ad view if any and then return
        if (!AdUnitSource.isAdsUser()) {
            adViewContainer.removeAllViews()
            return
        }

        // Don't move forward until the loadImmediate flag is set to true by callbacks (e.g. queryCallback)
        if (!loadImmediate) return

        // Get the ad unit Id by the slot id, fetch it from the server
        // if had never been fetched or outdated
        val adUnitId = AdUnitSource.getAdUnitBySlot(slotId)
        if (adUnitId == AdUnitSource.INVALID_UNIT_ID) {
            AdUnitSource.fetchAdUnits()
            return
        }

        // Return if the unit id is invalid or the same Ad had been loaded
        if (TextUtil.isTextEmpty(adUnitId)
            || (adViewContainer.childCount != 0 && showedAdUnitId == adUnitId)
        ) return
        this.adUnitId = adUnitId

        adViewContainer.removeAllViews()
        adView = AdManagerAdView(context)
        adView?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                showedAdUnitId = this@GoogleAdsLoader.adUnitId
            }

            override fun onAdFailedToLoad(error: LoadAdError?) {
                super.onAdFailedToLoad(error)
                logWarning("Ads failed to load:$error")
            }
        }
        adViewContainer.addView(adView)
        adViewContainer.viewTreeObserver.addOnGlobalLayoutListener {
            if (!initialLayoutComplete) {
                initialLayoutComplete = true
                loadBanner(adSize)
            }
        }

        if (initialLayoutComplete) loadBanner(adSize)
    }

    override fun onCreate(owner: LifecycleOwner) {
        // Register a broadcast receiver for the changing of the user account
        // Re-fetch the ad unit ids and Ad user status if account changed (e.g. a free user
        // has just upgraded to pro user, then should not show Ads)
        context.registerReceiver(
            updateAccountDetailsReceiver,
            IntentFilter(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
        )
    }

    override fun onStart(owner: LifecycleOwner) {
        // The Ad loader is interested in the update of the ad units
        // and the status change of the user
        AdUnitSource.addFetchCallback(this)
        // Start to set up the Ad banner view, provided that the view container
        // and the display metrics have been set. Put it at onStart() make the screen has
        // more chances to update the ad unit ids and the user status in time
        setUpBanner()
    }

    override fun onStop(owner: LifecycleOwner) {
        AdUnitSource.removeFetchCallback(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        adView?.destroy()

        AdUnitSource.setQueryShowOrNotCallback(null)
        context.unregisterReceiver(updateAccountDetailsReceiver)
    }

    override fun onPause(owner: LifecycleOwner) {
        adView?.pause()
    }

    override fun onResume(owner: LifecycleOwner) {
        adView?.resume()
    }

    /**
     * The Activity/Fragment set the container of the Ad view, as well as the display metrics
     * Typically, Activity call this method from its onCreate(), while fragment call it from
     * onCreateView()
     *
     * @param adViewContainer the container (View group) of the Ad banner view
     * @param displayMetrics the display metrics
     */
    fun setAdViewContainer(adViewContainer: ViewGroup, displayMetrics: DisplayMetrics) {
        this.adViewContainer = adViewContainer
        this.displayMetrics = displayMetrics
        isContainerSet = true
    }

    override fun adUnitsFetched() {
        setUpBanner()
    }

    /**
     * Query the server about whether should show Ads or not for the specific public handle.
     * For example, if the file/folder link is shared by a Pro user, it should never show the Ads
     *
     * @param handle the public handle of the link
     */
    fun queryShowOrNotByHandle(handle: Long) {
        AdUnitSource.setQueryShowOrNotCallback(this)
        AdUnitSource.queryShowOrNotByHandle(handle)
    }

    override fun queryShowOrNotDone(result: Int) {
        if (result != SHOW_AD_FOR_USER) return

        loadImmediate = true
        setUpBanner()
    }

    /** The account update receiver, for re-fetching the Ad units and user status */
    private val updateAccountDetailsReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            AdUnitSource.fetchAdUnits()
        }
    }

    companion object {
        private const val SHOW_AD_FOR_USER = 0
    }
}