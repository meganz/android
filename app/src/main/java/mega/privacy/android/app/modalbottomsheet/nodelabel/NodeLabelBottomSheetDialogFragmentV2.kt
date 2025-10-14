package mega.privacy.android.app.modalbottomsheet.nodelabel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.annotation.IdRes
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.BottomSheetNodeLabelV2Binding
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.nodelabel.NodeLabelBottomSheetDialogFragmentViewModelV2
import mega.privacy.android.app.modalbottomsheet.nodelabel.NodeLabelUiState
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaApiJava
import timber.log.Timber

/**
 * [NodeLabelBottomSheetDialogFragment] is a modal bottom sheet dialog fragment that allows users
 * to view and change the label of one or more nodes (files or folders).
 *
 * This fragment supports both single and multiple node selection. It displays a list of available
 * labels as radio buttons, and updates the selected label for the chosen node(s) via the ViewModel.
 *
 * The fragment observes LiveData from [NodeLabelBottomSheetDialogFragmentViewModel] to update the UI
 * and handle label changes. It uses Hilt for dependency injection and follows the Clean Architecture
 * principles, separating presentation logic from domain and data layers.
 */
@Deprecated(
    message = "NodeLabelBottomSheetDialogFragmentV2 is deprecated. " +
            "Use ChangeLabelBottomSheetContentM3 instead for new features and ongoing support.",
    replaceWith = ReplaceWith("ChangeLabelBottomSheetContentM3")
)
class NodeLabelBottomSheetDialogFragmentV2 : BaseBottomSheetDialogFragment() {
    private lateinit var binding: BottomSheetNodeLabelV2Binding
    private var node: TypedNode? = null
    private val nodes: MutableList<TypedNode> = mutableListOf()
    private var isMultipleSelection = false
    private val viewModel: NodeLabelBottomSheetDialogFragmentViewModelV2 by viewModels()
    private var isUpdatingProgrammatically = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = BottomSheetNodeLabelV2Binding.inflate(layoutInflater)
        contentView = binding.root
        itemsLayout = binding.radioGroupLabel
        return contentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val arguments = arguments ?: run {
            Timber.e("Arguments is null, cannot get node.")
            dismiss()
            return
        }

        setTextResourcesForLabel()
        setupUiStateObserver()

        isMultipleSelection = arguments.getBoolean(IS_MULTIPLE_SELECTION, false)

        if (isMultipleSelection) {
            val handles = arguments.getLongArray(NODE_HANDLES)
            if (handles != null) {
                // Use ViewModel to load nodes
                viewModel.loadNodes(handles)
            }
        } else {
            val nodeHandle = arguments.getLong(Constants.HANDLE, MegaApiJava.INVALID_HANDLE)
            // Use ViewModel to load node
            viewModel.loadNode(nodeHandle)
        }

        binding.radioGroupLabel.setOnCheckedChangeListener { group: RadioGroup?, checkedId: Int ->
            // Only update when user manually selects, not during programmatic updates
            if (!isUpdatingProgrammatically) {
                updateNodeLabel(checkedId)
                if (isMultipleSelection) {
                    parentFragmentManager.setFragmentResult(
                        LABELS_APPLIED_RESULT,
                        Bundle.EMPTY
                    )
                }
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    /**
     * Sets up UI state observer for ViewModel operations
     */
    private fun setupUiStateObserver() {
        collectFlow(viewModel.uiState) { uiState ->
            handleUiState(uiState)
        }
    }

    /**
     * Handles UI state changes
     */
    private fun handleUiState(uiState: NodeLabelUiState) {
        Timber.d("UI State changed: isLoading=${uiState.isLoading}")

        // Handle loading states - show loading indicator
        if (uiState.isLoading) {
            showLoadingIndicator(true)
        } else {
            // Hide loading indicator
            showLoadingIndicator(false)
        }

        // Handle node data updates (independent of loading/error states)
        if (uiState.hasNodes) {
            if (uiState.isMultipleSelection) {
                nodes.clear()
                nodes.addAll(uiState.nodes)
                showMultipleNodesLabel()
            } else {
                node = uiState.node
                showCurrentNodeLabel()
            }
        }

        // Handle error state - show general error message
        if (uiState.hasError) {
            showGeneralError()
        }

        // Handle dismiss state - only dismiss after successful operations
        if (uiState.shouldDismiss) {
            // Dismiss immediately for better UX
            dismiss()
        }
    }

    /**
     * Shows or hides the loading indicator with better UX
     */
    private fun showLoadingIndicator(isLoading: Boolean) {
        if (isLoading) {
            // Show loading container and hide radio group
            binding.loadingContainer.visibility = View.VISIBLE
            binding.radioGroupLabel.visibility = View.GONE
        } else {
            // Hide loading container and show radio group
            binding.loadingContainer.visibility = View.GONE
            binding.radioGroupLabel.visibility = View.VISIBLE

            // Recalculate bottom sheet height and position when switching to content
            view?.post {
                calculatePeekHeight()
                // Force the bottom sheet to show all content
                val behavior = BottomSheetBehavior.from(contentView.parent as View)
                if (behavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
    }

    /**
     * Shows a general error message using the app's standard error handling
     */
    private fun showGeneralError() {
        try {
            val context = requireContext()
            Util.showSnackbar(
                context,
                SNACKBAR_TYPE,
                getString(R.string.general_error),
                MegaApiJava.INVALID_HANDLE
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to show general error snackbar")
        }
    }

    private fun setTextResourcesForLabel() {
        binding.radioLabelRed.setText(R.string.label_red)
        binding.radioLabelOrange.setText(R.string.label_orange)
        binding.radioLabelYellow.setText(R.string.label_yellow)
        binding.radioLabelGreen.setText(R.string.label_green)
        binding.radioLabelBlue.setText(R.string.label_blue)
        binding.radioLabelPurple.setText(R.string.label_purple)
        binding.radioLabelGrey.setText(R.string.label_grey)
    }

    private fun showCurrentNodeLabel() {
        val currentNode = node ?: return

        @IdRes val radioButtonResId = getRadioButtonResIdForTypedNode(currentNode)

        if (binding.radioGroupLabel.checkedRadioButtonId != radioButtonResId) {
            isUpdatingProgrammatically = true
            binding.radioGroupLabel.check(radioButtonResId)
            binding.radioRemove.visibility = View.VISIBLE
            isUpdatingProgrammatically = false
        }
    }

    private fun showMultipleNodesLabel() {
        // Count how many nodes have labels
        val labeledCount = nodes.count { hasLabel(it) }

        // Show remove option if any nodes have labels
        if (labeledCount > 0) {
            binding.radioRemove.visibility = View.VISIBLE
        }

        // If ALL nodes have the same label, pre-select this option
        val uniformLabel = viewModel.getUniformLabel(nodes)
        if (uniformLabel != null) {
            @IdRes val radioButtonResId = getRadioButtonResIdForNodeLabel(uniformLabel)
            if (binding.radioGroupLabel.checkedRadioButtonId != radioButtonResId) {
                isUpdatingProgrammatically = true
                binding.radioGroupLabel.check(radioButtonResId)
                binding.radioRemove.visibility = View.VISIBLE
                isUpdatingProgrammatically = false
            }
        }
    }

    private fun hasLabel(node: TypedNode): Boolean {
        return viewModel.hasLabel(node)
    }

    private fun updateNodeLabel(checkedId: Int) {
        val nodeLabel = getNodeLabelFromCheckedId(checkedId)
        val isRemoveLabel = (checkedId == R.id.radio_remove)

        if (isMultipleSelection) {
            updateMultipleNodeLabels(nodeLabel, isRemoveLabel)
        } else {
            updateSingleNodeLabel(nodeLabel, isRemoveLabel)
        }
    }

    private fun getNodeLabelFromCheckedId(checkedId: Int): NodeLabel? {
        return when (checkedId) {
            R.id.radio_label_red -> NodeLabel.RED
            R.id.radio_label_orange -> NodeLabel.ORANGE
            R.id.radio_label_yellow -> NodeLabel.YELLOW
            R.id.radio_label_green -> NodeLabel.GREEN
            R.id.radio_label_blue -> NodeLabel.BLUE
            R.id.radio_label_purple -> NodeLabel.PURPLE
            R.id.radio_label_grey -> NodeLabel.GREY
            else -> null
        }
    }

    private fun updateMultipleNodeLabels(
        nodeLabel: NodeLabel?,
        isRemoveLabel: Boolean,
    ) {
        val nodeHandles = nodes.map { it.id.longValue }
        viewModel.updateMultipleNodeLabels(
            nodeHandles,
            if (isRemoveLabel) null else nodeLabel
        )
    }

    private fun updateSingleNodeLabel(
        nodeLabel: NodeLabel?,
        isRemoveLabel: Boolean,
    ) {
        val currentNode = node ?: return
        viewModel.updateNodeLabel(
            currentNode.id.longValue,
            if (isRemoveLabel) null else nodeLabel
        )
    }

    /**
     * Maps a TypedNode to the corresponding RadioButton resource ID.
     *
     * @param typedNode the TypedNode to get label from
     * @return the matching RadioButton resource ID, or -1 if the label is not recognized
     */
    private fun getRadioButtonResIdForTypedNode(typedNode: TypedNode): Int {
        return getRadioButtonResIdForNodeLabel(typedNode.nodeLabel)
    }

    /**
     * Maps a NodeLabel to the corresponding RadioButton resource ID.
     *
     * @param nodeLabel the NodeLabel enum
     * @return the matching RadioButton resource ID, or -1 if the label is not recognized
     */
    private fun getRadioButtonResIdForNodeLabel(nodeLabel: NodeLabel?): Int {
        return when (nodeLabel) {
            NodeLabel.RED -> R.id.radio_label_red
            NodeLabel.ORANGE -> R.id.radio_label_orange
            NodeLabel.YELLOW -> R.id.radio_label_yellow
            NodeLabel.GREEN -> R.id.radio_label_green
            NodeLabel.BLUE -> R.id.radio_label_blue
            NodeLabel.PURPLE -> R.id.radio_label_purple
            NodeLabel.GREY -> R.id.radio_label_grey
            else -> -1             // Unknown label → no matching RadioButton
        }
    }

    companion object {
        private const val NODE_HANDLES = "node_handles"
        private const val IS_MULTIPLE_SELECTION = "is_multiple_selection"
        private const val LABELS_APPLIED_RESULT = "labels_applied"

        /**
         * Creates a new instance of the Fragment for a single node
         *
         * @param nodeHandle The handle of the node
         * @return A new Fragment instance
         */
        fun newInstance(nodeHandle: Long): NodeLabelBottomSheetDialogFragmentV2 {
            val nodeLabelFragment = NodeLabelBottomSheetDialogFragmentV2()
            val arguments = Bundle()
            arguments.putLong(Constants.HANDLE, nodeHandle)
            arguments.putBoolean(IS_MULTIPLE_SELECTION, false)
            nodeLabelFragment.arguments = arguments
            return nodeLabelFragment
        }

        /**
         * Creates a new instance of the Fragment for multiple nodes
         *
         * @param nodeHandles Array of node handles
         * @return A new Fragment instance
         */
        fun newInstance(nodeHandles: LongArray?): NodeLabelBottomSheetDialogFragmentV2 {
            val nodeLabelFragment = NodeLabelBottomSheetDialogFragmentV2()
            val arguments = Bundle()
            arguments.putLongArray(NODE_HANDLES, nodeHandles)
            arguments.putBoolean(IS_MULTIPLE_SELECTION, true)
            nodeLabelFragment.arguments = arguments
            return nodeLabelFragment
        }

        /**
         * Creates a new instance of the Fragment for multiple nodes (List version)
         *
         * @param nodeHandles List of node handles
         * @return A new Fragment instance
         */
        fun newInstance(nodeHandles: List<Long>): NodeLabelBottomSheetDialogFragmentV2 {
            val nodeLabelFragment = NodeLabelBottomSheetDialogFragmentV2()
            val arguments = Bundle().apply {
                putLongArray(NODE_HANDLES, nodeHandles.toLongArray())
                putBoolean(IS_MULTIPLE_SELECTION, true)
            }
            nodeLabelFragment.arguments = arguments
            return nodeLabelFragment
        }
    }
}
