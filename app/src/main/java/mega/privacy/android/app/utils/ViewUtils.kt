package mega.privacy.android.app.utils

import android.content.Context
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.inputmethod.InputMethodManager
import mega.privacy.android.app.utils.Util.SHOW_IM_DELAY
import kotlin.math.abs

object ViewUtils {

    private var previousTimeStamp: Long = 0L

    private const val MINIMUM_DEBOUNCE_INTERVAL = 1000L

    /**
     * Global Layout Listener that runs an action when the View has been rendered.
     *
     * @param action    Action to be executed when the View has been rendered.
     *                  Must return `true` to remove listener, `false` otherwise.
     */
    @JvmStatic
    fun View.waitForLayout(action: () -> Boolean) {
        viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (action.invoke()) viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    /**
     * Show the soft keyboard input for the current View with a delay
     *
     * @param delayMillis   Time in millis to be delayed
     */
    @JvmStatic
    @JvmOverloads
    fun View.showSoftKeyboardDelayed(delayMillis: Long = SHOW_IM_DELAY) {
        postDelayed({ showSoftKeyboard() }, delayMillis)
    }

    /**
     * Show the soft keyboard input for the current View
     */
    @JvmStatic
    fun View.showSoftKeyboard() {
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?)
            ?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Hide the soft keyboard input for the current View
     */
    @JvmStatic
    fun View.hideKeyboard() {
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?)
            ?.hideSoftInputFromWindow(windowToken, 0)
    }

    /**
     * A variation of [View.setOnClickListener] that prevents multiple events from being fired
     * multiple times.
     *
     * The debounce interval is specified by [MINIMUM_DEBOUNCE_INTERVAL].
     *
     * @param action - Lambda parameter that defines an action when the [View] is clicked
     * and when it is under the debounce interval
     */
    fun View.debounceClick(action: () -> Unit) {
        setOnClickListener {
            val currentTimeStamp: Long = SystemClock.uptimeMillis()
            if (abs(currentTimeStamp - previousTimeStamp) > MINIMUM_DEBOUNCE_INTERVAL) {
                previousTimeStamp = currentTimeStamp
                action()
            }
        }
    }
}
