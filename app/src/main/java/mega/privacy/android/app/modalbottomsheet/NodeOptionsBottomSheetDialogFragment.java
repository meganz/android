package mega.privacy.android.app.modalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileContactListActivityLollipop;
import mega.privacy.android.app.lollipop.FilePropertiesActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.OfflineActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

public class NodeOptionsBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;
    MegaNode node = null;
    MegaOffline nodeOffline = null;
    NodeController nC;

    private BottomSheetBehavior mBehavior;

    LinearLayout mainLinearLayout;
    ImageView nodeThumb;
    TextView nodeName;
    TextView nodeInfo;
    LinearLayout optionDownload;
    LinearLayout optionInfo;
    TextView optionInfoText;
    ImageView optionInfoImage;
    LinearLayout optionLink;
    TextView optionLinkText;
    ImageView optionLinkImage;
    LinearLayout optionRemoveLink;
    LinearLayout optionShare;
    LinearLayout optionPermissions;
    LinearLayout optionClearShares;
    LinearLayout optionLeaveShares;
    LinearLayout optionSendInbox;
    LinearLayout optionRename;
    LinearLayout optionMove;
    LinearLayout optionCopy;
    LinearLayout optionRubbishBin;
    LinearLayout optionRemove;
    LinearLayout optionOpenFolder;
    LinearLayout optionDeleteOffline;

    DisplayMetrics outMetrics;

    static ManagerActivityLollipop.DrawerItem drawerItem = null;

    MegaApiAndroid megaApi;
    DatabaseHandler dbH;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if(context instanceof ManagerActivityLollipop){
            node = ((ManagerActivityLollipop) context).getSelectedNode();
            drawerItem = ((ManagerActivityLollipop) context).getDrawerItem();
        }

        nC = new NodeController(context);

        dbH = DatabaseHandler.getDbHandler(getActivity());
    }

    @Override
    public void setupDialog(final Dialog dialog, int style) {

        super.setupDialog(dialog, style);

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_node_item, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.node_bottom_sheet);

        nodeThumb = (ImageView) contentView.findViewById(R.id.node_thumbnail);
        nodeName = (TextView) contentView.findViewById(R.id.node_name_text);
        nodeInfo  = (TextView) contentView.findViewById(R.id.node_info_text);
        optionDownload = (LinearLayout) contentView.findViewById(R.id.option_download_layout);
        optionInfo = (LinearLayout) contentView.findViewById(R.id.option_properties_layout);
        optionInfoText = (TextView) contentView.findViewById(R.id.option_properties_text);
        optionInfoImage = (ImageView) contentView.findViewById(R.id.option_properties_image);
        optionLink = (LinearLayout) contentView.findViewById(R.id.option_link_layout);
        optionLinkText = (TextView) contentView.findViewById(R.id.option_link_text);
        optionLinkImage = (ImageView) contentView.findViewById(R.id.option_link_image);
        optionRemoveLink = (LinearLayout) contentView.findViewById(R.id.option_remove_link_layout);
        optionShare = (LinearLayout) contentView.findViewById(R.id.option_share_layout);
        optionPermissions = (LinearLayout) contentView.findViewById(R.id.option_permissions_layout);
        optionClearShares = (LinearLayout) contentView.findViewById(R.id.option_clear_share_layout);
        optionLeaveShares = (LinearLayout) contentView.findViewById(R.id.option_leave_share_layout);
        optionSendInbox = (LinearLayout) contentView.findViewById(R.id.option_send_inbox_layout);
        optionRename = (LinearLayout) contentView.findViewById(R.id.option_rename_layout);
        optionMove = (LinearLayout) contentView.findViewById(R.id.option_move_layout);
        optionCopy = (LinearLayout) contentView.findViewById(R.id.option_copy_layout);
        optionRubbishBin = (LinearLayout) contentView.findViewById(R.id.option_rubbish_bin_layout);
        optionRemove = (LinearLayout) contentView.findViewById(R.id.option_remove_layout);
        optionOpenFolder = (LinearLayout) contentView.findViewById(R.id.option_open_folder_layout);
        optionDeleteOffline = (LinearLayout) contentView.findViewById(R.id.option_delete_offline_layout);

        optionDownload.setOnClickListener(this);
        optionInfo.setOnClickListener(this);
        optionLink.setOnClickListener(this);
        optionRemoveLink.setOnClickListener(this);
        optionShare.setOnClickListener(this);
        optionPermissions.setOnClickListener(this);
        optionClearShares.setOnClickListener(this);
        optionLeaveShares.setOnClickListener(this);
        optionSendInbox.setOnClickListener(this);
        optionRename.setOnClickListener(this);
        optionMove.setOnClickListener(this);
        optionCopy.setOnClickListener(this);
        optionRubbishBin.setOnClickListener(this);
        optionRemove.setOnClickListener(this);
        optionOpenFolder.setOnClickListener(this);
        optionDeleteOffline.setOnClickListener(this);

        nodeName.setMaxWidth(Util.scaleWidthPx(200, outMetrics));
        nodeInfo.setMaxWidth(Util.scaleWidthPx(200, outMetrics));

        if(node!=null){
            if(Util.isOnline(context)){
                nodeName.setText(node.getName());

                if (node.isFolder()) {
                    nodeInfo.setText(MegaApiUtils.getInfoFolder(node, context, megaApi));
                    if (node.isShared()) {
                        nodeThumb.setImageResource(R.drawable.ic_folder_shared_list);
                    } else {
                        nodeThumb.setImageResource(R.drawable.ic_folder_list);
                    }
                }
                else{
                    long nodeSize = node.getSize();
                    nodeInfo.setText(Util.getSizeString(nodeSize));
                    nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                }
            }
        }

        switch (drawerItem){
            case CLOUD_DRIVE:{
                int tabSelected = ((ManagerActivityLollipop)context).getTabItemCloud();
                if(tabSelected==0){
                    log("show Cloud bottom sheet");

                    if (node.isFolder()) {
                        optionInfoText.setText(R.string.general_folder_info);
                        optionShare.setVisibility(View.VISIBLE);

                    }else{
                        optionInfoText.setText(R.string.general_file_info);
                        optionShare.setVisibility(View.GONE);
                    }

                    optionSendInbox.setVisibility(View.VISIBLE);
                    optionDownload.setVisibility(View.VISIBLE);
                    optionInfo.setVisibility(View.VISIBLE);
                    optionRubbishBin.setVisibility(View.VISIBLE);
                    optionLink.setVisibility(View.VISIBLE);
                    if(node.isExported()){
                        optionLinkText.setText(R.string.edit_link_option);
                        optionRemoveLink.setVisibility(View.VISIBLE);
                    }
                    else{
                        optionLinkText.setText(R.string.context_get_link_menu);
                        optionRemoveLink.setVisibility(View.GONE);
                    }

                    optionRubbishBin.setVisibility(View.VISIBLE);
                    optionRename.setVisibility(View.VISIBLE);
                    optionMove.setVisibility(View.VISIBLE);
                    optionCopy.setVisibility(View.VISIBLE);

                    //Hide
                    optionClearShares.setVisibility(View.GONE);
                    optionRemove.setVisibility(View.GONE);
                    optionPermissions.setVisibility(View.GONE);
                    optionLeaveShares.setVisibility(View.GONE);
                    optionOpenFolder.setVisibility(View.GONE);
                    optionDeleteOffline.setVisibility(View.GONE);
                }
                else if(tabSelected==1){
                    log("show Rubbish bottom sheet");
                    if (node.isFolder()) {
                        optionInfoText.setText(R.string.general_folder_info);
                    }else{
                        optionInfoText.setText(R.string.general_file_info);
                    }

                    optionMove.setVisibility(View.VISIBLE);
                    optionRemove.setVisibility(View.VISIBLE);
                    optionInfo.setVisibility(View.VISIBLE);

                    //Hide
                    optionClearShares.setVisibility(View.GONE);
                    optionPermissions.setVisibility(View.GONE);
                    optionLeaveShares.setVisibility(View.GONE);
                    optionRubbishBin.setVisibility(View.GONE);
                    optionRename.setVisibility(View.GONE);
                    optionCopy.setVisibility(View.GONE);
                    optionSendInbox.setVisibility(View.GONE);
                    optionShare.setVisibility(View.GONE);
                    optionDownload.setVisibility(View.GONE);
                    optionLink.setVisibility(View.GONE);
                    optionRemoveLink.setVisibility(View.GONE);
                    optionOpenFolder.setVisibility(View.GONE);
                    optionDeleteOffline.setVisibility(View.GONE);
                }
                break;

            }
            case INBOX:{

                if (node.isFolder()) {
                    optionInfoText.setText(R.string.general_folder_info);
                    optionShare.setVisibility(View.VISIBLE);

                }else{
                    optionInfoText.setText(R.string.general_file_info);
                    optionShare.setVisibility(View.GONE);
                }

                optionSendInbox.setVisibility(View.VISIBLE);
                optionDownload.setVisibility(View.VISIBLE);
                optionInfo.setVisibility(View.VISIBLE);
                optionRubbishBin.setVisibility(View.VISIBLE);
                optionLink.setVisibility(View.VISIBLE);
                if(node.isExported()){
                    optionLinkText.setText(R.string.edit_link_option);
                    optionRemoveLink.setVisibility(View.VISIBLE);
                }
                else{
                    optionLinkText.setText(R.string.context_get_link_menu);
                    optionRemoveLink.setVisibility(View.GONE);
                }

                optionRubbishBin.setVisibility(View.VISIBLE);
                optionRename.setVisibility(View.VISIBLE);
                optionMove.setVisibility(View.VISIBLE);
                optionCopy.setVisibility(View.VISIBLE);

                //Hide
                optionClearShares.setVisibility(View.GONE);
                optionRemove.setVisibility(View.GONE);
                optionPermissions.setVisibility(View.GONE);
                optionLeaveShares.setVisibility(View.GONE);
                optionOpenFolder.setVisibility(View.GONE);
                optionDeleteOffline.setVisibility(View.GONE);

                break;
            }
            case SHARED_ITEMS:{
                break;

            }
            case SAVED_FOR_OFFLINE:{
                nodeOffline = ((ManagerActivityLollipop) context).getSelectedOfflineNode();

                //Check if the node is the Master Key file
                if(nodeOffline.getHandle().equals("0")){
                    String path = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.rKFile;
                    File file= new File(path);
                    if(file.exists()){
                        optionMove.setVisibility(View.GONE);
                        optionRemove.setVisibility(View.GONE);
                        optionInfo.setVisibility(View.GONE);
                        optionClearShares.setVisibility(View.GONE);
                        optionPermissions.setVisibility(View.GONE);
                        optionLeaveShares.setVisibility(View.GONE);
                        optionRubbishBin.setVisibility(View.GONE);
                        optionRename.setVisibility(View.GONE);
                        optionCopy.setVisibility(View.GONE);
                        optionSendInbox.setVisibility(View.GONE);
                        optionShare.setVisibility(View.GONE);
                        optionDownload.setVisibility(View.GONE);
                        optionLink.setVisibility(View.GONE);
                        optionLink.setVisibility(View.GONE);
                        optionRemoveLink.setVisibility(View.GONE);
                        optionOpenFolder.setVisibility(View.GONE);
                        optionDeleteOffline.setVisibility(View.VISIBLE);
                    }
                }
                else{

                    if(Util.isOnline(context)){
                        long handle = Long.parseLong(nodeOffline.getHandle());
                        node = megaApi.getNodeByHandle(handle);

                        if(node!=null){

                            nodeName.setText(node.getName());

                            if (node.isFolder()) {
                                nodeInfo.setText(MegaApiUtils.getInfoFolder(node, context, megaApi));
                                if (node.isShared()) {
                                    nodeThumb.setImageResource(R.drawable.ic_folder_shared_list);
                                } else {
                                    nodeThumb.setImageResource(R.drawable.ic_folder_list);
                                }
                            }
                            else{
                                long nodeSize = node.getSize();
                                nodeInfo.setText(Util.getSizeString(nodeSize));
                                nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                            }

                            if (node.isFolder()) {
                                optionInfoText.setText(R.string.general_folder_info);
                            }else{
                                optionInfoText.setText(R.string.general_file_info);
                            }

                            optionDownload.setVisibility(View.VISIBLE);
                            optionInfo.setVisibility(View.VISIBLE);
                            optionDeleteOffline.setVisibility(View.VISIBLE);
                            optionRemoveLink.setVisibility(View.GONE);
                            optionMove.setVisibility(View.GONE);
                            optionRemove.setVisibility(View.GONE);
                            optionClearShares.setVisibility(View.GONE);
                            optionPermissions.setVisibility(View.GONE);
                            optionLeaveShares.setVisibility(View.GONE);
                            optionRubbishBin.setVisibility(View.GONE);
                            optionRename.setVisibility(View.GONE);
                            optionCopy.setVisibility(View.GONE);
                            optionSendInbox.setVisibility(View.GONE);
                            optionShare.setVisibility(View.GONE);
                            optionLink.setVisibility(View.GONE);
                            optionRemoveLink.setVisibility(View.GONE);
                            optionOpenFolder.setVisibility(View.GONE);
                        }
                        else{
                            //No node handle

                            nodeName.setText(nodeOffline.getName());

                            if (nodeOffline.isFolder()) {
                                optionInfoText.setText(R.string.general_folder_info);
//                                nodeInfo.setText(MegaApiUtils.getInfoFolder);
                                nodeThumb.setImageResource(R.drawable.ic_folder_list);

                            }else{
                                optionInfoText.setText(R.string.general_file_info);
//                                long nodeSize = nodeOffline.get
//                                nodeInfo.setText(Util.getSizeString(nodeSize));
                                nodeThumb.setImageResource(MimeTypeList.typeForName(nodeOffline.getName()).getIconResourceId());
                            }

                            log("node not found with handle");
                            optionDownload.setVisibility(View.GONE);
                            optionInfo.setVisibility(View.GONE);
                            optionRemoveLink.setVisibility(View.GONE);
                            optionMove.setVisibility(View.GONE);
                            optionRemove.setVisibility(View.GONE);
                            optionClearShares.setVisibility(View.GONE);
                            optionPermissions.setVisibility(View.GONE);
                            optionLeaveShares.setVisibility(View.GONE);
                            optionRubbishBin.setVisibility(View.GONE);
                            optionRename.setVisibility(View.GONE);
                            optionCopy.setVisibility(View.GONE);
                            optionSendInbox.setVisibility(View.GONE);
                            optionShare.setVisibility(View.GONE);
                            optionLink.setVisibility(View.GONE);
                            optionRemoveLink.setVisibility(View.GONE);
                            optionOpenFolder.setVisibility(View.GONE);
                            optionDeleteOffline.setVisibility(View.VISIBLE);

                        }
                    }
                    else{
                        if (nodeOffline.isFolder()) {
                            optionInfoText.setText(R.string.general_folder_info);
                        }else{
                            optionInfoText.setText(R.string.general_file_info);
                        }

                        optionDownload.setVisibility(View.GONE);
                        optionInfo.setVisibility(View.GONE);
                        optionRemoveLink.setVisibility(View.GONE);
                        optionMove.setVisibility(View.GONE);
                        optionRemove.setVisibility(View.GONE);
                        optionClearShares.setVisibility(View.GONE);
                        optionPermissions.setVisibility(View.GONE);
                        optionLeaveShares.setVisibility(View.GONE);
                        optionRubbishBin.setVisibility(View.GONE);
                        optionRename.setVisibility(View.GONE);
                        optionCopy.setVisibility(View.GONE);
                        optionSendInbox.setVisibility(View.GONE);
                        optionShare.setVisibility(View.GONE);
                        optionLink.setVisibility(View.GONE);
                        optionRemoveLink.setVisibility(View.GONE);
                        optionOpenFolder.setVisibility(View.GONE);
                        optionDeleteOffline.setVisibility(View.VISIBLE);
                    }
                }
                break;
            }
            case SEARCH:{
                if (node.isFolder()) {
                    optionInfoText.setText(R.string.general_folder_info);
                    optionShare.setVisibility(View.VISIBLE);

                }else{
                    optionInfoText.setText(R.string.general_file_info);
                    optionShare.setVisibility(View.GONE);
                }

                optionSendInbox.setVisibility(View.VISIBLE);
                optionDownload.setVisibility(View.VISIBLE);
                optionInfo.setVisibility(View.VISIBLE);
                optionRubbishBin.setVisibility(View.VISIBLE);
                optionLink.setVisibility(View.VISIBLE);
                if(node.isExported()){
                    optionLinkText.setText(R.string.edit_link_option);
                    optionRemoveLink.setVisibility(View.VISIBLE);
                }
                else{
                    optionLinkText.setText(R.string.context_get_link_menu);
                    optionRemoveLink.setVisibility(View.GONE);
                }

                optionRubbishBin.setVisibility(View.VISIBLE);
                optionRename.setVisibility(View.VISIBLE);
                optionOpenFolder.setVisibility(View.VISIBLE);

                //Hide
                optionMove.setVisibility(View.GONE);
                optionCopy.setVisibility(View.GONE);
                optionClearShares.setVisibility(View.GONE);
                optionRemove.setVisibility(View.GONE);
                optionPermissions.setVisibility(View.GONE);
                optionLeaveShares.setVisibility(View.GONE);
                optionDeleteOffline.setVisibility(View.GONE);
                break;
            }
        }

        dialog.setContentView(contentView);
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
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
                nC.prepareForDownload(handleList);
                break;
            }
            case R.id.option_properties_layout:{
                log("Properties option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                Intent i = new Intent(context, FilePropertiesActivityLollipop.class);
                i.putExtra("handle", node.getHandle());

                drawerItem = ((ManagerActivityLollipop) context).getDrawerItem();
                if(drawerItem== ManagerActivityLollipop.DrawerItem.SHARED_ITEMS){
                    if(((ManagerActivityLollipop) context).getTabItemShares()==0){
                        i.putExtra("from", FilePropertiesActivityLollipop.FROM_INCOMING_SHARES);
                    }
                }
                else if(drawerItem== ManagerActivityLollipop.DrawerItem.INBOX){
                    if(((ManagerActivityLollipop) context).getTabItemShares()==0){
                        i.putExtra("from", FilePropertiesActivityLollipop.FROM_INBOX);
                    }
                }

                if (node.isFolder()) {
                    if (megaApi.isShared(node)){
                        i.putExtra("imageId", R.drawable.folder_shared_mime);
                    }
                    else{
                        i.putExtra("imageId", R.drawable.folder_mime);
                    }
                }
                else {
                    i.putExtra("imageId", MimeTypeMime.typeForName(node.getName()).getIconResourceId());
                }
                i.putExtra("name", node.getName());
                MyAccountInfo accountInfo = ((ManagerActivityLollipop)context).getMyAccountInfo();
                if(accountInfo!=null){
                    i.putExtra("typeAccount", accountInfo.getAccountType());
                }
                context.startActivity(i);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_link_layout:{
                log("Public link option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                if(node.isExported()){
                    log("node is already exported: "+node.getName());
                    log("node link: "+node.getPublicLink());
                    ((ManagerActivityLollipop) context).showGetLinkPanel(node.getPublicLink(), node.getExpirationTime());
                }
                else{
                    nC.exportLink(node);
                }
                break;
            }
            case R.id.option_remove_link_layout:{
                log("REMOVE public link option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).showConfirmationRemovePublicLink(node);
                break;
            }
            case R.id.option_share_layout:{
                log("Share option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                nC.selectContactToShareFolder(node);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_permissions_layout:{
                log("Share with");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                Intent i = new Intent(context, FileContactListActivityLollipop.class);
                i.putExtra("name", node.getHandle());
                context.startActivity(i);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_clear_share_layout:{
                log("Clear shares");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ArrayList<MegaShare> shareList = megaApi.getOutShares(node);
                ((ManagerActivityLollipop) context).showConfirmationRemoveAllSharingContacts(shareList, node);
                break;
            }
            case R.id.option_leave_share_layout:{
                log("Leave share option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).showConfirmationLeaveIncomingShare(node);
                break;
            }
            case R.id.option_send_inbox_layout:{
                log("Send inbox option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                nC.selectContactToSendNode(node);
                break;
            }
            case R.id.option_rename_layout:{
                log("Rename option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).showRenameDialog(node, node.getName());

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
                nC.chooseLocationToMoveNodes(handleList);
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
                nC.chooseLocationToCopyNodes(handleList);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_rubbish_bin_layout:
            case R.id.option_remove_layout:{
                log("Delete/Move to rubbish option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                ((ManagerActivityLollipop) context).askConfirmationMoveToRubbish(handleList);
                break;
            }
            case R.id.option_open_folder_layout:{
                log("Open folder option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                nC.openFolderFromSearch(node.getHandle());
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_delete_offline_layout:{
                log("Delete Offline");
                if(context instanceof ManagerActivityLollipop){
                    String pathNavigation = ((ManagerActivityLollipop) context).getPathNavigationOffline();
                    MegaOffline mOff = ((ManagerActivityLollipop) context).getSelectedOfflineNode();
                    nC.deleteOffline(mOff, pathNavigation);
                }
                else if(context instanceof OfflineActivityLollipop){
                    log("OFFLINE_list_out_options option");
                    String pathNavigation = ((OfflineActivityLollipop) context).getPathNavigation();
                    MegaOffline mOff = ((OfflineActivityLollipop) context).getSelectedNode();
                    nC.deleteOffline(mOff, pathNavigation);
                }
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
        context = activity;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    private static void log(String log) {
        Util.log("NodeOptionsBottomSheetDialogFragment", log);
    }
}
