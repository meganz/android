package mega.privacy.android.app.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import mega.privacy.android.app.R
import org.jetbrains.anko.contentView
import org.jetbrains.anko.displayMetrics
import timber.log.Timber

/**
 * A helper class for showing highlight hint on certain UI elements.
 * Based on [PopupWindow].
 *
 * @param activity Current activity object.
 */
class HighLightHintHelper(private val activity: Activity) {

    /**
     * A full screen PopupWindow with a transparent background.
     */
    private var popupWindow: PopupWindow? = null

    /**
     * Location of the UI element that need to be highlighted.
     */
    private lateinit var targetLocation: Rect

    private var statusBarHeight = 0
    private var navigationBarHeight = 0

    /**
     * Width and height of the hint layout.
     */
    private val hintLayoutWidth: Int
    private val hintLayoutHeight: Int

    /**
     * Height of the cover layout for text hint, is a fixed value.
     */
    private val textCoverHeight: Int

    /**
     * Screen width and height of current device.
     */
    private val screenWidth: Int
    private val screenHeight: Int

    /**
     * Size of the white arrow icon.
     */
    private val arrowSize: Int

    init {
        statusBarHeight = getStatusBarHeight()
        navigationBarHeight =
                // Only on landscape and navigation bar may be on the left side need to consider the navigation bar's height.
                // If so need extra margin left the value is height of navigation bar.
            if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && navigationBarOnLeft()) {
                getNavigationBarHeight()
            } else {
                0
            }

        hintLayoutWidth = Util.dp2px(HINT_LAYOUT_WIDTH_DP)
        hintLayoutHeight = Util.dp2px(HINT_LAYOUT_HEIGHT_DP)
        textCoverHeight = Util.dp2px(TEXT_COVER_HEIGHT_DP)
        arrowSize = Util.dp2px(ARROW_SIZE_DP)

        screenWidth = activity.displayMetrics.widthPixels
        screenHeight = activity.displayMetrics.heightPixels
    }

    /**
     * Check if the navigation bar is on the left side of the screen when rotate to right.
     *
     * @return true if has navigation bar and it's on left.
     */
    private fun navigationBarOnLeft(): Boolean {
        val windowManager = activity.windowManager

        // No navigation bar shows.
        if (!hasNavigationBar()) return false

        // If it's a tablet.
        if (Util.isTablet(activity)) return false

        // If it's a HUAWEI device.
        if (MANUFACTURE_HUAWEI.equals(Build.MANUFACTURER, true)) return false

        // Rotate to right, navigation bar will stay at the left of the screen.
        @Suppress("DEPRECATION")
        return windowManager.defaultDisplay.rotation == Surface.ROTATION_270
    }

    /**
     * Get width of metrics and real metrics.
     *
     * @return A pair, the first element is real metrics, the second is metrics.
     */
    @Suppress("DEPRECATION")
    private fun getMetrics(): Pair<DisplayMetrics, DisplayMetrics> {
        val display = activity.windowManager.defaultDisplay

        val realDisplayMetrics = DisplayMetrics()
        display.getRealMetrics(realDisplayMetrics)

        val displayMetrics = DisplayMetrics()
        display.getMetrics(displayMetrics)

        return Pair(realDisplayMetrics, displayMetrics)
    }

    /**
     * Check if the device has navigation bar.
     *
     * @return true, has, false otherwise.
     */
    private fun hasNavigationBar(): Boolean {
        val pair = getMetrics()
        val realWidth = pair.first.widthPixels
        val width = pair.second.widthPixels

        return realWidth != width + statusBarHeight
    }

    /**
     * Show highlight hint on recent chats page.
     * Highlight the meeting icon on tool bar.
     *
     * @param targetViewId Meeting icon view's id for finding the view.
     * @param onDismissCallback Callback when the user press "Got it" and the highlight hint dismiss.
     */
    fun showHintForMeetingIcon(targetViewId: Int, onDismissCallback: () -> Unit) {
        // Find out the target view.
        val target = activity.findViewById<View>(targetViewId)
        if (target == null) {
            Timber.e("TargetView is null. Can't view with id $targetViewId in $activity")
            return
        }

        targetLocation = getLocationInParent(
            activity.contentView!!,
            target
        )

        // The content view of the PopupWindow.
        val contentView = getContentView()

        // Add cover, it's the highlight view covers on the target view.
        contentView.addView(getIconCover(), getIconCoverLayoutParams())
        // The arrow icon that connects two parts.
        contentView.addView(getArrow(), getIconArrowLayoutParams())
        contentView.addView(
            getHintView(onDismissCallback, R.string.tip_create_meeting),
            getIconHintLayoutParams()
        )

        // Show the full screen PopupWindow.
        popupWindow = PopupWindow(
            contentView,
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT,
            true
        ).apply {
            isClippingEnabled = false
            showAtLocation(activity.contentView!!, Gravity.NO_GRAVITY, 0, 0)
        }
    }

    /**
     * Dismiss the PopupWindow.
     */
    fun dismissPopupWindow() = popupWindow?.dismiss()

    /**
     * Get the content view of the PopupWindow,
     * other UI elements will be added on it as children views.
     *
     * It's a FrameLayout with transparent background.
     */
    private fun getContentView() = FrameLayout(activity).apply {
        // Mask color
        setBackgroundColor(getMaskColor())
    }

    /**
     * Get the layout of hint.
     *
     * @param onDismissCallback Callback when the user press "Got it" and the highlight hint dismiss.
     * @param hintTextId The string res id of the text shown on the layout.
     */
    @SuppressLint("InflateParams")
    private fun getHintView(onDismissCallback: () -> Unit, hintTextId: Int) =
        LayoutInflater.from(activity).inflate(R.layout.highlight_hint_meeting, null).apply {
            findViewById<TextView>(R.id.tv_tip).text =
                activity.getString(hintTextId)
            findViewById<View>(R.id.bt_ok).setOnClickListener {
                onDismissCallback.invoke()
            }
        }

    /**
     * Get the cover view for meeting icon.
     * It will cover on the target view to highlight it.
     */
    @SuppressLint("InflateParams")
    private fun getIconCover() = LayoutInflater.from(activity)
        .inflate(R.layout.highlight_cover_icon, null)

    /**
     * Get layout params that controls where to place the icon cover.
     */
    private fun getIconCoverLayoutParams() = FrameLayout.LayoutParams(
        targetLocation.right - targetLocation.left,
        targetLocation.bottom - targetLocation.top
    ).apply {
        leftMargin = targetLocation.left + navigationBarHeight
        topMargin = targetLocation.top + statusBarHeight
    }

    /**
     * Get layout params that controls where to place the text cover.
     */
    private fun getTextCoverLayoutParams() = FrameLayout.LayoutParams(
        targetLocation.right - targetLocation.left + arrowSize,
        textCoverHeight
    ).apply {
        leftMargin = targetLocation.left
        topMargin =
            targetLocation.top + statusBarHeight - (textCoverHeight - (targetLocation.bottom - targetLocation.top)) / 2
    }

    /**
     * Get arrow icon view.
     */
    fun getArrow() = View(activity).apply {
        background =
            ContextCompat.getDrawable(activity, R.drawable.background_meeting_hint_up_arrow)
    }

    /**
     * Get layout params that controls where to place the arrow icon for meeting icon cover.
     */
    private fun getIconArrowLayoutParams() = FrameLayout.LayoutParams(
        arrowSize,
        arrowSize
    ).apply {
        // Point to the middle of the cover.
        leftMargin =
            targetLocation.left + (targetLocation.right - targetLocation.left) / 2 - arrowSize / 2 + navigationBarHeight
        topMargin = statusBarHeight + targetLocation.bottom
    }

    /**
     * Get layout params that controls where to place the arrow icon for new meeting text cover.
     */
    private fun getTextArrowLayoutParams() = FrameLayout.LayoutParams(
        arrowSize,
        arrowSize
    ).apply {
        // Point to the middle of the cover.
        leftMargin =
            targetLocation.left + (targetLocation.right - targetLocation.left) / 2 - arrowSize / 2
        topMargin =
            targetLocation.bottom + statusBarHeight + (textCoverHeight - (targetLocation.bottom - targetLocation.top)) / 2
    }

    /**
     * Get layout params that controls where to place the text hint.
     */
    private fun getTextHintLayoutParams() = FrameLayout.LayoutParams(
        hintLayoutWidth,
        FrameLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        val arrowLeft =
            targetLocation.left + (targetLocation.right - targetLocation.left) / 2 - arrowSize / 2

        // arrowLeft / 6 is a bit of offset.
        leftMargin = arrowLeft / 6 + navigationBarHeight

        topMargin =
            targetLocation.bottom + statusBarHeight + arrowSize + (textCoverHeight - (targetLocation.bottom - targetLocation.top)) / 2
    }

    /**
     * Get layout params that controls where to place the icon hint.
     */
    private fun getIconHintLayoutParams() = FrameLayout.LayoutParams(
        hintLayoutWidth,
        hintLayoutHeight
    ).apply {
        val arrowLeft =
            targetLocation.left + (targetLocation.right - targetLocation.left) / 2 - arrowSize / 2
        val exceedsPart = hintLayoutWidth + arrowLeft - screenWidth

        leftMargin = if (exceedsPart >= 0) {
            // Exceeds screen end. (screenWidth - arrowLeft - arrowSize) / 4 is a bit of offset.
            arrowLeft - exceedsPart - (screenWidth - arrowLeft - arrowSize) / 4 + navigationBarHeight
        } else {
            // arrowLeft / 4 is a bit of offset.
            arrowLeft / 4 + navigationBarHeight
        }

        topMargin = targetLocation.bottom + statusBarHeight + arrowSize
    }

    /**
     * Show highlight hint on start conversation page.
     * Highlight the "NEW MEETING" text view.
     *
     * @param targetViewId Text view's id that shows "NEW MEETING", for finding the view.
     * @param onDismissCallback Callback when the user press "Got it" and the highlight hint dismiss.
     */
    fun showHintForMeetingText(targetViewId: Int, onDismissCallback: () -> Unit) {
        val target = activity.findViewById<View>(targetViewId)
        // Can't find target view or it is invisible.
        if (target == null || (target.width == 0 && target.height == 0)) {
            Timber.e("TargetView is null. Can't view with id $targetViewId in $activity")
            return
        }

        targetLocation = getLocationInParent(
            activity.contentView!!,
            target
        )

        targetLocation.left = targetLocation.left + navigationBarHeight
        targetLocation.right = targetLocation.right + navigationBarHeight

        val contentView = getContentView()
        contentView.addView(getTextCover(), getTextCoverLayoutParams())
        contentView.addView(getArrow(), getTextArrowLayoutParams())
        contentView.addView(
            getHintView(onDismissCallback, R.string.tip_setup_meeting),
            getTextHintLayoutParams()
        )

        popupWindow = PopupWindow(
            contentView,
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT,
            true
        ).apply {
            isClippingEnabled = false
            showAtLocation(activity.contentView, Gravity.NO_GRAVITY, 0, 0)
        }
    }

    /**
     * Get the cover view new meeting text.
     * It will cover on the target view to highlight it.
     */
    @SuppressLint("InflateParams")
    private fun getTextCover() = LayoutInflater.from(activity)
        .inflate(R.layout.highlight_cover_text, null)

    /**
     * Get status bar height on the device.
     */
    private fun getStatusBarHeight() =
        getSystemBarHeight(KEY_STATUS_BAR)

    /**
     * Get navigation bar height on the device.
     */
    private fun getNavigationBarHeight() =
        getSystemBarHeight(KEY_NAVIGATION_BAR)

    /**
     * Get system bar height on the device.
     */
    private fun getSystemBarHeight(name: String): Int {
        val resource = activity.applicationContext.resources
        // "dimen", "android" are fixed string from Android system.
        val resId = resource.getIdentifier(name, "dimen", "android")
        return resource.getDimensionPixelSize(resId)
    }

    /**
     * Get target view's location on its parent view.
     *
     * @param parent Parent view.
     * @param child Target view which will be highlighted.
     *
     * @return Rect which represents the target view's location.
     */
    private fun getLocationInParent(parent: View, child: View): Rect {
        var decorView: View? = null

        val activity = child.context
        if (activity is Activity) {
            decorView = activity.window.decorView
        }

        val result = Rect()
        val tmpRect = Rect()
        var tmp: View = child
        if (child === parent) {
            child.getHitRect(result)
            return result
        }

        while (tmp !== decorView && tmp !== parent) {
            tmp.getHitRect(tmpRect)
            if (tmp.javaClass.simpleName != NO_SAVE_STATE_FRAME_LAYOUT) {
                result.left += tmpRect.left
                result.top += tmpRect.top
            }

            tmp = tmp.parent as View
        }

        result.right = result.left + child.measuredWidth
        result.bottom = result.top + child.measuredHeight
        return result
    }

    /**
     * Get the background for the PopupWindow's content view.
     */
    private fun getMaskColor() = ContextCompat.getColor(activity, R.color.grey_alpha_032)

    companion object {
        /**
         * Keys from retrieving system resource value.
         */
        const val KEY_STATUS_BAR = "status_bar_height"
        const val KEY_NAVIGATION_BAR = "navigation_bar_height"
        const val NO_SAVE_STATE_FRAME_LAYOUT = "NoSaveStateFrameLayout"

        const val MANUFACTURE_HUAWEI = "HUAWEI"
        const val ARROW_SIZE_DP = 16f
        const val HINT_LAYOUT_WIDTH_DP = 290f
        const val HINT_LAYOUT_HEIGHT_DP = 75f
        const val TEXT_COVER_HEIGHT_DP = 56f
    }
}