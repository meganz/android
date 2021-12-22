package mega.privacy.android.app.imageviewer.dialog

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.res.ResourcesCompat
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
import mega.privacy.android.app.utils.ExtraUtils.extraNotNull
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelColor
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelDrawable
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelText
import mega.privacy.android.app.utils.NetworkUtil.isOnline
import mega.privacy.android.app.utils.SdkRestrictionUtils.isSaveToGalleryCompatible
import mega.privacy.android.app.utils.StringResourcesUtils.*
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode


/**
 * Bottom Sheet Dialog that represents the UI for a dialog containing image information.
 */
@AndroidEntryPoint
class ImageBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    companion object {
        private const val TAG = "ImageBottomSheetDialogFragment"

        /**
         * Main method to create a ImageBottomSheetDialogFragment.
         *
         * @param imageNodeHandle       Image to show information about
         * @return                      ImageBottomSheetDialogFragment to be shown
         */
        fun newInstance(imageNodeHandle: Long): ImageBottomSheetDialogFragment =
            ImageBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(INTENT_EXTRA_KEY_HANDLE, imageNodeHandle)
                }
            }
    }

    private val viewModel by viewModels<ImageViewerViewModel>({ requireActivity() })
    private val imageNodeHandle by extraNotNull(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)

    private lateinit var binding: BottomSheetImageOptionsBinding
    private lateinit var selectMoveFolderLauncher: ActivityResultLauncher<LongArray>
    private lateinit var selectCopyFolderLauncher: ActivityResultLauncher<LongArray>
    private lateinit var selectImportFolderLauncher: ActivityResultLauncher<LongArray>

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
        viewModel.loadSingleNode(imageNodeHandle, true)
        viewModel.onImage(imageNodeHandle).observe(viewLifecycleOwner, ::showNodeData)
        super.onViewCreated(view, savedInstanceState)
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
                val intent = if (!isOnline && nodeItem.isAvailableOffline) {
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
            optionInfo.isVisible = node?.isPublic != true

            // Favorite
            if (node != null) {
                val favoriteText = if (node.isFavourite) R.string.file_properties_unfavourite else R.string.file_properties_favourite
                val favoriteDrawable = if (node.isFavourite) R.drawable.ic_remove_favourite else R.drawable.ic_add_favourite
                optionFavorite.text = StringResourcesUtils.getString(favoriteText)
                optionFavorite.isVisible = isOnline && nodeItem.hasFullAccess
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
                optionLabelLayout.isVisible = isOnline && nodeItem.hasFullAccess
                optionLabelLayout.setOnClickListener {
                    NodeLabelBottomSheetDialogFragment.newInstance(nodeItem.handle).show(childFragmentManager, TAG)
                }
            } else {
                optionLabelLayout.isVisible = false
            }

            // Open with
            optionOpenWith.isVisible = !nodeItem.isFromRubbishBin && node?.isPublic != true
            optionOpenWith.setOnClickListener {
                if (!isOnline && nodeItem.isAvailableOffline) {
                    OfflineUtils.openWithOffline(requireContext(), nodeItem.handle)
                } else {
                    ModalBottomSheetUtil.openWith(requireContext(), node)
                }
            }

            // Download
            optionDownload.isVisible = !nodeItem.isFromRubbishBin
            optionDownload.setOnClickListener {
                if (!isOnline && nodeItem.isAvailableOffline) {
                    (activity as? ImageViewerActivity?)?.saveOfflineNode(nodeItem.handle)
                } else if (node != null) {
                    (activity as? ImageViewerActivity?)?.saveNode(node, false)
                }
                dismissAllowingStateLoss()
            }

            // Save to Gallery
            optionGallery.isVisible = isSaveToGalleryCompatible() && !nodeItem.isFromRubbishBin && node?.isPublic != true
            optionGallery.setOnClickListener {
                (activity as? ImageViewerActivity?)?.saveNode(node!!, false)
                dismissAllowingStateLoss()
            }

            // Offline
            optionOfflineLayout.isVisible = !nodeItem.isFromRubbishBin && nodeItem.hasReadAccess
            switchOffline.isChecked = nodeItem.isAvailableOffline
            val offlineAction = {
                viewModel.switchNodeOfflineAvailability(requireActivity(), nodeItem)
                dismissAllowingStateLoss()
            }
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
            optionRemoveLink.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(getQuantityString(R.plurals.remove_links_warning_text, 1))
                    .setPositiveButton(StringResourcesUtils.getString(R.string.general_remove)) { _, _ -> viewModel.removeLink(nodeItem.handle) }
                    .setNegativeButton(StringResourcesUtils.getString(R.string.general_cancel), null)
                    .show()
            }
            optionManageLink.isVisible = isOnline && nodeItem.hasOwnerAccess && !nodeItem.isFromRubbishBin
            optionRemoveLink.isVisible = isOnline && nodeItem.hasOwnerAccess && !nodeItem.isFromRubbishBin && node?.isExported == true

            // Send to contact
            optionSendToContact.setOnClickListener {
                (activity as? ImageViewerActivity?)?.attachNode(node!!)
                dismissAllowingStateLoss()
            }
            optionSendToContact.isVisible = isOnline && !nodeItem.isFromRubbishBin && node != null

            // Share
            optionShare.isVisible = !nodeItem.isFromRubbishBin && (nodeItem.hasOwnerAccess || !imageItem.nodePublicLink.isNullOrBlank())
            optionShare.setOnClickListener {
                when {
                    !isOnline && nodeItem.isAvailableOffline ->
                        OfflineUtils.shareOfflineNode(context, nodeItem.handle)
                    imageItem.nodePublicLink.isNullOrBlank() ->
                        MegaNodeUtil.shareLink(requireContext(), imageItem.nodePublicLink)
                    node != null ->
                        viewModel.shareNode(node).observe(viewLifecycleOwner) { link ->
                            if (!link.isNullOrBlank()) {
                                MegaNodeUtil.shareLink(requireContext(), link)
                            }
                        }
                }
            }

            // Rename
            optionRename.setOnClickListener {
                (activity as? ImageViewerActivity?)?.showRenameDialog(node!!)
            }
            optionRename.isVisible = isOnline && !nodeItem.isFromRubbishBin && nodeItem.hasFullAccess && node != null

            // Move
            optionMove.setOnClickListener {
                selectMoveFolderLauncher.launch(longArrayOf(nodeItem.handle))
            }
            optionMove.isVisible = isOnline && !nodeItem.isFromRubbishBin && nodeItem.hasFullAccess

            // Copy
            optionCopy.setOnClickListener {
                if (node?.isPublic == true) {
                    selectImportFolderLauncher.launch(longArrayOf(nodeItem.handle))
                } else {
                    selectCopyFolderLauncher.launch(longArrayOf(nodeItem.handle))
                }
            }
            val copyAction = if (node?.isPublic == true) R.string.general_import else R.string.context_copy
            val copyDrawable = if (node?.isPublic == true) R.drawable.ic_import_to_cloud_white else R.drawable.ic_menu_copy
            optionCopy.setCompoundDrawablesWithIntrinsicBounds(copyDrawable, 0, 0, 0)
            optionCopy.text = StringResourcesUtils.getString(copyAction)
            optionCopy.isVisible = isOnline && !nodeItem.isFromRubbishBin && viewModel.isUserLoggedIn()

            // Restore
            optionRestore.setOnClickListener {
                viewModel.moveNode(nodeItem.handle, node!!.restoreHandle)
                dismissAllowingStateLoss()
            }
            optionRestore.isVisible = isOnline && nodeItem.isFromRubbishBin && node != null && node.restoreHandle != INVALID_HANDLE

            // Rubbish bin
            optionRubbishBin.isVisible = isOnline && nodeItem.hasFullAccess
            if (nodeItem.isFromRubbishBin) {
                optionRubbishBin.setText(R.string.general_remove)
            } else {
                optionRubbishBin.setText(R.string.context_move_to_trash)
            }
            optionRubbishBin.setOnClickListener {
                val buttonText: Int
                val messageText: Int

                if (nodeItem.isFromRubbishBin) {
                    buttonText = R.string.general_remove
                    messageText = R.string.confirmation_delete_from_mega
                } else {
                    buttonText = R.string.general_move
                    messageText = R.string.confirmation_move_to_rubbish
                }

                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(StringResourcesUtils.getString(messageText))
                    .setPositiveButton(StringResourcesUtils.getString(buttonText)) { _, _ ->
                        if (nodeItem.isFromRubbishBin) {
                            viewModel.removeNode(nodeItem.handle)
                        } else {
                            viewModel.moveNodeToRubbishBin(nodeItem.handle)
                        }
                        dismissAllowingStateLoss()
                    }
                    .setNegativeButton(StringResourcesUtils.getString(R.string.general_cancel), null)
                    .show()
            }

            // Separators
            separatorLabel.isVisible = optionLabelLayout.isVisible
            separatorOpen.isVisible = optionOpenWith.isVisible
            separatorOffline.isVisible = optionOfflineLayout.isVisible
            separatorShare.isVisible = optionShare.isVisible || optionSendToContact.isVisible || optionManageLink.isVisible
            separatorRestore.isVisible = optionRestore.isVisible
            separatorCopy.isVisible = (optionRename.isVisible || optionCopy.isVisible || optionMove.isVisible)
                    && (optionRestore.isVisible || optionRubbishBin.isVisible)
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
     * Custom show method to avoid showing the same dialog multiple times
     */
    fun show(manager: FragmentManager) {
        if (manager.findFragmentByTag(TAG) == null) {
            super.show(manager, TAG)
        }
    }

    override fun shouldSetStatusBarColor(): Boolean = false
}
