package mega.privacy.android.app.modalbottomsheet.nodelabel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.annotation.IdRes
import androidx.lifecycle.ViewModelProvider
import mega.privacy.android.app.databinding.BottomSheetNodeLabelBinding
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.shared.resources.R
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
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
class NodeLabelBottomSheetDialogFragmentV2 : BaseBottomSheetDialogFragment() {
    private lateinit var binding: BottomSheetNodeLabelBinding
    private var node: MegaNode? = null
    private val nodes: MutableList<MegaNode> = mutableListOf()
    private var isMultipleSelection = false
    private var viewModel: NodeLabelBottomSheetDialogFragmentViewModel? = null
    private var isUpdatingProgrammatically = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = BottomSheetNodeLabelBinding.inflate(layoutInflater)
        contentView = binding.root.rootView
        itemsLayout = binding.radioGroupLabel
        return contentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val arguments = arguments ?: run {
            Timber.e("Arguments is null, cannot get node.")
            dismiss()
            return
        }

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[NodeLabelBottomSheetDialogFragmentViewModel::class.java]

        setTextResourcesForLabel()
        setupLiveDataObservers()

        isMultipleSelection = arguments.getBoolean(IS_MULTIPLE_SELECTION, false)

        viewModel?.let { viewModel ->
            if (isMultipleSelection) {
                val handles = arguments.getLongArray(NODE_HANDLES)
                if (handles != null) {
                    // Use ViewModel to load nodes with LiveData
                    viewModel.loadNodes(handles, megaApi)
                }
            } else {
                val nodeHandle = arguments.getLong(Constants.HANDLE, MegaApiJava.INVALID_HANDLE)
                // Use ViewModel to load node with LiveData
                viewModel.loadNode(nodeHandle, megaApi)
            }
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
     * Sets up LiveData observers for ViewModel operations
     */
    private fun setupLiveDataObservers() {
        viewModel?.let { viewModel ->
            // Observer for single node loading
            viewModel.nodeLoadResult.observe(this) { result ->
                when (result) {
                    is NodeLoadResult.Success -> {
                        node = result.node
                        showCurrentNodeLabel()
                    }

                    is NodeLoadResult.Error -> {
                        Timber.e(result.exception, "Failed to load node")
                        showErrorAndDismiss("Failed to load node")
                    }

                    is NodeLoadResult.Loading -> {
                        // Handle loading state if needed
                    }
                }
            }

            // Observer for multiple nodes loading
            viewModel.nodesLoadResult.observe(this) { result ->
                when (result) {
                    is NodesLoadResult.Success -> {
                        nodes.clear()
                        nodes.addAll(result.nodes)
                        showMultipleNodesLabel()
                    }

                    is NodesLoadResult.Error -> {
                        Timber.e(result.exception, "Failed to load nodes")
                        showErrorAndDismiss("Failed to load nodes")
                    }

                    is NodesLoadResult.Loading -> {
                        // Handle loading state if needed
                    }
                }
            }

            // Observer for single node label update
            viewModel.labelUpdateResult.observe(this) { result ->
                when (result) {
                    is LabelUpdateResult.Success -> {
                        dismiss()
                    }

                    is LabelUpdateResult.Error -> {
                        Timber.e(result.exception, "Failed to update node label")
                        showErrorAndDismiss("Failed to update node label")
                    }

                    is LabelUpdateResult.Loading -> {
                        // Handle loading state if needed
                    }
                }
            }

            // Observer for multiple nodes label update
            viewModel.multipleLabelsUpdateResult.observe(this) { result ->
                when (result) {
                    is LabelUpdateResult.Success -> {
                        dismiss()
                    }

                    is LabelUpdateResult.Error -> {
                        Timber.e(result.exception, "Failed to update multiple node labels")
                        showErrorAndDismiss("Failed to update multiple node labels")
                    }

                    is LabelUpdateResult.Loading -> {
                        // Handle loading state if needed
                    }
                }
            }
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

        @IdRes val radioButtonResId = getRadioButtonResIdForLabel(currentNode.label)

        if (binding.radioGroupLabel.checkedRadioButtonId != radioButtonResId) {
            isUpdatingProgrammatically = true
            binding.radioGroupLabel.check(radioButtonResId)
            binding.radioRemove.visibility = View.VISIBLE
            isUpdatingProgrammatically = false
        }
    }

    private fun showMultipleNodesLabel() {
        val viewModel = viewModel ?: return

        // Count how many nodes have labels
        val labeledCount = nodes.count { hasLabel(it) }

        // Show remove option if any nodes have labels
        if (labeledCount > 0) {
            binding.radioRemove.visibility = View.VISIBLE
        }

        // If ALL nodes have the same label, pre-select this option
        val uniformLabel = viewModel.getUniformLabel(nodes)
        if (uniformLabel != null) {
            val labelInt = viewModel.getIntFromNodeLabel(uniformLabel)
            @IdRes val radioButtonResId = getRadioButtonResIdForLabel(labelInt)
            if (binding.radioGroupLabel.checkedRadioButtonId != radioButtonResId) {
                isUpdatingProgrammatically = true
                binding.radioGroupLabel.check(radioButtonResId)
                binding.radioRemove.visibility = View.VISIBLE
                isUpdatingProgrammatically = false
            }
        }
    }

    fun hasLabel(node: MegaNode): Boolean {
        return viewModel?.hasLabel(node) ?: false
    }

    private fun updateNodeLabel(checkedId: Int) {
        val nodeLabel = getNodeLabelFromCheckedId(checkedId)
        val isRemoveLabel = (checkedId == mega.privacy.android.app.R.id.radio_remove)

        viewModel?.let { viewModel ->
            if (isMultipleSelection) {
                updateMultipleNodeLabels(viewModel, nodeLabel, isRemoveLabel)
            } else {
                updateSingleNodeLabel(viewModel, nodeLabel, isRemoveLabel)
            }
        }
    }

    private fun getNodeLabelFromCheckedId(checkedId: Int): NodeLabel? {
        return when (checkedId) {
            mega.privacy.android.app.R.id.radio_label_red -> NodeLabel.RED
            mega.privacy.android.app.R.id.radio_label_orange -> NodeLabel.ORANGE
            mega.privacy.android.app.R.id.radio_label_yellow -> NodeLabel.YELLOW
            mega.privacy.android.app.R.id.radio_label_green -> NodeLabel.GREEN
            mega.privacy.android.app.R.id.radio_label_blue -> NodeLabel.BLUE
            mega.privacy.android.app.R.id.radio_label_purple -> NodeLabel.PURPLE
            mega.privacy.android.app.R.id.radio_label_grey -> NodeLabel.GREY
            else -> null
        }
    }

    private fun updateMultipleNodeLabels(
        viewModel: NodeLabelBottomSheetDialogFragmentViewModel,
        nodeLabel: NodeLabel?,
        isRemoveLabel: Boolean,
    ) {
        val nodeHandles = nodes.map { it.handle }
        viewModel.updateMultipleNodeLabels(
            nodeHandles,
            if (isRemoveLabel) null else nodeLabel
        )
    }

    private fun updateSingleNodeLabel(
        viewModel: NodeLabelBottomSheetDialogFragmentViewModel,
        nodeLabel: NodeLabel?,
        isRemoveLabel: Boolean,
    ) {
        val currentNode = node ?: return
        viewModel.updateNodeLabel(
            currentNode.handle,
            if (isRemoveLabel) null else nodeLabel
        )
    }

    /**
     * Shows an error message and dismisses the dialog
     *
     * @param message The error message to log
     */
    private fun showErrorAndDismiss(message: String) {
        Timber.e(message)
        // TODO: Show error message to user (e.g., using Snackbar or Toast)
        dismiss()
    }

    /**
     * Maps a MegaNode label value to the corresponding RadioButton resource ID.
     *
     * @param label the label value from MegaNode (e.g., MegaNode.NODE_LBL_RED)
     * @return the matching RadioButton resource ID, or -1 if the label is not recognized
     */
    private fun getRadioButtonResIdForLabel(label: Int): Int {
        return when (label) {
            MegaNode.NODE_LBL_RED -> mega.privacy.android.app.R.id.radio_label_red
            MegaNode.NODE_LBL_ORANGE -> mega.privacy.android.app.R.id.radio_label_orange
            MegaNode.NODE_LBL_YELLOW -> mega.privacy.android.app.R.id.radio_label_yellow
            MegaNode.NODE_LBL_GREEN -> mega.privacy.android.app.R.id.radio_label_green
            MegaNode.NODE_LBL_BLUE -> mega.privacy.android.app.R.id.radio_label_blue
            MegaNode.NODE_LBL_PURPLE -> mega.privacy.android.app.R.id.radio_label_purple
            MegaNode.NODE_LBL_GREY -> mega.privacy.android.app.R.id.radio_label_grey
            else ->                 // Unknown label → no matching RadioButton
                -1
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
