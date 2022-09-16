package mega.privacy.android.app.imageviewer.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.OfflineFileInfoActivity
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.activities.contract.SelectFolderToCopyActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToImportActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToMoveActivityContract
import mega.privacy.android.app.databinding.BottomSheetImageOptionsBinding
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.imageviewer.ImageViewerFragmentDirections
import mega.privacy.android.app.imageviewer.ImageViewerViewModel
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.imageviewer.util.shouldShowChatRemoveOption
import mega.privacy.android.app.imageviewer.util.shouldShowCopyOption
import mega.privacy.android.app.imageviewer.util.shouldShowDisputeOption
import mega.privacy.android.app.imageviewer.util.shouldShowDownloadOption
import mega.privacy.android.app.imageviewer.util.shouldShowFavoriteOption
import mega.privacy.android.app.imageviewer.util.shouldShowForwardOption
import mega.privacy.android.app.imageviewer.util.shouldShowInfoOption
import mega.privacy.android.app.imageviewer.util.shouldShowLabelOption
import mega.privacy.android.app.imageviewer.util.shouldShowManageLinkOption
import mega.privacy.android.app.imageviewer.util.shouldShowMoveOption
import mega.privacy.android.app.imageviewer.util.shouldShowOfflineOption
import mega.privacy.android.app.imageviewer.util.shouldShowOpenWithOption
import mega.privacy.android.app.imageviewer.util.shouldShowRemoveLinkOption
import mega.privacy.android.app.imageviewer.util.shouldShowRenameOption
import mega.privacy.android.app.imageviewer.util.shouldShowRestoreOption
import mega.privacy.android.app.imageviewer.util.shouldShowRubbishBinOption
import mega.privacy.android.app.imageviewer.util.shouldShowSendToContactOption
import mega.privacy.android.app.imageviewer.util.shouldShowShareOption
import mega.privacy.android.app.imageviewer.util.shouldShowSlideshowOption
import mega.privacy.android.app.main.FileInfoActivity
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil
import mega.privacy.android.app.modalbottomsheet.nodelabel.NodeLabelBottomSheetDialogFragment
import mega.privacy.android.app.utils.Constants.DISPUTE_URL
import mega.privacy.android.app.utils.Constants.HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.NAME
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelColor
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelDrawable
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelText
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import timber.log.Timber

/**
 * Bottom Sheet Dialog that represents the UI for a dialog containing image information.
 */
@AndroidEntryPoint
class ImageBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    private enum class AlertDialogType { TYPE_REMOVE_LINK, TYPE_RUBBISH_BIN, TYPE_REMOVE_CHAT_NODE }

    companion object {
        private const val TAG = "ImageBottomSheetDialogFragment"
        private const val STATE_ALERT_DIALOG_TYPE = "STATE_ALERT_DIALOG_TYPE"

        /**
         * Main method to create a ImageBottomSheetDialogFragment.
         *
         * @param itemId        Item to show
         * @return              ImageBottomSheetDialogFragment to be shown
         */
        fun newInstance(itemId: Long): ImageBottomSheetDialogFragment =
            ImageBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(INTENT_EXTRA_KEY_HANDLE, itemId)
                }
            }
    }

    private val viewModel by viewModels<ImageViewerViewModel>({ requireActivity() })
    private val itemId by lazy { arguments?.getLong(INTENT_EXTRA_KEY_HANDLE) ?: error("Null Item Id") }
    private var alertDialog: Dialog? = null
    private var alertDialogType: AlertDialogType? = null

    private lateinit var binding: BottomSheetImageOptionsBinding
    private lateinit var selectMoveFolderLauncher: ActivityResultLauncher<LongArray>
    private lateinit var selectCopyFolderLauncher: ActivityResultLauncher<LongArray>
    private lateinit var selectImportFolderLauncher: ActivityResultLauncher<LongArray>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = BottomSheetImageOptionsBinding.inflate(inflater, container, false)
        contentView = binding.root
        itemsLayout = binding.layoutItems
        buildActivityLaunchers()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.loadSingleNode(itemId!!)
        viewModel.onImage(itemId!!).observe(viewLifecycleOwner, ::showNodeData)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState?.containsKey(STATE_ALERT_DIALOG_TYPE) == true) {
            val dialogType =
                AlertDialogType.values()[savedInstanceState.getInt(STATE_ALERT_DIALOG_TYPE)]
            binding.root.post { showAlertDialog(dialogType) }
        }
    }

    override fun onDestroyView() {
        alertDialog?.dismiss()
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (alertDialog?.isShowing == true && alertDialogType != null) {
            outState.putInt(STATE_ALERT_DIALOG_TYPE, alertDialogType!!.ordinal)
        }
        super.onSaveInstanceState(outState)
    }

    @SuppressLint("SetTextI18n")
    private fun showNodeData(imageItem: ImageItem?) {
        if (imageItem?.nodeItem == null) {
            Timber.w("Image node is null")
            dismissAllowingStateLoss()
            return
        }

        val node = imageItem.nodeItem?.node
        val nodeItem = imageItem.nodeItem
        val nodeHandle = imageItem.getNodeHandle()
        val isUserLoggedIn = viewModel.isUserLoggedIn()

        binding.apply {
            imageItem.imageResult?.getLowestResolutionAvailableUri()?.let { imageUri ->
                imgThumbnail.post {
                    imgThumbnail.controller = Fresco.newDraweeControllerBuilder()
                        .setImageRequest(
                            ImageRequestBuilder.newBuilderWithSource(imageUri)
                                .setRotationOptions(RotationOptions.autoRotate())
                                .setResizeOptions(
                                    ResizeOptions.forSquareSize(imgThumbnail.width)
                                )
                                .build()
                        )
                        .setOldController(binding.imgThumbnail.controller)
                        .build()
                }
            }

            txtName.text = imageItem.name
            txtInfo.text = imageItem.infoText

            if (nodeItem?.hasVersions == true) {
                txtInfo.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_baseline_history,
                    0,
                    0,
                    0
                )
            }

            // File Info
            optionInfo.isVisible = imageItem.shouldShowInfoOption(isUserLoggedIn)
            optionInfo.setOnClickListener {
                val intent = if (imageItem is ImageItem.OfflineNode) {
                    Intent(context, OfflineFileInfoActivity::class.java).apply {
                        putExtra(HANDLE, imageItem.handle.toString())
                    }
                } else {
                    Intent(context, FileInfoActivity::class.java).apply {
                        putExtra(HANDLE, nodeHandle)
                        putExtra(NAME, imageItem.name)
                    }
                }

                startActivity(intent)
            }

            // Favorite
            optionFavorite.isVisible = imageItem.shouldShowFavoriteOption()
            if (node != null) {
                val favoriteText =
                    if (node.isFavourite) R.string.file_properties_unfavourite else R.string.file_properties_favourite
                val favoriteDrawable =
                    if (node.isFavourite) R.drawable.ic_remove_favourite else R.drawable.ic_add_favourite
                optionFavorite.text = StringResourcesUtils.getString(favoriteText)
                optionFavorite.setCompoundDrawablesWithIntrinsicBounds(favoriteDrawable, 0, 0, 0)
                optionFavorite.setOnClickListener {
                    viewModel.markNodeAsFavorite(nodeHandle!!, !node.isFavourite)
                }
            }

            // Label
            optionLabelLayout.isVisible = imageItem.shouldShowLabelOption()
            if (node != null) {
                val labelColor =
                    ResourcesCompat.getColor(resources, getNodeLabelColor(node.label), null)
                val labelDrawable = getNodeLabelDrawable(node.label, resources)
                optionLabelCurrent.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                    null,
                    labelDrawable,
                    null)
                optionLabelCurrent.setTextColor(labelColor)
                optionLabelCurrent.text = getNodeLabelText(node.label)
                optionLabelCurrent.isVisible = node.label != MegaNode.NODE_LBL_UNKNOWN
                optionLabelLayout.setOnClickListener {
                    NodeLabelBottomSheetDialogFragment.newInstance(nodeHandle!!)
                        .show(childFragmentManager, TAG)
                }
            }

            // Dispute takedown
            optionDispute.isVisible = imageItem.shouldShowDisputeOption()
            optionDispute.setOnClickListener {
                val intent = Intent(requireContext(), WebViewActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .setData(DISPUTE_URL.toUri())
                startActivity(intent)
                dismissAllowingStateLoss()
            }

            // Slideshow
            optionSlideshow.isVisible = imageItem.shouldShowSlideshowOption() && viewModel.getImagesSize(false) > 1
            optionSlideshow.setOnClickListener {
                findNavController().navigate(ImageViewerFragmentDirections.actionViewerToSlideshow())
                dismissAllowingStateLoss()
            }

            // Open with
            optionOpenWith.isVisible = imageItem.shouldShowOpenWithOption(isUserLoggedIn)
            optionOpenWith.setOnClickListener {
                if (imageItem is ImageItem.OfflineNode) {
                    OfflineUtils.openWithOffline(requireActivity(), nodeHandle)
                } else {
                    ModalBottomSheetUtil.openWith(requireActivity(), node)
                }
                dismissAllowingStateLoss()
            }

            // Forward
            optionForward.isVisible = imageItem.shouldShowForwardOption()
            optionForward.setOnClickListener {
                (activity as? ImageViewerActivity?)?.attachNode(node!!)
                dismissAllowingStateLoss()
            }

            // Download
            optionDownload.isVisible = imageItem.shouldShowDownloadOption()
            optionDownload.setOnClickListener {
                if (nodeItem?.isAvailableOffline == true) {
                    (activity as? ImageViewerActivity?)?.saveOfflineNode(nodeHandle!!)
                } else if (node != null) {
                    (activity as? ImageViewerActivity?)?.saveNode(node)
                }
                dismissAllowingStateLoss()
            }

            // Offline
            optionOfflineLayout.isVisible = imageItem.shouldShowOfflineOption()
            optionOfflineRemove.isVisible = imageItem is ImageItem.OfflineNode
            switchOffline.isChecked = nodeItem?.isAvailableOffline == true
            val offlineAction = {
                viewModel.switchNodeOfflineAvailability(nodeItem!!, requireActivity())
                dismissAllowingStateLoss()
            }
            optionOfflineRemove.setOnClickListener {
                viewModel.removeOfflineNode(nodeHandle!!,
                    requireActivity())
            }
            switchOffline.post {
                optionOfflineLayout.setOnClickListener { offlineAction.invoke() }
                switchOffline.setOnCheckedChangeListener { _, _ -> offlineAction.invoke() }
            }

            // Links
            optionManageLink.isVisible = imageItem.shouldShowManageLinkOption()
            optionRemoveLink.isVisible = imageItem.shouldShowRemoveLinkOption()
            if (node?.isExported == true) {
                optionManageLink.text = StringResourcesUtils.getString(R.string.edit_link_option)
            } else {
                optionManageLink.text = getQuantityString(R.plurals.get_links, 1)
            }
            optionManageLink.setOnClickListener {
                LinksUtil.showGetLinkActivity(this@ImageBottomSheetDialogFragment, nodeHandle!!)
            }
            optionRemoveLink.setOnClickListener { showAlertDialog(AlertDialogType.TYPE_REMOVE_LINK) }

            // Send to contact
            optionSendToChat.isVisible = imageItem.shouldShowSendToContactOption(isUserLoggedIn)
            optionSendToChat.setOnClickListener {
                (activity as? ImageViewerActivity?)?.attachNode(node!!)
                dismissAllowingStateLoss()
            }

            // Share
            optionShare.isVisible = imageItem.shouldShowShareOption()
            optionShare.setOnClickListener {
                when {
                    imageItem is ImageItem.OfflineNode ->
                        OfflineUtils.shareOfflineNode(context, imageItem.handle)
                    imageItem.imageResult?.fullSizeUri?.toFile()?.exists() == true ->
                        FileUtil.shareFile(context, imageItem.imageResult!!.fullSizeUri!!.toFile())
                    imageItem is ImageItem.PublicNode ->
                        MegaNodeUtil.shareLink(requireActivity(), imageItem.nodePublicLink)
                    node != null ->
                        viewModel.exportNode(node).observe(viewLifecycleOwner) { link ->
                            if (!link.isNullOrBlank()) {
                                MegaNodeUtil.shareLink(requireActivity(), link)
                            }
                        }
                }
            }

            // Rename
            optionRename.isVisible = imageItem.shouldShowRenameOption()
            optionRename.setOnClickListener {
                (activity as? ImageViewerActivity?)?.showRenameDialog(node!!)
            }

            // Move
            optionMove.isVisible = imageItem.shouldShowMoveOption()
            optionMove.setOnClickListener {
                selectMoveFolderLauncher.launch(longArrayOf(nodeHandle!!))
            }

            // Copy
            optionCopy.isVisible = imageItem.shouldShowCopyOption(isUserLoggedIn)
            optionCopy.setOnClickListener {
                if (nodeItem?.isExternalNode == true || imageItem is ImageItem.ChatNode) {
                    selectImportFolderLauncher.launch(longArrayOf(nodeHandle!!))
                } else {
                    selectCopyFolderLauncher.launch(longArrayOf(nodeHandle!!))
                }
            }
            val copyAction =
                if (nodeItem?.isExternalNode == true) R.string.general_import else if (imageItem is ImageItem.ChatNode) R.string.add_to_cloud_node_chat else R.string.context_copy
            val copyDrawable =
                if (nodeItem?.isExternalNode == true || imageItem is ImageItem.ChatNode) R.drawable.ic_import_to_cloud_white else R.drawable.ic_menu_copy
            optionCopy.setCompoundDrawablesWithIntrinsicBounds(copyDrawable, 0, 0, 0)
            optionCopy.text = StringResourcesUtils.getString(copyAction)

            // Restore
            optionRestore.isVisible = imageItem.shouldShowRestoreOption()
            optionRestore.setOnClickListener {
                viewModel.moveNode(nodeHandle!!, node!!.restoreHandle)
                dismissAllowingStateLoss()
            }

            // Rubbish bin
            optionRubbishBin.apply {
                isVisible = imageItem.shouldShowRubbishBinOption()
                if (nodeItem?.isFromRubbishBin == true) {
                    setText(R.string.general_remove)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_remove, 0, 0, 0);
                } else {
                    setText(R.string.context_move_to_trash)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_rubbish_bin, 0, 0, 0);
                }
                setOnClickListener { showAlertDialog(AlertDialogType.TYPE_RUBBISH_BIN) }
            }

            // Chat Remove
            optionChatRemove.setOnClickListener { showAlertDialog(AlertDialogType.TYPE_REMOVE_CHAT_NODE) }
            optionChatRemove.isVisible = imageItem.shouldShowChatRemoveOption()

            // Separators
            separatorLabel.isVisible = optionLabelLayout.isVisible || optionInfo.isVisible
            separatorDispute.isVisible = optionDispute.isVisible
            separatorOpen.isVisible = optionSlideshow.isVisible || optionOpenWith.isVisible
                    || optionForward.isVisible
            separatorOffline.isVisible = optionOfflineLayout.isVisible || optionShare.isVisible
            separatorShare.isVisible =
                optionShare.isVisible || optionSendToChat.isVisible || optionManageLink.isVisible
            separatorRestore.isVisible = optionRestore.isVisible
            separatorCopy.isVisible =
                (optionRename.isVisible || optionCopy.isVisible || optionMove.isVisible)
                        && (optionRestore.isVisible || optionRubbishBin.isVisible || optionChatRemove.isVisible)
        }
    }

    private fun buildActivityLaunchers() {
        selectMoveFolderLauncher =
            registerForActivityResult(SelectFolderToMoveActivityContract()) { result ->
                if (result != null) {
                    val moveHandle = result.first.firstOrNull()
                    val toHandle = result.second
                    if (moveHandle != null && moveHandle != INVALID_HANDLE && toHandle != INVALID_HANDLE) {
                        viewModel.moveNode(moveHandle, toHandle)
                        dismissAllowingStateLoss()
                    }
                }
            }
        selectCopyFolderLauncher =
            registerForActivityResult(SelectFolderToCopyActivityContract()) { result ->
                if (result != null) {
                    val copyHandle = result.first.firstOrNull()
                    val toHandle = result.second
                    if (copyHandle != null && copyHandle != INVALID_HANDLE && toHandle != INVALID_HANDLE) {
                        viewModel.copyNode(copyHandle, toHandle)
                        dismissAllowingStateLoss()
                    }
                }
            }
        selectImportFolderLauncher =
            registerForActivityResult(SelectFolderToImportActivityContract()) { result ->
                if (result != null) {
                    val copyHandle = result.first.firstOrNull()
                    val toHandle = result.second
                    if (copyHandle != null && copyHandle != INVALID_HANDLE && toHandle != INVALID_HANDLE) {
                        viewModel.copyNode(copyHandle, toHandle)
                        dismissAllowingStateLoss()
                    }
                }
            }
    }

    /**
     * Show Alert Dialog based on specific action to the node
     *
     * @param type  Alert Dialog Type of action
     */
    private fun showAlertDialog(type: AlertDialogType) {
        val imageItem = viewModel.getImageItem(itemId!!) ?: return
        val nodeHandle = imageItem.getNodeHandle() ?: return
        alertDialog?.dismiss()
        alertDialogType = type
        when (type) {
            AlertDialogType.TYPE_REMOVE_LINK -> {
                alertDialog = MaterialAlertDialogBuilder(requireContext())
                    .setMessage(getQuantityString(R.plurals.remove_links_warning_text, 1))
                    .setPositiveButton(StringResourcesUtils.getString(R.string.general_remove)) { _, _ ->
                        viewModel.removeLink(nodeHandle)
                        dismissAllowingStateLoss()
                    }
                    .setNegativeButton(StringResourcesUtils.getString(R.string.general_cancel),
                        null)
                    .show()
            }
            AlertDialogType.TYPE_RUBBISH_BIN -> {
                val isFromRubbishBin = imageItem.nodeItem?.isFromRubbishBin ?: return
                val buttonText: Int
                val messageText: Int

                if (isFromRubbishBin) {
                    buttonText = R.string.general_remove
                    messageText = R.string.confirmation_delete_from_mega
                } else {
                    buttonText = R.string.general_move
                    messageText = R.string.confirmation_move_to_rubbish
                }

                alertDialog = MaterialAlertDialogBuilder(requireContext())
                    .setMessage(StringResourcesUtils.getString(messageText))
                    .setPositiveButton(StringResourcesUtils.getString(buttonText)) { _, _ ->
                        if (isFromRubbishBin) {
                            viewModel.removeNode(nodeHandle)
                        } else {
                            viewModel.moveNodeToRubbishBin(nodeHandle)
                        }
                        dismissAllowingStateLoss()
                    }
                    .setNegativeButton(StringResourcesUtils.getString(R.string.general_cancel),
                        null)
                    .show()
            }
            AlertDialogType.TYPE_REMOVE_CHAT_NODE -> {
                alertDialog = MaterialAlertDialogBuilder(requireContext())
                    .setMessage(StringResourcesUtils.getString(R.string.confirmation_delete_one_message))
                    .setPositiveButton(StringResourcesUtils.getString(R.string.general_remove)) { _, _ ->
                        viewModel.removeChatMessage(nodeHandle)
                        dismissAllowingStateLoss()
                    }
                    .setNegativeButton(
                        StringResourcesUtils.getString(R.string.general_cancel),
                        null
                    )
                    .show()
            }
        }
    }

    /**
     * Custom show method to avoid showing the same dialog multiple times
     */
    fun show(manager: FragmentManager) {
        if (manager.findFragmentByTag(TAG) == null) {
            super.show(manager, TAG)
        }
    }

    override fun shouldSetStatusBarColor(): Boolean = false
}
