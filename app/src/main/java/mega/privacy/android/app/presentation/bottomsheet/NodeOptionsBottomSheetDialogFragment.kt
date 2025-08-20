package mega.privacy.android.app.presentation.bottomsheet

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import coil3.load
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkDialogFragment
import mega.privacy.android.app.main.dialog.rubbishbin.ConfirmMoveToRubbishBinDialogFragment
import mega.privacy.android.app.main.dialog.shares.RemoveAllSharingContactDialogFragment
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.openWith
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.showCannotOpenFileDialog
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsViewModel.Companion.HIDE_HIDDEN_ACTIONS_KEY
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsViewModel.Companion.MODE_KEY
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsViewModel.Companion.NODE_DEVICE_CENTER_INFORMATION_KEY
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsViewModel.Companion.NODE_ID_KEY
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsViewModel.Companion.SHARE_DATA_KEY
import mega.privacy.android.app.presentation.bottomsheet.model.NodeBottomSheetUIState
import mega.privacy.android.app.presentation.bottomsheet.model.NodeDeviceCenterInformation
import mega.privacy.android.app.presentation.bottomsheet.model.NodeShareInformation
import mega.privacy.android.app.presentation.contact.authenticitycredendials.AuthenticityCredentialsActivity
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.presentation.extensions.isOutShare
import mega.privacy.android.app.presentation.filecontact.FileContactListActivity
import mega.privacy.android.app.presentation.filecontact.FileContactListComposeActivity
import mega.privacy.android.app.presentation.fileinfo.FileInfoActivity
import mega.privacy.android.app.presentation.hidenode.HiddenNodesOnboardingActivity
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.photos.albums.add.AddToAlbumActivity
import mega.privacy.android.app.presentation.shares.incoming.IncomingSharesComposeViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.StartDownloadViewModel
import mega.privacy.android.app.presentation.videosection.VideoSectionViewModel
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.BlurTransformation
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_NONE
import mega.privacy.android.app.utils.MegaNodeUtil.checkBackupNodeTypeByHandle
import mega.privacy.android.app.utils.MegaNodeUtil.getFileInfo
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelColor
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelDrawable
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelText
import mega.privacy.android.app.utils.MegaNodeUtil.isEmptyFolder
import mega.privacy.android.app.utils.MegaNodeUtil.manageEditTextFileIntent
import mega.privacy.android.app.utils.MegaNodeUtil.onNodeTapped
import mega.privacy.android.app.utils.MegaNodeUtil.shareNode
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.ViewUtils.isVisible
import mega.privacy.android.app.utils.wrapper.LegacyNodeWrapper
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilWrapper
import mega.privacy.android.core.nodecomponents.extension.getIcon
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.icon.pack.R as RPack
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.CloudDriveHideNodeMenuItemEvent
import mega.privacy.mobile.analytics.event.HideNodeMenuItemEvent
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber
import javax.inject.Inject

/**
 * [BaseBottomSheetDialogFragment] used to display actions of a particular Node
 */
@AndroidEntryPoint
class NodeOptionsBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {
    private var mode = DEFAULT_MODE
    private lateinit var nodeController: NodeController
    private var nodeInfoText: TextView? = null
    private var drawerItem: DrawerItem? = null
    private var cannotOpenFileDialog: AlertDialog? = null
    private var isHiddenNodesEnabled: Boolean = false

    private val nodeOptionsViewModel: NodeOptionsViewModel by viewModels()
    private val startDownloadViewModel: StartDownloadViewModel by activityViewModels()
    private val videoSectionViewModel: VideoSectionViewModel by activityViewModels()
    private val incomingSharesComposeViewModel: IncomingSharesComposeViewModel by activityViewModels()
    private var tempNodeId: NodeId? = null

    /**
     * Inject [GetFeatureFlagValueUseCase] to the Fragment
     */
    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    @Inject
    lateinit var megaNodeUtilWrapper: MegaNodeUtilWrapper

    @Inject
    lateinit var megaNavigator: MegaNavigator

    private val hiddenNodesOnboardingLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleHiddenNodesOnboardingResult,
        )

    private val addToAlbumLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleAddToAlbumResult,
        )

    private fun handleAddToAlbumResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val message = result.data?.getStringExtra("message") ?: return
            Util.showSnackbar(requireActivity(), message)
        }
        dismiss()
    }

    private val hideHiddenActions: Boolean by lazy {
        arguments?.getBoolean(HIDE_HIDDEN_ACTIONS_KEY) ?: false
    }

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

    private suspend fun isHiddenNodesActive() = runCatching {
        getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
    }.getOrNull() == true

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val nodeThumb = contentView.findViewById<ImageView>(R.id.node_thumbnail)
        val nodeName = contentView.findViewById<TextView>(R.id.node_name_text)
        nodeInfoText = contentView.findViewById(R.id.node_info_text)
        val nodeVersionsIcon = contentView.findViewById<ImageView>(R.id.node_info_versions_icon)
        val nodeStatusIcon = contentView.findViewById<ImageView>(R.id.node_status_icon)
        val optionOffline = contentView.findViewById<LinearLayout>(R.id.option_offline_layout)
        val permissionsIcon = contentView.findViewById<ImageView>(R.id.permissions_icon)
        val optionEdit = contentView.findViewById<LinearLayout>(R.id.edit_file_option)
        val optionInfo = contentView.findViewById<TextView>(R.id.properties_option)
        val optionAddToAlbum = contentView.findViewById<TextView>(R.id.add_to_album_option)
        // option Versions
        val optionVersionsLayout =
            contentView.findViewById<LinearLayout>(R.id.option_versions_layout)
        val versions = contentView.findViewById<TextView>(R.id.versions)
        //      optionFavourite
        val optionFavourite = contentView.findViewById<TextView>(R.id.favorite_option)
        //      optionHide
        val optionHideLayout = contentView.findViewById<LinearLayout>(R.id.option_hide_layout)
        val optionHide = contentView.findViewById<TextView>(R.id.hide_option)
        val optionHideProLabel = contentView.findViewById<TextView>(R.id.hide_option_pro_label)
        val optionHideHelp = contentView.findViewById<ImageView>(R.id.hide_option_help)
        //      optionLabel
        val optionLabel = contentView.findViewById<LinearLayout>(R.id.option_label_layout)
        val optionLabelCurrent = contentView.findViewById<TextView>(R.id.option_label_current)
        //      counterSave
        val optionDownload = contentView.findViewById<TextView>(R.id.download_option)
        val offlineSwitch = contentView.findViewById<MegaSwitch>(R.id.file_properties_switch)
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
        //      counterOpen
        val optionOpenFolder = contentView.findViewById<TextView>(R.id.open_folder_option)
        val optionOpenWith = contentView.findViewById<TextView>(R.id.open_with_option)
        //      counterRemove
        val optionLeaveShares = contentView.findViewById<TextView>(R.id.leave_share_option)
        val optionRubbishBin = contentView.findViewById<TextView>(R.id.rubbish_bin_option)
        val optionRemove = contentView.findViewById<TextView>(R.id.remove_option)
        val viewInFolder = contentView.findViewById<TextView>(R.id.view_in_folder_option)

        val optionSync = contentView.findViewById<LinearLayout>(R.id.option_sync_layout)
        val separatorInfo = contentView.findViewById<View>(R.id.separator_info_option)
        val separatorOpen = contentView.findViewById<LinearLayout>(R.id.separator_open_options)
        val separatorDownload =
            contentView.findViewById<LinearLayout>(R.id.separator_download_options)
        val separatorShares = contentView.findViewById<LinearLayout>(R.id.separator_share_options)
        val separatorModify = contentView.findViewById<LinearLayout>(R.id.separator_modify_options)
        val separatorSync = contentView.findViewById<View>(R.id.separator_sync)
        val optionRemoveRecentlyWatchedItem =
            contentView.findViewById<View>(R.id.remove_recently_watched_item_option)
        val optionAddVideoToPlaylistItem =
            contentView.findViewById<View>(R.id.option_add_video_to_playlist)
        if (!Util.isScreenInPortrait(requireContext())) {
            Timber.d("Landscape configuration")
            nodeName.maxWidth = Util.scaleWidthPx(275, resources.displayMetrics)
            nodeInfoText?.maxWidth = Util.scaleWidthPx(275, resources.displayMetrics)
        } else {
            nodeName.maxWidth = Util.scaleWidthPx(210, resources.displayMetrics)
            nodeInfoText?.maxWidth = Util.scaleWidthPx(210, resources.displayMetrics)
        }

        viewLifecycleOwner.collectFlow(
            nodeOptionsViewModel.state.onStart {
                isHiddenNodesEnabled = isHiddenNodesActive()
            }, Lifecycle.State.STARTED
        ) { state: NodeBottomSheetUIState ->

            var counterOpen = 2
            var counterSave = 2
            var counterShares = 6
            var counterModify = 4


            if (state.shareKeyCreated != null) {
                if (state.shareKeyCreated) {
                    showShareFolderOptions(state.legacyNodeWrapper)
                } else {
                    Util.showSnackbar(
                        requireActivity(),
                        getString(R.string.general_something_went_wrong_error)
                    )
                }
                nodeOptionsViewModel.shareDialogDisplayed()
            }

            if (state.canMoveNode) {
                state.legacyNodeWrapper?.let { onMoveClicked(it.node) }
                // Once the action has been acknowledged, notify the ViewModel
                nodeOptionsViewModel.setMoveNodeClicked(false)
            }
            if (state.canRestoreNode) {
                state.legacyNodeWrapper?.let { onRestoreClicked(it.node) }
                // Once the action has been acknowledged, notify the ViewModel
                nodeOptionsViewModel.setRestoreNodeClicked(false)
            }

            val accountType = state.accountType
            val isBusinessAccountExpired = state.isBusinessAccountExpired
            val isHiddenNodesOnboarded = state.isHiddenNodesOnboarded
            state.legacyNodeWrapper?.let { nodeInfo ->
                if (megaApi.isInRubbish(nodeInfo.node)) {
                    mode = RUBBISH_BIN_MODE
                } else if (nodeController.nodeComesFromIncoming(nodeInfo.node)) {
                    mode = SHARED_ITEMS_MODE
                }

                if (mode == RECENTS_MODE || mode == FAVOURITES_IN_TAB_MODE) {
                    viewInFolder.visibility = View.VISIBLE
                    viewInFolder.setOnClickListener { onClick { onViewInFolderClicked(nodeInfo.node) } }
                } else {
                    viewInFolder.visibility = View.GONE
                    viewInFolder.setOnClickListener(null)
                }

                optionEdit.setOnClickListener { onClick { onEditClicked(nodeInfo.node) } }
                optionLabel.setOnClickListener { onClick { onLabelClicked(nodeInfo.node) } }
                optionFavourite.setOnClickListener { onClick { onFavouriteClicked(nodeInfo.node) } }
                optionHideLayout.setOnClickListener {
                    onClick {
                        onHideClicked(
                            nodeInfo.node,
                            accountType,
                            isBusinessAccountExpired,
                            isHiddenNodesOnboarded
                        )
                    }
                }
                optionHideHelp.setOnClickListener { showHiddenNodesOnboarding(false) }
                optionDownload.setOnClickListener { onClick { onDownloadClicked(nodeInfo.node) } }
                optionOffline.setOnClickListener {
                    onClick {
                        onOfflineClicked(
                            isAvailableOffline = state.isAvailableOffline,
                            nodeId = nodeInfo.typedNode.id,
                        )
                    }
                }
                optionInfo.setOnClickListener { onClick { onInfoClicked(nodeInfo.node) } }
                optionLink.setOnClickListener {
                    onClick {
                        onLinkClicked(nodeInfo.node)
                    }
                }
                optionRemoveLink.setOnClickListener { onClick { onRemoveLinkClicked(nodeInfo.node) } }
                optionShare.setOnClickListener {
                    onClick {
                        shareNode(requireActivity(), nodeInfo.node)
                    }
                }
                optionShareFolder.setOnClickListener { onClick { nodeOptionsViewModel.createShareKey() } }
                optionClearShares.setOnClickListener { onClick { onClearShareClicked(nodeInfo.node) } }
                optionLeaveShares.setOnClickListener { onClick { onLeaveShareClicked(nodeInfo.node) } }
                optionRename.setOnClickListener { onClick { onRenameClicked(nodeInfo.node) } }
                optionSendChat.setOnClickListener { onClick { onSendChatClicked(nodeInfo.node) } }
                optionMove.setOnClickListener {
                    onClick {
                        nodeOptionsViewModel.setMoveNodeClicked(true)
                    }
                }
                optionCopy.setOnClickListener { onClick { onCopyClicked(nodeInfo.node) } }
                optionRubbishBin.setOnClickListener { onClick { onDeleteClicked(nodeInfo.node) } }
                optionRestoreFromRubbish.setOnClickListener {
                    onClick { nodeOptionsViewModel.setRestoreNodeClicked(true) }
                }
                optionRemove.setOnClickListener { onClick { onDeleteClicked(nodeInfo.node) } }
                optionOpenFolder.setOnClickListener { onClick { onOpenFolderClicked(nodeInfo.node) } }
                optionOpenWith.setOnClickListener {
                    onClick {
                        onOpenWithClicked(nodeInfo.node)
                    }
                }
                optionVersionsLayout.setOnClickListener { onClick { onVersionsClicked(nodeInfo.node) } }
                optionRemoveRecentlyWatchedItem.setOnClickListener {
                    videoSectionViewModel.removeRecentlyWatchedItem(nodeInfo.node.handle)
                    setStateBottomSheetBehaviorHidden()
                }
                optionAddVideoToPlaylistItem.setOnClickListener {
                    videoSectionViewModel.launchVideoToPlaylistActivity(nodeInfo.node.handle)
                    setStateBottomSheetBehaviorHidden()
                }

                val isTakenDown = nodeInfo.typedNode.isTakenDown
                val accessLevel = megaApi.getAccess(nodeInfo.node)
                if (nodeInfo.typedNode is TypedFileNode && !isTakenDown) {
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

                if (state.isOnline) {
                    nodeName.setupNodeTitleText(
                        nodeDeviceCenterInformation = state.nodeDeviceCenterInformation,
                        nodeShareInformation = state.shareData,
                        megaNode = nodeInfo.node,
                    )
                    nodeThumb.setupNodeIcon(
                        nodeDeviceCenterInformation = state.nodeDeviceCenterInformation,
                        typedNode = nodeInfo.typedNode,
                    )
                    nodeInfoText?.run {
                        setupNodeBodyText(
                            isClearSharesVisible = optionClearShares.isVisible,
                            nodeDeviceCenterInformation = state.nodeDeviceCenterInformation,
                            nodeShareInformation = state.shareData,
                            megaNode = nodeInfo.node,
                        )
                        setupNodeBodyIcon(
                            nodeVersionsImageView = nodeVersionsIcon,
                            nodeStatusImageView = nodeStatusIcon,
                            nodeBodyTextView = this,
                            nodeDeviceCenterInformation = state.nodeDeviceCenterInformation,
                            megaNode = nodeInfo.node,
                        )
                    }
                    if (nodeInfo.typedNode is TypedFolderNode) {
                        optionVersionsLayout.visibility = View.GONE
                        if (isEmptyFolder(nodeInfo.node)) {
                            counterSave--
                            optionOffline.visibility = View.GONE
                        }
                        counterShares--
                        optionSendChat.visibility = View.GONE
                    } else {
                        if (typeForName(nodeInfo.typedNode.name).isOpenableTextFile(
                                nodeInfo.node.size
                            ) && accessLevel >= MegaShare.ACCESS_READWRITE && !isTakenDown
                        ) {
                            optionEdit.visibility = View.VISIBLE
                        }
                        if (megaApi.hasVersions(nodeInfo.node) && !isTakenDown) {
                            optionVersionsLayout.visibility = View.VISIBLE
                            versions.text = (megaApi.getNumVersions(nodeInfo.node) - 1).toString()
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
                        onClick {
                            context.launchUrl(Constants.DISPUTE_URL)
                            dismiss()
                        }
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
                    offlineSwitch.isChecked = state.isAvailableOffline
                }
                optionLabel.visibility = if (isTakenDown) View.GONE else View.VISIBLE
                optionFavourite.visibility = if (isTakenDown) View.GONE else View.VISIBLE
                optionHideLayout.visibility =
                    if (isHiddenNodesEnabled
                        && !hideHiddenActions
                        && mode != SHARED_ITEMS_MODE
                        && accountType != null
                        && isHiddenNodesOnboarded != null
                        && state.isHidingActionAllowed
                    ) {
                        val parentNode = megaApi.getParentNode(nodeInfo.node)
                        val isSensitiveInherited =
                            parentNode?.let { megaApi.isSensitiveInherited(it) } == true

                        if (!isSensitiveInherited || !accountType.isPaid || isBusinessAccountExpired) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    } else {
                        View.GONE
                    }
                optionHideProLabel.visibility =
                    if (accountType?.isPaid != true || isBusinessAccountExpired) View.VISIBLE else View.GONE
                optionHideHelp.visibility =
                    if (accountType?.isPaid == true && !isBusinessAccountExpired && !nodeInfo.typedNode.isMarkedSensitive) View.VISIBLE else View.GONE
                optionAddToAlbum.let { option ->
                    val fileType = (nodeInfo.typedNode as? TypedFileNode)?.type
                    if (fileType is ImageFileTypeInfo || fileType is VideoFileTypeInfo) {
                        option.visibility = View.VISIBLE
                        option.setText(if (fileType is ImageFileTypeInfo) sharedR.string.album_add_to_image else sharedR.string.album_add_to_media)
                    } else {
                        option.visibility = View.GONE
                    }

                    option.setOnClickListener {
                        val intent =
                            Intent(requireContext(), AddToAlbumActivity::class.java).apply {
                                val ids = listOf(nodeInfo.node.handle).toTypedArray()
                                val type = if (fileType is ImageFileTypeInfo) 0 else 1
                                putExtra("ids", ids)
                                putExtra("type", type)
                            }
                        addToAlbumLauncher.launch(intent)
                    }
                }
                if (accessLevel != MegaShare.ACCESS_OWNER || isTakenDown) {
                    counterShares--
                    optionShare.visibility = View.GONE
                }
                if (nodeInfo.typedNode is TypedFolderNode) {
                    if (isTakenDown) {
                        counterShares--
                        optionShareFolder.visibility = View.GONE
                        counterShares--
                        optionClearShares.visibility = View.GONE
                    } else {
                        optionShareFolder.visibility = View.VISIBLE
                        if (nodeInfo.typedNode.isOutShare()) {
                            optionShareFolder.setText(R.string.manage_share)
                            optionShareFolder.setCompoundDrawablesWithIntrinsicBounds(
                                RPack.drawable.ic_gear_six_medium_thin_outline,
                                0,
                                0,
                                0
                            )
                            optionClearShares.visibility = View.VISIBLE
                        } else {
                            optionShareFolder.setText(R.string.context_share_folder)
                            optionShareFolder.setCompoundDrawablesWithIntrinsicBounds(
                                RPack.drawable.ic_folder_users_medium_thin_outline,
                                0,
                                0,
                                0
                            )
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
                    if (nodeInfo.node.isExported) {
                        //Node has public link
                        optionLink.setText(R.string.edit_link_option)
                        optionRemoveLink.visibility = View.VISIBLE
                    } else {
                        optionLink.text =
                            resources.getQuantityString(sharedR.plurals.label_share_links, 1)
                        counterShares--
                        optionRemoveLink.visibility = View.GONE
                    }
                }
                if (mode == DEFAULT_MODE) {
                    mapDrawerItemToMode(state.nodeDeviceCenterInformation)
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    if (mode == CLOUD_DRIVE_MODE && !isTakenDown && state.isOnline && state.isSyncActionAllowed) {
                        optionSync.visibility = View.VISIBLE
                        separatorSync.visibility = View.VISIBLE
                        optionSync.setOnClickListener {
                            openNewSyncScreen(nodeInfo.node)
                        }
                    } else {
                        optionSync.visibility = View.GONE
                        separatorSync.visibility = View.GONE
                        optionSync.setOnClickListener(null)
                    }
                }

                when (mode) {
                    CLOUD_DRIVE_MODE,
                    SEARCH_MODE,
                    VIDEO_RECENTLY_WATCHED_MODE,
                    VIDEO_SECTION_MODE,
                    VIDEO_PLAYLIST_DETAIL,
                        -> {
                        Timber.d("show Cloud bottom sheet")
                        optionRemove.visibility = View.GONE
                        optionLeaveShares.visibility = View.GONE
                        counterOpen--
                        optionOpenFolder.visibility = View.GONE
                        counterModify--
                        optionRestoreFromRubbish.visibility = View.GONE

                        optionRemoveRecentlyWatchedItem.visibility =
                            if (mode == VIDEO_RECENTLY_WATCHED_MODE) {
                                View.VISIBLE
                            } else {
                                View.GONE
                            }

                        viewLifecycleOwner.lifecycleScope.launch {
                            optionAddVideoToPlaylistItem.visibility =
                                if (mode == VIDEO_SECTION_MODE) {
                                    View.VISIBLE
                                } else {
                                    View.GONE
                                }
                        }

                        if (mode in listOf(
                                VIDEO_SECTION_MODE,
                                VIDEO_RECENTLY_WATCHED_MODE,
                                VIDEO_PLAYLIST_DETAIL
                            )
                        ) {
                            optionAddToAlbum.visibility = View.GONE
                        }
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
                        optionHideLayout.visibility = View.GONE
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
                        optionAddToAlbum.visibility = View.GONE
                    }

                    SHARED_ITEMS_MODE -> {
                        val tabSelected = (requireActivity() as ManagerActivity).tabItemShares
                        if (tabSelected === SharesTab.INCOMING_TAB || nodeController.nodeComesFromIncoming(
                                nodeInfo.node
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
                            val dBT = nodeController.getIncomingLevel(nodeInfo.node)
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
                            optionAddToAlbum.visibility = View.GONE
                            when (accessLevel) {
                                MegaShare.ACCESS_FULL -> {
                                    Timber.d("access FULL")
                                    optionFavourite.visibility = View.GONE
                                    optionHideLayout.visibility = View.GONE
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
                                    optionHideLayout.visibility = View.GONE
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
                                    optionHideLayout.visibility = View.GONE
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
                            if (!isTakenDown && nodeInfo.node.isShared) {
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
                    node = nodeInfo.node,
                    decrementOpen = { counterOpen-- },
                    decrementModify = { counterModify-- },
                )
                separatorOpen.visibility = if (counterOpen <= 0) View.GONE else View.VISIBLE
                separatorDownload.visibility = if (counterSave <= 0) View.GONE else View.VISIBLE
                separatorShares.visibility = if (counterShares <= 0) View.GONE else View.VISIBLE
                separatorModify.visibility = if (counterModify <= 0) View.GONE else View.VISIBLE
                offlineSwitch.setOnCheckedChangeListener { _, _ ->
                    onClick {
                        onOfflineClicked(
                            isAvailableOffline = state.isAvailableOffline,
                            nodeId = nodeInfo.typedNode.id,
                        )
                    }
                }
                optionFavourite.setText(if (nodeInfo.typedNode.isFavourite) R.string.file_properties_unfavourite else R.string.file_properties_favourite)
                optionFavourite.setCompoundDrawablesWithIntrinsicBounds(
                    if (nodeInfo.typedNode.isFavourite) RPack.drawable.ic_heart_broken_medium_thin_outline else RPack.drawable.ic_heart_medium_thin_outline,
                    0, 0, 0
                )
                optionHide.setText(if (accountType?.isPaid != true || isBusinessAccountExpired || !nodeInfo.typedNode.isMarkedSensitive) R.string.general_hide_node else R.string.general_unhide_node)
                optionHide.setCompoundDrawablesWithIntrinsicBounds(
                    if (accountType?.isPaid != true || isBusinessAccountExpired || !nodeInfo.typedNode.isMarkedSensitive) RPack.drawable.ic_eye_off_medium_thin_outline else RPack.drawable.ic_eye_medium_thin_outline,
                    0,
                    0,
                    0
                )
                if (nodeInfo.typedNode.label != MegaNode.NODE_LBL_UNKNOWN) {
                    val color = ResourcesCompat.getColor(
                        resources, getNodeLabelColor(
                            nodeInfo.typedNode.label
                        ), null
                    )
                    val drawable = getNodeLabelDrawable(
                        nodeInfo.typedNode.label, resources
                    )
                    optionLabelCurrent.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        null,
                        null,
                        drawable,
                        null
                    )
                    optionLabelCurrent.text =
                        getNodeLabelText(nodeInfo.typedNode.label, requireContext())
                    optionLabelCurrent.setTextColor(color)
                    optionLabelCurrent.visibility = View.VISIBLE
                } else {
                    optionLabelCurrent.visibility = View.GONE
                }
                state.shareData?.let { data -> hideNodeActions(data, nodeInfo.node) }

                if (savedInstanceState?.getBoolean(
                        Constants.CANNOT_OPEN_FILE_SHOWN,
                        false
                    ) == true
                ) {
                    contentView.post {
                        cannotOpenFileDialog = this.showCannotOpenFileDialog(requireActivity()) {
                            (requireActivity() as ManagerActivity).saveNodeByTap(nodeInfo.node)
                        }
                    }
                }
            }
            // Recalculate bottom sheet peek height after visibility of views are updated
            calculatePeekHeight()
        }
        super.onViewCreated(view, savedInstanceState)
    }

    private fun openNewSyncScreen(node: MegaNode) {
        val managerActivity =
            requireActivity() as? ManagerActivity ?: return
        megaNavigator.openNewSync(
            context = managerActivity,
            syncType = SyncType.TYPE_TWOWAY,
            isFromManagerActivity = true,
            remoteFolderHandle = node.handle,
            remoteFolderName = node.name,
        )
        dismiss()
    }

    private fun onClick(action: () -> Unit) {
        if (nodeOptionsViewModel.isOnline()) {
            action()
        } else {
            dismiss()
            Util.showSnackbar(
                requireActivity(),
                getString(R.string.error_server_connection_problem)
            )
        }
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
        typedNode: TypedNode,
    ) {
        if (nodeDeviceCenterInformation != null) {
            setImageResource(nodeDeviceCenterInformation.icon)
        } else {
            val iconResource = typedNode.getIcon(
                originShares = drawerItem == DrawerItem.SHARED_ITEMS,
                fileTypeIconMapper = FileTypeIconMapper(),
            )
            if (typedNode is TypedFolderNode) {
                load(
                    iconResource
                ) {
                    if (typedNode.isMarkedSensitive || typedNode.isSensitiveInherited) {
                        transformations(
                            BlurTransformation(
                                requireContext(),
                                radius = 16f,
                            )
                        )
                    }
                }
            } else {
                val thumbnailParams = layoutParams as? ConstraintLayout.LayoutParams
                thumbnailParams?.let {
                    load(ThumbnailRequest(typedNode.id)) {
                        size(Util.dp2px(Constants.THUMB_SIZE_DP.toFloat()))

                        transformations(
                            buildList {
                                if (typedNode.isMarkedSensitive || typedNode.isSensitiveInherited) {
                                    add(BlurTransformation(requireContext(), radius = 16f))
                                }
                                add(
                                    RoundedCornersTransformation(
                                        Util.dp2px(Constants.THUMB_CORNER_RADIUS_DP).toFloat()
                                    )
                                )
                            }
                        )

                        listener(
                            onSuccess = { _, _ ->
                                thumbnailParams.width =
                                    Util.dp2px(Constants.THUMB_SIZE_DP.toFloat())
                                thumbnailParams.height = thumbnailParams.width
                                thumbnailParams.setMargins(0, 0, 0, 0)
                                layoutParams = thumbnailParams
                            },
                            onError = { _, _ ->
                                thumbnailParams.width = Util.dp2px(Constants.ICON_SIZE_DP.toFloat())
                                thumbnailParams.height = thumbnailParams.width
                                thumbnailParams.setMargins(0, 0, 0, 0)
                                setImageResource(iconResource)
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
        val optionHideLayout = contentView.findViewById<LinearLayout>(R.id.option_hide_layout)
        val optionLabel = contentView.findViewById<LinearLayout>(R.id.option_label_layout)
        val optionRename = contentView.findViewById<TextView>(R.id.rename_option)
        val optionMove = contentView.findViewById<TextView>(R.id.move_option)
        val optionLeaveShares = contentView.findViewById<TextView>(R.id.leave_share_option)
        val optionOpenFolder = contentView.findViewById<TextView>(R.id.open_folder_option)
        val optionRubbishBin = contentView.findViewById<TextView>(R.id.rubbish_bin_option)
        val optionRemove = contentView.findViewById<TextView>(R.id.remove_option)
        if (node != null && megaApi.isInVault(node)) {
            optionEdit.visibility = View.GONE
            optionFavourite.visibility = View.GONE
            optionHideLayout.visibility = View.GONE
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
                nodeInfoText?.text
            )
        }
        optionVerifyUser.setOnClickListener {
            onClick {
                onVerifyUserClicked(nodeShareInformation, node)
            }
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

    /**
     * Executes logic when the "Save to device" Option is clicked
     *
     * @param node The [MegaNode] to be downloaded
     */
    private fun onDownloadClicked(node: MegaNode) {
        if (mode == VIDEO_PLAYLIST_DETAIL && getStorageState() == StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning()
        } else {
            startDownloadViewModel.onDownloadClicked(
                nodeId = NodeId(node.handle),
                withStartMessage = false,
            )
        }
        setStateBottomSheetBehaviorHidden()
    }

    private fun onFavouriteClicked(node: MegaNode) {
        megaApi.setNodeFavourite(node, !node.isFavourite)
        setStateBottomSheetBehaviorHidden()
    }

    private fun onHideClicked(
        node: MegaNode,
        accountType: AccountType?,
        isBusinessAccountExpired: Boolean,
        isHiddenNodesOnboarded: Boolean?,
    ) {
        if (drawerItem == DrawerItem.CLOUD_DRIVE && !node.isMarkedSensitive) {
            // We only want to send analytics for when the action is hiding
            Analytics.tracker.trackEvent(CloudDriveHideNodeMenuItemEvent)
        }

        if (!node.isMarkedSensitive) {
            Analytics.tracker.trackEvent(HideNodeMenuItemEvent)
        }

        val isPaid = accountType?.isPaid ?: false
        val isHiddenNodesOnboarded = isHiddenNodesOnboarded ?: false

        if (!isPaid || isBusinessAccountExpired) {
            val intent = HiddenNodesOnboardingActivity.createScreen(
                context = requireContext(),
                isOnboarding = false,
            )
            hiddenNodesOnboardingLauncher.launch(intent)
            activity?.overridePendingTransition(0, 0)

            setStateBottomSheetBehaviorHidden()
        } else if (node.isMarkedSensitive || isHiddenNodesOnboarded) {
            nodeOptionsViewModel.hideOrUnhideNode(
                handle = node.handle,
                hidden = !node.isMarkedSensitive,
            )

            val message = resources.getQuantityString(
                if (!node.isMarkedSensitive) {
                    R.plurals.hidden_nodes_result_message
                } else {
                    sharedR.plurals.unhidden_nodes_result_message
                }, 1, 1
            )
            Util.showSnackbar(requireActivity(), message)

            setStateBottomSheetBehaviorHidden()
        } else {
            tempNodeId = nodeOptionsViewModel.state.value.legacyNodeWrapper?.typedNode?.id
            showHiddenNodesOnboarding(true)
        }
    }

    private fun showHiddenNodesOnboarding(isOnboarding: Boolean) {
        nodeOptionsViewModel.setHiddenNodesOnboarded()

        val intent = HiddenNodesOnboardingActivity.createScreen(
            context = requireContext(),
            isOnboarding = isOnboarding,
        )
        hiddenNodesOnboardingLauncher.launch(intent)
        activity?.overridePendingTransition(0, 0)
    }

    private fun onLabelClicked(node: MegaNode) {
        (requireActivity() as ManagerActivity).showNodeLabelsPanel(
            NodeId(node.handle)
        )
        setStateBottomSheetBehaviorHidden()
    }

    private fun onOfflineClicked(
        isAvailableOffline: Boolean,
        nodeId: NodeId,
    ) {
        if (isAvailableOffline) {
            nodeOptionsViewModel.removeOfflineNode(nodeId)
            refreshView()
            Util.showSnackbar(activity, resources.getString(R.string.file_removed_offline))
        } else {
            if (mode == VIDEO_PLAYLIST_DETAIL && getStorageState() == StorageState.PayWall) {
                showOverDiskQuotaPaywallWarning()
            } else {
                startDownloadViewModel.onSaveOfflineClicked(
                    nodeId,
                    withStartMessage = false,
                )
            }
        }
        setStateBottomSheetBehaviorHidden()
    }

    /**
     * Navigates to [FileInfoActivity] with data, and hides the Bottom Dialog
     *
     * @param node The [MegaNode] that was passed
     */
    private fun onInfoClicked(node: MegaNode) {
        val managerActivity = requireActivity() as? ManagerActivity
        val fileInfoIntent = Intent(requireContext(), FileInfoActivity::class.java)

        fileInfoIntent.putExtra(Constants.HANDLE, node.handle)
        if (drawerItem === DrawerItem.SHARED_ITEMS && managerActivity?.tabItemShares === SharesTab.OUTGOING_TAB) {
            fileInfoIntent.putExtra(
                Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
                NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER,
            )
        }

        startActivityForResult(fileInfoIntent, Constants.REQUEST_CODE_FILE_INFO)
        dismissAllowingStateLoss()
        setStateBottomSheetBehaviorHidden()
    }

    private fun onLinkClicked(node: MegaNode) {
        if (mode == VIDEO_PLAYLIST_DETAIL && getStorageState() == StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning()
        } else {
            (requireActivity() as ManagerActivity).showGetLinkActivity(
                node.handle
            )
        }
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
        incomingSharesComposeViewModel.setShowLeaveShareConfirmationDialog(listOf(node.handle))
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

    private fun onOpenFolderClicked(node: MegaNode) {
        nodeController.openFolderFromSearch(node.handle)
        dismissAllowingStateLoss()
        setStateBottomSheetBehaviorHidden()
    }

    private fun onOpenWithClicked(node: MegaNode) {
        if (nodeOptionsViewModel.isFilePreviewOnline(node = node)) {
            openWith(
                context = requireActivity(),
                megaNodeUtilWrapper = megaNodeUtilWrapper,
                node = node,
            ) {
                (requireActivity() as ManagerActivity).saveNodeByOpenWith(node)
            }
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
        dismissAllowingStateLoss()
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
        (activity as? ManagerActivity)?.versionsActivityLauncher?.launch(node.handle)
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

            else -> {}
        }
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

    private fun mapDrawerItemToMode(nodeDeviceCenterInformation: NodeDeviceCenterInformation?) {
        when (drawerItem) {
            DrawerItem.CLOUD_DRIVE -> mode = CLOUD_DRIVE_MODE
            DrawerItem.RUBBISH_BIN -> mode = RUBBISH_BIN_MODE
            DrawerItem.BACKUPS -> mode = BACKUPS_MODE
            DrawerItem.DEVICE_CENTER -> {
                mode = if (nodeDeviceCenterInformation?.isBackupsFolder == true) {
                    BACKUPS_MODE
                } else {
                    CLOUD_DRIVE_MODE
                }
            }

            DrawerItem.SHARED_ITEMS -> mode = SHARED_ITEMS_MODE
            else -> Unit
        }
    }

    private val adapterType: Int
        get() = when (mode) {
            CLOUD_DRIVE_MODE -> NodeSourceTypeInt.FILE_BROWSER_ADAPTER
            RUBBISH_BIN_MODE -> NodeSourceTypeInt.RUBBISH_BIN_ADAPTER
            BACKUPS_MODE -> NodeSourceTypeInt.BACKUPS_ADAPTER
            SHARED_ITEMS_MODE -> when ((requireActivity() as ManagerActivity).tabItemShares) {
                SharesTab.INCOMING_TAB -> NodeSourceTypeInt.INCOMING_SHARES_ADAPTER
                SharesTab.OUTGOING_TAB -> NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER
                SharesTab.LINKS_TAB -> NodeSourceTypeInt.LINKS_ADAPTER
                else -> Constants.INVALID_VALUE
            }

            SEARCH_MODE -> Constants.SEARCH_ADAPTER
            RECENTS_MODE -> Constants.RECENTS_ADAPTER
            FAVOURITES_IN_TAB_MODE, FAVOURITES_MODE -> NodeSourceTypeInt.FAVOURITES_ADAPTER
            else -> Constants.INVALID_VALUE
        }

    private fun showShareFolderOptions(legacyNodeWrapper: LegacyNodeWrapper?) {
        legacyNodeWrapper?.let {
            val nodeType = checkBackupNodeTypeByHandle(megaApi, it.node)
            if (it.typedNode.isOutShare()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val intent =
                        if (getFeatureFlagValueUseCase(AppFeatures.SingleActivity)) {
                            FileContactListComposeActivity.newIntent(
                                context = requireContext(),
                                nodeHandle = it.typedNode.id.longValue,
                                nodeName = it.typedNode.name
                            )
                        } else {
                            Intent(requireContext(), FileContactListActivity::class.java)
                        }
                    intent.putExtra(Constants.NAME, it.typedNode.id.longValue)
                    startActivity(intent)
                    dismissAllowingStateLoss()
                }
            } else {
                if (nodeType != BACKUP_NONE) {
                    (requireActivity() as ManagerActivity).showShareBackupsFolderWarningDialog(
                        node = it.node,
                        nodeType = nodeType,
                    )
                } else {
                    nodeController.selectContactToShareFolder(it.typedNode.id.longValue)
                }
                dismissAllowingStateLoss()
            }
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

    private fun handleHiddenNodesOnboardingResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val nodeId = tempNodeId ?: return

            nodeOptionsViewModel.hideOrUnhideNode(
                handle = nodeId.longValue,
                hidden = true,
            )

            val message = resources.getQuantityString(R.plurals.hidden_nodes_result_message, 1, 1)
            Util.showSnackbar(requireActivity(), message)
        }
        dismiss()
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
         * @param hideHiddenActions if it is true, then don't show hide/unhide, otherwise show it by logic
         */
        fun newInstance(
            nodeId: NodeId,
            shareData: ShareData? = null,
            mode: Int? = null,
            nodeDeviceCenterInformation: NodeDeviceCenterInformation? = null,
            hideHiddenActions: Boolean = false,
        ): NodeOptionsBottomSheetDialogFragment {
            val fragment = NodeOptionsBottomSheetDialogFragment()

            val args = Bundle()
            args.putLong(NODE_ID_KEY, nodeId.longValue)
            args.putInt(
                MODE_KEY,
                mode?.takeIf { it in DEFAULT_MODE..VIDEO_PLAYLIST_DETAIL } ?: DEFAULT_MODE)
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
            args.putBoolean(HIDE_HIDDEN_ACTIONS_KEY, hideHiddenActions)

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

        /**
         * For Video Recently Watched
         */
        const val VIDEO_RECENTLY_WATCHED_MODE = 9

        /**
         * For Video Section
         */
        const val VIDEO_SECTION_MODE = 10

        /**
         * For Video playlist detail
         */
        const val VIDEO_PLAYLIST_DETAIL = 11
    }
}
