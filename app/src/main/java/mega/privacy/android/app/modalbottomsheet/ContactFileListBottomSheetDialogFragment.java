package mega.privacy.android.app.modalbottomsheet;

import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.setNodeThumbnail;
import static mega.privacy.android.app.utils.Constants.CONTACT_FILE_ADAPTER;
import static mega.privacy.android.app.utils.Constants.FROM_INCOMING_SHARES;
import static mega.privacy.android.app.utils.Constants.HANDLE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FIRST_LEVEL;
import static mega.privacy.android.app.utils.Constants.NAME;
import static mega.privacy.android.app.utils.MegaApiUtils.getMegaNodeFolderInfo;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog;
import static mega.privacy.android.app.utils.MegaNodeUtil.manageEditTextFileIntent;
import static mega.privacy.android.app.utils.MegaNodeUtil.showConfirmationLeaveIncomingShare;
import static mega.privacy.android.app.utils.Util.getSizeString;
import static mega.privacy.android.app.utils.Util.scaleWidthPx;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;

import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.interfaces.ActionNodeCallback;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.main.ContactFileListActivity;
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity;
import mega.privacy.android.app.presentation.fileinfo.FileInfoActivity;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import timber.log.Timber;

public class ContactFileListBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private MegaNode node = null;

    private ContactFileListActivity contactFileListActivity;
    private ContactInfoActivity contactInfoActivity;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        contentView = View.inflate(getContext(), R.layout.bottom_sheet_contact_file_list, null);
        itemsLayout = contentView.findViewById(R.id.item_list_bottom_sheet_contact_file);

        if (requireActivity() instanceof ContactFileListActivity) {
            contactFileListActivity = (ContactFileListActivity) requireActivity();
        } else if (requireActivity() instanceof ContactInfoActivity) {
            contactInfoActivity = (ContactInfoActivity) requireActivity();
        }

        if (savedInstanceState != null) {
            long handle = savedInstanceState.getLong(HANDLE, INVALID_HANDLE);
            node = megaApi.getNodeByHandle(handle);
        } else if (requireActivity() instanceof ContactFileListActivity) {
            node = contactFileListActivity.getSelectedNode();
        } else if (requireActivity() instanceof ContactInfoActivity) {
            node = contactInfoActivity.getSelectedNode();
        }

        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (node == null) {
            Timber.w("Node NULL");
            return;
        }

        ImageView nodeThumb = contentView.findViewById(R.id.contact_file_list_thumbnail);
        TextView nodeName = contentView.findViewById(R.id.contact_file_list_name_text);
        TextView nodeInfo = contentView.findViewById(R.id.contact_file_list_info_text);
        RelativeLayout nodeIconLayout = contentView.findViewById(R.id.contact_file_list_relative_layout_icon);
        ImageView nodeIcon = contentView.findViewById(R.id.contact_file_list_icon);
        TextView optionDownload = contentView.findViewById(R.id.download_option);
        TextView optionInfo = contentView.findViewById(R.id.properties_option);
        TextView optionLeave = contentView.findViewById(R.id.leave_option);
        TextView optionCopy = contentView.findViewById(R.id.copy_option);
        TextView optionMove = contentView.findViewById(R.id.move_option);
        TextView optionRename = contentView.findViewById(R.id.rename_option);
        TextView optionRubbish = contentView.findViewById(R.id.rubbish_bin_option);

        optionDownload.setOnClickListener(this);
        optionInfo.setOnClickListener(this);
        optionCopy.setOnClickListener(this);
        optionMove.setOnClickListener(this);
        optionRename.setOnClickListener(this);
        optionLeave.setOnClickListener(this);
        optionRubbish.setOnClickListener(this);

        LinearLayout separatorInfo = contentView.findViewById(R.id.separator_info);
        LinearLayout separatorDownload = contentView.findViewById(R.id.separator_download);
        LinearLayout separatorModify = contentView.findViewById(R.id.separator_modify);

        nodeName.setMaxWidth(scaleWidthPx(200, getResources().getDisplayMetrics()));
        nodeInfo.setMaxWidth(scaleWidthPx(200, getResources().getDisplayMetrics()));

        nodeName.setText(node.getName());

        boolean firstLevel = node.isInShare();
        long parentHandle = INVALID_HANDLE;
        if (requireActivity() instanceof ContactFileListActivity) {
            parentHandle = contactFileListActivity.getParentHandle();
        }

        int accessLevel = megaApi.getAccess(node);

        optionInfo.setText(R.string.general_info);
        if (node.isFolder()) {
            nodeThumb.setImageResource(R.drawable.ic_folder_incoming);
            nodeInfo.setText(getMegaNodeFolderInfo(node, requireContext()));

            if (!node.isTakenDown() && (firstLevel || parentHandle == INVALID_HANDLE)) {
                switch (accessLevel) {
                    case MegaShare.ACCESS_FULL:
                        nodeIcon.setImageResource(R.drawable.ic_shared_fullaccess);
                        break;

                    case MegaShare.ACCESS_READ:
                        nodeIcon.setImageResource(R.drawable.ic_shared_read);
                        break;

                    case MegaShare.ACCESS_READWRITE:
                        nodeIcon.setImageResource(R.drawable.ic_shared_read_write);
                        break;
                }
            } else {
                optionLeave.setVisibility(View.GONE);
                nodeIconLayout.setVisibility(View.GONE);
            }
        } else {
            long nodeSize = node.getSize();
            nodeInfo.setText(getSizeString(nodeSize, requireContext()));
            nodeIconLayout.setVisibility(View.GONE);
            setNodeThumbnail(requireContext(), node, nodeThumb);
            optionLeave.setVisibility(View.GONE);

            if (MimeTypeList.typeForName(node.getName()).isOpenableTextFile(node.getSize())
                    && accessLevel >= MegaShare.ACCESS_READWRITE) {
                LinearLayout optionEdit = contentView.findViewById(R.id.edit_file_option);
                optionEdit.setVisibility(View.VISIBLE);
                optionEdit.setOnClickListener(this);
            }
        }

        switch (accessLevel) {
            case MegaShare.ACCESS_FULL:
                if (firstLevel || parentHandle == INVALID_HANDLE) {
                    optionRubbish.setVisibility(View.GONE);
                }

                break;

            case MegaShare.ACCESS_READ:
            case MegaShare.ACCESS_READWRITE:
                optionMove.setVisibility(View.GONE);
                optionRename.setVisibility(View.GONE);
                optionRubbish.setVisibility(View.GONE);
                break;
        }

        if (node.isTakenDown()) {
            optionDownload.setVisibility(View.GONE);
            optionCopy.setVisibility(View.GONE);
        }

        if (optionInfo.getVisibility() == View.GONE || (optionDownload.getVisibility() == View.GONE && optionCopy.getVisibility() == View.GONE
                && optionMove.getVisibility() == View.GONE && optionLeave.getVisibility() == View.GONE
                && optionRename.getVisibility() == View.GONE && optionRubbish.getVisibility() == View.GONE)) {
            separatorInfo.setVisibility(View.GONE);
        } else {
            separatorInfo.setVisibility(View.VISIBLE);
        }

        if (optionDownload.getVisibility() == View.GONE || (optionCopy.getVisibility() == View.GONE && optionMove.getVisibility() == View.GONE
                && optionRename.getVisibility() == View.GONE && optionLeave.getVisibility() == View.GONE && optionRubbish.getVisibility() == View.GONE)) {
            separatorDownload.setVisibility(View.GONE);
        } else {
            separatorDownload.setVisibility(View.VISIBLE);
        }

        if ((optionCopy.getVisibility() == View.GONE
                && optionMove.getVisibility() == View.GONE && optionRename.getVisibility() == View.GONE) || (optionLeave.getVisibility() == View.GONE && optionRubbish.getVisibility() == View.GONE)) {
            separatorModify.setVisibility(View.GONE);
        } else {
            separatorModify.setVisibility(View.VISIBLE);
        }

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        if (node == null) {
            Timber.w("The selected node is NULL");
            return;
        }

        ArrayList<Long> handleList = new ArrayList<>();
        handleList.add(node.getHandle());

        int id = v.getId();
        if (id == R.id.download_option) {
            if (requireActivity() instanceof ContactFileListActivity) {
                contactFileListActivity.downloadFile(Collections.singletonList(node));
            } else if (requireActivity() instanceof ContactInfoActivity) {
                contactInfoActivity.downloadFile(Collections.singletonList(node));
            }
        } else if (id == R.id.properties_option) {
            Intent i = new Intent(requireContext(), FileInfoActivity.class);
            i.putExtra(HANDLE, node.getHandle());
            i.putExtra("from", FROM_INCOMING_SHARES);
            i.putExtra(INTENT_EXTRA_KEY_FIRST_LEVEL, node.isInShare());
            i.putExtra(NAME, node.getName());
            startActivity(i);
        } else if (id == R.id.leave_option) {
            showConfirmationLeaveIncomingShare(requireActivity(),
                    (SnackbarShower) requireActivity(), node);
        } else if (id == R.id.rename_option) {
            showRenameNodeDialog(requireActivity(), node, (SnackbarShower) getActivity(),
                    (ActionNodeCallback) getActivity());
        } else if (id == R.id.move_option) {
            if (requireActivity() instanceof ContactFileListActivity) {
                contactFileListActivity.showMove(handleList);
            } else if (requireActivity() instanceof ContactInfoActivity) {
                contactInfoActivity.showMove(handleList);
            }
        } else if (id == R.id.copy_option) {
            if (requireActivity() instanceof ContactFileListActivity) {
                contactFileListActivity.showCopy(handleList);
            } else if (requireActivity() instanceof ContactInfoActivity) {
                contactInfoActivity.showCopy(handleList);
            }
        } else if (id == R.id.rubbish_bin_option) {
            if (requireActivity() instanceof ContactFileListActivity) {
                contactFileListActivity.askConfirmationMoveToRubbish(handleList);
            }
        } else if (id == R.id.edit_file_option) {
            manageEditTextFileIntent(requireContext(), node, CONTACT_FILE_ADAPTER);
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
