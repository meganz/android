package mega.privacy.android.app.modalbottomsheet;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
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
import mega.privacy.android.app.MimeTypeThumbnail;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import nz.mega.sdk.MegaShare;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class ContactInfoBottomSheetDialogFragment extends ContactFileListBottomSheetDialogFragment {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        logDebug("ContactInfoBottomSheetDialogFragment onCreate");
        super.onCreate(savedInstanceState);
    
        if(savedInstanceState!=null) {
            logDebug("Bundle is NOT NULL");
            long handle = savedInstanceState.getLong("handle", -1);
            logDebug("Handle of the node: "+handle);
            node = megaApi.getNodeByHandle(handle);
        }
        else{
            logWarning("Bundle NULL");
            if(context instanceof ContactInfoActivityLollipop){
                node = ((ContactInfoActivityLollipop) context).getSelectedNode();
            }
        }
    
        nC = new NodeController(context);
        dbH = DatabaseHandler.getDbHandler(getActivity());
    }
    
    @Override
    public void setupDialog(final Dialog dialog,int style) {
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
            logDebug("Node is NOT null");
        
            nodeName.setText(node.getName());
        
            boolean firstLevel = true;
            logDebug("First LEVEL is: " + firstLevel);
            long parentHandle = -1;
            logDebug("Parent handle is: " + parentHandle);
            int accessLevel = megaApi.getAccess(node);
        
            if (node.isFolder()) {
            
                nodeThumb.setImageResource(R.drawable.ic_folder_incoming);
                optionInfoText.setText(R.string.general_folder_info);
                nodeInfo.setText(getInfoFolder(node, context, megaApi));
            
                if(firstLevel||parentHandle == -1){
                    logDebug("Fist level!!");
                    optionLeave.setVisibility(View.VISIBLE);
                
                    switch (accessLevel) {
                        case MegaShare.ACCESS_FULL: {
                            logDebug("LEVEL 0 - Access FULL");
                            nodeIcon.setImageResource(R.drawable.ic_shared_fullaccess);
                            break;
                        }
                        case MegaShare.ACCESS_READ: {
                            logDebug("LEVEL 0 - Access read");
                            nodeIcon.setImageResource(R.drawable.ic_shared_read);
                            break;
                        }
                        case MegaShare.ACCESS_READWRITE: {
                            logDebug("LEVEL 0 - Access read & write");
                            nodeIcon.setImageResource(R.drawable.ic_shared_read_write);
                        }
                    }
                    nodeIconLayout.setVisibility(View.VISIBLE);
                }
                else{
                    optionLeave.setVisibility(View.GONE);
                    nodeIconLayout.setVisibility(View.GONE);
                }
            
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
                    logDebug("Access read");
                    optionRename.setVisibility(View.GONE);
                    optionRubbish.setVisibility(View.GONE);
                    optionMove.setVisibility(View.GONE);
                    break;
                }
                case MegaShare.ACCESS_READWRITE: {
                    logDebug("Access read & write");
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
        
            mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, context, 81));
            mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        
            final ContactFileListBottomSheetDialogFragment thisclass = this;
        
            mBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet,int newState) {
                    if(newState == BottomSheetBehavior.STATE_HIDDEN){
                        dismissAllowingStateLoss();
                    }
                }
            
                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
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
                ((ContactInfoActivityLollipop) context).onFileClick(handleList);
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
                boolean firstLevel = true;
                logDebug("File Info: First LEVEL is: " + firstLevel);
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
                    i.putExtra("imageId", MimeTypeThumbnail.typeForName(node.getName()).getIconResourceId());
                }
                i.putExtra("name", node.getName());
                context.startActivity(i);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_leave_layout:{
                logDebug("Share with option");
                if(node==null){
                    logWarning("The selected node is NULL");
                    return;
                }
                ((ContactInfoActivityLollipop) context).showConfirmationLeaveIncomingShare(node);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_rename_layout:{
                logDebug("Rename option");
                if(node==null){
                    logWarning("The selected node is NULL");
                    return;
                }
                ((ContactInfoActivityLollipop) context).showRenameDialog(node, node.getName());
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
                ((ContactInfoActivityLollipop) context).showMoveLollipop(handleList);
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
                ((ContactInfoActivityLollipop) context).showCopyLollipop(handleList);
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
                ((ContactInfoActivityLollipop) context).askConfirmationMoveToRubbish(handleList);
                break;
            }
        }

//        dismiss();
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }
}
