package mega.privacy.android.app.modalbottomsheet.nodelabel;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.databinding.BottomSheetNodeLabelBinding;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.Constants.HANDLE;

public class NodeLabelBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private static final float HALF_EXPANDED_RATIO_OFFSET = 1.5f;

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
        binding = BottomSheetNodeLabelBinding.inflate(inflater, container, false);
        node = getMegaApi().getNodeByHandle(getArguments().getLong(HANDLE));
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        showCurrentNodeLabel();
        binding.radioGroupLabel.setOnCheckedChangeListener((group, checkedId) -> updateNodeLabel(checkedId));

        getDialog().setOnShowListener(dialog -> {
                    float displayHeight = getDisplayHeight();
                    int itemHeight = getItemHeight();
                    int viewHeight = view.getMeasuredHeight();
                    float expandedRatio = (viewHeight - itemHeight * HALF_EXPANDED_RATIO_OFFSET) / displayHeight;

                    BottomSheetBehavior<FrameLayout> behavior = ((BottomSheetDialog) dialog).getBehavior();
                    behavior.setSkipCollapsed(true);
                    behavior.setHalfExpandedRatio(expandedRatio);

                    if (behavior.getState() != BottomSheetBehavior.STATE_HALF_EXPANDED) {
                        behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                    } else {
                        view.requestLayout();
                    }
                }
        );
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

    private int getItemHeight() {
        return binding.txtTitle.getMeasuredHeight();
    }

    private float getDisplayHeight() {
        Rect rectangle = new Rect();
        Window window = getActivity().getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
        return rectangle.bottom - rectangle.top;
    }

    private MegaApiAndroid getMegaApi() {
        return MegaApplication.getInstance().getMegaApi();
    }
}
