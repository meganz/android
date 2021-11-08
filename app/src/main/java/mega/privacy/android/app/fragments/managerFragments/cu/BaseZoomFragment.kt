package mega.privacy.android.app.fragments.managerFragments.cu

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.viewModels
import mega.privacy.android.app.R
import mega.privacy.android.app.components.GestureScaleListener.GestureScaleCallback
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.fragments.homepage.photos.ZoomViewModel
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.utils.ColorUtils

abstract class BaseZoomFragment : BaseFragment(), GestureScaleCallback {

    protected lateinit var menu: Menu
    protected val zoomViewModel by viewModels<ZoomViewModel>()


    abstract fun handleZoomChange(zoom:Int)

    fun subscribeObservers() {
        zoomViewModel.zoom.observe(viewLifecycleOwner, { zoom: Int ->
            zoomViewModel.setCurrentZoom(zoom)
           // Out 3X: organize by year, In 1X: oragnize by day, both need to reload nodes.
            handleZoomChange(zoom)
       })
   }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeObservers()
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if((activity as ManagerActivityLollipop).drawerItem != ManagerActivityLollipop.DrawerItem.PHOTOS) {
            return
        }

        inflater.inflate(R.menu.fragment_images_toolbar, menu)
        this.menu = menu
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_zoom_in -> {
                zoomViewModel.zoomIn()
                handleZoomMenuItemStatus()
            }
            R.id.action_zoom_out -> {
                zoomOut()
                handleZoomMenuItemStatus()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleZoomMenuItemStatus() {
        val canZoomOut = zoomViewModel.canZoomOut()
        val canZoomIn = zoomViewModel.canZoomIn()
        if (!canZoomIn && canZoomOut) {
            handleEnableToolbarMenuIcon(R.id.action_zoom_in, false)
            handleEnableToolbarMenuIcon(R.id.action_zoom_out, true)
        } else if (canZoomIn && !canZoomOut) {
            handleEnableToolbarMenuIcon(R.id.action_zoom_in, true)
            handleEnableToolbarMenuIcon(R.id.action_zoom_out, false)
        } else {
            //canZoomOut && canZoomIn
            handleEnableToolbarMenuIcon(R.id.action_zoom_in, true)
            handleEnableToolbarMenuIcon(R.id.action_zoom_out, true)
        }
    }

    private fun handleEnableToolbarMenuIcon(menuItemId: Int, isEnable: Boolean) {
        val menuItem = this.menu.findItem(menuItemId)
        var colorRes = ColorUtils.getThemeColor(context, R.attr.colorControlNormal)
        if (!isEnable) {
            colorRes = ContextCompat.getColor(context, R.color.grey_038_white_038)
        }
        DrawableCompat.setTint(
            menuItem.icon,
            colorRes
        )
        menuItem.isEnabled = isEnable
    }

    fun handleOptionsMenuUpdate(shouldShow: Boolean) {
        if (this::menu.isInitialized) {
            menu.findItem(R.id.action_zoom_in)?.isVisible = shouldShow
            menu.findItem(R.id.action_zoom_out)?.isVisible = shouldShow
            menu.findItem(R.id.action_menu_sort_by)?.isVisible = shouldShow
        }
    }

    override fun zoomIn() {
        zoomViewModel.zoomIn()
        handleZoomMenuItemStatus()
    }

    override fun zoomOut() {
        zoomViewModel.zoomOut()
        handleZoomMenuItemStatus()
    }

    protected fun getCurrentZoom():Int{
        return zoomViewModel.getCurrentZoom()
    }
}