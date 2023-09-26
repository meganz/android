package mega.privacy.android.app.main

import androidx.drawerlayout.widget.DrawerLayout

/**
 * Navigation drawer manager
 *
 */
interface NavigationDrawerManager {
    /**
     * Current Drawer item
     */
    var drawerItem: DrawerItem?

    /**
     * Close drawer
     *
     */
    fun closeDrawer()

    /**
     * Add drawer listener
     *
     * @param listener
     */
    fun addDrawerListener(listener: DrawerLayout.DrawerListener)

/**
     * Remove drawer listener
     *
     * @param listener
     */
    fun removeDrawerListener(listener: DrawerLayout.DrawerListener)

    /**
     * Select drawer item
     *
     * @param item
     */
    fun drawerItemClicked(item: DrawerItem)
}