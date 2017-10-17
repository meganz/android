package mega.privacy.android.app.modalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeInfo;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactFileListActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

public class ContactFileListBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

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
    private LinearLayout optionInfo;
    private TextView optionInfoText;
    private ImageView optionInfoImage;
    private LinearLayout optionLeave;
    private LinearLayout optionCopy;
    private LinearLayout optionMove;
    private LinearLayout optionRename;
    private LinearLayout optionRubbish;
    private LinearLayout items_layout;

    private DisplayMetrics outMetrics;

    private Bitmap thumb = null;

    private int height = -1;
    private boolean heightseted = false;
    private int heightReal = -1;

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
            if(context instanceof ContactFileListActivityLollipop){
                node = ((ContactFileListActivityLollipop) context).getSelectedNode();
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

        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_contact_file_list, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.contact_file_list_bottom_sheet);

        mainLinearLayout.post(new Runnable() {
            @Override
            public void run() {
                heightReal = mainLinearLayout.getHeight();
            }
        });

        nodeThumb = (ImageView) contentView.findViewById(R.id.contact_file_list_thumbnail);
        nodeName = (TextView) contentView.findViewById(R.id.contact_file_list_name_text);
        nodeInfo  = (TextView) contentView.findViewById(R.id.contact_file_list_info_text);
        nodeIconLayout = (RelativeLayout) contentView.findViewById(R.id.contact_file_list_relative_layout_icon);
        nodeIcon = (ImageView) contentView.findViewById(R.id.contact_file_list_icon);
        optionDownload = (LinearLayout) contentView.findViewById(R.id.option_download_layout);
        optionInfo = (LinearLayout) contentView.findViewById(R.id.option_properties_layout);
        optionInfoText = (TextView) contentView.findViewById(R.id.option_properties_text);
        optionInfoImage = (ImageView) contentView.findViewById(R.id.option_properties_image);
        optionLeave = (LinearLayout) contentView.findViewById(R.id.option_leave_layout);
        optionCopy = (LinearLayout) contentView.findViewById(R.id.option_copy_layout);
        optionMove = (LinearLayout) contentView.findViewById(R.id.option_move_layout);
        optionRename = (LinearLayout) contentView.findViewById(R.id.option_rename_layout);
        optionRubbish = (LinearLayout) contentView.findViewById(R.id.option_rubbish_bin_layout);

        items_layout = (LinearLayout) contentView.findViewById(R.id.item_list_bottom_sheet_contact_file);

        optionDownload.setOnClickListener(this);
        optionInfo.setOnClickListener(this);
        optionCopy.setOnClickListener(this);
        optionMove.setOnClickListener(this);
        optionRename.setOnClickListener(this);
        optionLeave.setOnClickListener(this);
        optionRubbish.setOnClickListener(this);

        nodeName.setMaxWidth(Util.scaleWidthPx(200, outMetrics));
        nodeInfo.setMaxWidth(Util.scaleWidthPx(200, outMetrics));

        if(node!=null) {
            log("node is NOT null");

            nodeName.setText(node.getName());

            boolean firstLevel = ((ContactFileListActivityLollipop) context).isEmptyParentHandleStack();
            log("First LEVEL is: "+firstLevel);
            long parentHandle = -1;
            parentHandle = ((ContactFileListActivityLollipop) context).getParentHandle();
            log("Parent handle is: "+parentHandle);
            int accessLevel = megaApi.getAccess(node);

            if (node.isFolder()) {

                nodeThumb.setImageResource(R.drawable.ic_folder_incoming);
                optionInfoText.setText(R.string.general_folder_info);
                nodeInfo.setText(MegaApiUtils.getInfoFolder(node, context, megaApi));

                if(firstLevel||parentHandle == -1){
                    log("Fist level!!");
                    optionLeave.setVisibility(View.VISIBLE);

                    switch (accessLevel) {
                        case MegaShare.ACCESS_FULL: {
                            log("LEVEL 0 - access FULL");
                            nodeIcon.setImageResource(R.drawable.ic_shared_fullaccess);
                            break;
                        }
                        case MegaShare.ACCESS_READ: {
                            log("LEVEL 0 - access read");
                            nodeIcon.setImageResource(R.drawable.ic_shared_read);
                            break;
                        }
                        case MegaShare.ACCESS_READWRITE: {
                            log("LEVEL 0 - readwrite");
                            nodeIcon.setImageResource(R.drawable.ic_shared_read_write);
                        }
                    }
                    nodeIconLayout.setVisibility(View.VISIBLE);
                }
                else{
                    optionLeave.setVisibility(View.GONE);
                    nodeIconLayout.setVisibility(View.GONE);
                }

            } else {
                optionInfoText.setText(R.string.general_file_info);
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
                optionLeave.setVisibility(View.GONE);
            }

            switch (accessLevel) {
                case MegaShare.ACCESS_FULL: {
                    optionMove.setVisibility(View.GONE);
                    optionRename.setVisibility(View.VISIBLE);

                    if(firstLevel||parentHandle == -1){
                        optionRubbish.setVisibility(View.GONE);
                    }
                    else{
                        optionRubbish.setVisibility(View.VISIBLE);
                    }

                    break;
                }
                case MegaShare.ACCESS_READ: {
                    log("read");
                    optionRename.setVisibility(View.GONE);
                    optionRubbish.setVisibility(View.GONE);
                    optionMove.setVisibility(View.GONE);
                    break;
                }
                case MegaShare.ACCESS_READWRITE: {
                    log("readwrite");
                    optionMove.setVisibility(View.GONE);
                    optionRename.setVisibility(View.GONE);
                    optionRubbish.setVisibility(View.GONE);
                    break;
                }
            }

            dialog.setContentView(contentView);
            mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
            mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

            final ContactFileListBottomSheetDialogFragment thisclass = this;

            mBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if(newState == BottomSheetBehavior.STATE_HIDDEN){
                        dismissAllowingStateLoss();
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    if(slideOffset> 0 && !heightseted){
                        if(context instanceof ContactFileListBottomSheetDialogFragment.CustomHeight){
                            height = ((ContactFileListBottomSheetDialogFragment.CustomHeight) context).getHeightToPanel(thisclass);
                        }
                        if(height != -1 && heightReal != -1){
                            heightseted = true;
                            int numSons = 0;
                            int num = items_layout.getChildCount();
                            for(int i=0; i<num; i++){
                                View v = items_layout.getChildAt(i);
                                if(v.getVisibility() == View.VISIBLE){
                                    numSons++;
                                }
                            }

                            if(heightReal > height){
                                ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
                                params.height = height;
                                bottomSheet.setLayoutParams(params);
                            }
                        }
                    }
                }
            });
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
                    else if (node.isOutShare()){
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
