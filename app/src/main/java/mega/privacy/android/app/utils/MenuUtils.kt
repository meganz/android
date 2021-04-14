package mega.privacy.android.app.utils

import android.view.Menu

object MenuUtils {

    /**
     * Updates all the menu options visibility.
     *
     * @param visible True if options should be visible, false otherwise.
     */
    @JvmStatic
    fun Menu.toggleAllMenuItemsVisibility(visible: Boolean) {
        for (i in 0 until size()) {
            getItem(i).isVisible = visible
        }
    }
}