package mega.privacy.android.app.modalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactFileListActivityLollipop;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class ContactFileListBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    protected MegaNode node = null;
    protected NodeController nC;
    protected BottomSheetBehavior mBehavior;
    protected LinearLayout mainLinearLayout;
    protected ImageView nodeThumb;
    protected TextView nodeName;
    protected TextView nodeInfo;
    protected RelativeLayout nodeIconLayout;
    protected ImageView nodeIcon;
    protected LinearLayout optionDownload;
    protected LinearLayout optionInfo;
    protected TextView optionInfoText;
    protected ImageView optionInfoImage;
    protected LinearLayout optionLeave;
    protected LinearLayout optionCopy;
    protected LinearLayout optionMove;
    protected LinearLayout optionRename;
    protected LinearLayout optionRubbish;
    protected Bitmap thumb = null;

    private ContactFileListActivityLollipop contactFileListActivity;
    private ContactInfoActivityLollipop contactInfoActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (context instanceof ContactFileListActivityLollipop) {
            contactFileListActivity = (ContactFileListActivityLollipop) context;
        } else if (context instanceof ContactInfoActivityLollipop) {
            contactInfoActivity = (ContactInfoActivityLollipop) context;
        }

        if (savedInstanceState != null) {
            long handle = savedInstanceState.getLong(HANDLE, INVALID_HANDLE);
            node = megaApi.getNodeByHandle(handle);
        } else if (context instanceof ContactFileListActivityLollipop) {
            node = contactFileListActivity.getSelectedNode();
        } else if (context instanceof ContactInfoActivityLollipop) {
            node = contactInfoActivity.getSelectedNode();
        }

        nC = new NodeController(context);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        if (node == null) {
            logWarning("Node NULL");
            return;
        }

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_contact_file_list, null);

        mainLinearLayout = contentView.findViewById(R.id.contact_file_list_bottom_sheet);

        nodeThumb = contentView.findViewById(R.id.contact_file_list_thumbnail);
        nodeName = contentView.findViewById(R.id.contact_file_list_name_text);
        nodeInfo = contentView.findViewById(R.id.contact_file_list_info_text);
        nodeIconLayout = contentView.findViewById(R.id.contact_file_list_relative_layout_icon);
        nodeIcon = contentView.findViewById(R.id.contact_file_list_icon);
        optionDownload = contentView.findViewById(R.id.option_download_layout);
        optionInfo = contentView.findViewById(R.id.option_properties_layout);
        optionInfoText = contentView.findViewById(R.id.option_properties_text);
        optionInfoImage = contentView.findViewById(R.id.option_properties_image);
        optionLeave = contentView.findViewById(R.id.option_leave_layout);
        optionCopy = contentView.findViewById(R.id.option_copy_layout);
        optionMove = contentView.findViewById(R.id.option_move_layout);
        optionRename = contentView.findViewById(R.id.option_rename_layout);
        optionRubbish = contentView.findViewById(R.id.option_rubbish_bin_layout);

        items_layout = contentView.findViewById(R.id.item_list_bottom_sheet_contact_file);

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

        nodeName.setMaxWidth(scaleWidthPx(200, outMetrics));
        nodeInfo.setMaxWidth(scaleWidthPx(200, outMetrics));

        nodeName.setText(node.getName());

        boolean firstLevel = getFirstLevel();
        long parentHandle = INVALID_HANDLE;
        if (context instanceof ContactFileListActivityLollipop) {
            parentHandle = contactFileListActivity.getParentHandle();
        }

        int accessLevel = megaApi.getAccess(node);

        if (node.isFolder()) {
            nodeThumb.setImageResource(R.drawable.ic_folder_incoming);
            optionInfoText.setText(R.string.general_folder_info);
            nodeInfo.setText(getInfoFolder(node, context, megaApi));

            if (firstLevel || parentHandle == INVALID_HANDLE) {
                optionLeave.setVisibility(View.VISIBLE);

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

                nodeIconLayout.setVisibility(View.VISIBLE);
            } else {
                optionLeave.setVisibility(View.GONE);
                nodeIconLayout.setVisibility(View.GONE);
            }
        } else {
            optionInfoText.setText(R.string.general_file_info);
            long nodeSize = node.getSize();
            nodeInfo.setText(getSizeString(nodeSize));
            nodeIconLayout.setVisibility(View.GONE);

            if (node.hasThumbnail()) {
                RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) nodeThumb.getLayoutParams();
                params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                params1.setMargins(20, 0, 12, 0);
                nodeThumb.setLayoutParams(params1);

                thumb = getThumbnailFromCache(node);
                if (thumb != null) {
                    nodeThumb.setImageBitmap(thumb);
                } else {
                    thumb = getThumbnailFromFolder(node, context);
                    if (thumb != null) {
                        nodeThumb.setImageBitmap(thumb);
                    } else {
                        nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                    }
                }
            } else {
                nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
            }

            optionLeave.setVisibility(View.GONE);
        }

        switch (accessLevel) {
            case MegaShare.ACCESS_FULL:
                optionMove.setVisibility(View.GONE);
                optionRename.setVisibility(View.VISIBLE);

                if (firstLevel || parentHandle == INVALID_HANDLE) {
                    optionRubbish.setVisibility(View.GONE);
                } else {
                    optionRubbish.setVisibility(View.VISIBLE);
                }

                break;

            case MegaShare.ACCESS_READ:
                optionRename.setVisibility(View.GONE);
                optionRubbish.setVisibility(View.GONE);
                optionMove.setVisibility(View.GONE);
                break;

            case MegaShare.ACCESS_READWRITE:
                optionMove.setVisibility(View.GONE);
                optionRename.setVisibility(View.GONE);
                optionRubbish.setVisibility(View.GONE);
                break;
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

        dialog.setContentView(contentView);

        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, true);
    }

    private boolean getFirstLevel() {
        if (context instanceof ContactFileListActivityLollipop) {
            contactFileListActivity.isEmptyParentHandleStack();
        }

        return true;
    }

    @Override
    public void onClick(View v) {
        if (node == null) {
            logWarning("The selected node is NULL");
            return;
        }

        ArrayList<Long> handleList = new ArrayList<>();
        handleList.add(node.getHandle());

        switch (v.getId()) {
            case R.id.option_download_layout:
                if (context instanceof ContactFileListActivityLollipop) {
                    contactFileListActivity.onFileClick(handleList);
                } else if (context instanceof ContactInfoActivityLollipop) {
                    contactInfoActivity.onFileClick(handleList);
                }
                break;

            case R.id.option_properties_layout:
                Intent i = new Intent(context, FileInfoActivityLollipop.class);
                i.putExtra(HANDLE, node.getHandle());
                i.putExtra("from", FROM_INCOMING_SHARES);
                boolean firstLevel = getFirstLevel();
                i.putExtra("firstLevel", firstLevel);
                i.putExtra("name", node.getName());
                context.startActivity(i);
                break;

            case R.id.option_leave_layout:
                if (context instanceof ContactFileListActivityLollipop) {
                    contactFileListActivity.showConfirmationLeaveIncomingShare(node);
                } else if (context instanceof ContactInfoActivityLollipop) {
                    contactInfoActivity.showConfirmationLeaveIncomingShare(node);
                }
                break;

            case R.id.option_rename_layout:
                if (context instanceof ContactFileListActivityLollipop) {
                    contactFileListActivity.showRenameDialog(node, node.getName());
                } else if (context instanceof ContactInfoActivityLollipop) {
                    contactInfoActivity.showRenameDialog(node, node.getName());
                }
                break;

            case R.id.option_move_layout:
                if (context instanceof ContactFileListActivityLollipop) {
                    contactFileListActivity.showMoveLollipop(handleList);
                } else if (context instanceof ContactInfoActivityLollipop) {
                    contactInfoActivity.showMoveLollipop(handleList);
                }
                break;

            case R.id.option_copy_layout:
                if (context instanceof ContactFileListActivityLollipop) {
                    contactFileListActivity.showCopyLollipop(handleList);
                } else if (context instanceof ContactInfoActivityLollipop) {
                    contactInfoActivity.showCopyLollipop(handleList);
                }
                break;

            case R.id.option_rubbish_bin_layout:
                if (context instanceof ContactFileListActivityLollipop) {
                    contactFileListActivity.askConfirmationMoveToRubbish(handleList);
                } else if (context instanceof ContactInfoActivityLollipop) {
                    contactInfoActivity.askConfirmationMoveToRubbish(handleList);
                }
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
