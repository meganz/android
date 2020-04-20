package mega.privacy.android.app.modalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.VersionsFileActivity;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaShare.*;

public class VersionBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private MegaNode node = null;
    private NodeController nC;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            long handle = savedInstanceState.getLong(HANDLE, INVALID_HANDLE);
            node = megaApi.getNodeByHandle(handle);
        } else if (context instanceof VersionsFileActivity) {
            node = ((VersionsFileActivity) context).getSelectedNode();
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

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_versions_file, null);
        mainLinearLayout = contentView.findViewById(R.id.versions_file_bottom_sheet);
        items_layout = contentView.findViewById(R.id.item_list_bottom_sheet_contact_file);

        ImageView nodeThumb = contentView.findViewById(R.id.versions_file_thumbnail);
        TextView nodeName = contentView.findViewById(R.id.versions_file_name_text);
        TextView nodeInfo = contentView.findViewById(R.id.versions_file_info_text);

        LinearLayout optionDownload = contentView.findViewById(R.id.option_download_layout);
        LinearLayout optionRevert = contentView.findViewById(R.id.option_revert_layout);
        LinearLayout optionDelete = contentView.findViewById(R.id.option_delete_layout);


        optionDownload.setOnClickListener(this);
        optionRevert.setOnClickListener(this);
        optionDelete.setOnClickListener(this);

        LinearLayout separatorRevert = contentView.findViewById(R.id.separator_revert);
        LinearLayout separatorDelete = contentView.findViewById(R.id.separator_delete);

        nodeName.setMaxWidth(scaleWidthPx(200, outMetrics));
        nodeInfo.setMaxWidth(scaleWidthPx(200, outMetrics));

        nodeName.setText(node.getName());

        long nodeSize = node.getSize();
        String fileInfo = getSizeString(nodeSize) + " . " + getNodeDate(node);
        nodeInfo.setText(fileInfo);

        if (node.hasThumbnail()) {
            RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) nodeThumb.getLayoutParams();
            params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
            params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
            params1.setMargins(20, 0, 12, 0);
            nodeThumb.setLayoutParams(params1);

            Bitmap thumb = getThumbnailFromCache(node);
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

        boolean isRevertVisible;

        switch (((VersionsFileActivity) context).getAccessLevel()) {
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

        if (!isRevertVisible || ((VersionsFileActivity) context).getSelectedPosition() == 0) {
            optionRevert.setVisibility(View.GONE);
            separatorRevert.setVisibility(View.GONE);
        } else {
            optionRevert.setVisibility(View.VISIBLE);
            separatorRevert.setVisibility(View.VISIBLE);
        }

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, false);
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
                nC.prepareForDownload(handleList, false);
                break;

            case R.id.option_revert_layout:
                ((VersionsFileActivity) context).checkRevertVersion();
                dismissAllowingStateLoss();
                break;

            case R.id.option_delete_layout:
                ((VersionsFileActivity) context).showConfirmationRemoveVersion();
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }

    private String getNodeDate(MegaNode node) {
        Calendar calendar = calculateDateFromTimestamp(node.getModificationTime());
        return new SimpleDateFormat("d MMM yyyy HH:mm", Locale.getDefault()).format(calendar.getTime());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        long handle = node.getHandle();
        outState.putLong(HANDLE, handle);
    }
}
