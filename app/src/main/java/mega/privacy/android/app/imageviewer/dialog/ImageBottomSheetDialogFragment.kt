package mega.privacy.android.app.imageviewer.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetImageOptionsBinding
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.imageviewer.ImageViewerViewModel
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop.ACTION_PICK_COPY_FOLDER
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop.ACTION_PICK_IMPORT_FOLDER
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetImageOptionsBinding.inflate(inflater, container, false)
        contentView = binding.root
        itemsLayout = binding.layoutItems
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.onImage(imageNodeHandle).observe(viewLifecycleOwner, ::showNodeData)
        super.onViewCreated(view, savedInstanceState)
    }

    @SuppressLint("SetTextI18n")
    private fun showNodeData(imageItem: ImageItem?) {
        if (imageItem?.nodeItem == null) {
            logWarning("Image node is null")
            dismiss()
            return
        }

        val node = imageItem.nodeItem.node
        val nodeItem = imageItem.nodeItem
        val imageUri = imageItem.imageResult?.getLowestResolutionAvailableUri()

        binding.apply {
            imgThumbnail.controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(ImageRequest.fromUri(imageUri))
                .setOldController(binding.imgThumbnail.controller)
                .build()

            txtName.text = node.name
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
                val intent = Intent(context, FileInfoActivityLollipop::class.java).apply {
                    putExtra(HANDLE, node.handle)
                    putExtra(NAME, node.name)
                }

                startActivity(intent)
            }
            optionInfo.isVisible = !node.isPublic

            // Favorite
            val favoriteText = if (node.isFavourite) R.string.file_properties_unfavourite else R.string.file_properties_favourite
            val favoriteDrawable = if (!node.isFavourite) R.drawable.ic_add_favourite else R.drawable.ic_remove_favourite
            optionFavorite.text = StringResourcesUtils.getString(favoriteText)
            optionFavorite.isVisible = !nodeItem.isFromRubbishBin && nodeItem.hasFullAccess
            optionFavorite.setCompoundDrawablesWithIntrinsicBounds(favoriteDrawable, 0, 0, 0)
            optionFavorite.setOnClickListener {
                viewModel.markNodeAsFavorite(node.handle, !node.isFavourite)
            }

            // Label
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
            optionLabelLayout.isVisible = !nodeItem.isFromRubbishBin && nodeItem.hasFullAccess
            optionLabelLayout.setOnClickListener {
                NodeLabelBottomSheetDialogFragment.newInstance(node.handle)
                    .show(childFragmentManager, TAG)
            }

            // Open with
            optionOpenWith.isVisible = !nodeItem.isFromRubbishBin && !node.isPublic
            optionOpenWith.setOnClickListener {
                ModalBottomSheetUtil.openWith(requireContext(), node)
            }

            // Download
            optionDownload.isVisible = !nodeItem.isFromRubbishBin
            optionDownload.setOnClickListener {
                (activity as? ImageViewerActivity?)?.saveNode(node, false)
                dismiss()
            }

            // Save to Gallery
            optionGallery.isVisible = !nodeItem.isFromRubbishBin && !node.isPublic
            optionGallery.setOnClickListener {
                (activity as? ImageViewerActivity?)?.saveNode(node, true)
                dismiss()
            }

            // Offline
            optionOfflineLayout.isVisible = !nodeItem.isFromRubbishBin && !node.isPublic
            switchOffline.isChecked = nodeItem.isAvailableOffline
            switchOffline.setOnCheckedChangeListener { _, _ ->
                viewModel.setNodeAvailableOffline(
                    requireActivity(),
                    node.handle,
                    !nodeItem.isAvailableOffline
                )
                dismiss()
            }
            optionOfflineLayout.setOnClickListener {
                viewModel.setNodeAvailableOffline(
                    requireActivity(),
                    node.handle,
                    !nodeItem.isAvailableOffline
                )
                dismiss()
            }

            // Links
            if (node.isExported) {
                optionManageLink.text = StringResourcesUtils.getString(R.string.edit_link_option)
            } else {
                optionManageLink.text = getQuantityString(R.plurals.get_links, 1)
            }
            optionManageLink.setOnClickListener {
                LinksUtil.showGetLinkActivity(this@ImageBottomSheetDialogFragment, node.handle)
            }
            optionRemoveLink.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(getQuantityString(R.plurals.remove_links_warning_text, 1))
                    .setPositiveButton(StringResourcesUtils.getString(R.string.general_remove)) { _, _ ->
                        (activity as? ImageViewerActivity?)?.removeLink(node.handle) ?: run {
                            viewModel.removeLink(node.handle)
                        }
                    }
                    .setNegativeButton(StringResourcesUtils.getString(R.string.general_cancel), null)
                    .show()
            }
            optionManageLink.isVisible = nodeItem.hasFullAccess && !nodeItem.isFromRubbishBin
            optionRemoveLink.isVisible = node.isExported

            // Send to contact
            optionSendToContact.setOnClickListener {
                (activity as? ImageViewerActivity?)?.attachNode(node)
                dismiss()
            }
            optionSendToContact.isVisible = nodeItem.hasFullAccess && !nodeItem.isFromRubbishBin

            // Share
            optionShare.setOnClickListener {
                viewModel.shareNode(node.handle).observe(viewLifecycleOwner) { link ->
                    MegaNodeUtil.startShareIntent(requireContext(), Intent(Intent.ACTION_SEND), link)
                    dismiss()
                }
            }
            optionShare.isVisible = !nodeItem.isFromRubbishBin && !node.isPublic

            // Rename
            optionRename.setOnClickListener {
                (activity as? ImageViewerActivity?)?.showRenameDialog(node)
            }
            optionRename.isVisible = nodeItem.hasFullAccess && !nodeItem.isFromRubbishBin

            // Move
            optionMove.setOnClickListener {
                val intent = Intent(context, FileExplorerActivityLollipop::class.java).apply {
                    action = FileExplorerActivityLollipop.ACTION_PICK_MOVE_FOLDER
                    putExtra(INTENT_EXTRA_KEY_MOVE_FROM, longArrayOf(node.handle))
                }

                startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER_TO_MOVE)
            }
            optionMove.isVisible = nodeItem.hasFullAccess && !nodeItem.isFromRubbishBin

            // Copy
            optionCopy.setOnClickListener {
                val intent = Intent(context, FileExplorerActivityLollipop::class.java).apply {
                    if (node.isPublic) {
                        action = ACTION_PICK_IMPORT_FOLDER
                        putExtra(INTENT_EXTRA_KEY_IMPORT_CHAT, longArrayOf(node.handle))
                    } else {
                        action = ACTION_PICK_COPY_FOLDER
                        putExtra(INTENT_EXTRA_KEY_COPY_FROM, longArrayOf(node.handle))
                    }
                }
                startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER_TO_COPY)
            }
            val copyAction = if (node.isPublic) R.string.general_import else R.string.context_copy
            optionCopy.text = StringResourcesUtils.getString(copyAction)
            optionCopy.isVisible = !nodeItem.isFromRubbishBin

            // Restore
            optionRestore.setOnClickListener {
                viewModel.moveNode(node.handle, node.restoreHandle)
                dismiss()
            }
            optionRestore.isVisible = nodeItem.isFromRubbishBin && node.restoreHandle != INVALID_HANDLE

            // Rubbish bin
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
                            viewModel.removeNode(node.handle)
                        } else {
                            viewModel.moveNodeToRubbishBin(node.handle)
                        }
                        dismiss()
                    }
                    .setNegativeButton(StringResourcesUtils.getString(R.string.general_cancel), null)
                    .show()
            }

            // Separators
            separatorOpen.isVisible = optionOpenWith.isVisible
            separatorLabel.isVisible = optionLabelLayout.isVisible
            separatorOpen.isVisible = optionOpenWith.isVisible
            separatorOffline.isVisible = optionOfflineLayout.isVisible
            separatorShare.isVisible = optionShare.isVisible
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_SELECT_FOLDER_TO_MOVE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val moveHandle = data?.getLongArrayExtra(INTENT_EXTRA_KEY_MOVE_HANDLES)?.firstOrNull() ?: INVALID_HANDLE
                    val toHandle = data?.getLongExtra(INTENT_EXTRA_KEY_MOVE_TO, INVALID_HANDLE) ?: INVALID_HANDLE
                    if (moveHandle != INVALID_HANDLE && toHandle != INVALID_HANDLE) {
                        viewModel.moveNode(moveHandle, toHandle)
                    }
                }
            }
            REQUEST_CODE_SELECT_FOLDER_TO_COPY -> {
                if (resultCode == Activity.RESULT_OK) {
                    val copyHandle =
                        data?.getLongArrayExtra(INTENT_EXTRA_KEY_COPY_HANDLES)?.firstOrNull()
                            ?: data?.getLongArrayExtra(INTENT_EXTRA_KEY_IMPORT_CHAT)?.firstOrNull()
                            ?: INVALID_HANDLE

                    val toHandle =
                        data?.getLongExtra(INTENT_EXTRA_KEY_COPY_TO, INVALID_HANDLE)
                            ?: data?.getLongExtra(INTENT_EXTRA_KEY_IMPORT_TO, INVALID_HANDLE)
                            ?: INVALID_HANDLE

                    if (copyHandle != INVALID_HANDLE && toHandle != INVALID_HANDLE) {
                        viewModel.copyNode(copyHandle, toHandle)
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
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
