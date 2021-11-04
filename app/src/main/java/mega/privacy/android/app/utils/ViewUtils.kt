package mega.privacy.android.app.utils

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import mega.privacy.android.app.utils.Util.SHOW_IM_DELAY

@Suppress("DEPRECATION")
object ViewUtils {

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
     * Make status bar transparent
     * Requires to call setFitsSystemWindows(false) on view that need it
     */
    fun Activity.setStatusBarTransparent() {
        setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, true)
        window.decorView.systemUiVisibility = SYSTEM_UI_FLAG_LAYOUT_STABLE or
                SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE
        window.statusBarColor = Color.TRANSPARENT
        getLayoutRoot()?.fitsSystemWindows = false
    }

    /**
     * Make status bar with solid color
     * Requires to call setFitsSystemWindows(true) on view that need it
     *
     * @param statusBarColor    Color to tint status bar
     */
    fun Activity.setStatusBarColor(statusBarColor: Int) {
        window.statusBarColor = statusBarColor
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        getLayoutRoot()?.fitsSystemWindows = true
    }

    fun Activity.getLayoutRoot(): ViewGroup? {
        val contentView = findViewById<ViewGroup>(android.R.id.content)
        val layoutRoot = contentView.getChildAt(0)
        return if (layoutRoot != null && layoutRoot is ViewGroup) {
            layoutRoot
        } else {
            null
        }
    }

    fun Activity.setWindowFlag(bits: Int, on: Boolean) {
        val winParams = window.attributes
        if (on) {
            winParams.flags = winParams.flags or bits
        } else {
            winParams.flags = winParams.flags and bits.inv()
        }
        window.attributes = winParams
    }
}
