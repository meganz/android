package mega.privacy.android.app.modalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeInfo;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactFileListActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.VersionsFileActivity;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

public class VersionBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    private Context context;
    private MegaNode node = null;
    private NodeController nC;

    private BottomSheetBehavior mBehavior;

    private LinearLayout mainLinearLayout;
    private ImageView nodeThumb;
    private TextView nodeName;
    private TextView nodeInfo;
    private RelativeLayout nodeIconLayout;
    private ImageView nodeIcon;
    private LinearLayout optionDownload;
    private LinearLayout optionRevert;
    private LinearLayout optionDelete;

    private DisplayMetrics outMetrics;

    private Bitmap thumb = null;

    private MegaApiAndroid megaApi;
    private DatabaseHandler dbH;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        log("onCreate");
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if(savedInstanceState!=null) {
            log("Bundle is NOT NULL");
            long handle = savedInstanceState.getLong("handle", -1);
            log("Handle of the node: "+handle);
            node = megaApi.getNodeByHandle(handle);
        }
        else{
            log("Bundle NULL");
            if(context instanceof VersionsFileActivity){
                node = ((VersionsFileActivity) context).getSelectedNode();
            }
        }

        nC = new NodeController(context);

        dbH = DatabaseHandler.getDbHandler(getActivity());
    }

    @Override
    public void setupDialog(final Dialog dialog, int style) {

        super.setupDialog(dialog, style);
        log("setupDialog");
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_versions_file, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.versions_file_bottom_sheet);

        nodeThumb = (ImageView) contentView.findViewById(R.id.versions_file_thumbnail);
        nodeName = (TextView) contentView.findViewById(R.id.versions_file_name_text);
        nodeInfo  = (TextView) contentView.findViewById(R.id.versions_file_info_text);
        nodeIconLayout = (RelativeLayout) contentView.findViewById(R.id.versions_file_relative_layout_icon);
        nodeIcon = (ImageView) contentView.findViewById(R.id.versions_file_icon);
        optionDownload = (LinearLayout) contentView.findViewById(R.id.option_download_layout);
        optionRevert = (LinearLayout) contentView.findViewById(R.id.option_leave_layout);
        optionDelete = (LinearLayout) contentView.findViewById(R.id.option_rubbish_bin_layout);

        optionDownload.setOnClickListener(this);
        optionRevert.setOnClickListener(this);
        optionDelete.setOnClickListener(this);

        nodeName.setMaxWidth(Util.scaleWidthPx(200, outMetrics));
        nodeInfo.setMaxWidth(Util.scaleWidthPx(200, outMetrics));

        if(node!=null) {
            log("node is NOT null");

            nodeName.setText(node.getName());

            long nodeSize = node.getSize();
            nodeInfo.setText(Util.getSizeString(nodeSize));
            nodeIconLayout.setVisibility(View.GONE);

            if (node.hasThumbnail()) {
                log("Node has thumbnail");
                RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) nodeThumb.getLayoutParams();
                params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                params1.setMargins(20, 0, 12, 0);
                nodeThumb.setLayoutParams(params1);

                thumb = ThumbnailUtils.getThumbnailFromCache(node);
                if (thumb != null) {
                    nodeThumb.setImageBitmap(thumb);
                } else {
                    thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
                    if (thumb != null) {
                        nodeThumb.setImageBitmap(thumb);
                    } else {
                        nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                    }
                }
            } else {
                nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
            }
            optionDownload.setVisibility(View.VISIBLE);
            optionDelete.setVisibility(View.VISIBLE);
            optionRevert.setVisibility(View.VISIBLE);

            dialog.setContentView(contentView);
            mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
            mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
        else{
            log("Node NULL");
        }
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.option_download_layout:{
                log("Download option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                ((ContactFileListActivityLollipop) context).onFileClick(handleList);
                break;
            }
            case R.id.option_properties_layout:{
                log("Properties option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                Intent i = new Intent(context, FileInfoActivityLollipop.class);
                i.putExtra("handle", node.getHandle());
                i.putExtra("from", FileInfoActivityLollipop.FROM_INCOMING_SHARES);
                boolean firstLevel = ((ContactFileListActivityLollipop) context).isEmptyParentHandleStack();
                log("onClick File Info: First LEVEL is: "+firstLevel);
                i.putExtra("firstLevel", firstLevel);

                if (node.isFolder()) {
                    if(node.isInShare()){
                        i.putExtra("imageId", R.drawable.ic_folder_incoming);
                    }
                    else if (node.isOutShare()||megaApi.isPendingShare(node)){
                        i.putExtra("imageId", R.drawable.ic_folder_outgoing);
                    }
                    else{
                        i.putExtra("imageId", R.drawable.ic_folder);
                    }
                }
                else {
                    i.putExtra("imageId", MimeTypeInfo.typeForName(node.getName()).getIconResourceId());
                }
                i.putExtra("name", node.getName());
                context.startActivity(i);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_leave_layout:{
                log("Share with");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ((ContactFileListActivityLollipop) context).showConfirmationLeaveIncomingShare(node);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_rename_layout:{
                log("Rename option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ((ContactFileListActivityLollipop) context).showRenameDialog(node, node.getName());
                break;
            }
            case R.id.option_move_layout:{
                log("Move option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                ((ContactFileListActivityLollipop) context).showMoveLollipop(handleList);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_copy_layout:{
                log("Copy option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                ((ContactFileListActivityLollipop) context).showCopyLollipop(handleList);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_rubbish_bin_layout:{
                log("Delete/Move to rubbish option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                ((ContactFileListActivityLollipop) context).askConfirmationMoveToRubbish(handleList);
                break;
            }
        }

//        dismiss();
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }


    @Override
    public void onAttach(Activity activity) {
        log("onAttach");
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
        log("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        long handle = node.getHandle();
        log("Handle of the node: "+handle);
        outState.putLong("handle", handle);
    }

    public interface CustomHeight{
        int getHeightToPanel(BottomSheetDialogFragment dialog);
    }

    private static void log(String log) {
        Util.log("ContactFileListBottomSheetDialogFragment", log);
    }
}
