package mega.privacy.android.app.modalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactFileListActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class ContactFileListBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    protected Context context;
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
    protected LinearLayout items_layout;
    protected DisplayMetrics outMetrics;
    protected Bitmap thumb = null;
    protected int height = -1;
    protected boolean heightseted = false;
    protected int heightReal = -1;
    protected int heightDisplay;
    protected MegaApiAndroid megaApi;
    protected DatabaseHandler dbH;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        logDebug("onCreate");
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if(savedInstanceState!=null) {
            logDebug("Bundle is NOT NULL");
            long handle = savedInstanceState.getLong("handle", -1);
            logDebug("Handle of the node: " + handle);
            node = megaApi.getNodeByHandle(handle);
        }
        else{
            logWarning("Bundle NULL");
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
        logDebug("setupDialog");
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        heightDisplay = outMetrics.heightPixels;

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

        LinearLayout separatorInfo = (LinearLayout) contentView.findViewById(R.id.separator_info);
        LinearLayout separatorDownload = (LinearLayout) contentView.findViewById(R.id.separator_download);
        LinearLayout separatorLeave = (LinearLayout) contentView.findViewById(R.id.separator_leave);
        LinearLayout separatorModify = (LinearLayout) contentView.findViewById(R.id.separator_modify);

        nodeName.setMaxWidth(scaleWidthPx(200, outMetrics));
        nodeInfo.setMaxWidth(scaleWidthPx(200, outMetrics));

        if(node!=null) {
            logDebug("node is NOT null");

            nodeName.setText(node.getName());

            boolean firstLevel = ((ContactFileListActivityLollipop) context).isEmptyParentHandleStack();
            logDebug("First LEVEL is: " + firstLevel);
            long parentHandle = -1;
            parentHandle = ((ContactFileListActivityLollipop) context).getParentHandle();
            logDebug("Parent handle is: " + parentHandle);
            int accessLevel = megaApi.getAccess(node);

            if (node.isFolder()) {

                nodeThumb.setImageResource(R.drawable.ic_folder_incoming);
                optionInfoText.setText(R.string.general_folder_info);
                nodeInfo.setText(getInfoFolder(node, context, megaApi));

                if(firstLevel||parentHandle == -1){
                    logDebug("First level!!");
                    optionLeave.setVisibility(View.VISIBLE);

                    switch (accessLevel) {
                        case MegaShare.ACCESS_FULL: {
                            logDebug("LEVEL 0 - access FULL");
                            nodeIcon.setImageResource(R.drawable.ic_shared_fullaccess);
                            break;
                        }
                        case MegaShare.ACCESS_READ: {
                            logDebug("LEVEL 0 - access read");
                            nodeIcon.setImageResource(R.drawable.ic_shared_read);
                            break;
                        }
                        case MegaShare.ACCESS_READWRITE: {
                            logDebug("LEVEL 0 - readwrite");
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
                nodeInfo.setText(getSizeString(nodeSize));
                nodeIconLayout.setVisibility(View.GONE);

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
                    logDebug("Read");
                    optionRename.setVisibility(View.GONE);
                    optionRubbish.setVisibility(View.GONE);
                    optionMove.setVisibility(View.GONE);
                    break;
                }
                case MegaShare.ACCESS_READWRITE: {
                    logDebug("Read & write");
                    optionMove.setVisibility(View.GONE);
                    optionRename.setVisibility(View.GONE);
                    optionRubbish.setVisibility(View.GONE);
                    break;
                }
            }

            if (optionInfo.getVisibility() == View.GONE || (optionDownload.getVisibility() == View.GONE && optionLeave.getVisibility() == View.GONE
                    && optionCopy.getVisibility() == View.GONE &&  optionMove.getVisibility() == View.GONE && optionRename.getVisibility() == View.GONE
                    && optionRubbish.getVisibility() == View.GONE)){
                separatorInfo.setVisibility(View.GONE);
            }
            else {
                separatorInfo.setVisibility(View.VISIBLE);
            }

            if (optionDownload.getVisibility() == View.GONE || (optionLeave.getVisibility() == View.GONE && optionCopy.getVisibility() == View.GONE
                    && optionMove.getVisibility() == View.GONE && optionRename.getVisibility() == View.GONE && optionRubbish.getVisibility() == View.GONE)){
                separatorDownload.setVisibility(View.GONE);
            }
            else {
                separatorDownload.setVisibility(View.VISIBLE);
            }

            if (optionLeave.getVisibility() == View.GONE || (optionCopy.getVisibility() == View.GONE
                    && optionMove.getVisibility() == View.GONE && optionRename.getVisibility() == View.GONE && optionRubbish.getVisibility() == View.GONE)) {
                separatorLeave.setVisibility(View.GONE);
            }
            else {
                separatorLeave.setVisibility(View.VISIBLE);
            }

            if ((optionCopy.getVisibility() == View.GONE
                    && optionMove.getVisibility() == View.GONE && optionRename.getVisibility() == View.GONE) || optionRubbish.getVisibility() == View.GONE) {
                separatorModify.setVisibility(View.GONE);
            }
            else {
                separatorModify.setVisibility(View.VISIBLE);
            }

            dialog.setContentView(contentView);
            mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
//            mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

            mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, context, 81));
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
//                    if(slideOffset> 0 && !heightseted){
//                        if(context instanceof ContactFileListBottomSheetDialogFragment.CustomHeight){
//                            height = ((ContactFileListBottomSheetDialogFragment.CustomHeight) context).getHeightToPanel(thisclass);
//                        }
//                        if(height != -1 && heightReal != -1){
//                            heightseted = true;
//                            int numSons = 0;
//                            int num = items_layout.getChildCount();
//                            for(int i=0; i<num; i++){
//                                View v = items_layout.getChildAt(i);
//                                if(v.getVisibility() == View.VISIBLE){
//                                    numSons++;
//                                }
//                            }
//
//                            if(heightReal > height){
//                                ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
//                                params.height = height;
//                                bottomSheet.setLayoutParams(params);
//                            }
//                        }
//                    }
                    if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
                        ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
                        if (getActivity() != null && getActivity().findViewById(R.id.toolbar_main_contact_properties) != null) {
                            int tBHeight = getActivity().findViewById(R.id.toolbar_main_contact_properties).getHeight();
                            Rect rectangle = new Rect();
                            getActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(rectangle);
                            int windowHeight = rectangle.bottom;
                            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());
                            int maxHeight = windowHeight - tBHeight - rectangle.top - padding;

                            logDebug("bottomSheet.height: " + mainLinearLayout.getHeight() + " maxHeight: " + maxHeight);
                            if (mainLinearLayout.getHeight() > maxHeight) {
                                params.height = maxHeight;
                                bottomSheet.setLayoutParams(params);
                            }
                        }
                    }
                }
            });
        }
        else{
            logWarning("Node NULL");
        }
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
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                ((ContactFileListActivityLollipop) context).onFileClick(handleList);
                break;
            }
            case R.id.option_properties_layout:{
                logDebug("Properties option");
                if(node==null){
                    logWarning("The selected node is NULL");
                    return;
                }
                Intent i = new Intent(context, FileInfoActivityLollipop.class);
                i.putExtra("handle", node.getHandle());
                i.putExtra("from", FROM_INCOMING_SHARES);
                boolean firstLevel = ((ContactFileListActivityLollipop) context).isEmptyParentHandleStack();
                logDebug("File Info: First LEVEL is: " + firstLevel);
                i.putExtra("firstLevel", firstLevel);
                i.putExtra("name", node.getName());
                context.startActivity(i);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_leave_layout:{
                logDebug("Share with");
                if(node==null){
                    logWarning("The selected node is NULL");
                    return;
                }
                ((ContactFileListActivityLollipop) context).showConfirmationLeaveIncomingShare(node);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_rename_layout:{
                logDebug("Rename option");
                if(node==null){
                    logWarning("The selected node is NULL");
                    return;
                }
                ((ContactFileListActivityLollipop) context).showRenameDialog(node, node.getName());
                break;
            }
            case R.id.option_move_layout:{
                logDebug("Move option");
                if(node==null){
                    logWarning("The selected node is NULL");
                    return;
                }
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                ((ContactFileListActivityLollipop) context).showMoveLollipop(handleList);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_copy_layout:{
                logDebug("Copy option");
                if(node==null){
                    logWarning("The selected node is NULL");
                    return;
                }
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                ((ContactFileListActivityLollipop) context).showCopyLollipop(handleList);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_rubbish_bin_layout:{
                logDebug("Delete/Move to rubbish option");
                if(node==null){
                    logWarning("The selected node is NULL");
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
        logDebug("onAttach");
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

    public interface CustomHeight{
        int getHeightToPanel(BottomSheetDialogFragment dialog);
    }
}
