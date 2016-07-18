package mega.privacy.android.app.lollipop.listeners;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileContactListActivityLollipop;
import mega.privacy.android.app.lollipop.FilePropertiesActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.OfflineActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

public class NodeOptionsPanelListener implements View.OnClickListener {

    Context context;
    ManagerActivityLollipop.DrawerItem drawerItem;
    MegaApiAndroid megaApi;
    NodeController nC;

    public NodeOptionsPanelListener(Context context){
        log("NodeOptionsPanelListener created");
        this.context = context;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
        nC = new NodeController(context);
    }

    @Override
    public void onClick(View v) {
        log("onClick NodeOptionsPanelListener");
        MegaNode selectedNode = null;
        if(context instanceof ManagerActivityLollipop){
            selectedNode = ((ManagerActivityLollipop) context).getSelectedNode();
        }
        switch(v.getId()){

            case R.id.file_list_out_options:{
                log("file_list_out_options option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                break;
            }

            case R.id.file_list_option_download_layout: {
                log("Download option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                if(selectedNode==null){
                    log("The selected node is NULL");
                    return;
                }
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(selectedNode.getHandle());
//                ((ManagerActivityLollipop) context).onFileClick(handleList);
                nC.prepareForDownload(handleList);
                break;
            }

            case R.id.file_list_option_send_inbox_layout: {
                log("Send inbox option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                if(selectedNode==null){
                    log("The selected node is NULL");
                    return;
                }
                nC.selectContactToSendNode(selectedNode);
                break;
            }
            case R.id.file_list_option_move_layout:{
                log("Move option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                if(selectedNode==null){
                    log("The selected node is NULL");
                    return;
                }
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(selectedNode.getHandle());
                nC.chooseLocationToMoveNodes(handleList);
                break;
            }

            case R.id.file_list_option_properties_layout: {
                log("Properties option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                if(selectedNode==null){
                    log("The selected node is NULL");
                    return;
                }
                Intent i = new Intent(context, FilePropertiesActivityLollipop.class);
                i.putExtra("handle", selectedNode.getHandle());

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

                if (selectedNode.isFolder()) {
                    if (megaApi.isShared(selectedNode)){
                        i.putExtra("imageId", R.drawable.folder_shared_mime);
                    }
                    else{
                        i.putExtra("imageId", R.drawable.folder_mime);
                    }
                }
                else {
                    i.putExtra("imageId", MimeTypeMime.typeForName(selectedNode.getName()).getIconResourceId());
                }
                i.putExtra("name", selectedNode.getName());
                context.startActivity(i);
                break;
            }

            case R.id.file_list_option_delete_layout:
            case R.id.file_list_option_remove_layout:{
                log("Delete/Move to rubbish option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                if(selectedNode==null){
                    log("The selected node is NULL");
                    return;
                }
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(selectedNode.getHandle());
                ((ManagerActivityLollipop) context).askConfirmationMoveToRubbish(handleList);
                break;
            }

            case R.id.file_list_option_public_link_layout: {
                log("Public link option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                if(selectedNode==null){
                    log("The selected node is NULL");
                    return;
                }
                if(selectedNode.isExported()){
                    log("node is already exported: "+selectedNode.getName());
                    log("node link: "+selectedNode.getPublicLink());
                    ((ManagerActivityLollipop) context).showGetLinkPanel(selectedNode.getPublicLink(), selectedNode.getExpirationTime());
                }
                else{
                    nC.exportLink(selectedNode);
                }
                break;
            }

            case R.id.file_list_option_remove_link_layout: {
                log("REMOVE public link option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                if(selectedNode==null){
                    log("The selected node is NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).showConfirmationRemovePublicLink(selectedNode);
                break;
            }

            case R.id.file_list_option_rename_layout:{
                log("Rename option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                if(selectedNode==null){
                    log("The selected node is NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).showRenameDialog(selectedNode, selectedNode.getName());
                break;
            }

            case R.id.file_list_option_share_layout:{
                log("Share option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                if(selectedNode==null){
                    log("The selected node is NULL");
                    return;
                }
                nC.selectContactToShareFolder(selectedNode);
                break;
            }

            case R.id.file_list_option_copy_layout:{
                log("Copy option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                if(selectedNode==null){
                    log("The selected node is NULL");
                    return;
                }
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(selectedNode.getHandle());
                nC.chooseLocationToCopyNodes(handleList);
                break;
            }
            case R.id.file_list_option_clear_share_layout:{
                log("Clear shares");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                if(selectedNode==null){
                    log("The selected node is NULL");
                    return;
                }
                ArrayList<MegaShare> shareList = megaApi.getOutShares(selectedNode);
                ((ManagerActivityLollipop) context).showConfirmationRemoveAllSharingContacts(shareList, selectedNode);
                break;
            }

            case R.id.file_list_option_permissions_layout: {
                log("Share with");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                if(selectedNode==null){
                    log("The selected node is NULL");
                    return;
                }
                Intent i = new Intent(context, FileContactListActivityLollipop.class);
                i.putExtra("name", selectedNode.getHandle());
                context.startActivity(i);
                break;
            }
            case R.id.file_list_option_leave_share_layout:{
                log("Leave share option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                if(selectedNode==null){
                    log("The selected node is NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).showConfirmationLeaveIncomingShare(selectedNode);
                break;
            }
            case R.id.file_list_option_open_folder_layout: {
                log("Open folder option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                if(selectedNode==null){
                    log("The selected node is NULL");
                    return;
                }
                nC.openFolderFromSearch(selectedNode.getHandle());
                break;
            }
            case R.id.offline_list_option_delete_layout:{
                log("Delete Offline");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                String pathNavigation = ((ManagerActivityLollipop) context).getPathNavigationOffline();
                MegaOffline mOff = ((ManagerActivityLollipop) context).getSelectedOfflineNode();
                nC.deleteOffline(mOff, pathNavigation);
                break;

            }
            case R.id.offline_list_option_remove_layout:{
                log("OFFLINE_list_out_options option");
                ((OfflineActivityLollipop) context).hideOptionsPanel();
                String pathNavigation = ((OfflineActivityLollipop) context).getPathNavigation();
                MegaOffline mOff = ((OfflineActivityLollipop) context).getSelectedNode();
                nC.deleteOffline(mOff, pathNavigation);
                break;
            }

            case R.id.offline_list_out_options:{
                log("OFFLINE_list_out_options option");
                ((OfflineActivityLollipop) context).hideOptionsPanel();
                break;
            }
        }

    }

    public static void log(String message) {
        Util.log("NodeOptionsPanelListener", message);
    }
}
