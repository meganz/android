package mega.privacy.android.app.modalbottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.BottomSheetVersionsFileBinding
import mega.privacy.android.app.presentation.versions.dialog.VersionsBottomSheetDialogViewModel
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare

/**
 * [BaseBottomSheetDialogFragment] that displays the list of options when selecting the Menu icon
 * of a Version
 */
@AndroidEntryPoint
class VersionsBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    private var _binding: BottomSheetVersionsFileBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<VersionsBottomSheetDialogViewModel>()

    private val accessLevel by lazy { arguments?.getInt(ACCESS_LEVEL) }
    private val nodeHandle by lazy { arguments?.getLong(NODE_HANDLE) }
    private val selectedPosition by lazy { arguments?.getInt(SELECTED_POSITION) }

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetVersionsFileBinding.inflate(inflater, container, false)

        contentView = binding.root
        itemsLayout = binding.itemListBottomSheetContactFile
        binding.setupViews()

        viewModel.init(nodeHandle)
        return binding.root
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    /**
     * When the Fragment is destroyed, make sure to set the View Binding as null
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Sets up the Dialog Views
     */
    private fun BottomSheetVersionsFileBinding.setupViews() {
        with(this) {
            optionDownloadLayout.setOnClickListener { handleDownloadClick() }
            optionRevertLayout.setOnClickListener { handleRevertClick() }
            optionDeleteLayout.setOnClickListener { handleDeleteClick() }

            versionsFileNameText.maxWidth = Util.scaleWidthPx(200, resources.displayMetrics)
            versionsFileInfoText.maxWidth = Util.scaleWidthPx(200, resources.displayMetrics)
        }
    }

    /**
     * Handles behavior when the Download option is selected
     */
    private fun handleDownloadClick() {
        setFragmentResult(
            requestKey = REQUEST_KEY_VERSIONS_DIALOG,
            result = bundleOf(BUNDLE_KEY_VERSIONS_DIALOG to ACTION_DOWNLOAD_VERSION),
        )
        setStateBottomSheetBehaviorHidden()
    }

    /**
     * Handles behavior when the Revert option is selected
     */
    private fun handleRevertClick() {
        setFragmentResult(
            requestKey = REQUEST_KEY_VERSIONS_DIALOG,
            result = bundleOf(BUNDLE_KEY_VERSIONS_DIALOG to ACTION_REVERT_VERSION),
        )
        dismissAllowingStateLoss()
        setStateBottomSheetBehaviorHidden()
    }

    /**
     * Handles behavior when the Delete option is selected
     */
    private fun handleDeleteClick() {
        setFragmentResult(
            requestKey = REQUEST_KEY_VERSIONS_DIALOG,
            result = bundleOf(BUNDLE_KEY_VERSIONS_DIALOG to ACTION_DELETE_VERSION),
        )
        setStateBottomSheetBehaviorHidden()
    }

    /**
     * Establishes the ViewModel Observers
     */
    private fun setupObservers() {
        viewLifecycleOwner.collectFlow(viewModel.state) { state ->
            state.node?.let { binding.setupContent(it) }
        }
    }

    /**
     * Sets up the View content
     *
     * @param node The Node to set the content
     */
    private fun BottomSheetVersionsFileBinding.setupContent(node: MegaNode) {
        with(this) {
            versionsFileNameText.text = node.name
            versionsFileInfoText.text = MegaNodeUtil.getFileInfo(
                node = node,
                context = requireContext(),
            )
            ModalBottomSheetUtil.setNodeThumbnail(
                context = requireContext(),
                node = node,
                nodeThumb = versionsFileThumbnail,
            )
            val isRevertVisible = when (accessLevel) {
                MegaShare.ACCESS_READWRITE -> {
                    optionDeleteLayout.visibility = View.GONE
                    separatorDelete.visibility = View.GONE
                    true
                }
                MegaShare.ACCESS_FULL, MegaShare.ACCESS_OWNER -> {
                    optionDeleteLayout.visibility = View.VISIBLE
                    separatorDelete.visibility = View.VISIBLE
                    true
                }
                else -> {
                    optionDeleteLayout.visibility = View.GONE
                    separatorDelete.visibility = View.GONE
                    false
                }
            }
            if (!isRevertVisible || selectedPosition == 0) {
                optionRevertLayout.visibility = View.GONE
                separatorRevert.visibility = View.GONE
            } else {
                optionRevertLayout.visibility = View.VISIBLE
                separatorRevert.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        /**
         * Action to Revert a Version
         */
        const val ACTION_REVERT_VERSION = "ACTION_REVERT_VERSION"

        /**
         * Action to Delete a Version
         */
        const val ACTION_DELETE_VERSION = "ACTION_DELETE_VERSION"

        /**
         * Action to Download a Version
         */
        const val ACTION_DOWNLOAD_VERSION = "ACTION_DOWNLOAD_VERSION"

        /**
         * Bundle Key
         */
        const val BUNDLE_KEY_VERSIONS_DIALOG = "BUNDLE_KEY_VERSIONS_DIALOG"

        /**
         * Request Key
         */
        const val REQUEST_KEY_VERSIONS_DIALOG = "REQUEST_KEY_VERSIONS_DIALOG"

        // Properties
        private const val ACCESS_LEVEL = "ACCESS_LEVEL"
        private const val NODE_HANDLE = "NODE_HANDLE"
        private const val SELECTED_POSITION = "SELECTED_POSITION"

        /**
         * Instantiates [VersionsBottomSheetDialogFragment] with properties
         *
         * @param accessLevel The Node Access Level
         * @param nodeHandle A potentially nullable Node Handle
         * @param selectedPosition The selected position
         */
        fun newInstance(
            accessLevel: Int,
            nodeHandle: Long?,
            selectedPosition: Int,
        ): VersionsBottomSheetDialogFragment {
            val fragment = VersionsBottomSheetDialogFragment()
            fragment.arguments = bundleOf(
                ACCESS_LEVEL to accessLevel,
                NODE_HANDLE to nodeHandle,
                SELECTED_POSITION to selectedPosition,
            )
            return fragment
        }
    }
}