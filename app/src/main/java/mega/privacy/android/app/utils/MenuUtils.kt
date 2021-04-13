package mega.privacy.android.app.utils

import android.view.Menu

class MenuUtils {

    companion object {

        /**
         * Updates all the menu options visibility.
         *
         * @param menu    Menu to update.
         * @param visible True if options should be visible, false otherwise.
         */
        @JvmStatic
        fun toggleAllMenuItemsVisibility(menu: Menu, visible: Boolean) {
            for (i in 0 until menu.size()) {
                menu.getItem(i).isVisible = visible
            }
        }
    }
}