package mega.privacy.android.app.utils

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView

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

    /**
     * Setup SearchView Menu Item with a single callback
     *
     * @param queryCallback Callback to be called when text has changed. Returns null if it's closed.
     */
    fun MenuItem.setupSearchView(queryCallback: (String?) -> Unit) {
        setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                queryCallback.invoke(null)
                return true
            }

            override fun onMenuItemActionExpand(item: MenuItem): Boolean = true
        })
        (actionView as SearchView?)?.apply {
            setOnCloseListener {
                queryCallback.invoke(null)
                false
            }
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String?): Boolean {
                    queryCallback.invoke(newText)
                    return true
                }

                override fun onQueryTextSubmit(query: String?): Boolean = false
            })
        }
    }
}