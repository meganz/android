package mega.privacy.android.app.service.ads

import android.content.Context
import android.util.DisplayMetrics
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver

/**
 * Stub for product flavor compilation
 */
@Suppress("UNUSED_PARAMETER")
class GoogleAdsLoader(
    private val context: Context,
    private val slotId: String,
    private var loadImmediate: Boolean = true
) : DefaultLifecycleObserver {

    fun setAdViewContainer(adViewContainer: ViewGroup, displayMetrics: DisplayMetrics) {}

    fun queryShowOrNotByHandle(handle: Long) {}
}