package mega.privacy.android.app.service.ads

import android.content.Context
import android.util.DisplayMetrics
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver

/**
 * Stub for product flavor compilation
 */
@SuppressWarnings("unused")
class GoogleAdsLoader(
    private val context: Context,
    private val slotId: String,
    private var loadImmediate: Boolean = true
) : DefaultLifecycleObserver {

    @SuppressWarnings("unused")
    fun setAdViewContainer(adViewContainer: ViewGroup, displayMetrics: DisplayMetrics) {}

    @SuppressWarnings("unused")
    fun queryShowOrNotByHandle(handle: Long) {}
}