package mega.privacy.android.app.imageviewer.dialog

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetImageOptionsBinding
import mega.privacy.android.app.imageviewer.ImageViewerViewModel
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil
import mega.privacy.android.app.modalbottomsheet.nodelabel.NodeLabelBottomSheetDialogFragment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.ExtraUtils.extraNotNull
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelColor
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelDrawable
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelText
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
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

        viewModel.getNode(imageNodeHandle).observe(viewLifecycleOwner, ::showNodeData)
        viewModel.getImage(imageNodeHandle).observe(viewLifecycleOwner) { imageItem ->
            binding.imgThumbnail.setImageURI(imageItem?.thumbnailUri)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showNodeData(node: MegaNode?) {
        requireNotNull(node) { "Node not found" }

        binding.apply {
            txtName.text = node.name

            val nodeSizeText = Util.getSizeString(node.size)
            val nodeDateText = TimeUtils.formatLongDateTime(node.creationTime)
            txtInfo.text = "$nodeSizeText · $nodeDateText"

            optionInfo.setOnClickListener {
                val intent = Intent(context, FileInfoActivityLollipop::class.java).apply {
                    putExtra(Constants.HANDLE, node.handle)
                    putExtra(Constants.NAME, node.name)
                }

                startActivity(intent)
                dismiss()
            }

            val favoriteText = if (node.isFavourite) R.string.file_properties_unfavourite else R.string.file_properties_favourite
            val favoriteDrawable = if (!node.isFavourite) R.drawable.ic_add_favourite else R.drawable.ic_remove_favourite
            optionFavorite.setText(favoriteText)
            optionFavorite.setCompoundDrawablesWithIntrinsicBounds(favoriteDrawable, 0, 0, 0)
            optionFavorite.setOnClickListener {
                viewModel.markNodeAsFavorite(node.handle, !node.isFavourite)
                dismiss()
            }

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
            optionLabelLayout.setOnClickListener {
                NodeLabelBottomSheetDialogFragment.newInstance(node.handle)
                    .show(childFragmentManager, TAG)
            }

            optionOpenWith.setOnClickListener {
                ModalBottomSheetUtil.openWith(requireContext(), node)
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
}
