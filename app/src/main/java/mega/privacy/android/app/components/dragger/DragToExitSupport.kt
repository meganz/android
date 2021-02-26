package mega.privacy.android.app.components.dragger

import android.content.Context
import android.content.Intent
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import java.util.*

/**
 * Class that encapsulate all logic related to drag to exit support, and the enter animation.
 *
 * It uses LiveEventBus to decouple thumbnail display screen, e.g. OfflineFragment, and
 * draggable view display screen, e.g. MediaPlayerActivity.
 *
 * @param context Android context
 * @param dragActivated callback for drag event, parameter will be true when drag is activated
 * @param fadeOutFinishCallback callback to fade out and finish activity
 */
class DragToExitSupport(
    private val context: Context,
    private val dragActivated: (Boolean) -> Unit,
    private val fadeOutFinishCallback: () -> Unit
) : DraggableView.DraggableListener, ViewAnimator.Listener {
    private var draggableView: DraggableView? = null
    private var ivShadow: ImageView? = null

    private var currentHandle = INVALID_HANDLE

    /**
     * Wrap content view with draggable view.
     *
     * @param layoutResID the content view layout resource id
     * @return the wrapped view, should be set as Activity content view
     */
    fun wrapContentView(@LayoutRes layoutResID: Int): View {
        return wrapContentView(LayoutInflater.from(context).inflate(layoutResID, null))
    }

    /**
     * Wrap content view with draggable view.
     *
     * @param contentView the content view
     * @return the wrapped view, should be set as Activity content view
     */
    fun wrapContentView(contentView: View): View {
        val container = FrameLayout(context)

        val draggable = DraggableView(context, this)
        draggable.setDraggableListener(this)
        draggable.setViewAnimator(ExitViewAnimator())
        draggable.addView(contentView)

        val shadow = ImageView(context)
        shadow.setBackgroundColor(ContextCompat.getColor(context, R.color.black_p50));

        container.addView(
            shadow, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        container.addView(
            draggable, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )

        draggableView = draggable
        ivShadow = shadow

        return container
    }

    /**
     * Observe thumbnail view location on screen.
     *
     * @param lifecycleOwner LifecycleOwner
     */
    fun observeThumbnailLocation(lifecycleOwner: LifecycleOwner) {
        LiveEventBus.get(
            EVENT_DRAG_TO_EXIT_THUMBNAIL_LOCATION, IntArray::class.java
        ).observe(lifecycleOwner) {
            logDebug("EVENT_DRAG_TO_EXIT_THUMBNAIL_LOCATION ${Arrays.toString(it)}")

            val newLoc = intArrayOf(*it)
            newLoc[LOCATION_INDEX_LEFT] += newLoc[LOCATION_INDEX_WIDTH] / 2
            newLoc[LOCATION_INDEX_TOP] += newLoc[LOCATION_INDEX_HEIGHT] / 2

            draggableView?.screenPosition = newLoc
        }
    }

    /**
     * Run enter animation.
     *
     * @param launchIntent the activity launch intent, which contains thumbnail location on screen
     * @param mainView the main view which will be animated
     * @param animationCallback callback for animation, parameter will be true when animation starts
     */
    fun runEnterAnimation(
        launchIntent: Intent,
        mainView: View,
        animationCallback: (Boolean) -> Unit
    ) {
        mainView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                mainView.viewTreeObserver.removeOnPreDrawListener(this)

                val mainViewLocation = IntArray(2)
                mainView.getLocationOnScreen(mainViewLocation)

                val thumbnailLocation =
                    launchIntent.getIntArrayExtra(INTENT_EXTRA_KEY_SCREEN_POSITION)

                val displayMetrics = DisplayMetrics()
                mainView.display.getMetrics(displayMetrics)

                val leftDelta: Float
                val topDelta: Float
                val widthScale: Float
                val heightScale: Float

                if (thumbnailLocation == null) {
                    leftDelta =
                        displayMetrics.widthPixels / 2 - mainViewLocation[LOCATION_INDEX_LEFT].toFloat()
                    topDelta =
                        displayMetrics.heightPixels / 2 - mainViewLocation[LOCATION_INDEX_TOP].toFloat()

                    widthScale = displayMetrics.widthPixels / 4 / mainView.width.toFloat()
                    heightScale = displayMetrics.heightPixels / 4 / mainView.height.toFloat()
                } else {
                    leftDelta =
                        (thumbnailLocation[LOCATION_INDEX_LEFT] - mainViewLocation[LOCATION_INDEX_LEFT]).toFloat()
                    topDelta =
                        (thumbnailLocation[LOCATION_INDEX_TOP] - mainViewLocation[LOCATION_INDEX_TOP]).toFloat()

                    widthScale = thumbnailLocation[LOCATION_INDEX_WIDTH].toFloat() / mainView.width
                    heightScale =
                        thumbnailLocation[LOCATION_INDEX_HEIGHT].toFloat() / mainView.height
                }

                animationCallback(true)

                mainView.pivotX = 0F
                mainView.pivotY = 0F
                mainView.scaleX = widthScale
                mainView.scaleY = heightScale
                mainView.translationX = leftDelta
                mainView.translationY = topDelta

                ivShadow?.alpha = 0F

                mainView.animate()
                    .setDuration(ENTER_ANIMATION_DURATION_MS)
                    .scaleX(1F)
                    .scaleY(1F)
                    .translationX(0F)
                    .translationY(0F)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        animationCallback(false)
                    }

                ivShadow?.animate()
                    ?.setDuration(ENTER_ANIMATION_DURATION_MS)
                    ?.alpha(1F)

                return true
            }
        })
    }

    /**
     * Notify node changes.
     *
     * @param handle new node handle
     */
    fun nodeChanged(handle: Long) {
        logDebug("nodeChanged $handle, currentHandle $currentHandle")

        if (handle == currentHandle) {
            return
        }

        val event = ThumbnailVisibilityEvent(handle, false, currentHandle)
        val visibilityEvent = {
            LiveEventBus.get(
                EVENT_DRAG_TO_EXIT_THUMBNAIL_VISIBILITY, ThumbnailVisibilityEvent::class.java
            ).post(event)
        }

        if (currentHandle != INVALID_HANDLE) {
            LiveEventBus.get(
                EVENT_DRAG_TO_EXIT_SCROLL, Long::class.java
            ).post(handle)

            // When we need scroll, post the visibility event on the next UI cycle,
            // in case the item isn't scrolled up to visible at now.
            post(visibilityEvent)
        } else {
            visibilityEvent()
        }

        currentHandle = handle
    }

    fun setDraggable(draggable: Boolean) {
        draggableView?.draggable = draggable
    }

    fun setCurrentView(currentView: View) {
        draggableView?.currentView = currentView
    }

    override fun onViewPositionChanged(fractionScreen: Float) {
        ivShadow?.alpha = 1 - fractionScreen
    }

    override fun onDragActivated(activated: Boolean) {
        dragActivated(activated)
    }

    override fun showPreviousHiddenThumbnail() {
        LiveEventBus.get(
            EVENT_DRAG_TO_EXIT_THUMBNAIL_VISIBILITY, ThumbnailVisibilityEvent::class.java
        ).post(ThumbnailVisibilityEvent(currentHandle, true))
    }

    override fun fadeOutFinish() {
        fadeOutFinishCallback()
    }

    companion object {
        private const val ENTER_ANIMATION_DURATION_MS = 600L

        /**
         * Put thumbnail location on screen into viewer launch intent.
         *
         * @param launchIntent view activity launch intent
         * @param rv the RecyclerView
         * @param position the adapter position of opening node
         * @param thumbnailGetter DragThumbnailGetter
         */
        fun putThumbnailLocation(
            launchIntent: Intent,
            rv: RecyclerView,
            position: Int,
            thumbnailGetter: DragThumbnailGetter
        ) {
            val viewHolder = rv.findViewHolderForLayoutPosition(position) ?: return
            val thumbnail = thumbnailGetter.getThumbnail(viewHolder) ?: return

            launchIntent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, getThumbnailLocation(thumbnail))
        }

        /**
         * Get thumbnail location on screen.
         *
         * @param thumbnail the thumbnail view
         * @return thumbnail location on screen
         */
        fun getThumbnailLocation(thumbnail: View?): IntArray? {
            if (thumbnail == null) {
                return null
            }

            val leftTop = IntArray(2)
            thumbnail.getLocationOnScreen(leftTop)

            return intArrayOf(
                leftTop[LOCATION_INDEX_LEFT], leftTop[LOCATION_INDEX_TOP],
                thumbnail.width, thumbnail.height
            )
        }

        /**
         * Observe drag support events, including thumbnail visibility event, scroll event.
         *
         * @param lifecycleOwner LifecycleOwner
         * @param rv the RecyclerView, its adapter should implements DragThumbnailGetter interface
         */
        fun observeDragSupportEvents(
            lifecycleOwner: LifecycleOwner,
            rv: RecyclerView
        ) {
            LiveEventBus.get(
                EVENT_DRAG_TO_EXIT_THUMBNAIL_VISIBILITY, ThumbnailVisibilityEvent::class.java
            ).observe(lifecycleOwner) {
                logDebug("EVENT_DRAG_TO_EXIT_THUMBNAIL_VISIBILITY $it")

                val thumbnailGetter = rv.adapter as? DragThumbnailGetter ?: return@observe

                if (it.previousHiddenHandle != INVALID_HANDLE) {
                    val thumbnail = getThumbnail(rv, thumbnailGetter, it.previousHiddenHandle)
                    logDebug("previous thumbnail $thumbnail")

                    if (thumbnail != null) {
                        thumbnail.visibility = View.VISIBLE
                    }
                }

                val thumbnail = getThumbnail(rv, thumbnailGetter, it.handle)
                logDebug("current thumbnail $thumbnail")

                if (thumbnail != null) {
                    thumbnail.visibility = if (it.visible) View.VISIBLE else View.INVISIBLE

                    val location = getThumbnailLocation(thumbnail)
                    if (!it.visible && location != null) {
                        LiveEventBus.get(
                            EVENT_DRAG_TO_EXIT_THUMBNAIL_LOCATION, IntArray::class.java
                        ).post(location)
                    }
                }
            }

            LiveEventBus.get(
                EVENT_DRAG_TO_EXIT_SCROLL, Long::class.java
            ).observe(lifecycleOwner) {
                val thumbnailGetter = rv.adapter as? DragThumbnailGetter ?: return@observe

                val position = thumbnailGetter.getNodePosition(it)
                logDebug("EVENT_DRAG_TO_EXIT_SCROLL handle $it, position $position")

                if (position != INVALID_POSITION) {
                    rv.scrollToPosition(position)
                }
            }
        }

        private fun getThumbnail(
            rv: RecyclerView,
            thumbnailGetter: DragThumbnailGetter,
            handle: Long
        ): View? {
            val position = thumbnailGetter.getNodePosition(handle)
            val viewHolder = rv.findViewHolderForLayoutPosition(position) ?: return null
            return thumbnailGetter.getThumbnail(viewHolder)
        }
    }
}
