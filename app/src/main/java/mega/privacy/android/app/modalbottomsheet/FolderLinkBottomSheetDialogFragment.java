package mega.privacy.android.app.modalbottomsheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FolderLinkActivity;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.setNodeThumbnail;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FolderLinkBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private MegaNode node = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        contentView = View.inflate(getContext(), R.layout.bottom_sheet_folder_link, null);
        itemsLayout = contentView.findViewById(R.id.items_layout);

        if (savedInstanceState != null) {
            long handle = savedInstanceState.getLong(HANDLE, INVALID_HANDLE);
            node = megaApi.getNodeByHandle(handle);
        } else if (requireActivity() instanceof FolderLinkActivity) {
            node = ((FolderLinkActivity) requireActivity()).getSelectedNode();
        }

        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ImageView nodeThumb = contentView.findViewById(R.id.folder_link_thumbnail);
        TextView nodeName = contentView.findViewById(R.id.folder_link_name_text);
        TextView nodeInfo = contentView.findViewById(R.id.folder_link_info_text);
        LinearLayout optionDownload = contentView.findViewById(R.id.option_download_layout);
        LinearLayout optionImport = contentView.findViewById(R.id.option_import_layout);

        optionDownload.setOnClickListener(this);
        optionImport.setOnClickListener(this);

        nodeName.setMaxWidth(scaleWidthPx(200, getResources().getDisplayMetrics()));
        nodeInfo.setMaxWidth(scaleWidthPx(200, getResources().getDisplayMetrics()));

        if (dbH != null) {
            if (dbH.getCredentials() != null) {
                optionImport.setVisibility(View.VISIBLE);
            } else {
                optionImport.setVisibility(View.GONE);
            }
        }

        if (isOnline(requireContext())) {
            nodeName.setText(node.getName());

            if (node.isFolder()) {
                nodeInfo.setText(getMegaNodeFolderInfo(node));
                nodeThumb.setImageResource(R.drawable.ic_folder_list);
            } else {
                long nodeSize = node.getSize();
                nodeInfo.setText(getSizeString(nodeSize));
                setNodeThumbnail(requireContext(), node, nodeThumb);
            }
        }

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        if (node == null) {
            logWarning("The selected node is NULL");
            return;
        }

        switch (v.getId()) {

            case R.id.option_download_layout:
                ((FolderLinkActivity) requireActivity()).downloadNode();
                break;

            case R.id.option_import_layout:
                ((FolderLinkActivity) requireActivity()).importNode();
                break;

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
