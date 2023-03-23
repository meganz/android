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
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaOffline
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
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.openWith
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.setNodeThumbnail
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.showCannotOpenFileDialog
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsViewModel.Companion.MODE_KEY
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsViewModel.Companion.NODE_ID_KEY
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsViewModel.Companion.SHARE_DATA_KEY
import mega.privacy.android.app.presentation.bottomsheet.model.NodeBottomSheetUIState
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
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaUser
import timber.log.Timber
import java.io.File
import java.util.stream.Collectors

@AndroidEntryPoint
class NodeOptionsBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {
    /**
     * Values used to control the display of option separators
     */

    private var mode = DEFAULT_MODE
    private lateinit var nodeController: NodeController
    private lateinit var nodeInfo: TextView
    private var drawerItem: DrawerItem? = null
    private var cannotOpenFileDialog: AlertDialog? = null
    private var user: MegaUser? = null


    private val nodeOptionsViewModel: NodeOptionsViewModel by viewModels()
    private val searchViewModel: SearchViewModel by viewModels()


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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val nodeThumb = contentView.findViewById<ImageView>(R.id.node_thumbnail)
        val nodeName = contentView.findViewById<TextView>(R.id.node_name_text)
        nodeInfo = contentView.findViewById(R.id.node_info_text)
        val nodeVersionsIcon = contentView.findViewById<ImageView>(R.id.node_info_versions_icon)
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
            nodeInfo.maxWidth = Util.scaleWidthPx(275, resources.displayMetrics)
        } else {
            nodeName.maxWidth = Util.scaleWidthPx(210, resources.displayMetrics)
            nodeInfo.maxWidth = Util.scaleWidthPx(210, resources.displayMetrics)
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
                optionLink.setOnClickListener { onLinkClicked(node) }
                optionRemoveLink.setOnClickListener { onRemoveLinkClicked(node) }
                optionShare.setOnClickListener { shareNode(requireActivity(), node) }
                optionShareFolder.setOnClickListener { nodeOptionsViewModel.createShareKey() }
                optionClearShares.setOnClickListener { onClearShareClicked(node) }
                optionLeaveShares.setOnClickListener { onLeaveShareClicked(node) }
                optionRename.setOnClickListener { onRenameClicked(node) }
                optionSendChat.setOnClickListener { onSendChatClicked(node) }
                optionMove.setOnClickListener { onMoveClicked(node) }
                optionCopy.setOnClickListener { onCopyClicked(node) }
                optionRubbishBin.setOnClickListener { onDeleteClicked(node) }
                optionRestoreFromRubbish.setOnClickListener { onRestoreClicked(node) }
                optionRemove.setOnClickListener { onDeleteClicked(node) }
                optionOpenFolder.setOnClickListener { onOpenFolderClicked(node) }
                optionSlideshow.setOnClickListener { onSlideShowClicked(node) }
                optionOpenWith.setOnClickListener { onOpenWithClicked(node) }
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
                    nodeName.text = node.name
                    if (node.isFolder) {
                        optionVersionsLayout.visibility = View.GONE
                        nodeInfo.text = MegaApiUtils.getMegaNodeFolderInfo(node)
                        nodeVersionsIcon.visibility = View.GONE
                        drawerItem?.let { item ->
                            nodeThumb.setImageResource(
                                getFolderIcon(
                                    node,
                                    item
                                )
                            )
                        }
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
                        nodeInfo.text = getFileInfo(node)
                        if (megaApi.hasVersions(node) && !isTakenDown) {
                            nodeVersionsIcon.visibility = View.VISIBLE
                            optionVersionsLayout.visibility = View.VISIBLE
                            versions.text = megaApi.getNumVersions(node).toString()
                        } else {
                            nodeVersionsIcon.visibility = View.GONE
                            optionVersionsLayout.visibility = View.GONE
                        }
                        setNodeThumbnail(requireContext(), node, nodeThumb)
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
                    INBOX_MODE -> Timber.d("show My Backups bottom sheet")
                    RUBBISH_BIN_MODE -> {
                        Timber.d("show Rubbish bottom sheet")
                        optionEdit.visibility = View.GONE
                        val restoreHandle = node.restoreHandle
                        val restoreNode = megaApi.getNodeByHandle(restoreHandle)
                        if (restoreHandle == MegaApiJava.INVALID_HANDLE || !megaApi.isInRubbish(node) || restoreNode == null || megaApi.isInRubbish(
                                restoreNode
                            ) || megaApi.isInInbox(restoreNode)
                        ) {
                            counterModify--
                            optionRestoreFromRubbish.visibility = View.GONE
                        }
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
                                //Show the owner of the shared folder
                                showOwnerSharedFolder(node)
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
                            if (!isTakenDown && (requireActivity() as ManagerActivity).deepBrowserTreeOutgoing == Constants.FIRST_NAVIGATION_LEVEL && optionClearShares.isVisible()) {
                                //Show the number of contacts who shared the folder
                                val sl = megaApi.getOutShares(node)
                                    .stream().filter { obj: MegaShare -> obj.isVerified }
                                    .collect(Collectors.toList())
                                if (sl.size != 0) {
                                    nodeInfo.text = resources.getQuantityString(
                                        R.plurals.general_num_shared_with,
                                        sl.size, sl.size
                                    )
                                }
                            } else if (optionClearShares.isVisible()) {
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
                    RECENTS_MODE, FAVOURITES_IN_TAB_MODE, FAVOURITES_MODE ->                 // If the Dialog Fragment is opened from the Favourites page, handle the display
                        // of Dialog options accordingly
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
                    optionLabelCurrent.text = getNodeLabelText(node.label)
                    optionLabelCurrent.setTextColor(color)
                    optionLabelCurrent.visibility = View.VISIBLE
                } else {
                    optionLabelCurrent.visibility = View.GONE
                }
                state.shareData?.let { data ->
                    setUnverifiedOutgoingNodeUserName(data)
                    hideNodeActions(data, node)
                }


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

    override fun onDestroyView() {
        dismissAlertDialogIfExists(cannotOpenFileDialog)
        super.onDestroyView()
    }

    /**
     * Apply read-only Restrictions for Backup Nodes by hiding the following options:
     *
     *
     * 1.) Rename
     * 2.) Move to
     * 3.) Move to Rubbish Bin
     * 4.) Favourite
     * 5.) Label
     * 6.) Versions
     *
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
        val optionRestoreFromRubbish = contentView.findViewById<TextView>(R.id.restore_option)
        val optionRubbishBin = contentView.findViewById<TextView>(R.id.rubbish_bin_option)
        val optionRemove = contentView.findViewById<TextView>(R.id.remove_option)
        val optionVersions = contentView.findViewById<LinearLayout>(R.id.option_versions_layout)
        if (node != null && megaApi.isInInbox(node)) {
            optionEdit.visibility = View.GONE
            optionFavourite.visibility = View.GONE
            optionLabel.visibility = View.GONE
            optionRename.visibility = View.GONE
            optionMove.visibility = View.GONE
            optionRubbishBin.visibility = View.GONE
            optionVersions.visibility = View.GONE
            optionRemove.visibility = View.GONE
            optionLeaveShares.visibility = View.GONE
            optionOpenFolder.visibility = View.GONE
            optionRestoreFromRubbish.visibility = View.GONE
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

    private fun showOwnerSharedFolder(node: MegaNode?) {
        val sharesIncoming = megaApi.inSharesList
        for (j in sharesIncoming.indices) {
            val mS = sharesIncoming[j]
            if (mS.nodeHandle == node?.handle) {
                user = megaApi.getContact(mS.user)
                if (user != null) {
                    nodeInfo.text = ContactUtil.getMegaUserNameDB(user)
                } else {
                    nodeInfo.text = mS.user
                }
            }
        }
    }

    /**
     * Set the node info of the unverified node with the name of the contact
     * @param nodeShareInformation
     */
    private fun setUnverifiedOutgoingNodeUserName(nodeShareInformation: NodeShareInformation) {
        user = megaApi.getContact(nodeShareInformation.user)
        if (user != null) {
            nodeInfo.text = ContactUtil.getMegaUserNameDB(user)
        } else {
            nodeInfo.text = nodeShareInformation.user
        }
    }

    private fun hideNodeActions(nodeShareInformation: NodeShareInformation, node: MegaNode) {
        val optionVerifyUser = contentView.findViewById<TextView>(R.id.verify_user_option)
        optionVerifyUser.visibility = View.VISIBLE
        if (!nodeShareInformation.isVerified) {
            optionVerifyUser.text = getString(
                R.string.shared_items_bottom_sheet_menu_verify_user,
                nodeInfo.text
            )
        }
        optionVerifyUser.setOnClickListener { onVerifyUserClicked(nodeShareInformation, node) }
        val nodeName = contentView.findViewById<TextView>(R.id.node_name_text)
        if (nodeController.nodeComesFromIncoming(node)) {
            nodeName.text =
                resources.getString(R.string.shared_items_verify_credentials_undecrypted_folder)
        } else {
            nodeName.text = node.name
        }
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

    private fun onDownloadClicked(node: MegaNode) {
        (requireActivity() as ManagerActivity).saveNodesToDevice(
            nodes = listOf(node),
            highPriority = false,
            isFolderLink = false,
            fromMediaViewer = false,
            fromChat = false
        )
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
            val mOffDelete = dbH.findByHandle(node.handle)
            removeFromOffline(mOffDelete)
            Util.showSnackbar(activity, resources.getString(R.string.file_removed_offline))
        } else {
            saveForOffline(node)
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
        } else if (drawerItem === DrawerItem.INBOX) {
            if ((requireActivity() as ManagerActivity).tabItemShares === SharesTab.INCOMING_TAB) {
                fileInfoIntent.putExtra(INTENT_EXTRA_KEY_FROM, Constants.FROM_INBOX)
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
        (requireActivity() as ManagerActivity).showConfirmationRemovePublicLink(
            node
        )
        setStateBottomSheetBehaviorHidden()
    }

    private fun onClearShareClicked(node: MegaNode?) {
        val shareList = megaApi.getOutShares(node)
        (requireActivity() as ManagerActivity).showConfirmationRemoveAllSharingContacts(
            shareList,
            node
        )
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
        (requireActivity() as ManagerActivity).askConfirmationMoveToRubbish(
            listOf(node.handle)
        )
        setStateBottomSheetBehaviorHidden()
    }

    private fun onSlideShowClicked(node: MegaNode) {
        val intent = getIntentForParentNode(
            requireContext(),
            megaApi.getParentNode(node).handle,
            SortOrder.ORDER_PHOTO_ASC,
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

    private fun onRestoreClicked(node: MegaNode?) {
        val nodes: MutableList<MegaNode?> = ArrayList()
        nodes.add(node)
        (requireActivity() as ManagerActivity).restoreFromRubbish(nodes)
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
        version.putExtra("handle", node.handle)
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
            DrawerItem.INBOX -> (requireActivity() as ManagerActivity).onNodesInboxUpdate()
            DrawerItem.SHARED_ITEMS -> (requireActivity() as ManagerActivity).refreshSharesFragments()
            DrawerItem.SEARCH -> (requireActivity() as ManagerActivity).onNodesSearchUpdate()
            DrawerItem.HOMEPAGE -> LiveEventBus.get<Boolean>(Constants.EVENT_NODES_CHANGE)
                .post(false)
            else -> {}
        }
    }

    private fun removeFromOffline(mOffDelete: MegaOffline?) {
        OfflineUtils.removeOffline(mOffDelete, dbH, requireContext())
        refreshView()
    }

    private fun saveForOffline(node: MegaNode?) {
        var adapterType = Constants.FROM_OTHERS
        when (drawerItem) {
            DrawerItem.INBOX -> adapterType = Constants.FROM_INBOX
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
                    val mOffDelete = dbH.findbyPathAndName(parentName, node?.name)
                    removeFromOffline(mOffDelete)
                }
            }
        }

        // Save the new file to offline
        OfflineUtils.saveOffline(offlineParent, node, requireActivity())
    }

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
            DrawerItem.INBOX -> mode = INBOX_MODE
            DrawerItem.SHARED_ITEMS -> mode = SHARED_ITEMS_MODE
            DrawerItem.SEARCH -> mode = SEARCH_MODE
            else -> {}
        }
    }

    private val adapterType: Int
        get() = when (mode) {
            CLOUD_DRIVE_MODE -> Constants.FILE_BROWSER_ADAPTER
            RUBBISH_BIN_MODE -> Constants.RUBBISH_BIN_ADAPTER
            INBOX_MODE -> Constants.INBOX_ADAPTER
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

        fun newInstance(
            nodeId: NodeId,
            shareData: ShareData? = null,
            mode: Int? = null,
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
         * For Inbox
         */
        const val INBOX_MODE = 3

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