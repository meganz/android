package mega.privacy.android.app.modalbottomsheet.nodelabel;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.databinding.BottomSheetNodeLabelBinding;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.Constants.HANDLE;

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

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(@NonNull Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        binding = BottomSheetNodeLabelBinding.inflate(getLayoutInflater());

        node = getMegaApi().getNodeByHandle(getArguments().getLong(HANDLE));
        showCurrentNodeLabel();

        binding.radioGroupLabel.setOnCheckedChangeListener((group, checkedId) -> updateNodeLabel(checkedId));

        contentView = binding.getRoot().getRootView();
        mainLinearLayout = binding.nodeBottomSheet;
        items_layout = binding.radioGroupLabel;

        dialog.setContentView(contentView);
        setRadioGroupViewBottomSheetBehaviour();
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
        switch (checkedId) {
            case R.id.radio_label_red:
                getMegaApi().setNodeLabel(node, MegaNode.NODE_LBL_RED);
                break;
            case R.id.radio_label_orange:
                getMegaApi().setNodeLabel(node, MegaNode.NODE_LBL_ORANGE);
                break;
            case R.id.radio_label_yellow:
                getMegaApi().setNodeLabel(node, MegaNode.NODE_LBL_YELLOW);
                break;
            case R.id.radio_label_green:
                getMegaApi().setNodeLabel(node, MegaNode.NODE_LBL_GREEN);
                break;
            case R.id.radio_label_blue:
                getMegaApi().setNodeLabel(node, MegaNode.NODE_LBL_BLUE);
                break;
            case R.id.radio_label_purple:
                getMegaApi().setNodeLabel(node, MegaNode.NODE_LBL_PURPLE);
                break;
            case R.id.radio_label_grey:
                getMegaApi().setNodeLabel(node, MegaNode.NODE_LBL_GREY);
                break;
            case R.id.radio_remove:
                getMegaApi().resetNodeLabel(node);
                break;
        }

        dismiss();
    }

    private MegaApiAndroid getMegaApi() {
        return MegaApplication.getInstance().getMegaApi();
    }
}
