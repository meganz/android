package mega.privacy.android.app.modalbottomsheet;

import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.setNodeThumbnail;
import static mega.privacy.android.app.utils.Constants.HANDLE;
import static mega.privacy.android.app.utils.MegaNodeUtil.getFileInfo;
import static mega.privacy.android.app.utils.Util.scaleWidthPx;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaShare.ACCESS_FULL;
import static nz.mega.sdk.MegaShare.ACCESS_OWNER;
import static nz.mega.sdk.MegaShare.ACCESS_READWRITE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;

import mega.privacy.android.app.R;
import mega.privacy.android.app.main.VersionsFileActivity;
import nz.mega.sdk.MegaNode;
import timber.log.Timber;

public class VersionBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private MegaNode node = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        contentView = View.inflate(getContext(), R.layout.bottom_sheet_versions_file, null);
        itemsLayout = contentView.findViewById(R.id.item_list_bottom_sheet_contact_file);

        if (savedInstanceState != null) {
            long handle = savedInstanceState.getLong(HANDLE, INVALID_HANDLE);
            node = megaApi.getNodeByHandle(handle);
        } else if (requireActivity() instanceof VersionsFileActivity) {
            node = ((VersionsFileActivity) requireActivity()).getSelectedNode();
        }

        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (node == null) {
            Timber.w("Node NULL");
            return;
        }

        ImageView nodeThumb = contentView.findViewById(R.id.versions_file_thumbnail);
        TextView nodeName = contentView.findViewById(R.id.versions_file_name_text);
        TextView nodeInfo = contentView.findViewById(R.id.versions_file_info_text);

        LinearLayout optionDownload = contentView.findViewById(R.id.option_download_layout);
        LinearLayout optionRevert = contentView.findViewById(R.id.option_revert_layout);
        LinearLayout optionDelete = contentView.findViewById(R.id.option_delete_layout);


        optionDownload.setOnClickListener(this);
        optionRevert.setOnClickListener(this);
        optionDelete.setOnClickListener(this);

        View separatorRevert = contentView.findViewById(R.id.separator_revert);
        View separatorDelete = contentView.findViewById(R.id.separator_delete);

        nodeName.setMaxWidth(scaleWidthPx(200, getResources().getDisplayMetrics()));
        nodeInfo.setMaxWidth(scaleWidthPx(200, getResources().getDisplayMetrics()));

        nodeName.setText(node.getName());
        nodeInfo.setText(getFileInfo(node, requireContext()));

        setNodeThumbnail(requireContext(), node, nodeThumb);

        boolean isRevertVisible;

        switch (((VersionsFileActivity) requireActivity()).getAccessLevel()) {
            case ACCESS_READWRITE:
                isRevertVisible = true;
                optionDelete.setVisibility(View.GONE);
                separatorDelete.setVisibility(View.GONE);
                break;

            case ACCESS_FULL:
            case ACCESS_OWNER:
                isRevertVisible = true;
                optionDelete.setVisibility(View.VISIBLE);
                separatorDelete.setVisibility(View.VISIBLE);
                break;

            default:
                isRevertVisible = false;
                optionDelete.setVisibility(View.GONE);
                separatorDelete.setVisibility(View.GONE);

        }

        if (!isRevertVisible || ((VersionsFileActivity) requireActivity()).getSelectedPosition() == 0) {
            optionRevert.setVisibility(View.GONE);
            separatorRevert.setVisibility(View.GONE);
        } else {
            optionRevert.setVisibility(View.VISIBLE);
            separatorRevert.setVisibility(View.VISIBLE);
        }

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        if (node == null) {
            Timber.w("The selected node is NULL");
            return;
        }

        int id = v.getId();
        if (id == R.id.option_download_layout) {
            ((VersionsFileActivity) requireActivity()).downloadNodes(Collections.singletonList(node));
        } else if (id == R.id.option_revert_layout) {
            ((VersionsFileActivity) requireActivity()).checkRevertVersion();
            dismissAllowingStateLoss();
        } else if (id == R.id.option_delete_layout) {
            ((VersionsFileActivity) requireActivity()).showConfirmationRemoveVersion();
        }

        setStateBottomSheetBehaviorHidden();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        long handle = node.getHandle();
        outState.putLong(HANDLE, handle);
    }
}
