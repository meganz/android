package mega.privacy.android.app.modalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class FolderLinkBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;
    MegaNode node = null;

    private BottomSheetBehavior mBehavior;
    private LinearLayout items_layout;

    LinearLayout mainLinearLayout;
    ImageView nodeThumb;
    TextView nodeName;
    TextView nodeInfo;
    LinearLayout optionDownload;
    LinearLayout optionImport;

    DisplayMetrics outMetrics;
    private int heightDisplay;

    Bitmap thumb = null;

    MegaApiAndroid megaApi;
    DatabaseHandler dbH;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApiFolder();
        }

        if(savedInstanceState!=null) {
            logDebug("Bundle is NOT NULL");
            long handle = savedInstanceState.getLong("handle", -1);
            logDebug("Handle of the node: " + handle);
            node = megaApi.getNodeByHandle(handle);
        }
        else{
            logWarning("Bundle NULL");
            if(context instanceof FolderLinkActivityLollipop){
                node = ((FolderLinkActivityLollipop) context).getSelectedNode();
            }
        }

        dbH = DatabaseHandler.getDbHandler(getActivity());
    }

    @Override
    public void setupDialog(final Dialog dialog, int style) {

        super.setupDialog(dialog, style);

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        heightDisplay = outMetrics.heightPixels;

        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_folder_link, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.folder_link_bottom_sheet);
        items_layout = (LinearLayout) contentView.findViewById(R.id.items_layout);

        nodeThumb = (ImageView) contentView.findViewById(R.id.folder_link_thumbnail);
        nodeName = (TextView) contentView.findViewById(R.id.folder_link_name_text);
        nodeInfo  = (TextView) contentView.findViewById(R.id.folder_link_info_text);
        optionDownload = (LinearLayout) contentView.findViewById(R.id.option_download_layout);
        optionImport = (LinearLayout) contentView.findViewById(R.id.option_import_layout);

        optionDownload.setOnClickListener(this);
        optionImport.setOnClickListener(this);

        nodeName.setMaxWidth(scaleWidthPx(200, outMetrics));
        nodeInfo.setMaxWidth(scaleWidthPx(200, outMetrics));

        if (dbH != null){
            if (dbH.getCredentials() != null){
                optionImport.setVisibility(View.VISIBLE);
            }
            else{
                optionImport.setVisibility(View.GONE);
            }
        }

        if(node!=null){
            if(isOnline(context)){
                nodeName.setText(node.getName());

                if (node.isFolder()) {
                    nodeInfo.setText(getInfoFolder(node, context, megaApi));
                    nodeThumb.setImageResource(R.drawable.ic_folder_list);
                }
                else{
                    long nodeSize = node.getSize();
                    nodeInfo.setText(getSizeString(nodeSize));

                    if (node.hasThumbnail()) {
                        logDebug("Node has thumbnail");
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
                }
            }
        }

        dialog.setContentView(contentView);
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, context, 81));
        mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }


    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.option_download_layout:{
                logDebug("Download option");
                if(node==null){
                    logWarning("The selected node is NULL");
                    return;
                }
                ((FolderLinkActivityLollipop) context).downloadNode();
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_import_layout:{
                logDebug("Import option");
                if(node==null){
                    logWarning("The selected node is NULL");
                    return;
                }
                ((FolderLinkActivityLollipop) context).importNode();
                dismissAllowingStateLoss();
                break;
            }
        }

//        dismiss();
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.context = activity;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        logDebug("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        long handle = node.getHandle();
        logDebug("Handle of the node: " + handle);
        outState.putLong("handle", handle);
    }
}
