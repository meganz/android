package mega.privacy.android.app.utils

import android.os.SystemClock
import android.view.View

/**
 * Custom click listener to avoid multiple clicks at the same time.
 *
 * @property clickListener  Click Listener
 * @constructor Create empty On single click listener
 */
class OnSingleClickListener(
    private val clickListener: View.OnClickListener,
) : View.OnClickListener {

    companion object {
        private const val TIME_BETWEEN_CLICKS_IN_MS = 1200L

        /**
         * Register a callback to be invoked when this view is clicked.
         *
         * @param clickListener The callback that will run
         */
        @JvmStatic
        fun View.setOnSingleClickListener(clickListener: View.OnClickListener) {
            setOnClickListener(OnSingleClickListener(clickListener))
        }
    }

    private var lastClickTime = 0L

    /**
     * Called when a view has been clicked.
     *
     * @param view The view that was clicked.
     */
    override fun onClick(view: View) {
        if (SystemClock.elapsedRealtime() - lastClickTime < TIME_BETWEEN_CLICKS_IN_MS) {
            return
        }
        lastClickTime = SystemClock.elapsedRealtime()

        clickListener.onClick(view)
    }
}
