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

import mega.privacy.android.app.R;
import mega.privacy.android.app.databinding.BottomSheetNodeLabelBinding;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;
import nz.mega.sdk.MegaNode;
import timber.log.Timber;

public class NodeLabelBottomSheetDialogFragment extends BaseBottomSheetDialogFragment {

    private BottomSheetNodeLabelBinding binding;
    private MegaNode node = null;

    public static NodeLabelBottomSheetDialogFragment newInstance(long nodeHandle) {
        NodeLabelBottomSheetDialogFragment nodeLabelFragment = new NodeLabelBottomSheetDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putLong(HANDLE, nodeHandle);
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

        node = megaApi.getNodeByHandle(arguments.getLong(HANDLE, INVALID_HANDLE));
        showCurrentNodeLabel();

        binding.radioGroupLabel.setOnCheckedChangeListener((group, checkedId) -> updateNodeLabel(checkedId));

        super.onViewCreated(view, savedInstanceState);
    }

    private void showCurrentNodeLabel() {
        @IdRes int radioButtonResId = -1;

        switch (node.getLabel()) {
            case MegaNode.NODE_LBL_RED:
                radioButtonResId = R.id.radio_label_red;
                break;
            case MegaNode.NODE_LBL_ORANGE:
                radioButtonResId = R.id.radio_label_orange;
                break;
            case MegaNode.NODE_LBL_YELLOW:
                radioButtonResId = R.id.radio_label_yellow;
                break;
            case MegaNode.NODE_LBL_GREEN:
                radioButtonResId = R.id.radio_label_green;
                break;
            case MegaNode.NODE_LBL_BLUE:
                radioButtonResId = R.id.radio_label_blue;
                break;
            case MegaNode.NODE_LBL_PURPLE:
                radioButtonResId = R.id.radio_label_purple;
                break;
            case MegaNode.NODE_LBL_GREY:
                radioButtonResId = R.id.radio_label_grey;
                break;
        }

        if (binding.radioGroupLabel.getCheckedRadioButtonId() != radioButtonResId) {
            binding.radioGroupLabel.check(radioButtonResId);
            binding.radioRemove.setVisibility(View.VISIBLE);
        }
    }

    private void updateNodeLabel(int checkedId) {
        if (checkedId == R.id.radio_label_red) {
            megaApi.setNodeLabel(node, MegaNode.NODE_LBL_RED);
        } else if (checkedId == R.id.radio_label_orange) {
            megaApi.setNodeLabel(node, MegaNode.NODE_LBL_ORANGE);
        } else if (checkedId == R.id.radio_label_yellow) {
            megaApi.setNodeLabel(node, MegaNode.NODE_LBL_YELLOW);
        } else if (checkedId == R.id.radio_label_green) {
            megaApi.setNodeLabel(node, MegaNode.NODE_LBL_GREEN);
        } else if (checkedId == R.id.radio_label_blue) {
            megaApi.setNodeLabel(node, MegaNode.NODE_LBL_BLUE);
        } else if (checkedId == R.id.radio_label_purple) {
            megaApi.setNodeLabel(node, MegaNode.NODE_LBL_PURPLE);
        } else if (checkedId == R.id.radio_label_grey) {
            megaApi.setNodeLabel(node, MegaNode.NODE_LBL_GREY);
        } else if (checkedId == R.id.radio_remove) {
            megaApi.resetNodeLabel(node);
        }

        dismiss();
    }
}
