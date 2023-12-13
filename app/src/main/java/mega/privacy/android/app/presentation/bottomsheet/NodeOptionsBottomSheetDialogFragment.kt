package mega.privacy.android.app.presentation.bottomsheet

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.RoundedCornersTransformation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.imageviewer.ImageViewerActivity.Companion.getIntentForParentNode
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.FileContactListActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.VersionsFileActivity
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkDialogFragment
import mega.privacy.android.app.main.dialog.rubbishbin.ConfirmMoveToRubbishBinDialogFragment
import mega.privacy.android.app.main.dialog.shares.RemoveAllSharingContactDialogFragment
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.openWith
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.showCannotOpenFileDialog
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsViewModel.Companion.MODE_KEY
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsViewModel.Companion.NODE_DEVICE_CENTER_INFORMATION_KEY
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsViewModel.Companion.NODE_ID_KEY
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsViewModel.Companion.SHARE_DATA_KEY
import mega.privacy.android.app.presentation.bottomsheet.model.NodeBottomSheetUIState
import mega.privacy.android.app.presentation.bottomsheet.model.NodeDeviceCenterInformation
import mega.privacy.android.app.presentation.bottomsheet.model.NodeShareInformation
import mega.privacy.android.app.presentation.contact.authenticitycredendials.AuthenticityCredentialsActivity
import mega.privacy.android.app.presentation.fileinfo.FileInfoActivity
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.search.SearchViewModel
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FROM
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_SHARE_FOLDER
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_NONE
import mega.privacy.android.app.utils.MegaNodeUtil.checkBackupNodeTypeByHandle
import mega.privacy.android.app.utils.MegaNodeUtil.getFileInfo
import mega.privacy.android.app.utils.MegaNodeUtil.getFolderIcon
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelColor
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelDrawable
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelText
import mega.privacy.android.app.utils.MegaNodeUtil.isEmptyFolder
import mega.privacy.android.app.utils.MegaNodeUtil.isOutShare
import mega.privacy.android.app.utils.MegaNodeUtil.manageEditTextFileIntent
import mega.privacy.android.app.utils.MegaNodeUtil.onNodeTapped
import mega.privacy.android.app.utils.MegaNodeUtil.shareNode
import mega.privacy.android.app.utils.MegaNodeUtil.showConfirmationLeaveIncomingShare
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.ViewUtils.isVisible
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.mobile.analytics.event.SearchResultGetLinkMenuItemEvent
import mega.privacy.mobile.analytics.event.SearchResultOpenWithMenuItemEvent
import mega.privacy.mobile.analytics.event.SearchResultSaveToDeviceMenuItemEvent
import mega.privacy.mobile.analytics.event.SearchResultShareMenuItemEvent
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber
import java.io.File

/**
 * [BaseBottomSheetDialogFragment] used to display actions of a particular Node
 */
@AndroidEntryPoint
class NodeOptionsBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {
    private var mode = DEFAULT_MODE
    private lateinit var nodeController: NodeController
    private var nodeInfo: TextView? = null
    private var drawerItem: DrawerItem? = null
    private var cannotOpenFileDialog: AlertDialog? = null

    private val nodeOptionsViewModel: NodeOptionsViewModel by viewModels()
    private val searchViewModel: SearchViewModel by activityViewModels()
    private val nodeOptionsDownloadViewModel: NodeOptionsDownloadViewModel by activityViewModels()

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        contentView = View.inflate(context, R.layout.bottom_sheet_node_item, null)
        itemsLayout = contentView.findViewById(R.id.items_layout_bottom_sheet_node)
        mode = arguments?.getInt(MODE_KEY) ?: savedInstanceState?.getInt(MODE_KEY) ?: DEFAULT_MODE
        drawerItem = (requireActivity() as? ManagerActivity)?.drawerItem
        nodeController = NodeController(requireActivity())
        return contentView
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val nodeThumb = contentView.findViewById<ImageView>(R.id.node_thumbnail)
        val nodeName = contentView.findViewById<TextView>(R.id.node_name_text)
        nodeInfo = contentView.findViewById(R.id.node_info_text)
        val nodeVersionsIcon = contentView.findViewById<ImageView>(R.id.node_info_versions_icon)
        val nodeStatusIcon = contentView.findViewById<ImageView>(R.id.node_status_icon)
        val optionOffline = contentView.findViewById<LinearLayout>(R.id.option_offline_layout)
        val permissionsIcon = contentView.findViewById<ImageView>(R.id.permissions_icon)
        val optionEdit = contentView.findViewById<LinearLayout>(R.id.edit_file_option)
        val optionInfo = contentView.findViewById<TextView>(R.id.properties_option)
        // option Versions
        val optionVersionsLayout =
            contentView.findViewById<LinearLayout>(R.id.option_versions_layout)
        val versions = contentView.findViewById<TextView>(R.id.versions)
        //      optionFavourite
        val optionFavourite = contentView.findViewById<TextView>(R.id.favorite_option)
        //      optionLabel
        val optionLabel = contentView.findViewById<LinearLayout>(R.id.option_label_layout)
        val optionLabelCurrent = contentView.findViewById<TextView>(R.id.option_label_current)
        //      counterSave
        val optionDownload = contentView.findViewById<TextView>(R.id.download_option)
        val offlineSwitch = contentView.findViewById<SwitchMaterial>(R.id.file_properties_switch)
        //      counterShares
        val optionLink = contentView.findViewById<TextView>(R.id.link_option)
        val optionRemoveLink = contentView.findViewById<TextView>(R.id.remove_link_option)
        val optionShare = contentView.findViewById<TextView>(R.id.share_option)
        val optionShareFolder = contentView.findViewById<TextView>(R.id.share_folder_option)
        val optionClearShares = contentView.findViewById<TextView>(R.id.clear_share_option)
        val optionSendChat = contentView.findViewById<TextView>(R.id.send_chat_option)
        //      counterModify
        val optionRename = contentView.findViewById<TextView>(R.id.rename_option)
        val optionMove = contentView.findViewById<TextView>(R.id.move_option)
        val optionCopy = contentView.findViewById<TextView>(R.id.copy_option)
        val optionRestoreFromRubbish = contentView.findViewById<TextView>(R.id.restore_option)
        //      slideShow
        val optionSlideshow = contentView.findViewById<TextView>(R.id.option_slideshow)
        //      counterOpen
        val optionOpenFolder = contentView.findViewById<TextView>(R.id.open_folder_option)
        val optionOpenWith = contentView.findViewById<TextView>(R.id.open_with_option)
        //      counterRemove
        val optionLeaveShares = contentView.findViewById<TextView>(R.id.leave_share_option)
        val optionRubbishBin = contentView.findViewById<TextView>(R.id.rubbish_bin_option)
        val optionRemove = contentView.findViewById<TextView>(R.id.remove_option)
        val viewInFolder = contentView.findViewById<TextView>(R.id.view_in_folder_option)

        val separatorInfo = contentView.findViewById<View>(R.id.separator_info_option)
        val separatorOpen = contentView.findViewById<LinearLayout>(R.id.separator_open_options)
        val separatorDownload =
            contentView.findViewById<LinearLayout>(R.id.separator_download_options)
        val separatorShares = contentView.findViewById<LinearLayout>(R.id.separator_share_options)
        val separatorModify = contentView.findViewById<LinearLayout>(R.id.separator_modify_options)
        if (!Util.isScreenInPortrait(requireContext())) {
            Timber.d("Landscape configuration")
            nodeName.maxWidth = Util.scaleWidthPx(275, resources.displayMetrics)
            nodeInfo?.maxWidth = Util.scaleWidthPx(275, resources.displayMetrics)
        } else {
            nodeName.maxWidth = Util.scaleWidthPx(210, resources.displayMetrics)
            nodeInfo?.maxWidth = Util.scaleWidthPx(210, resources.displayMetrics)
        }

        viewLifecycleOwner.collectFlow(
            nodeOptionsViewModel.state, Lifecycle.State.STARTED
        ) { state: NodeBottomSheetUIState ->

            var counterOpen = 2
            var counterSave = 2
            var counterShares = 6
            var counterModify = 4


            if (state.shareKeyCreated != null) {
                if (state.shareKeyCreated) {
                    showShareFolderOptions(state.node)
                } else {
                    Util.showSnackbar(
                        requireActivity(),
                        getString(R.string.general_something_went_wrong_error)
                    )
                }
                nodeOptionsViewModel.shareDialogDisplayed()
            }

            if (state.canMoveNode) {
                state.node?.let { onMoveClicked(it) }
                // Once the action has been acknowledged, notify the ViewModel
                nodeOptionsViewModel.setMoveNodeClicked(false)
            }
            if (state.canRestoreNode) {
                state.node?.let { onRestoreClicked(it) }
                // Once the action has been acknowledged, notify the ViewModel
                nodeOptionsViewModel.setRestoreNodeClicked(false)
            }

            state.node?.let { node ->
                if (megaApi.isInRubbish(node)) {
                    mode = RUBBISH_BIN_MODE
                } else if (nodeController.nodeComesFromIncoming(node)) {
                    mode = SHARED_ITEMS_MODE
                }

                if (mode == RECENTS_MODE || mode == FAVOURITES_IN_TAB_MODE) {
                    viewInFolder.visibility = View.VISIBLE
                    viewInFolder.setOnClickListener { onViewInFolderClicked(node) }
                } else {
                    viewInFolder.visibility = View.GONE
                    viewInFolder.setOnClickListener(null)
                }

                optionEdit.setOnClickListener { onEditClicked(node) }
                optionLabel.setOnClickListener { onLabelClicked(node) }
                optionFavourite.setOnClickListener { onFavouriteClicked(node) }
                optionDownload.setOnClickListener { onDownloadClicked(node) }
                optionOffline.setOnClickListener { onOfflineClicked(node) }
                optionInfo.setOnClickListener { onPropertiesClicked(node) }
                optionLink.setOnClickListener {
                    if (drawerItem == DrawerItem.SEARCH) {
                        Analytics.tracker.trackEvent(SearchResultGetLinkMenuItemEvent)
                    }
                    onLinkClicked(node)
                }
                optionRemoveLink.setOnClickListener { onRemoveLinkClicked(node) }
                optionShare.setOnClickListener {
                    if (drawerItem == DrawerItem.SEARCH) {
                        Analytics.tracker.trackEvent(SearchResultShareMenuItemEvent)
                    }
                    shareNode(requireActivity(), node)
                }
                optionShareFolder.setOnClickListener { nodeOptionsViewModel.createShareKey() }
                optionClearShares.setOnClickListener { onClearShareClicked(node) }
                optionLeaveShares.setOnClickListener { onLeaveShareClicked(node) }
                optionRename.setOnClickListener { onRenameClicked(node) }
                optionSendChat.setOnClickListener { onSendChatClicked(node) }
                optionMove.setOnClickListener { nodeOptionsViewModel.setMoveNodeClicked(true) }
                optionCopy.setOnClickListener { onCopyClicked(node) }
                optionRubbishBin.setOnClickListener { onDeleteClicked(node) }
                optionRestoreFromRubbish.setOnClickListener {
                    nodeOptionsViewModel.setRestoreNodeClicked(true)
                }
                optionRemove.setOnClickListener { onDeleteClicked(node) }
                optionOpenFolder.setOnClickListener { onOpenFolderClicked(node) }
                optionSlideshow.setOnClickListener { onSlideShowClicked(node) }
                optionOpenWith.setOnClickListener {
                    if (drawerItem == DrawerItem.SEARCH) {
                        Analytics.tracker.trackEvent(SearchResultOpenWithMenuItemEvent)
                    }
                    onOpenWithClicked(node)
                }
                optionVersionsLayout.setOnClickListener { onVersionsClicked(node) }

                val isTakenDown = node.isTakenDown
                val accessLevel = megaApi.getAccess(node)
                if (node.isFile && !isTakenDown) {
                    optionOpenWith.visibility = View.VISIBLE
                } else {
                    counterOpen--
                    optionOpenWith.visibility = View.GONE
                }
                if (isTakenDown) {
                    nodeName.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.red_800_red_400
                        )
                    )
                }

                //Due to requirement change, not just hide slideshow entry in node context menu
                optionSlideshow.visibility = View.GONE
                if (state.isOnline) {
                    nodeName.setupNodeTitleText(
                        nodeDeviceCenterInformation = state.nodeDeviceCenterInformation,
                        nodeShareInformation = state.shareData,
                        megaNode = node,
                    )
                    nodeThumb.setupNodeIcon(
                        nodeDeviceCenterInformation = state.nodeDeviceCenterInformation,
                        megaNode = node,
                    )
                    nodeInfo?.run {
                        setupNodeBodyText(
                            isClearSharesVisible = optionClearShares.isVisible,
                            nodeDeviceCenterInformation = state.nodeDeviceCenterInformation,
                            nodeShareInformation = state.shareData,
                            megaNode = node,
                        )
                        setupNodeBodyIcon(
                            nodeVersionsImageView = nodeVersionsIcon,
                            nodeStatusImageView = nodeStatusIcon,
                            nodeBodyTextView = this,
                            nodeDeviceCenterInformation = state.nodeDeviceCenterInformation,
                            megaNode = node,
                        )
                    }
                    if (node.isFolder) {
                        optionVersionsLayout.visibility = View.GONE
                        if (isEmptyFolder(node)) {
                            counterSave--
                            optionOffline.visibility = View.GONE
                        }
                        counterShares--
                        optionSendChat.visibility = View.GONE
                    } else {
                        if (typeForName(node.name).isOpenableTextFile(
                                node.size
                            ) && accessLevel >= MegaShare.ACCESS_READWRITE && !isTakenDown
                        ) {
                            optionEdit.visibility = View.VISIBLE
                        }
                        if (megaApi.hasVersions(node) && !isTakenDown) {
                            optionVersionsLayout.visibility = View.VISIBLE
                            versions.text = (megaApi.getNumVersions(node) - 1).toString()
                        } else {
                            optionVersionsLayout.visibility = View.GONE
                        }
                        if (isTakenDown) {
                            counterShares--
                            optionSendChat.visibility = View.GONE
                        } else {
                            optionSendChat.visibility = View.VISIBLE
                        }
                    }
                }
                if (isTakenDown) {
                    contentView.findViewById<View>(R.id.dispute_option).visibility = View.VISIBLE
                    contentView.findViewById<View>(R.id.dispute_option).setOnClickListener {
                        startActivity(
                            Intent(requireContext(), WebViewActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                .setData(Uri.parse(Constants.DISPUTE_URL))
                        )
                        dismiss()
                    }
                    counterSave--
                    optionDownload.visibility = View.GONE
                    if (optionOffline.isVisible()) {
                        counterSave--
                        optionOffline.visibility = View.GONE
                    }
                    separatorInfo.visibility = View.GONE
                    optionRename.visibility = View.VISIBLE
                } else {
                    offlineSwitch.isChecked = OfflineUtils.availableOffline(requireContext(), node)
                }
                optionLabel.visibility = if (isTakenDown) View.GONE else View.VISIBLE
                optionFavourite.visibility = if (isTakenDown) View.GONE else View.VISIBLE
                if (accessLevel != MegaShare.ACCESS_OWNER || isTakenDown) {
                    counterShares--
                    optionShare.visibility = View.GONE
                }
                if (node.isFolder) {
                    if (isTakenDown) {
                        counterShares--
                        optionShareFolder.visibility = View.GONE
                        counterShares--
                        optionClearShares.visibility = View.GONE
                    } else {
                        optionShareFolder.visibility = View.VISIBLE
                        if (isOutShare(node)) {
                            optionShareFolder.setText(R.string.manage_share)
                            optionClearShares.visibility = View.VISIBLE
                        } else {
                            optionShareFolder.setText(R.string.context_share_folder)
                            counterShares--
                            optionClearShares.visibility = View.GONE
                        }
                    }
                } else {
                    counterShares--
                    optionShareFolder.visibility = View.GONE
                    counterShares--
                    optionClearShares.visibility = View.GONE
                }
                if (isTakenDown) {
                    counterShares--
                    optionLink.visibility = View.GONE
                    counterShares--
                    optionRemoveLink.visibility = View.GONE
                    counterModify--
                    optionCopy.visibility = View.GONE
                } else {
                    optionLink.visibility = View.VISIBLE
                    if (node.isExported) {
                        //Node has public link
                        optionLink.setText(R.string.edit_link_option)
                        optionRemoveLink.visibility = View.VISIBLE
                    } else {
                        optionLink.text = resources.getQuantityString(R.plurals.get_links, 1)
                        counterShares--
                        optionRemoveLink.visibility = View.GONE
                    }
                }
                if (mode == DEFAULT_MODE) {
                    mapDrawerItemToMode(drawerItem)
                }
                when (mode) {
                    CLOUD_DRIVE_MODE, SEARCH_MODE -> {
                        Timber.d("show Cloud bottom sheet")
                        optionRemove.visibility = View.GONE
                        optionLeaveShares.visibility = View.GONE
                        counterOpen--
                        optionOpenFolder.visibility = View.GONE
                        counterModify--
                        optionRestoreFromRubbish.visibility = View.GONE
                    }

                    BACKUPS_MODE -> {
                        Timber.d("show My Backups bottom sheet")
                        counterModify--
                        optionRestoreFromRubbish.visibility = View.GONE
                    }

                    RUBBISH_BIN_MODE -> {
                        Timber.d("show Rubbish bottom sheet")
                        optionEdit.visibility = View.GONE
                        optionLabel.visibility = View.GONE
                        optionFavourite.visibility = View.GONE
                        if (optionOpenWith.isVisible()) {
                            counterOpen--
                            optionOpenWith.visibility = View.GONE
                        }
                        counterModify--
                        optionMove.visibility = View.GONE
                        counterModify--
                        optionRename.visibility = View.GONE
                        if (optionCopy.isVisible()) {
                            counterModify--
                            optionCopy.visibility = View.GONE
                        }
                        if (optionClearShares.isVisible()) {
                            counterShares--
                            optionClearShares.visibility = View.GONE
                        }
                        optionLeaveShares.visibility = View.GONE
                        optionRubbishBin.visibility = View.GONE
                        if (optionShare.isVisible()) {
                            counterShares--
                            optionShare.visibility = View.GONE
                        }
                        if (optionShareFolder.isVisible()) {
                            counterShares--
                            optionShareFolder.visibility = View.GONE
                        }
                        if (optionLink.isVisible()) {
                            counterShares--
                            optionLink.visibility = View.GONE
                        }
                        if (optionRemoveLink.isVisible()) {
                            counterShares--
                            optionRemoveLink.visibility = View.GONE
                        }
                        counterOpen--
                        optionOpenFolder.visibility = View.GONE
                        if (optionDownload.isVisible()) {
                            counterSave--
                            optionDownload.visibility = View.GONE
                        }
                        if (optionOffline.isVisible()) {
                            counterSave--
                            optionOffline.visibility = View.GONE
                        }
                        if (optionSendChat.isVisible()) {
                            counterShares--
                            optionSendChat.visibility = View.GONE
                        }
                    }

                    SHARED_ITEMS_MODE -> {
                        val tabSelected = (requireActivity() as ManagerActivity).tabItemShares
                        if (tabSelected === SharesTab.INCOMING_TAB || nodeController.nodeComesFromIncoming(
                                node
                            )
                        ) {
                            Timber.d("showOptionsPanelIncoming")
                            optionRemove.visibility = View.GONE
                            if (optionShareFolder.isVisible()) {
                                counterShares--
                                optionShareFolder.visibility = View.GONE
                            }
                            if (optionClearShares.isVisible()) {
                                counterShares--
                                optionClearShares.visibility = View.GONE
                            }
                            val dBT = nodeController.getIncomingLevel(node)
                            Timber.d("DeepTree value:%s", dBT)
                            if (dBT > Constants.FIRST_NAVIGATION_LEVEL) {
                                optionLeaveShares.visibility = View.GONE
                            } else {
                                optionLeaveShares.visibility =
                                    if (isTakenDown) View.GONE else View.VISIBLE
                                permissionsIcon.visibility = View.VISIBLE
                                when (accessLevel) {
                                    MegaShare.ACCESS_FULL -> {
                                        Timber.d("LEVEL 0 - access FULL")
                                        permissionsIcon.setImageResource(R.drawable.ic_shared_fullaccess)
                                    }

                                    MegaShare.ACCESS_READ -> {
                                        Timber.d("LEVEL 0 - access read")
                                        permissionsIcon.setImageResource(R.drawable.ic_shared_read)
                                    }

                                    MegaShare.ACCESS_READWRITE -> {
                                        Timber.d("LEVEL 0 - readwrite")
                                        permissionsIcon.setImageResource(R.drawable.ic_shared_read_write)
                                    }

                                    else -> {}
                                }
                            }
                            if (optionLink.isVisible()) {
                                counterShares--
                                optionLink.visibility = View.GONE
                            }
                            if (optionRemoveLink.isVisible()) {
                                counterShares--
                                optionRemoveLink.visibility = View.GONE
                            }
                            when (accessLevel) {
                                MegaShare.ACCESS_FULL -> {
                                    Timber.d("access FULL")
                                    optionFavourite.visibility = View.GONE
                                    if (dBT <= Constants.FIRST_NAVIGATION_LEVEL) {
                                        optionRubbishBin.visibility = View.GONE
                                        counterModify--
                                        optionMove.visibility = View.GONE
                                    }
                                }

                                MegaShare.ACCESS_READ -> {
                                    Timber.d("access read")
                                    optionLabel.visibility = View.GONE
                                    optionFavourite.visibility = View.GONE
                                    counterModify--
                                    optionRename.visibility = View.GONE
                                    counterModify--
                                    optionMove.visibility = View.GONE
                                    optionRubbishBin.visibility = View.GONE
                                }

                                MegaShare.ACCESS_READWRITE -> {
                                    Timber.d("readwrite")
                                    optionLabel.visibility = View.GONE
                                    optionFavourite.visibility = View.GONE
                                    counterModify--
                                    optionRename.visibility = View.GONE
                                    counterModify--
                                    optionMove.visibility = View.GONE
                                    optionRubbishBin.visibility = View.GONE
                                }

                                else -> {}
                            }
                        } else if (tabSelected === SharesTab.OUTGOING_TAB) {
                            Timber.d("showOptionsPanelOutgoing")
                            val isClearSharesVisible = optionClearShares.isVisible
                            if (!canShowOutgoingShareContacts(
                                    isNodeTakenDown = isTakenDown,
                                    isClearSharesVisible = isClearSharesVisible,
                                ) && isClearSharesVisible
                            ) {
                                counterShares--
                                optionClearShares.visibility = View.GONE
                            }
                            counterModify--
                            optionMove.visibility = View.GONE
                            optionRemove.visibility = View.GONE
                            optionLeaveShares.visibility = View.GONE
                        } else if (tabSelected === SharesTab.LINKS_TAB) {
                            if (!isTakenDown && node.isShared) {
                                optionClearShares.visibility = View.VISIBLE
                            } else if (optionClearShares.isVisible()) {
                                counterShares--
                                optionClearShares.visibility = View.GONE
                            }
                            counterModify--
                            optionMove.visibility = View.GONE
                            optionRemove.visibility = View.GONE
                            optionLeaveShares.visibility = View.GONE
                        }
                        counterOpen--
                        optionOpenFolder.visibility = View.GONE
                        counterModify--
                        optionRestoreFromRubbish.visibility = View.GONE
                    }

                    RECENTS_MODE, FAVOURITES_IN_TAB_MODE, FAVOURITES_MODE ->
                        // If the Dialog Fragment is opened from the Favourites page, handle the
                        // display of the Dialog options accordingly
                        handleRecentsAndFavouritesOptionsDisplay(
                            accessLevel = accessLevel,
                            decrementShares = { counterShares-- },
                            decrementOpen = { counterOpen-- },
                            decrementModify = { counterModify-- },
                        )

                    else -> {}
                }

                // After setting up the content to be displayed in the Options Dialog, check if
                // read-only properties should be applied.
                checkIfShouldApplyReadOnlyState(
                    node = node,
                    decrementOpen = { counterOpen-- },
                    decrementModify = { counterModify-- },
                )
                separatorOpen.visibility = if (counterOpen <= 0) View.GONE else View.VISIBLE
                separatorDownload.visibility = if (counterSave <= 0) View.GONE else View.VISIBLE
                separatorShares.visibility = if (counterShares <= 0) View.GONE else View.VISIBLE
                separatorModify.visibility = if (counterModify <= 0) View.GONE else View.VISIBLE
                offlineSwitch.setOnCheckedChangeListener { _: CompoundButton, _: Boolean ->
                    onOfflineClicked(node)
                }
                optionFavourite.setText(if (node.isFavourite) R.string.file_properties_unfavourite else R.string.file_properties_favourite)
                optionFavourite.setCompoundDrawablesWithIntrinsicBounds(
                    if (node.isFavourite) R.drawable.ic_remove_favourite else R.drawable.ic_add_favourite,
                    0, 0, 0
                )
                if (node.label != MegaNode.NODE_LBL_UNKNOWN) {
                    val color = ResourcesCompat.getColor(
                        resources, getNodeLabelColor(
                            node.label
                        ), null
                    )
                    val drawable = getNodeLabelDrawable(
                        node.label, resources
                    )
                    optionLabelCurrent.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        null,
                        null,
                        drawable,
                        null
                    )
                    optionLabelCurrent.text = getNodeLabelText(node.label, requireContext())
                    optionLabelCurrent.setTextColor(color)
                    optionLabelCurrent.visibility = View.VISIBLE
                } else {
                    optionLabelCurrent.visibility = View.GONE
                }
                state.shareData?.let { data -> hideNodeActions(data, node) }

                if (savedInstanceState?.getBoolean(
                        Constants.CANNOT_OPEN_FILE_SHOWN,
                        false
                    ) == true
                ) {
                    contentView.post {
                        cannotOpenFileDialog = this.showCannotOpenFileDialog(
                            requireActivity(),
                            node,
                            (requireActivity() as ManagerActivity)::saveNodeByTap
                        )
                    }
                }
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }

    /**
     * Sets the Node Icon based on varying conditions
     *
     * @param nodeDeviceCenterInformation Contains specific information of a Device Center Node to
     * be displayed
     * @param megaNode The [MegaNode] that was loaded
     */
    private fun ImageView.setupNodeIcon(
        nodeDeviceCenterInformation: NodeDeviceCenterInformation?,
        megaNode: MegaNode,
    ) {
        if (nodeDeviceCenterInformation != null) {
            setImageResource(nodeDeviceCenterInformation.icon)
        } else {
            if (megaNode.isFolder) {
                drawerItem?.let { nonNullDrawerItem ->
                    setImageResource(
                        getFolderIcon(
                            node = megaNode,
                            drawerItem = nonNullDrawerItem,
                        )
                    )
                }
            } else {
                val thumbnailParams = layoutParams as? ConstraintLayout.LayoutParams
                thumbnailParams?.let {
                    load(ThumbnailRequest(NodeId(megaNode.handle))) {
                        size(Util.dp2px(Constants.THUMB_SIZE_DP.toFloat()))
                        transformations(
                            RoundedCornersTransformation(
                                Util.dp2px(Constants.THUMB_CORNER_RADIUS_DP).toFloat()
                            )
                        )
                        listener(
                            onSuccess = { _, _ ->
                                thumbnailParams.width = Util.dp2px(Constants.THUMB_SIZE_DP.toFloat())
                                thumbnailParams.height = thumbnailParams.width
                                thumbnailParams.setMargins(0, 0, 0, 0)
                                layoutParams = thumbnailParams
                            },
                            onError = { _, _ ->
                                thumbnailParams.width = Util.dp2px(Constants.ICON_SIZE_DP.toFloat())
                                thumbnailParams.height = thumbnailParams.width
                                thumbnailParams.setMargins(0, 0, 0, 0)
                                setImageResource(typeForName(megaNode.name).iconResourceId)
                                layoutParams = thumbnailParams
                            },
                        )
                    }
                }
            }
        }
    }

    /**
     * Sets the Node Title text based on varying conditions
     *
     * @param nodeDeviceCenterInformation Contains specific information of a Device Center Node to
     * be displayed
     * @param nodeShareInformation The Share Information
     * @param megaNode The [MegaNode] that was loaded
     */
    private fun TextView.setupNodeTitleText(
        nodeDeviceCenterInformation: NodeDeviceCenterInformation?,
        nodeShareInformation: NodeShareInformation?,
        megaNode: MegaNode,
    ) {
        text = when {
            // Triggered if the Bottom Sheet is accessed from Device Center
            nodeDeviceCenterInformation != null -> nodeDeviceCenterInformation.name
            nodeShareInformation != null && nodeController.nodeComesFromIncoming(megaNode) -> {
                resources.getString(R.string.shared_items_verify_credentials_undecrypted_folder)
            }

            else -> megaNode.name
        }
    }

    /**
     * Sets the Node Body Text based on varying conditions
     *
     * @param isClearSharesVisible true if the "Clear Shares" is visible and false if otherwise
     * @param nodeDeviceCenterInformation Contains specific information of a Device Center Node to
     * be displayed
     * @param nodeShareInformation The Share Information
     * @param megaNode The [MegaNode] that was loaded
     */
    private fun TextView.setupNodeBodyText(
        isClearSharesVisible: Boolean,
        nodeDeviceCenterInformation: NodeDeviceCenterInformation?,
        nodeShareInformation: NodeShareInformation?,
        megaNode: MegaNode,
    ) {
        if (nodeDeviceCenterInformation != null) {
            text = nodeDeviceCenterInformation.status
            nodeDeviceCenterInformation.statusColorInt?.let { statusColorInt ->
                setTextColor(statusColorInt)
            }
        } else {
            text = if (megaNode.isFolder) {
                MegaApiUtils.getMegaNodeFolderInfo(megaNode, context)
            } else {
                getFileInfo(megaNode, requireContext())
            }
            if (megaNode.isFirstNavIncomingNode()) {
                setIncomingSharesNodeBodyText(megaNode)
            } else if (megaNode.isOutgoingNodeBeingShared(isClearSharesVisible)) {
                setOutgoingSharesNodeBodyText(megaNode)
            }
            setUnverifiedOutgoingNodeUserName(nodeShareInformation)
        }
    }

    /**
     * Checks whether the provided [MegaNode] originated from Incoming Shares and is in the first
     * Navigation Level
     *
     * @return True if the [MegaNode] is in Incoming Shares and is in the first Navigation Level.
     * False is returned if any of the conditions do not match
     */
    private fun MegaNode.isFirstNavIncomingNode(): Boolean {
        val tabSelected = (requireActivity() as? ManagerActivity)?.tabItemShares
        val isInSharedItemsMode = mode == SHARED_ITEMS_MODE
        val isIncomingNode =
            (tabSelected === SharesTab.INCOMING_TAB) || (nodeController.nodeComesFromIncoming(
                this
            ))
        val isIncomingFirstNavLevel =
            nodeController.getIncomingLevel(this) <= Constants.FIRST_NAVIGATION_LEVEL

        return isInSharedItemsMode && isIncomingNode && isIncomingFirstNavLevel
    }

    /**
     * Sets the Incoming Shares Node Body Text by retrieving the owner of the Shared [MegaNode]
     *
     * @param megaNode The passed [MegaNode]
     */
    private fun TextView.setIncomingSharesNodeBodyText(megaNode: MegaNode) {
        megaApi.inSharesList?.let { incomingShares ->
            incomingShares.forEach {
                it?.let { incomingShare ->
                    if (incomingShare.nodeHandle == megaNode.handle) {
                        text = megaApi.getContact(incomingShare.user)?.let { megaUser ->
                            ContactUtil.getMegaUserNameDB(megaUser)
                        } ?: incomingShare.user
                    }
                }
            }
        }
    }

    /**
     * Checks whether the [MegaNode] originated from Outgoing Shares and is being Shared
     *
     * @param isClearSharesVisible true if the "Clear Shares" is visible and false if otherwise
     * @return true if the [MegaNode] is in Outgoing Shares and is being shared. It is false if any
     * of the conditions fail
     */
    private fun MegaNode.isOutgoingNodeBeingShared(
        isClearSharesVisible: Boolean,
    ): Boolean = (requireActivity() as? ManagerActivity)?.let { managerActivity ->
        val isInSharedItemsMode = mode == SHARED_ITEMS_MODE
        val isOutgoingTabSelected = managerActivity.tabItemShares === SharesTab.OUTGOING_TAB

        isInSharedItemsMode && isOutgoingTabSelected && canShowOutgoingShareContacts(
            isNodeTakenDown = isTakenDown,
            isClearSharesVisible = isClearSharesVisible,
        )
    } ?: false

    /**
     * Checks if the Contacts listed in the Outgoing Shares can be shown or not
     *
     * @param isNodeTakenDown true if the [MegaNode] is taken down, and false if otherwise
     * @param isClearSharesVisible true if the "Clear Shares" is visible and false if otherwise
     *
     * @return true if the listed Contacts can be shown, and false if otherwise
     */
    private fun canShowOutgoingShareContacts(
        isNodeTakenDown: Boolean,
        isClearSharesVisible: Boolean,
    ): Boolean = (requireActivity() as? ManagerActivity)?.let { managerActivity ->
        val isOutgoingInFirstNavigationLevel =
            managerActivity.deepBrowserTreeOutgoing == Constants.FIRST_NAVIGATION_LEVEL
        !isNodeTakenDown && isOutgoingInFirstNavigationLevel && isClearSharesVisible
    } ?: false

    /**
     * Sets the Outgoing Shares Node Body Text by retrieving the number of contacts who shared the [MegaNode]
     *
     * @param megaNode The passed [MegaNode]
     */
    private fun TextView.setOutgoingSharesNodeBodyText(megaNode: MegaNode) {
        megaApi.getOutShares(megaNode)?.let { outgoingShares ->
            val verifiedOutgoingShares =
                outgoingShares.filter { megaShare -> megaShare != null && megaShare.isVerified }
            if (verifiedOutgoingShares.isNotEmpty()) {
                text = resources.getQuantityString(
                    R.plurals.general_num_shared_with,
                    verifiedOutgoingShares.size,
                    verifiedOutgoingShares.size,
                )
            }
        }
    }

    /**
     * Sets the Node Information of the Unverified Node with the Contact Name
     *
     * @param nodeShareInformation The [NodeShareInformation] which can be potentially nullable
     */
    private fun TextView.setUnverifiedOutgoingNodeUserName(nodeShareInformation: NodeShareInformation?) {
        nodeShareInformation?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                text = nodeOptionsViewModel.getUnverifiedOutgoingNodeUserName()
            }
        }
    }

    /**
     * Sets the Node Body Icon based on varying conditions
     *
     * @param nodeVersionsImageView The [ImageView] holding the Versions Icon
     * @param nodeStatusImageView The [ImageView] holding the Node Status Icon
     * @param nodeBodyTextView The [TextView] holding the Node Body Text
     * @param nodeDeviceCenterInformation Contains specific information of a Device Center Node to
     * be displayed
     * @param megaNode The [MegaNode] that was loaded
     */
    private fun setupNodeBodyIcon(
        nodeVersionsImageView: ImageView,
        nodeStatusImageView: ImageView,
        nodeBodyTextView: TextView,
        nodeDeviceCenterInformation: NodeDeviceCenterInformation?,
        megaNode: MegaNode,
    ) {
        if (nodeDeviceCenterInformation != null) {
            nodeVersionsImageView.visibility = View.GONE
            nodeStatusImageView.setupNodeStatusIcon(nodeDeviceCenterInformation)
        } else {
            nodeStatusImageView.visibility = View.GONE
            nodeVersionsImageView.setupVersionsIconVisibility(megaNode)
        }
        nodeBodyTextView.handleStartMargin(
            isNodeVersionsImageVisible = nodeVersionsImageView.isVisible,
            isNodeStatusImageVisible = nodeStatusImageView.isVisible,
        )
    }

    /**
     * Sets the Node Status Icon
     *
     * @param nodeDeviceCenterInformation Contains specific information of a Device Center Node to
     * be displayed
     */
    private fun ImageView.setupNodeStatusIcon(
        nodeDeviceCenterInformation: NodeDeviceCenterInformation,
    ) {
        val nodeStatusIcon = nodeDeviceCenterInformation.statusIcon
        val nodeStatusIconColorInt = nodeDeviceCenterInformation.statusColorInt

        if (nodeStatusIcon != null) {
            visibility = View.VISIBLE
            setImageResource(nodeStatusIcon)
            nodeStatusIconColorInt?.let { nonNullNodeStatusIconColorInt ->
                setColorFilter(nonNullNodeStatusIconColorInt)
            }
        } else {
            visibility = View.GONE
        }
    }

    /**
     * Sets the Versions Icon Visibility
     *
     * @param megaNode The [MegaNode] that was loaded
     */
    private fun ImageView.setupVersionsIconVisibility(
        megaNode: MegaNode,
    ) = if (!megaNode.isFolder && !megaNode.isTakenDown && megaApi.hasVersions(megaNode)) {
        visibility = View.VISIBLE
    } else {
        visibility = View.GONE
    }

    /**
     * Checks whether the Node Body Text should apply a Start Margin of 8 dp or not
     *
     * @param isNodeVersionsImageVisible true if the Node Versions Image is visible, and false if otherwise
     * @param isNodeStatusImageVisible true if the Node Status Image is visible, and false if otherwise
     */
    private fun TextView.handleStartMargin(
        isNodeVersionsImageVisible: Boolean,
        isNodeStatusImageVisible: Boolean,
    ) {
        if (isNodeVersionsImageVisible || isNodeStatusImageVisible) {
            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginStart = Util.dp2px(8F)
            }
        }
    }

    /**
     * onDestroyView
     */
    override fun onDestroyView() {
        dismissAlertDialogIfExists(cannotOpenFileDialog)
        super.onDestroyView()
    }

    /**
     * When the Node is a Backup Node, apply read-only Restrictions by hiding specific options
     *
     * Hiding the aforementioned options will reduce the counter values for the Modify and Open
     * group options
     */
    private fun checkIfShouldApplyReadOnlyState(
        node: MegaNode?,
        decrementOpen: () -> Unit,
        decrementModify: () -> Unit,
    ) {
        val optionEdit = contentView.findViewById<LinearLayout>(R.id.edit_file_option)
        val optionFavourite = contentView.findViewById<TextView>(R.id.favorite_option)
        val optionLabel = contentView.findViewById<LinearLayout>(R.id.option_label_layout)
        val optionRename = contentView.findViewById<TextView>(R.id.rename_option)
        val optionMove = contentView.findViewById<TextView>(R.id.move_option)
        val optionLeaveShares = contentView.findViewById<TextView>(R.id.leave_share_option)
        val optionOpenFolder = contentView.findViewById<TextView>(R.id.open_folder_option)
        val optionRubbishBin = contentView.findViewById<TextView>(R.id.rubbish_bin_option)
        val optionRemove = contentView.findViewById<TextView>(R.id.remove_option)
        if (node != null && megaApi.isInInbox(node)) {
            optionEdit.visibility = View.GONE
            optionFavourite.visibility = View.GONE
            optionLabel.visibility = View.GONE
            optionRename.visibility = View.GONE
            optionMove.visibility = View.GONE
            optionRubbishBin.visibility = View.GONE
            optionRemove.visibility = View.GONE
            optionLeaveShares.visibility = View.GONE
            optionOpenFolder.visibility = View.GONE
            decrementModify()
            decrementModify()
            decrementModify()
            decrementOpen()
        }
    }

    /**
     * Displays specific actions when the Dialog Fragment is accessed from the Recents or Favourites page.
     * This will also reduce the counter values of different group options
     *
     * @param accessLevel - Access Level of the Node
     */
    private fun handleRecentsAndFavouritesOptionsDisplay(
        accessLevel: Int,
        decrementShares: () -> Unit,
        decrementOpen: () -> Unit,
        decrementModify: () -> Unit,
    ) {
        val optionFavourite = contentView.findViewById<TextView>(R.id.favorite_option)
        val optionLabel = contentView.findViewById<LinearLayout>(R.id.option_label_layout)
        val optionRename = contentView.findViewById<TextView>(R.id.rename_option)
        val optionMove = contentView.findViewById<TextView>(R.id.move_option)
        val optionLeaveShares = contentView.findViewById<TextView>(R.id.leave_share_option)
        val optionOpenFolder = contentView.findViewById<TextView>(R.id.open_folder_option)
        val optionRestoreFromRubbish = contentView.findViewById<TextView>(R.id.restore_option)
        val optionRubbishBin = contentView.findViewById<TextView>(R.id.rubbish_bin_option)
        val optionRemove = contentView.findViewById<TextView>(R.id.remove_option)
        val optionLink = contentView.findViewById<TextView>(R.id.link_option)
        val optionRemoveLink = contentView.findViewById<TextView>(R.id.remove_link_option)
        val optionShareFolder = contentView.findViewById<TextView>(R.id.share_folder_option)
        val optionClearShares = contentView.findViewById<TextView>(R.id.clear_share_option)
        if (optionShareFolder.isVisible()) {
            decrementShares()
            optionShareFolder.visibility = View.GONE
        }
        if (optionClearShares.isVisible()) {
            decrementShares()
            optionClearShares.visibility = View.GONE
        }
        optionRemove.visibility = View.GONE
        optionLeaveShares.visibility = View.GONE
        decrementOpen()
        optionOpenFolder.visibility = View.GONE
        decrementModify()
        optionRestoreFromRubbish.visibility = View.GONE
        when (accessLevel) {
            MegaShare.ACCESS_READWRITE, MegaShare.ACCESS_READ, MegaShare.ACCESS_UNKNOWN -> {
                optionLabel.visibility = View.GONE
                optionFavourite.visibility = View.GONE
                decrementModify()
                optionRename.visibility = View.GONE
                decrementModify()
                optionMove.visibility = View.GONE
                optionRubbishBin.visibility = View.GONE
                if (optionLink.isVisible()) {
                    decrementShares()
                    optionLink.visibility = View.GONE
                }
                if (optionRemoveLink.isVisible()) {
                    decrementShares()
                    optionRemoveLink.visibility = View.GONE
                }
            }

            else -> {}
        }
    }

    private fun hideNodeActions(nodeShareInformation: NodeShareInformation, node: MegaNode) {
        val optionVerifyUser = contentView.findViewById<TextView>(R.id.verify_user_option)
        optionVerifyUser.visibility = View.VISIBLE
        contentView.findViewById<View>(R.id.separator_info_option).visibility = View.VISIBLE
        if (!nodeShareInformation.isVerified) {
            optionVerifyUser.text = getString(
                R.string.shared_items_bottom_sheet_menu_verify_user,
                nodeInfo?.text
            )
        }
        optionVerifyUser.setOnClickListener { onVerifyUserClicked(nodeShareInformation, node) }
        contentView.findViewById<View>(R.id.favorite_option).visibility = View.GONE
        contentView.findViewById<View>(R.id.rename_option).visibility = View.GONE
        contentView.findViewById<View>(R.id.link_option).visibility =
            View.GONE
        contentView.findViewById<View>(R.id.remove_option).visibility = View.GONE
        contentView.findViewById<View>(R.id.download_option).visibility =
            View.GONE
        contentView.findViewById<View>(R.id.option_offline_layout).visibility = View.GONE
        contentView.findViewById<View>(R.id.copy_option).visibility =
            View.GONE
        contentView.findViewById<View>(R.id.rubbish_bin_option).visibility = View.GONE
        contentView.findViewById<View>(R.id.share_option).visibility = View.GONE
        contentView.findViewById<View>(R.id.clear_share_option).visibility = View.GONE
    }

    /**
     * Executes logic when the "Save to device" Option is clicked
     *
     * @param node The [MegaNode] to be downloaded
     */
    private fun onDownloadClicked(node: MegaNode) {
        if (drawerItem == DrawerItem.SEARCH) {
            Analytics.tracker.trackEvent(SearchResultSaveToDeviceMenuItemEvent)
        }
        lifecycleScope.launch {
            if (nodeOptionsDownloadViewModel.shouldDownloadWithDownloadWorker()) {
                nodeOptionsDownloadViewModel.onDownloadClicked(NodeId(node.handle))
            } else {
                (activity as? ManagerActivity)?.saveNodesToDevice(
                    nodes = listOf(node),
                    highPriority = false,
                    isFolderLink = false,
                    fromMediaViewer = false,
                    fromChat = false,
                )
            }
        }
        setStateBottomSheetBehaviorHidden()
    }

    private fun onFavouriteClicked(node: MegaNode) {
        megaApi.setNodeFavourite(node, !node.isFavourite)
        setStateBottomSheetBehaviorHidden()
    }

    private fun onLabelClicked(node: MegaNode) {
        (requireActivity() as ManagerActivity).showNodeLabelsPanel(
            NodeId(node.handle)
        )
        setStateBottomSheetBehaviorHidden()
    }

    private fun onOfflineClicked(node: MegaNode) {
        if (OfflineUtils.availableOffline(
                requireContext(),
                node
            )
        ) {
            nodeOptionsViewModel.removeOfflineNode(node.handle)
            refreshView()
            Util.showSnackbar(activity, resources.getString(R.string.file_removed_offline))
        } else {
            lifecycleScope.launch {
                if (nodeOptionsDownloadViewModel.shouldDownloadWithDownloadWorker()) {
                    nodeOptionsDownloadViewModel.onSaveOfflineClicked(NodeId(node.handle))
                } else {
                    saveForOffline(node)
                }
            }
        }
        setStateBottomSheetBehaviorHidden()
    }

    private fun onPropertiesClicked(
        node: MegaNode,
    ) {
        val fileInfoIntent = Intent(requireContext(), FileInfoActivity::class.java)
        fileInfoIntent.putExtra(Constants.HANDLE, node.handle)
        if (drawerItem === DrawerItem.SHARED_ITEMS) {
            if ((requireActivity() as ManagerActivity).tabItemShares === SharesTab.INCOMING_TAB) {
                fileInfoIntent.putExtra(INTENT_EXTRA_KEY_FROM, Constants.FROM_INCOMING_SHARES)
                fileInfoIntent.putExtra(
                    Constants.INTENT_EXTRA_KEY_FIRST_LEVEL,
                    (requireActivity() as ManagerActivity).deepBrowserTreeIncoming <= Constants.FIRST_NAVIGATION_LEVEL
                )
            } else if ((requireActivity() as ManagerActivity).tabItemShares === SharesTab.OUTGOING_TAB) {
                fileInfoIntent.putExtra(
                    Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
                    Constants.OUTGOING_SHARES_ADAPTER
                )
            }
        } else if (drawerItem === DrawerItem.BACKUPS) {
            if ((requireActivity() as ManagerActivity).tabItemShares === SharesTab.INCOMING_TAB) {
                fileInfoIntent.putExtra(INTENT_EXTRA_KEY_FROM, Constants.FROM_BACKUPS)
            }
        } else if (drawerItem === DrawerItem.SEARCH && nodeController.nodeComesFromIncoming(
                node
            )
        ) {
            fileInfoIntent.putExtra(INTENT_EXTRA_KEY_FROM, Constants.FROM_INCOMING_SHARES)
            val dBT = nodeController.getIncomingLevel(node)
            fileInfoIntent.putExtra(
                Constants.INTENT_EXTRA_KEY_FIRST_LEVEL,
                dBT <= Constants.FIRST_NAVIGATION_LEVEL
            )
        }
        fileInfoIntent.putExtra(Constants.NAME, node.name)
        startActivityForResult(fileInfoIntent, Constants.REQUEST_CODE_FILE_INFO)
        dismissAllowingStateLoss()
        setStateBottomSheetBehaviorHidden()
    }

    private fun onLinkClicked(node: MegaNode) {
        (requireActivity() as ManagerActivity).showGetLinkActivity(
            node.handle
        )
        setStateBottomSheetBehaviorHidden()
    }

    private fun onViewInFolderClicked(node: MegaNode) {
        (requireActivity() as ManagerActivity).viewNodeInFolder(
            node
        )
        setStateBottomSheetBehaviorHidden()
    }

    private fun onRemoveLinkClicked(node: MegaNode) {
        RemovePublicLinkDialogFragment.newInstance(listOf(node.handle))
            .show(requireActivity().supportFragmentManager, RemovePublicLinkDialogFragment.TAG)
        setStateBottomSheetBehaviorHidden()
    }

    private fun onClearShareClicked(node: MegaNode?) {
        node?.let {
            RemoveAllSharingContactDialogFragment.newInstance(listOf(node.handle))
                .show(parentFragmentManager, RemoveAllSharingContactDialogFragment.TAG)
        }
        setStateBottomSheetBehaviorHidden()
    }

    private fun onLeaveShareClicked(node: MegaNode) {
        showConfirmationLeaveIncomingShare(
            requireActivity(),
            (requireActivity() as SnackbarShower), node
        )
        setStateBottomSheetBehaviorHidden()
    }

    private fun onRenameClicked(node: MegaNode) {
        (requireActivity() as ManagerActivity).showRenameDialog(node)
        setStateBottomSheetBehaviorHidden()
    }

    private fun onSendChatClicked(node: MegaNode?) {
        (requireActivity() as ManagerActivity).attachNodeToChats(node)
        dismissAllowingStateLoss()
        setStateBottomSheetBehaviorHidden()
    }

    private fun onMoveClicked(node: MegaNode) {
        nodeController.chooseLocationToMoveNodes(listOf(node.handle))
        dismissAllowingStateLoss()
        setStateBottomSheetBehaviorHidden()
    }

    private fun onCopyClicked(node: MegaNode) {
        nodeController.chooseLocationToCopyNodes(listOf(node.handle))
        dismissAllowingStateLoss()
        setStateBottomSheetBehaviorHidden()
    }

    private fun onDeleteClicked(node: MegaNode) {
        ConfirmMoveToRubbishBinDialogFragment.newInstance(listOf(node.handle))
            .show(
                requireActivity().supportFragmentManager,
                ConfirmMoveToRubbishBinDialogFragment.TAG
            )
        setStateBottomSheetBehaviorHidden()
    }

    private fun onSlideShowClicked(node: MegaNode) {
        val intent = getIntentForParentNode(
            requireContext(),
            megaApi.getParentNode(node)?.handle,
            SortOrder.ORDER_DEFAULT_ASC,
            node.handle,
            true
        )
        startActivity(intent)
        dismissAllowingStateLoss()
        setStateBottomSheetBehaviorHidden()
    }

    private fun onOpenFolderClicked(node: MegaNode) {
        searchViewModel.setTextSubmitted(true)
        nodeController.openFolderFromSearch(node.handle)
        dismissAllowingStateLoss()
        setStateBottomSheetBehaviorHidden()
    }

    private fun onOpenWithClicked(node: MegaNode) {
        if (nodeOptionsViewModel.isFilePreviewOnline(node = node)) {
            openWith(
                context = requireActivity(),
                node = node,
                (requireActivity() as ManagerActivity)::saveNodeByOpenWith
            )
        } else {
            onNodeTapped(
                requireActivity(),
                node,
                (requireActivity() as ManagerActivity)::saveNodeByOpenWith,
                (requireActivity() as ManagerActivity),
                (requireActivity() as ManagerActivity),
                true
            )
        }
        dismissAllowingStateLoss()
    }

    private fun onRestoreClicked(node: MegaNode) {
        (requireActivity() as ManagerActivity).restoreFromRubbish(listOf(node))
        setStateBottomSheetBehaviorHidden()
    }

    private fun onEditClicked(node: MegaNode) {
        manageEditTextFileIntent(
            requireContext(),
            node,
            adapterType
        )
        setStateBottomSheetBehaviorHidden()
    }

    private fun onVersionsClicked(node: MegaNode) {
        val version = Intent(activity, VersionsFileActivity::class.java)
        version.putExtra(Constants.HANDLE, node.handle)
        requireActivity().startActivityForResult(
            version,
            Constants.REQUEST_CODE_DELETE_VERSIONS_HISTORY
        )
        setStateBottomSheetBehaviorHidden()
    }

    private fun onVerifyUserClicked(
        nodeShareInformation: NodeShareInformation,
        node: MegaNode,
    ) {
        if (!nodeShareInformation.isVerified && nodeShareInformation.isPending) {
            showCanNotVerifyContact(nodeShareInformation.user)
        } else {
            openAuthenticityCredentials(nodeShareInformation.user, node)
        }
        setStateBottomSheetBehaviorHidden()
    }


    private fun refreshView() {
        when (drawerItem) {
            DrawerItem.CLOUD_DRIVE, DrawerItem.RUBBISH_BIN -> (requireActivity() as ManagerActivity).onNodesCloudDriveUpdate()
            DrawerItem.BACKUPS -> (requireActivity() as ManagerActivity).onNodesBackupsUpdate()
            DrawerItem.SHARED_ITEMS -> (requireActivity() as ManagerActivity).refreshSharesFragments()
            DrawerItem.SEARCH -> (requireActivity() as ManagerActivity).onNodesSearchUpdate()

            else -> {}
        }
    }

    private fun saveForOffline(node: MegaNode?) {
        var adapterType = Constants.FROM_OTHERS
        when (drawerItem) {
            DrawerItem.BACKUPS -> adapterType = Constants.FROM_BACKUPS
            DrawerItem.SHARED_ITEMS -> if ((requireActivity() as ManagerActivity).tabItemShares === SharesTab.INCOMING_TAB) {
                adapterType = Constants.FROM_INCOMING_SHARES
            }

            else -> {}
        }
        val offlineParent =
            OfflineUtils.getOfflineParentFile(requireActivity(), adapterType, node, megaApi)
        if (FileUtil.isFileAvailable(offlineParent)) {
            val offlineFile = node?.name?.let { File(offlineParent, it) }
            if (FileUtil.isFileAvailable(offlineFile)) {
                if (FileUtil.isFileDownloadedLatest(offlineFile, node)
                    && offlineFile?.length() == node?.size
                ) {
                    // if the file matches to the latest on the cloud, do nothing
                    return
                } else {
                    // if the file does not match the latest on the cloud, delete the old file offline database record
                    val parentName = OfflineUtils.getOfflineParentFileName(
                        requireContext(),
                        node
                    ).absolutePath + File.separator
                    node?.let {
                        nodeOptionsViewModel.removeOfflineNode(it.handle)
                        refreshView()
                    }
                }
            }
        }

        // Save the new file to offline
        OfflineUtils.saveOffline(offlineParent, node, requireActivity())
    }

    /**
     * onSaveInstanceState
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putAll(arguments)
        outState.putBoolean(
            Constants.CANNOT_OPEN_FILE_SHOWN,
            isAlertDialogShown(cannotOpenFileDialog)
        )
    }

    private fun mapDrawerItemToMode(drawerItem: DrawerItem?) {
        when (drawerItem) {
            DrawerItem.CLOUD_DRIVE -> mode = CLOUD_DRIVE_MODE
            DrawerItem.RUBBISH_BIN -> mode = RUBBISH_BIN_MODE
            DrawerItem.BACKUPS -> mode = BACKUPS_MODE
            DrawerItem.SHARED_ITEMS -> mode = SHARED_ITEMS_MODE
            DrawerItem.SEARCH -> mode = SEARCH_MODE
            else -> {}
        }
    }

    private val adapterType: Int
        get() = when (mode) {
            CLOUD_DRIVE_MODE -> Constants.FILE_BROWSER_ADAPTER
            RUBBISH_BIN_MODE -> Constants.RUBBISH_BIN_ADAPTER
            BACKUPS_MODE -> Constants.BACKUPS_ADAPTER
            SHARED_ITEMS_MODE -> when ((requireActivity() as ManagerActivity).tabItemShares) {
                SharesTab.INCOMING_TAB -> Constants.INCOMING_SHARES_ADAPTER
                SharesTab.OUTGOING_TAB -> Constants.OUTGOING_SHARES_ADAPTER
                SharesTab.LINKS_TAB -> Constants.LINKS_ADAPTER
                else -> Constants.INVALID_VALUE
            }

            SEARCH_MODE -> Constants.SEARCH_ADAPTER
            RECENTS_MODE -> Constants.RECENTS_ADAPTER
            FAVOURITES_IN_TAB_MODE, FAVOURITES_MODE -> Constants.FAVOURITES_ADAPTER
            else -> Constants.INVALID_VALUE
        }

    private fun showShareFolderOptions(node: MegaNode?) {
        node?.let {
            val nodeType = checkBackupNodeTypeByHandle(megaApi, node)
            if (isOutShare(it)) {
                val intent = Intent(requireContext(), FileContactListActivity::class.java)
                intent.putExtra(Constants.NAME, it.handle)
                startActivity(intent)
            } else {
                if (nodeType != BACKUP_NONE) {
                    (requireActivity() as ManagerActivity).showWarningDialogOfShare(
                        it,
                        nodeType,
                        ACTION_BACKUP_SHARE_FOLDER
                    )
                } else {
                    nodeController.selectContactToShareFolder(node)
                }
            }
            dismissAllowingStateLoss()
        }
    }

    /**
     * Show cannot verify contact dialog
     * @param email : Email of the user
     */
    private fun showCanNotVerifyContact(email: String?) {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Mega_MaterialAlertDialog)
            .setTitle(getString(R.string.shared_items_contact_not_in_contact_list_dialog_title))
            .setMessage(
                getString(
                    R.string.shared_items_contact_not_in_contact_list_dialog_content,
                    email
                )
            )
            .setPositiveButton(getString(R.string.general_ok)) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
            .show()
    }

    /**
     * Open authenticityCredentials screen to verify user
     * @param email : Email of the user
     */
    private fun openAuthenticityCredentials(email: String?, node: MegaNode?) {
        val authenticityCredentialsIntent =
            Intent(activity, AuthenticityCredentialsActivity::class.java)
        authenticityCredentialsIntent.putExtra(
            Constants.IS_NODE_INCOMING,
            nodeController.nodeComesFromIncoming(node)
        )
        authenticityCredentialsIntent.putExtra(Constants.EMAIL, email)
        requireActivity().startActivity(authenticityCredentialsIntent)
    }

    companion object {
        /**
         * Instantiates the Fragment with specified parameters
         *
         * @param nodeId The [NodeId]
         * @param shareData The [ShareData], which can be nullable
         * @param mode The Mode used to display what Node actions are available
         * @param nodeDeviceCenterInformation When instantiating the Bottom Dialog from Device
         * Center, this holds specific information of the Device Center Node to be displayed
         */
        fun newInstance(
            nodeId: NodeId,
            shareData: ShareData? = null,
            mode: Int? = null,
            nodeDeviceCenterInformation: NodeDeviceCenterInformation? = null,
        ): NodeOptionsBottomSheetDialogFragment {
            val fragment = NodeOptionsBottomSheetDialogFragment()

            val args = Bundle()
            args.putLong(NODE_ID_KEY, nodeId.longValue)
            args.putInt(
                MODE_KEY,
                mode?.takeIf { it in DEFAULT_MODE..FAVOURITES_MODE } ?: DEFAULT_MODE)
            shareData?.let {
                val shareInfo = NodeShareInformation(
                    user = it.user,
                    isPending = it.isPending,
                    isVerified = it.isVerified
                )
                args.putParcelable(SHARE_DATA_KEY, shareInfo)
            }
            nodeDeviceCenterInformation?.let {
                args.putParcelable(NODE_DEVICE_CENTER_INFORMATION_KEY, it)
            }

            fragment.arguments = args

            return fragment
        }

        /** The "modes" are defined to allow the client to specify the dialog style more flexibly.
         * At the same time, compatible with old code. For which mode corresponds to which dialog style,
         * please refer to the code  */
        /**
         * No definite mode, map the drawerItem to a specific mode
         */
        const val DEFAULT_MODE = 0

        /**
         * For Cloud Drive
         */
        const val CLOUD_DRIVE_MODE = 1

        /**
         * For Rubbish Bin
         */
        const val RUBBISH_BIN_MODE = 2

        /**
         * For Backups
         */
        const val BACKUPS_MODE = 3

        /**
         * For Shared items
         */
        const val SHARED_ITEMS_MODE = 4

        /**
         * For Search
         */
        const val SEARCH_MODE = 5

        /**
         * For Recents
         */
        const val RECENTS_MODE = 6

        /**
         * For Favourites of HomePage tab
         */
        const val FAVOURITES_IN_TAB_MODE = 7

        /**
         * For Favourites
         */
        const val FAVOURITES_MODE = 8

    }
}