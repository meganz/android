package mega.privacy.android.app.modalbottomsheet.nodelabel;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.databinding.BottomSheetNodeLabelBinding;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

public class NodeLabelBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private BottomSheetNodeLabelBinding binding;
    private MegaApiAndroid megaApi; // TODO Inject MegaApiAndroid when available
    private MegaNode node = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetNodeLabelBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        megaApi = MegaApplication.getInstance().getMegaApi();
        node = ((ManagerActivityLollipop) getActivity()).getSelectedNode(); // TODO Get MegaNode by argument

        showCurrentNodeLabel();

        binding.radioGroupLabel.setOnCheckedChangeListener((group, checkedId) -> updateNodeLabel(checkedId));
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
                megaApi.setNodeLabel(node, MegaNode.NODE_LBL_RED);
                break;
            case R.id.radio_label_orange:
                megaApi.setNodeLabel(node, MegaNode.NODE_LBL_ORANGE);
                break;
            case R.id.radio_label_yellow:
                megaApi.setNodeLabel(node, MegaNode.NODE_LBL_YELLOW);
                break;
            case R.id.radio_label_green:
                megaApi.setNodeLabel(node, MegaNode.NODE_LBL_GREEN);
                break;
            case R.id.radio_label_blue:
                megaApi.setNodeLabel(node, MegaNode.NODE_LBL_BLUE);
                break;
            case R.id.radio_label_purple:
                megaApi.setNodeLabel(node, MegaNode.NODE_LBL_PURPLE);
                break;
            case R.id.radio_label_grey:
                megaApi.setNodeLabel(node, MegaNode.NODE_LBL_GREY);
                break;
            case R.id.radio_remove:
                megaApi.resetNodeLabel(node);
                break;
        }

        dismiss();
    }
}
