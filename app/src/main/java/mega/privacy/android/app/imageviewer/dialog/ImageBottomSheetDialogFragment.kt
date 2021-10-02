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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetImageOptionsBinding
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.imageviewer.ImageViewerViewModel
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil
import mega.privacy.android.app.modalbottomsheet.nodelabel.NodeLabelBottomSheetDialogFragment
import mega.privacy.android.app.usecase.MegaNodeItem
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.ExtraUtils.extraNotNull
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelColor
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelDrawable
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelText
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
        mainLinearLayout = binding.layoutRoot
        items_layout = binding.layoutItems
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.post { setBottomSheetBehavior(HEIGHT_HEADER_LARGE, true) }

        viewModel.getNode(requireContext(), imageNodeHandle).observe(viewLifecycleOwner, ::showNodeData)
        viewModel.getImage(imageNodeHandle).observe(viewLifecycleOwner) { imageItem ->
            binding.imgThumbnail.setImageURI(imageItem?.thumbnailUri)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showNodeData(item: MegaNodeItem?) {
        if (item?.node == null || item.node.isTakenDown) {
            (activity as? ImageViewerActivity?)?.showSnackbar(getString(R.string.error_download_takendown_node))
            dismiss()
            return
        }

        binding.apply {
            txtName.text = item.node.name

            val nodeSizeText = Util.getSizeString(item.node.size)
            val nodeDateText = TimeUtils.formatLongDateTime(item.node.creationTime)
            txtInfo.text = "$nodeSizeText · $nodeDateText"

            // File Info
            optionInfo.setOnClickListener {
                val intent = Intent(context, FileInfoActivityLollipop::class.java).apply {
                    putExtra(HANDLE, item.node.handle)
                    putExtra(NAME, item.node.name)
                }

                startActivity(intent)
                dismiss()
            }

            // Favorite
            val favoriteText = if (item.node.isFavourite) R.string.file_properties_unfavourite else R.string.file_properties_favourite
            val favoriteDrawable = if (!item.node.isFavourite) R.drawable.ic_add_favourite else R.drawable.ic_remove_favourite
            optionFavorite.setText(favoriteText)
            optionFavorite.setCompoundDrawablesWithIntrinsicBounds(favoriteDrawable, 0, 0, 0)
            optionFavorite.setOnClickListener {
                viewModel.markNodeAsFavorite(item.node.handle, !item.node.isFavourite)
                dismiss()
            }

            // Label
            val labelColor = ResourcesCompat.getColor(resources, getNodeLabelColor(item.node.label), null)
            val labelDrawable = getNodeLabelDrawable(item.node.label, resources)
            optionLabelCurrent.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null,
                null,
                labelDrawable,
                null
            )
            optionLabelCurrent.setTextColor(labelColor)
            optionLabelCurrent.text = getNodeLabelText(item.node.label)
            optionLabelCurrent.isVisible = item.node.label != MegaNode.NODE_LBL_UNKNOWN
            optionLabelLayout.setOnClickListener {
                NodeLabelBottomSheetDialogFragment.newInstance(item.node.handle).show(childFragmentManager, TAG)
            }

            // Open with
            optionOpenWith.setOnClickListener {
                ModalBottomSheetUtil.openWith(requireContext(), item.node)
                dismiss()
            }

            // Download
            optionDownload.setOnClickListener {
                (activity as? ImageViewerActivity?)?.saveNode(item.node.handle, false)
                dismiss()
            }

            // Save to Gallery
            optionGallery.setOnClickListener {
                (activity as? ImageViewerActivity?)?.saveNode(item.node.handle, true)
                dismiss()
            }

            // Offline
            switchOffline.isChecked = item.isAvailableOffline
            switchOffline.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setNodeAvailableOffline(
                    requireActivity(),
                    item.node.handle,
                    !isChecked
                )
                dismiss()
            }
            optionOfflineLayout.setOnClickListener {
                viewModel.setNodeAvailableOffline(
                    requireActivity(),
                    item.node.handle,
                    !switchOffline.isChecked
                )
                dismiss()
            }

            // Links
            if (item.node.isExported) {
                optionManageLink.setText(R.string.edit_link_option)
                optionRemoveLink.isVisible = true
            } else {
                optionManageLink.text = resources.getQuantityString(R.plurals.get_links, 1)
                optionRemoveLink.isVisible = false
            }
            optionManageLink.setOnClickListener {
                LinksUtil.showGetLinkActivity(this@ImageBottomSheetDialogFragment, item.node.handle)
                dismiss()
            }
            optionRemoveLink.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(resources.getQuantityString(R.plurals.remove_links_warning_text, 1))
                    .setPositiveButton(R.string.general_remove) { _, _ ->
                        viewModel.removeLink(item.node.handle)
                        dismiss()
                    }
                    .setNegativeButton(R.string.general_cancel, null)
                    .show()
            }

            // Send to contact
            optionSendToContact.setOnClickListener {
                (activity as? ImageViewerActivity?)?.attachNode(item.node.handle)
                dismiss()
            }

            // Share
            optionShare.setOnClickListener {
                viewModel.shareNode(item.node.handle).observe(viewLifecycleOwner) { link ->
                    MegaNodeUtil.startShareIntent(context, Intent(Intent.ACTION_SEND), link)
                    dismiss()
                }
            }

            // Rename
            optionRename.setOnClickListener {
                (activity as? ImageViewerActivity?)?.showRenameDialog(item.node)
                dismiss()
            }

            // Move
            optionMove.setOnClickListener {
                val intent = Intent(context, FileExplorerActivityLollipop::class.java).apply {
                    action = FileExplorerActivityLollipop.ACTION_PICK_MOVE_FOLDER
                    putExtra(INTENT_EXTRA_KEY_MOVE_FROM, longArrayOf(item.node.handle))
                }

                startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER_TO_MOVE)
                dismiss()
            }

            // Copy
            optionCopy.setOnClickListener {
                val intent = Intent(context, FileExplorerActivityLollipop::class.java).apply {
                    action = FileExplorerActivityLollipop.ACTION_PICK_COPY_FOLDER
                    putExtra(INTENT_EXTRA_KEY_COPY_FROM, longArrayOf(item.node.handle))
                }

                startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER_TO_COPY)
                dismiss()
            }

            // Rubbish bin
            optionRubbishBin.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.confirmation_move_to_rubbish)
                    .setPositiveButton(R.string.general_move) { _, _ ->
                        viewModel.moveNodeToRubishBin(item.node.handle)
                        dismiss()
                        activity?.finish()
                    }
                    .setNegativeButton(R.string.general_cancel, null)
                    .show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_SELECT_FOLDER_TO_MOVE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val moveHandle = data?.getLongArrayExtra(INTENT_EXTRA_KEY_MOVE_HANDLES)?.get(0) ?: INVALID_HANDLE
                    val toHandle = data?.getLongExtra(INTENT_EXTRA_KEY_MOVE_TO, INVALID_HANDLE) ?: INVALID_HANDLE
                    if (moveHandle != INVALID_HANDLE && toHandle != INVALID_HANDLE) {
                        viewModel.moveNode(moveHandle, toHandle)
                    }
                }
            }
            REQUEST_CODE_SELECT_FOLDER_TO_COPY -> {
                if (resultCode == Activity.RESULT_OK) {
                    val copyHandle = data?.getLongArrayExtra(INTENT_EXTRA_KEY_COPY_HANDLES)?.get(0) ?: INVALID_HANDLE
                    val toHandle = data?.getLongExtra(INTENT_EXTRA_KEY_COPY_TO, INVALID_HANDLE) ?: INVALID_HANDLE
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
}
