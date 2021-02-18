package mega.privacy.android.app.utils

import android.app.Activity
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation

/**
 * Helper for changing status bar color.
 */
object StatusBarColorHelper {

    /**
     * Under dark mode, status bar's color should be change along with app bar layout's background color.
     */
    @JvmStatic
    fun changeStatusBarColorForElevation(activity: Activity, withElevation: Boolean) {
        // Only for dark mode.
        if (!Util.isDarkMode(activity)) return

        if (withElevation) {
            val elevation: Float = activity.resources.getDimension(R.dimen.toolbar_elevation)
            val toolbarElevationColor = getColorForElevation(activity, elevation)
            RunOnUIThreadUtils.post { activity.window.statusBarColor = toolbarElevationColor }
        } else {
            RunOnUIThreadUtils.post { activity.window.statusBarColor = android.R.color.transparent }
        }
    }
}