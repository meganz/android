package mega.privacy.android.app.modalbottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.BottomSheetVersionsFileBinding
import mega.privacy.android.app.presentation.versions.dialog.VersionsBottomSheetDialogViewModel
import mega.privacy.android.app.presentation.versions.dialog.model.VersionsBottomSheetDialogState
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.Util

/**
 * [BaseBottomSheetDialogFragment] that displays the list of options when selecting the Menu icon
 * of a Version
 */
@AndroidEntryPoint
class VersionsBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    private var _binding: BottomSheetVersionsFileBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<VersionsBottomSheetDialogViewModel>()

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
        viewLifecycleOwner.collectFlow(viewModel.state) { uiState ->
            binding.setupContent(uiState)
        }
    }

    /**
     * Sets up the View content
     *
     * @param uiState The UI State
     */
    private fun BottomSheetVersionsFileBinding.setupContent(uiState: VersionsBottomSheetDialogState) {
        uiState.node?.let { node ->
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
                optionDeleteLayout.isVisible = uiState.canDeleteVersion
                separatorDelete.isVisible = uiState.canDeleteVersion
                optionRevertLayout.isVisible = uiState.canRevertVersion
                separatorRevert.isVisible = uiState.canRevertVersion
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

        /**
         * Parameter Key to receive the Node Handle
         */
        const val PARAM_NODE_HANDLE = "PARAM_NODE_HANDLE"

        /**
         * Parameter Key to receive the Version's selected position
         */
        const val PARAM_SELECTED_POSITION = "PARAM_SELECTED_POSITION"

        /**
         * Parameter Key that specifies the current number of Versions in the file
         */
        const val PARAM_VERSIONS_COUNT = "PARAM_VERSIONS_COUNT"

        /**
         * Instantiates [VersionsBottomSheetDialogFragment] with properties
         *
         * @param nodeHandle A potentially nullable Node Handle
         * @param selectedPosition The selected position
         * @param versionsCount The current number of versions in the file
         */
        fun newInstance(
            nodeHandle: Long?,
            selectedPosition: Int,
            versionsCount: Int,
        ): VersionsBottomSheetDialogFragment {
            val fragment = VersionsBottomSheetDialogFragment()
            fragment.arguments = bundleOf(
                PARAM_NODE_HANDLE to nodeHandle,
                PARAM_SELECTED_POSITION to selectedPosition,
                PARAM_VERSIONS_COUNT to versionsCount,
            )
            return fragment
        }
    }
}