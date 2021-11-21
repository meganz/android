package mega.privacy.android.app.fragments.managerFragments.cu

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.viewModels
import mega.privacy.android.app.R
import mega.privacy.android.app.components.GestureScaleListener.GestureScaleCallback
import mega.privacy.android.app.components.scrollBar.FastScroller
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.fragments.homepage.photos.ZoomViewModel
import mega.privacy.android.app.fragments.managerFragments.cu.PhotosFragment.ALL_VIEW
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants.MIN_ITEMS_SCROLLBAR
import mega.privacy.android.app.utils.StyleUtils
import mega.privacy.android.app.utils.ZoomUtil
import org.w3c.dom.Text

/**
 * A parent fragment with basic zoom UI logic, like menu, gestureScaleCallback.
 */
abstract class BaseZoomFragment : BaseFragment(), GestureScaleCallback {

    protected lateinit var menu: Menu
    protected val zoomViewModel by viewModels<ZoomViewModel>()

    /**
     * When zoom changes,handle zoom for sub class
     */
    abstract fun handleZoomChange(zoom: Int, needReload: Boolean)

    /**
     * Handle menus for sub class
     */
    abstract fun handleOnCreateOptionsMenu()

    private fun subscribeObservers() {
        zoomViewModel.zoom.observe(viewLifecycleOwner, { zoom: Int ->
            val needReload = ZoomUtil.needReload(getCurrentZoom(), zoom)
            zoomViewModel.setCurrentZoom(zoom)
            handleZoomChange(zoom, needReload)
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeObservers()
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_images_toolbar, menu)
        this.menu = menu
        handleOnCreateOptionsMenu()
        handleZoomMenuItemStatus()
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
        //handle can zoom in then handle can zoom out
        handleEnableToolbarMenuIcon(R.id.action_zoom_in, canZoomIn)
        handleEnableToolbarMenuIcon(R.id.action_zoom_out, canZoomOut)
    }

    private fun handleEnableToolbarMenuIcon(menuItemId: Int, isEnable: Boolean) {
        if (!this::menu.isInitialized)
            return
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

    fun handleSortByMenuIcon(shouldShow: Boolean){
        if (this::menu.isInitialized) {
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

    protected fun getCurrentZoom(): Int {
        return zoomViewModel.getCurrentZoom()
    }

    protected fun updateViewSelected(allButton:TextView,daysButton:TextView,monthsButton:TextView,yearsButton:TextView,selectedView:Int) {
        setViewTypeButtonStyle(allButton, false)
        setViewTypeButtonStyle(daysButton, false)
        setViewTypeButtonStyle(monthsButton, false)
        setViewTypeButtonStyle(yearsButton, false)

        when (selectedView) {
            PhotosFragment.DAYS_VIEW -> setViewTypeButtonStyle(daysButton, true)
            PhotosFragment.MONTHS_VIEW -> setViewTypeButtonStyle(monthsButton, true)
            PhotosFragment.YEARS_VIEW -> setViewTypeButtonStyle(yearsButton, true)
            else -> setViewTypeButtonStyle(allButton, true)
        }
    }

    /**
     * Apply selected/unselected style for the TextView button.
     *
     * @param textView The TextView button to be applied with the style.
     * @param enabled true, apply selected style; false, apply unselected style.
     */
    private fun setViewTypeButtonStyle(textView: TextView, enabled: Boolean) {
        textView.setBackgroundResource(
            if (enabled)
                R.drawable.background_18dp_rounded_selected_button
            else
                R.drawable.background_18dp_rounded_unselected_button
        )

        StyleUtils.setTextStyle(
            context,
            textView,
            if (enabled) R.style.TextAppearance_Mega_Subtitle2_Medium_WhiteGrey87 else R.style.TextAppearance_Mega_Subtitle2_Normal_Grey87White87,
            if (enabled) R.color.white_grey_087 else R.color.grey_087_white_087,
            false
        )
    }

    protected fun updateFastScrollerVisibility(selectedView:Int,scroller: FastScroller,itemCount:Int) {
        val gridView = selectedView == ALL_VIEW

        scroller.visibility =
            if (!gridView && itemCount >= MIN_ITEMS_SCROLLBAR)
                View.VISIBLE
            else
                View.GONE
    }

    /**
     * Get how many items will be shown per row, depends on screen direction and zoom level if all view is selected.
     *
     * @param isPortrait true, on portrait mode, false otherwise.
     */
    protected fun getSpanCount(selectedView:Int,isPortrait: Boolean): Int {
        return if (selectedView != ALL_VIEW) {
            if (isPortrait) PhotosFragment.SPAN_CARD_PORTRAIT else PhotosFragment.SPAN_CARD_LANDSCAPE
        } else {
            ZoomUtil.getSpanCount(isPortrait, zoomViewModel.getCurrentZoom())
        }
    }
}