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

import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class FolderLinkBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private MegaNode node = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            long handle = savedInstanceState.getLong(HANDLE, INVALID_HANDLE);
            node = megaApi.getNodeByHandle(handle);
        } else if (context instanceof FolderLinkActivityLollipop) {
            node = ((FolderLinkActivityLollipop) context).getSelectedNode();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_folder_link, null);
        mainLinearLayout = contentView.findViewById(R.id.folder_link_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout);

        ImageView nodeThumb = contentView.findViewById(R.id.folder_link_thumbnail);
        TextView nodeName = contentView.findViewById(R.id.folder_link_name_text);
        TextView nodeInfo = contentView.findViewById(R.id.folder_link_info_text);
        LinearLayout optionDownload = contentView.findViewById(R.id.option_download_layout);
        LinearLayout optionImport = contentView.findViewById(R.id.option_import_layout);

        optionDownload.setOnClickListener(this);
        optionImport.setOnClickListener(this);

        nodeName.setMaxWidth(scaleWidthPx(200, outMetrics));
        nodeInfo.setMaxWidth(scaleWidthPx(200, outMetrics));

        if (dbH != null) {
            if (dbH.getCredentials() != null) {
                optionImport.setVisibility(View.VISIBLE);
            } else {
                optionImport.setVisibility(View.GONE);
            }
        }

        if (isOnline(context)) {
            nodeName.setText(node.getName());

            if (node.isFolder()) {
                nodeInfo.setText(getInfoFolder(node, context, megaApi));
                nodeThumb.setImageResource(R.drawable.ic_folder_list);
            } else {
                long nodeSize = node.getSize();
                nodeInfo.setText(getSizeString(nodeSize));

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
            }
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

        switch (v.getId()) {

            case R.id.option_download_layout:
                ((FolderLinkActivityLollipop) context).downloadNode();
                break;

            case R.id.option_import_layout:
                ((FolderLinkActivityLollipop) context).importNode();
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
