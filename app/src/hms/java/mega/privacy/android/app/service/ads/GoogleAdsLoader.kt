package mega.privacy.android.app.service.ads

import android.content.Context
import android.util.DisplayMetrics
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver

/**
 * Stub for product flavor compilation
 */
@Suppress("unused")
class GoogleAdsLoader(
    private val context: Context,
    private val slotId: String,
    private var loadImmediate: Boolean = true
) : DefaultLifecycleObserver {

    @Suppress("unused")
    fun setAdViewContainer(adViewContainer: ViewGroup, displayMetrics: DisplayMetrics) {}

    @Suppress("unused")
    fun queryShowOrNotByHandle(handle: Long) {}
}