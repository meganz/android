package mega.privacy.android.app.modalbottomsheet.nodelabel;

import static mega.privacy.android.app.utils.Constants.HANDLE;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.ViewModelProvider;
import kotlin.Unit;
import mega.privacy.android.analytics.Analytics;
import mega.privacy.android.app.R;
import mega.privacy.android.app.databinding.BottomSheetNodeLabelBinding;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;
import mega.privacy.android.domain.entity.NodeLabel;
import mega.privacy.android.domain.entity.node.NodeId;
import mega.privacy.mobile.analytics.event.LabelAddedMenuItemEvent;
import mega.privacy.mobile.analytics.event.LabelRemovedMenuItemEvent;
import nz.mega.sdk.MegaNode;
import timber.log.Timber;


public class NodeLabelBottomSheetDialogFragment extends BaseBottomSheetDialogFragment {

    private static final String NODE_HANDLES = "node_handles";
    private static final String IS_MULTIPLE_SELECTION = "is_multiple_selection";

    private BottomSheetNodeLabelBinding binding;
    private MegaNode node = null;
    private List<MegaNode> nodes = new ArrayList<>();
    private boolean isMultipleSelection = false;
    private NodeLabelBottomSheetDialogFragmentViewModel viewModel;

    /**
     * Creates a new instance of the Fragment for a single node
     *
     * @param nodeHandle The handle of the node
     * @return A new Fragment instance
     */
    public static NodeLabelBottomSheetDialogFragment newInstance(long nodeHandle) {
        NodeLabelBottomSheetDialogFragment nodeLabelFragment = new NodeLabelBottomSheetDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putLong(HANDLE, nodeHandle);
        arguments.putBoolean(IS_MULTIPLE_SELECTION, false);
        nodeLabelFragment.setArguments(arguments);
        return nodeLabelFragment;
    }

    /**
     * Creates a new instance of the Fragment for multiple nodes
     *
     * @param nodeHandles Array of node handles
     * @return A new Fragment instance
     */
    public static NodeLabelBottomSheetDialogFragment newInstance(long[] nodeHandles) {
        NodeLabelBottomSheetDialogFragment nodeLabelFragment = new NodeLabelBottomSheetDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putLongArray(NODE_HANDLES, nodeHandles);
        arguments.putBoolean(IS_MULTIPLE_SELECTION, true);
        nodeLabelFragment.setArguments(arguments);
        return nodeLabelFragment;
    }

    /**
     * Creates a new instance of the Fragment for multiple nodes (List version)
     *
     * @param nodeHandles List of node handles
     * @return A new Fragment instance
     */
    public static NodeLabelBottomSheetDialogFragment newInstance(List<Long> nodeHandles) {
        NodeLabelBottomSheetDialogFragment nodeLabelFragment = new NodeLabelBottomSheetDialogFragment();
        Bundle arguments = new Bundle();
        long[] handles = new long[nodeHandles.size()];
        for (int i = 0; i < nodeHandles.size(); i++) {
            handles[i] = nodeHandles.get(i);
        }
        arguments.putLongArray(NODE_HANDLES, handles);
        arguments.putBoolean(IS_MULTIPLE_SELECTION, true);
        nodeLabelFragment.setArguments(arguments);
        return nodeLabelFragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetNodeLabelBinding.inflate(getLayoutInflater());
        contentView = binding.getRoot().getRootView();
        itemsLayout = binding.radioGroupLabel;
        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (arguments == null) {
            Timber.e("Arguments is null, cannot get node.");
            return;
        }

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(NodeLabelBottomSheetDialogFragmentViewModel.class);

        setTextResourcesForLabel();

        isMultipleSelection = arguments.getBoolean(IS_MULTIPLE_SELECTION, false);
        
        if (isMultipleSelection) {
            long[] handles = arguments.getLongArray(NODE_HANDLES);
            if (handles != null) {
                // Use ViewModel to load nodes with callback (moves API call to background thread)
                viewModel.loadMegaNodes(handles, megaApi, (megaNodes) -> {
                    nodes.addAll(megaNodes);
                    // Ensure UI updates run on main thread
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(this::showMultipleNodesLabel);
                    }
                    return kotlin.Unit.INSTANCE;
                });
            }
        } else {
            long nodeHandle = arguments.getLong(HANDLE, INVALID_HANDLE);
            
            // Use ViewModel to load node with callback (moves API call to background thread)
            viewModel.loadMegaNode(nodeHandle, megaApi, (megaNode) -> {
                if (megaNode != null) {
                    node = megaNode;
                    // Ensure UI updates run on main thread
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(this::showCurrentNodeLabel);
                    }
                }
                return kotlin.Unit.INSTANCE;
            });
        }

        binding.radioGroupLabel.setOnCheckedChangeListener((group, checkedId) ->
        {
            updateNodeLabel(checkedId);
            if (isMultipleSelection) {
                getParentFragmentManager().setFragmentResult("labels_applied", Bundle.EMPTY);
            }
        });

        super.onViewCreated(view, savedInstanceState);
    }


    private void setTextResourcesForLabel() {
        binding.radioLabelRed.setText(mega.privacy.android.shared.resources.R.string.label_red);
        binding.radioLabelOrange.setText(mega.privacy.android.shared.resources.R.string.label_orange);
        binding.radioLabelYellow.setText(mega.privacy.android.shared.resources.R.string.label_yellow);
        binding.radioLabelGreen.setText(mega.privacy.android.shared.resources.R.string.label_green);
        binding.radioLabelBlue.setText(mega.privacy.android.shared.resources.R.string.label_blue);
        binding.radioLabelPurple.setText(mega.privacy.android.shared.resources.R.string.label_purple);
        binding.radioLabelGrey.setText(mega.privacy.android.shared.resources.R.string.label_grey);
    }

    private void showCurrentNodeLabel() {
        @IdRes int radioButtonResId = getRadioButtonResIdForLabel(node.getLabel());

        if (binding.radioGroupLabel.getCheckedRadioButtonId() != radioButtonResId) {
            binding.radioGroupLabel.check(radioButtonResId);
            binding.radioRemove.setVisibility(View.VISIBLE);
        }
    }

    private void showMultipleNodesLabel() {
        // Count how many nodes have labels
        int labeledCount = 0;
        for (MegaNode node : nodes) {
            if (hasLabel(node)) {
                labeledCount++;
            }
        }
        
        // Show remove option if any nodes have labels
        if (labeledCount > 0) {
            binding.radioRemove.setVisibility(View.VISIBLE);
        }
        
        // If ALL nodes have the same label, pre-select this option
        NodeLabel uniformLabel = viewModel.getUniformLabelFromMegaNodes(nodes);
        if (uniformLabel != null) {
            int labelInt = viewModel.getIntFromNodeLabel(uniformLabel);
            @IdRes int radioButtonResId = getRadioButtonResIdForLabel(labelInt);
            if (binding.radioGroupLabel.getCheckedRadioButtonId() != radioButtonResId) {
                binding.radioGroupLabel.check(radioButtonResId);
                binding.radioRemove.setVisibility(View.VISIBLE);
            }
        }
    }

    boolean hasLabel(MegaNode node) {
        return viewModel.hasLabelMegaNode(node);
    }

    private void updateNodeLabel(int checkedId) {
        boolean isRemoveLabel = (checkedId == R.id.radio_remove);
        NodeLabel nodeLabel = null;
        
        if (checkedId == R.id.radio_label_red) {
            nodeLabel = NodeLabel.RED;
        } else if (checkedId == R.id.radio_label_orange) {
            nodeLabel = NodeLabel.ORANGE;
        } else if (checkedId == R.id.radio_label_yellow) {
            nodeLabel = NodeLabel.YELLOW;
        } else if (checkedId == R.id.radio_label_green) {
            nodeLabel = NodeLabel.GREEN;
        } else if (checkedId == R.id.radio_label_blue) {
            nodeLabel = NodeLabel.BLUE;
        } else if (checkedId == R.id.radio_label_purple) {
            nodeLabel = NodeLabel.PURPLE;
        } else if (checkedId == R.id.radio_label_grey) {
            nodeLabel = NodeLabel.GREY;
        }

        if (isMultipleSelection) {
            // Apply label to all selected nodes using ViewModel
            List<Long> nodeHandles = new ArrayList<>();
            for (MegaNode node : nodes) {
                nodeHandles.add(node.getHandle());
            }
            viewModel.updateMultipleNodeLabelsByHandles(nodeHandles, isRemoveLabel ? null : nodeLabel);
        } else {
            // Apply label to single node using ViewModel
            viewModel.updateNodeLabelByHandle(node.getHandle(), isRemoveLabel ? null : nodeLabel);
        }

        // Dismiss the dialog after initiating the operation
        // The ViewModel handles the actual API calls, error handling, and analytics
        dismiss();
    }


    /**
     * Maps a MegaNode label value to the corresponding RadioButton resource ID.
     *
     * @param label the label value from MegaNode (e.g., MegaNode.NODE_LBL_RED)
     * @return the matching RadioButton resource ID, or -1 if the label is not recognized
     */
    private int getRadioButtonResIdForLabel(int label) {
        switch (label) {
            case MegaNode.NODE_LBL_RED:
                return R.id.radio_label_red;
            case MegaNode.NODE_LBL_ORANGE:
                return R.id.radio_label_orange;
            case MegaNode.NODE_LBL_YELLOW:
                return R.id.radio_label_yellow;
            case MegaNode.NODE_LBL_GREEN:
                return R.id.radio_label_green;
            case MegaNode.NODE_LBL_BLUE:
                return R.id.radio_label_blue;
            case MegaNode.NODE_LBL_PURPLE:
                return R.id.radio_label_purple;
            case MegaNode.NODE_LBL_GREY:
                return R.id.radio_label_grey;
            default:
                // Unknown label → no matching RadioButton
                return -1;
        }
    }
}
