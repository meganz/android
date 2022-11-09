package mega.privacy.android.app.presentation.extensions

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import mega.privacy.android.app.R

/**
 * Hides keyboard.
 */
fun Activity.hideKeyboard() {
    currentFocus?.let {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(it.windowToken, 0)
    }
}

/**
 * Changes the status bar elevation depending on if the theme is dark mode and if the
 * view is scrolled.
 *
 * @param scrolled  True if the view is scrolled, false otherwise.
 * @param isDark    True if the theme is dark, false if is light.
 */
fun Activity.changeStatusBarColor(scrolled: Boolean, isDark: Boolean) {
    window?.statusBarColor = when {
        scrolled && isDark -> ContextCompat.getColor(this, R.color.action_mode_background)
        isDark -> ContextCompat.getColor(this, R.color.dark_grey)
        else -> ContextCompat.getColor(this, android.R.color.white)
    }
}