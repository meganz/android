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
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.OfflineFileInfoActivity
import mega.privacy.android.app.activities.contract.SelectFolderToCopyActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToImportActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToMoveActivityContract
import mega.privacy.android.app.databinding.BottomSheetImageOptionsBinding
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.imageviewer.ImageViewerViewModel
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil
import mega.privacy.android.app.modalbottomsheet.nodelabel.NodeLabelBottomSheetDialogFragment
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.ExtraUtils.extra
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelColor
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelDrawable
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelText
import mega.privacy.android.app.utils.NetworkUtil.isOnline
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode


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
         * @param nodeHandle    Image node to show information from
         * @return              ImageBottomSheetDialogFragment to be shown
         */
        fun newInstance(nodeHandle: Long): ImageBottomSheetDialogFragment =
            ImageBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(INTENT_EXTRA_KEY_HANDLE, nodeHandle)
                }
            }
    }

    private val viewModel by viewModels<ImageViewerViewModel>({ requireActivity() })
    private val nodeHandle: Long? by extra(INTENT_EXTRA_KEY_HANDLE)
    private var alertDialog: Dialog? = null
    private var alertDialogType: AlertDialogType? = null

    private lateinit var binding: BottomSheetImageOptionsBinding
    private lateinit var selectMoveFolderLauncher: ActivityResultLauncher<LongArray>
    private lateinit var selectCopyFolderLauncher: ActivityResultLauncher<LongArray>
    private lateinit var selectImportFolderLauncher: ActivityResultLauncher<LongArray>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireNotNull(nodeHandle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetImageOptionsBinding.inflate(inflater, container, false)
        contentView = binding.root
        itemsLayout = binding.layoutItems
        buildActivityLaunchers()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.loadSingleNode(nodeHandle!!)
        viewModel.onImage(nodeHandle!!).observe(viewLifecycleOwner, ::showNodeData)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState?.containsKey(STATE_ALERT_DIALOG_TYPE) == true) {
            val dialogType = AlertDialogType.values()[savedInstanceState.getInt(STATE_ALERT_DIALOG_TYPE)]
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
            logWarning("Image node is null")
            dismissAllowingStateLoss()
            return
        }

        val node = imageItem.nodeItem.node
        val nodeItem = imageItem.nodeItem
        val imageUri = imageItem.imageResult?.getLowestResolutionAvailableUri()
        val isUserLoggedIn = viewModel.isUserLoggedIn()
        val isOnline = requireContext().isOnline()

        binding.apply {
            imgThumbnail.controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(ImageRequest.fromUri(imageUri))
                .setOldController(binding.imgThumbnail.controller)
                .build()

            txtName.text = nodeItem.name
            txtInfo.text = nodeItem.infoText

            if (nodeItem.hasVersions) {
                txtInfo.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_baseline_history,
                    0,
                    0,
                    0
                )
            }

            // File Info
            optionInfo.setOnClickListener {
                val intent = if (imageItem.isOffline) {
                    Intent(context, OfflineFileInfoActivity::class.java).apply {
                        putExtra(HANDLE, nodeItem.handle.toString())
                    }
                } else {
                    Intent(context, FileInfoActivityLollipop::class.java).apply {
                        putExtra(HANDLE, nodeItem.handle)
                        putExtra(NAME, nodeItem.name)
                    }
                }

                startActivity(intent)
            }
            optionInfo.isVisible = isUserLoggedIn && !nodeItem.isExternalNode && nodeItem.hasReadAccess

            // Favorite
            if (node != null) {
                val favoriteText = if (node.isFavourite) R.string.file_properties_unfavourite else R.string.file_properties_favourite
                val favoriteDrawable = if (node.isFavourite) R.drawable.ic_remove_favourite else R.drawable.ic_add_favourite
                optionFavorite.text = StringResourcesUtils.getString(favoriteText)
                optionFavorite.isVisible = isOnline && nodeItem.hasFullAccess && !imageItem.isFromChat()
                optionFavorite.setCompoundDrawablesWithIntrinsicBounds(favoriteDrawable, 0, 0, 0)
                optionFavorite.setOnClickListener {
                    viewModel.markNodeAsFavorite(nodeItem.handle, !node.isFavourite)
                }
            } else {
                optionFavorite.isVisible = false
            }

            // Label
            if (node != null) {
                val labelColor = ResourcesCompat.getColor(resources, getNodeLabelColor(node.label), null)
                val labelDrawable = getNodeLabelDrawable(node.label, resources)
                optionLabelCurrent.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        null,
                        null,
                        labelDrawable,
                        null
                )
                optionLabelCurrent.setTextColor(labelColor)
                optionLabelCurrent.text = getNodeLabelText(node.label)
                optionLabelCurrent.isVisible = node.label != MegaNode.NODE_LBL_UNKNOWN
                optionLabelLayout.isVisible = isOnline && nodeItem.hasFullAccess && !imageItem.isFromChat()
                optionLabelLayout.setOnClickListener {
                    NodeLabelBottomSheetDialogFragment.newInstance(nodeItem.handle).show(childFragmentManager, TAG)
                }
            } else {
                optionLabelLayout.isVisible = false
            }

            // Open with
            optionOpenWith.isVisible = isUserLoggedIn && !nodeItem.isExternalNode && !nodeItem.isFromRubbishBin && nodeItem.hasReadAccess && !imageItem.isFromChat()
            optionOpenWith.setOnClickListener {
                if (imageItem.isOffline) {
                    OfflineUtils.openWithOffline(requireActivity(), nodeItem.handle)
                } else {
                    ModalBottomSheetUtil.openWith(requireActivity(), node)
                }
                dismissAllowingStateLoss()
            }

            // Forward
            optionForward.isVisible = imageItem.isFromChat()
            optionForward.setOnClickListener {
                (activity as? ImageViewerActivity?)?.attachNode(node!!)
                dismissAllowingStateLoss()
            }

            // Download
            optionDownload.isVisible = !nodeItem.isFromRubbishBin
            optionDownload.setOnClickListener {
                if (nodeItem.isAvailableOffline) {
                    (activity as? ImageViewerActivity?)?.saveOfflineNode(nodeItem.handle)
                } else if (node != null) {
                    (activity as? ImageViewerActivity?)?.saveNode(node)
                }
                dismissAllowingStateLoss()
            }

            // Offline
            optionOfflineLayout.isVisible = !imageItem.isOffline && !nodeItem.isFromRubbishBin
            optionOfflineRemove.isVisible = imageItem.isOffline
            switchOffline.isChecked = nodeItem.isAvailableOffline
            val offlineAction = {
                viewModel.switchNodeOfflineAvailability(nodeItem, requireActivity())
                dismissAllowingStateLoss()
            }
            optionOfflineRemove.setOnClickListener { viewModel.removeOfflineNode(imageItem.handle, requireActivity()) }
            switchOffline.post {
                optionOfflineLayout.setOnClickListener { offlineAction.invoke() }
                switchOffline.setOnCheckedChangeListener { _, _ -> offlineAction.invoke() }
            }

            // Links
            if (node?.isExported == true) {
                optionManageLink.text = StringResourcesUtils.getString(R.string.edit_link_option)
            } else {
                optionManageLink.text = getQuantityString(R.plurals.get_links, 1)
            }
            optionManageLink.setOnClickListener {
                LinksUtil.showGetLinkActivity(this@ImageBottomSheetDialogFragment, nodeItem.handle)
            }
            optionRemoveLink.setOnClickListener { showAlertDialog(AlertDialogType.TYPE_REMOVE_LINK) }
            optionManageLink.isVisible = isOnline && nodeItem.hasOwnerAccess && !nodeItem.isFromRubbishBin && !imageItem.isFromChat()
            optionRemoveLink.isVisible = optionManageLink.isVisible && node?.isExported == true

            // Send to contact
            optionSendToChat.setOnClickListener {
                (activity as? ImageViewerActivity?)?.attachNode(node!!)
                dismissAllowingStateLoss()
            }
            optionSendToChat.isVisible = isOnline && isUserLoggedIn && !nodeItem.isExternalNode && node != null && !nodeItem.isFromRubbishBin && nodeItem.hasReadAccess && !imageItem.isFromChat()

            // Share
            optionShare.isVisible = !nodeItem.isFromRubbishBin && (imageItem.isOffline || nodeItem.hasOwnerAccess || !imageItem.nodePublicLink.isNullOrBlank())
            optionShare.setOnClickListener {
                when {
                    imageItem.isOffline ->
                        OfflineUtils.shareOfflineNode(context, nodeItem.handle)
                    imageItem.imageResult?.fullSizeUri?.toFile()?.exists() == true ->
                        FileUtil.shareFile(context, imageItem.imageResult.fullSizeUri!!.toFile())
                    !imageItem.nodePublicLink.isNullOrBlank() ->
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
            optionRename.setOnClickListener {
                (activity as? ImageViewerActivity?)?.showRenameDialog(node!!)
            }
            optionRename.isVisible = isOnline && !nodeItem.isFromRubbishBin && nodeItem.hasFullAccess && !imageItem.isFromChat()

            // Move
            optionMove.setOnClickListener {
                selectMoveFolderLauncher.launch(longArrayOf(nodeItem.handle))
            }
            optionMove.isVisible = isOnline && !nodeItem.isFromRubbishBin && nodeItem.hasFullAccess && !imageItem.isFromChat()

            // Copy
            optionCopy.setOnClickListener {
                if (nodeItem.isExternalNode || imageItem.isFromChat()) {
                    selectImportFolderLauncher.launch(longArrayOf(nodeItem.handle))
                } else {
                    selectCopyFolderLauncher.launch(longArrayOf(nodeItem.handle))
                }
            }
            val copyAction = if (nodeItem.isExternalNode) R.string.general_import else if (imageItem.isFromChat()) R.string.add_to_cloud_node_chat else R.string.context_copy
            val copyDrawable = if (nodeItem.isExternalNode || imageItem.isFromChat()) R.drawable.ic_import_to_cloud_white else R.drawable.ic_menu_copy
            optionCopy.setCompoundDrawablesWithIntrinsicBounds(copyDrawable, 0, 0, 0)
            optionCopy.text = StringResourcesUtils.getString(copyAction)
            optionCopy.isVisible = isOnline && isUserLoggedIn && !nodeItem.isFromRubbishBin && !imageItem.isOffline

            // Restore
            optionRestore.setOnClickListener {
                viewModel.moveNode(nodeItem.handle, node!!.restoreHandle)
                dismissAllowingStateLoss()
            }
            optionRestore.isVisible = isOnline && nodeItem.isFromRubbishBin && node != null && node.restoreHandle != INVALID_HANDLE

            // Rubbish bin
            optionRubbishBin.isVisible = isOnline && nodeItem.hasFullAccess && !imageItem.isFromChat()
            if (nodeItem.isFromRubbishBin) {
                optionRubbishBin.setText(R.string.general_remove)
            } else {
                optionRubbishBin.setText(R.string.context_move_to_trash)
            }
            optionRubbishBin.setOnClickListener { showAlertDialog(AlertDialogType.TYPE_RUBBISH_BIN) }

            // Chat Remove
            optionChatRemove.setOnClickListener { showAlertDialog(AlertDialogType.TYPE_REMOVE_CHAT_NODE) }
            optionChatRemove.isVisible = imageItem.isChatDeletable && imageItem.isFromChat() && nodeItem.hasFullAccess

            // Separators
            separatorLabel.isVisible = optionLabelLayout.isVisible || optionInfo.isVisible
            separatorOpen.isVisible = optionOpenWith.isVisible || optionForward.isVisible
            separatorOffline.isVisible = optionOfflineLayout.isVisible || optionShare.isVisible
            separatorShare.isVisible = optionShare.isVisible || optionSendToChat.isVisible || optionManageLink.isVisible
            separatorRestore.isVisible = optionRestore.isVisible
            separatorCopy.isVisible = (optionRename.isVisible || optionCopy.isVisible || optionMove.isVisible)
                    && (optionRestore.isVisible || optionRubbishBin.isVisible || optionChatRemove.isVisible)
        }
    }

    private fun buildActivityLaunchers() {
        selectMoveFolderLauncher = registerForActivityResult(SelectFolderToMoveActivityContract()) { result ->
            if (result != null) {
                val moveHandle = result.first.firstOrNull()
                val toHandle = result.second
                if (moveHandle != null && moveHandle != INVALID_HANDLE && toHandle != INVALID_HANDLE) {
                    viewModel.moveNode(moveHandle, toHandle)
                }
            }
        }
        selectCopyFolderLauncher = registerForActivityResult(SelectFolderToCopyActivityContract()) { result ->
            if (result != null) {
                val copyHandle = result.first.firstOrNull()
                val toHandle = result.second
                if (copyHandle != null && copyHandle != INVALID_HANDLE && toHandle != INVALID_HANDLE) {
                    viewModel.copyNode(copyHandle, toHandle)
                }
            }
        }
        selectImportFolderLauncher = registerForActivityResult(SelectFolderToImportActivityContract()) { result ->
            if (result != null) {
                val copyHandle = result.first.firstOrNull()
                val toHandle = result.second
                if (copyHandle != null && copyHandle != INVALID_HANDLE && toHandle != INVALID_HANDLE) {
                    viewModel.copyNode(copyHandle, toHandle)
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
        val imageItem = viewModel.getImageItem(nodeHandle!!) ?: return
        alertDialog?.dismiss()
        alertDialogType = type
        when(type) {
            AlertDialogType.TYPE_REMOVE_LINK -> {
                alertDialog = MaterialAlertDialogBuilder(requireContext())
                    .setMessage(getQuantityString(R.plurals.remove_links_warning_text, 1))
                    .setPositiveButton(StringResourcesUtils.getString(R.string.general_remove)) { _, _ ->
                        viewModel.removeLink(imageItem.handle)
                        dismissAllowingStateLoss()
                    }
                    .setNegativeButton(StringResourcesUtils.getString(R.string.general_cancel), null)
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
                            viewModel.removeNode(imageItem.handle)
                        } else {
                            viewModel.moveNodeToRubbishBin(imageItem.handle)
                        }
                        dismissAllowingStateLoss()
                    }
                    .setNegativeButton(StringResourcesUtils.getString(R.string.general_cancel), null)
                    .show()
            }
            AlertDialogType.TYPE_REMOVE_CHAT_NODE -> {
                alertDialog = MaterialAlertDialogBuilder(requireContext())
                    .setMessage(StringResourcesUtils.getString(R.string.confirmation_delete_one_message))
                    .setPositiveButton(StringResourcesUtils.getString(R.string.general_remove)) { _, _ ->
                        viewModel.removeChatMessage(imageItem.chatRoomId!!, imageItem.chatMessageId!!)
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
