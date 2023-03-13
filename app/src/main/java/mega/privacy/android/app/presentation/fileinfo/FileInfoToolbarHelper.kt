package mega.privacy.android.app.presentation.fileinfo

import android.content.Context
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.appbar.AppBarLayout
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import net.opacapp.multilinecollapsingtoolbar.CollapsingToolbarLayout
import kotlin.math.abs

/**
 * This class is to encapsulate all view logic related with the toolbar of FileInfoActivity,
 * including the different bars and collapsing toolbar and the options menu shown
 */
internal class FileInfoToolbarHelper(
    private val context: Context,
    private val appBar: AppBarLayout,
    private val toolbar: Toolbar,
    private val collapsingToolbarLayout: CollapsingToolbarLayout,
    private val supportActionBar: ActionBar?,
    private val fileInfoToolbarImage: SimpleDraweeView,
    private val fileInfoImageLayout: ViewGroup,
    private val fileInfoIconLayout: ViewGroup,
    private val nestedView: NestedScrollView,
) {

    private var upArrow: Drawable? = null
    private var drawableRemoveLink: Drawable? = null
    private var drawableLink: Drawable? = null
    private var drawableShare: Drawable? = null
    private var drawableDots: Drawable? = null
    private var drawableDownload: Drawable? = null
    private var drawableLeave: Drawable? = null
    private var drawableCopy: Drawable? = null
    private var drawableChat: Drawable? = null
    private var drawableRename: Drawable? = null
    private var drawableRubbishBin: Drawable? = null
    private var drawableMove: Drawable? = null
    private var drawableDisputeTakeDown: Drawable? = null
    private val allDrawables
        get() = arrayListOf(
            upArrow,
            drawableRemoveLink,
            drawableLink,
            drawableShare,
            drawableDots,
            drawableDownload,
            drawableLeave,
            drawableCopy,
            drawableChat,
            drawableRename,
            drawableRubbishBin,
            drawableMove,
            drawableDisputeTakeDown,
        )

    private var downloadMenuItem: MenuItem? = null
    private var shareMenuItem: MenuItem? = null
    private var getLinkMenuItem: MenuItem? = null
    private var editLinkMenuItem: MenuItem? = null
    private var removeLinkMenuItem: MenuItem? = null
    private var renameMenuItem: MenuItem? = null
    private var moveMenuItem: MenuItem? = null
    private var copyMenuItem: MenuItem? = null
    private var rubbishMenuItem: MenuItem? = null
    private var deleteMenuItem: MenuItem? = null
    private var leaveMenuItem: MenuItem? = null
    private var sendToChatMenuItem: MenuItem? = null
    private var disputeTakeDownMenuItem: MenuItem? = null
    private val allMenuItems
        get() = arrayListOf(
            downloadMenuItem,
            shareMenuItem,
            getLinkMenuItem,
            editLinkMenuItem,
            removeLinkMenuItem,
            renameMenuItem,
            moveMenuItem,
            copyMenuItem,
            rubbishMenuItem,
            deleteMenuItem,
            leaveMenuItem,
            sendToChatMenuItem,
            disputeTakeDownMenuItem,
        )

    private var currentColorFilter = 0

    init {
        getActionBarDrawables()
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    /**
     * initial setup of the toolbar once we have the view created and the node fetched
     */
    fun setupToolbar(visibleTop: Int) {
        val params = fileInfoIconLayout.layoutParams as CollapsingToolbarLayout.LayoutParams
        params.setMargins(Util.dp2px(16f), Util.dp2px(90f) + visibleTop, 0, Util.dp2px(14f))
        fileInfoIconLayout.layoutParams = params
        fileInfoImageLayout.isVisible = false
        val statusBarColor =
            ColorUtils.getColorForElevation(
                context,
                context.resources.getDimension(R.dimen.toolbar_elevation)
            )
        collapsingToolbarLayout.setStatusBarScrimColor(statusBarColor)
        if (Util.isDarkMode(context)) {
            collapsingToolbarLayout.setContentScrimColor(statusBarColor)
        }
    }

    /**
     * Setup of the options menu once is created in onCreateOptionsMenu
     */
    fun setupOptionsMenu(menu: Menu) {
        downloadMenuItem = menu.findItem(R.id.cab_menu_file_info_download)
        shareMenuItem = menu.findItem(R.id.cab_menu_file_info_share_folder)
        getLinkMenuItem = menu.findItem(R.id.cab_menu_file_info_get_link)
        getLinkMenuItem?.title = context.resources.getQuantityString(R.plurals.get_links, 1)
        editLinkMenuItem = menu.findItem(R.id.cab_menu_file_info_edit_link)
        removeLinkMenuItem = menu.findItem(R.id.cab_menu_file_info_remove_link)
        renameMenuItem = menu.findItem(R.id.cab_menu_file_info_rename)
        moveMenuItem = menu.findItem(R.id.cab_menu_file_info_move)
        copyMenuItem = menu.findItem(R.id.cab_menu_file_info_copy)
        rubbishMenuItem = menu.findItem(R.id.cab_menu_file_info_rubbish)
        deleteMenuItem = menu.findItem(R.id.cab_menu_file_info_delete)
        leaveMenuItem = menu.findItem(R.id.cab_menu_file_info_leave)
        sendToChatMenuItem = menu.findItem(R.id.cab_menu_file_info_send_to_chat)
        disputeTakeDownMenuItem = menu.findItem(R.id.cab_menu_file_info_dispute)
    }

    fun updateOptionsMenu(
        node: TypedNode,
        isInRubbish: Boolean,
        isInInbox: Boolean,
        fromIncomingShares: Boolean,
        firstIncomingLevel: Boolean,
        nodeAccess: AccessPermission?,
    ) {
        allMenuItems.forEach {
            it?.isVisible = false
            it?.isEnabled = true
        }
        setIconsColorFilter()
        setupOptionsToolbar(
            node = node,
            isInRubbish = isInRubbish,
            fromIncomingShares = fromIncomingShares,
            firstIncomingLevel = firstIncomingLevel,
            nodeAccess = nodeAccess
        )
        // Check if read-only properties should be applied on MenuItems
        shouldApplyMenuItemReadOnlyState(isInInbox)

        val isCollapsible = (node as? TypedFileNode)?.hasPreview == true
        setupCollapsible(isCollapsible, nestedView)
    }

    fun disableMenu() {
        allMenuItems.forEach { it?.isEnabled = true }
    }

    /**
     * sets the node name as a title in the toolbar
     */
    fun setNodeName(name: String) {
        collapsingToolbarLayout.title = name
    }

    /**
     * sets the preview of the node in the collapsing toolbar
     */
    fun setPreview(previewUriString: String?) {
        fileInfoToolbarImage.setImageURI(previewUriString)
        fileInfoImageLayout.isVisible = previewUriString != null
        fileInfoIconLayout.isVisible = previewUriString == null
    }

    private fun getActionBarDrawables() {
        drawableDots = getMutatedDrawable(R.drawable.ic_dots_vertical_white)
        upArrow = getMutatedDrawable(R.drawable.ic_arrow_back_white)
        drawableRemoveLink = getMutatedDrawable(R.drawable.ic_remove_link)
        drawableLink = getMutatedDrawable(R.drawable.ic_link_white)
        drawableShare = getMutatedDrawable(R.drawable.ic_share)
        drawableDownload = getMutatedDrawable(R.drawable.ic_download_white)
        drawableLeave = getMutatedDrawable(R.drawable.ic_leave_share_w)
        drawableCopy = getMutatedDrawable(R.drawable.ic_copy_white)
        drawableChat = getMutatedDrawable(R.drawable.ic_send_to_contact)
        drawableRename = getMutatedDrawable(R.drawable.ic_rename)
        drawableRubbishBin = getMutatedDrawable(R.drawable.ic_move_to_rubbish_bin)
        drawableMove = getMutatedDrawable(R.drawable.ic_move_white)
        drawableDisputeTakeDown = getMutatedDrawable(R.drawable.ic_taken_down_file_info)
    }

    private fun getMutatedDrawable(@DrawableRes res: Int) =
        ContextCompat.getDrawable(context, res)?.mutate()

    /**
     * Sets the toolbar icons color.
     */
    private fun setIconsColorFilter() {
        val colorFilter = PorterDuffColorFilter(currentColorFilter, PorterDuff.Mode.SRC_IN)
        allDrawables.filterNotNull().forEach {
            it.colorFilter = colorFilter
        }

        removeLinkMenuItem?.icon = drawableRemoveLink
        getLinkMenuItem?.icon = drawableLink
        downloadMenuItem?.icon = drawableDownload
        shareMenuItem?.icon = drawableShare
        leaveMenuItem?.icon = drawableLeave
        copyMenuItem?.icon = drawableCopy
        sendToChatMenuItem?.icon = drawableChat
        renameMenuItem?.icon = drawableRename
        rubbishMenuItem?.icon = drawableRubbishBin
        moveMenuItem?.icon = drawableMove
        disputeTakeDownMenuItem?.icon = drawableDisputeTakeDown
    }

    /**
     * Applies rubbish restrictions (delete) or default options
     * @param node the affected node
     * @param isInRubbish the node is in rubbish or not
     */
    private fun setupOptionsToolbar(
        node: TypedNode,
        isInRubbish: Boolean,
        fromIncomingShares: Boolean,
        firstIncomingLevel: Boolean,
        nodeAccess: AccessPermission?,
    ) {
        if (isInRubbish) {
            deleteMenuItem?.isVisible = true
        } else {
            setDefaultOptionsToolbar(node, fromIncomingShares, firstIncomingLevel, nodeAccess)
        }
    }

    /**
     * setup the collapsible bar and its nested view
     */
    private fun setupCollapsible(isCollapsible: Boolean, nestedView: NestedScrollView) {
        if (isCollapsible) {
            appBar.addOnOffsetChangedListener { appBar: AppBarLayout, offset: Int ->
                val collapsed = offset < 0 && abs(offset) >= appBar.totalScrollRange / 2
                setActionBarDrawablesColorFilter(
                    context.resources.getColor(
                        if (collapsed) R.color.grey_087_white_087 else R.color.white_alpha_087,
                        null
                    )
                )
            }
            collapsingToolbarLayout.setCollapsedTitleTextColor(
                ContextCompat.getColor(context, R.color.grey_087_white_087)
            )
            collapsingToolbarLayout.setExpandedTitleColor(
                context.resources.getColor(R.color.white_alpha_087, null)
            )
        } else {
            setActionBarDrawablesColorFilter(
                context.resources.getColor(
                    R.color.grey_087_white_087,
                    null
                )
            )
        }
        nestedView.setOnScrollChangeListener { v: NestedScrollView, _: Int, _: Int, _: Int, _: Int ->
            Util.changeViewElevation(
                supportActionBar,
                v.canScrollVertically(-1) && v.visibility == View.VISIBLE,
                Resources.getSystem().displayMetrics
            )
        }
    }

    /**
     * Applies read-only restrictions (unable to Favourite, Rename, Move, or Move to Rubbish Bin)
     * if the MegaNode is a Backup node.
     *
     * @param isInInbox the node is in inbox or not
     */
    private fun shouldApplyMenuItemReadOnlyState(isInInbox: Boolean) {
        if (isInInbox) {
            renameMenuItem?.isVisible = false
            moveMenuItem?.isVisible = false
            rubbishMenuItem?.isVisible = false
        }
    }

    /**
     * Sets up the default items to be displayed on the Toolbar Menu
     */
    private fun setDefaultOptionsToolbar(
        node: TypedNode,
        fromIncomingShares: Boolean,
        firstIncomingLevel: Boolean,
        nodeAccess: AccessPermission?,
    ) {
        if (!node.isTakenDown && node is TypedFileNode) {
            sendToChatMenuItem?.isVisible = true
            sendToChatMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        if (fromIncomingShares) {
            if (!node.isTakenDown) {
                downloadMenuItem?.isVisible = true
                downloadMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                leaveMenuItem?.isVisible = firstIncomingLevel
                leaveMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                copyMenuItem?.isVisible = true
            }
            when (nodeAccess) {
                AccessPermission.OWNER, AccessPermission.FULL -> {
                    rubbishMenuItem?.isVisible = !firstIncomingLevel
                    renameMenuItem?.isVisible = true
                }
                AccessPermission.READ -> {
                    copyMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                    renameMenuItem?.isVisible = false
                    rubbishMenuItem?.isVisible = false
                }
                else -> {
                    renameMenuItem?.isVisible = false
                    rubbishMenuItem?.isVisible = false
                }
            }
        } else {
            if (!node.isTakenDown) {
                downloadMenuItem?.isVisible = true
                downloadMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                if (node is TypedFolderNode) {
                    shareMenuItem?.isVisible = true
                    shareMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                }
                if (node.isExported) {
                    editLinkMenuItem?.isVisible = true
                    removeLinkMenuItem?.isVisible = true
                    removeLinkMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                } else {
                    getLinkMenuItem?.isVisible = true
                    getLinkMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                }
                copyMenuItem?.isVisible = true
            } else {
                rubbishMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                renameMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                disputeTakeDownMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                moveMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                disputeTakeDownMenuItem?.isVisible = true
            }
            rubbishMenuItem?.isVisible = true
            renameMenuItem?.isVisible = true
            moveMenuItem?.isVisible = true
        }
    }

    /**
     * Changes the drawables color in ActionBar depending on the color received.
     *
     * @param color Can be Color.WHITE or Color.WHITE.
     */
    private fun setActionBarDrawablesColorFilter(color: Int) {
        if (currentColorFilter == color) {
            return
        }
        currentColorFilter = color
        upArrow?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        supportActionBar?.setHomeAsUpIndicator(upArrow)
        drawableDots?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        toolbar.overflowIcon = drawableDots
        setIconsColorFilter()
    }
}