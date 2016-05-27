package mega.privacy.android.app.lollipop.listeners;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileContactListActivityLollipop;
import mega.privacy.android.app.lollipop.FilePropertiesActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
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
        MegaNode selectedNode = ((ManagerActivityLollipop) context).getSelectedNode();

        if(selectedNode==null){
            log("The selected node is NULL");
            return;
        }
        switch(v.getId()){

            case R.id.file_list_out_options:{
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                break;
            }

            case R.id.file_list_option_download_layout: {
                log("Download option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(selectedNode.getHandle());
//                ((ManagerActivityLollipop) context).onFileClick(handleList);
                nC.prepareForDownload(handleList);
                break;
            }

            case R.id.file_list_option_send_inbox_layout: {
                log("Send inbox option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                nC.selectContactToSendNode(selectedNode);
                break;
            }
            case R.id.file_list_option_move_layout:{
                log("Move option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(selectedNode.getHandle());
                nC.chooseLocationToMoveNodes(handleList);
                break;
            }

            case R.id.file_list_option_properties_layout: {
                log("Properties option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                Intent i = new Intent(context, FilePropertiesActivityLollipop.class);
                i.putExtra("handle", selectedNode.getHandle());

                drawerItem = ((ManagerActivityLollipop) context).getDrawerItem();
                if(drawerItem== ManagerActivityLollipop.DrawerItem.SHARED_ITEMS){
                    if(((ManagerActivityLollipop) context).getIndexShares()==0){
                        i.putExtra("from", FilePropertiesActivityLollipop.FROM_INCOMING_SHARES);
                    }
                }
                else if(drawerItem== ManagerActivityLollipop.DrawerItem.INBOX){
                    if(((ManagerActivityLollipop) context).getIndexShares()==0){
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

            case R.id.file_list_option_delete_layout: {
                log("Delete option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(selectedNode.getHandle());
                ((ManagerActivityLollipop) context).askConfirmationMoveToRubbish(handleList);

                break;
            }

            case R.id.file_list_option_public_link_layout: {
                log("Public link option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                nC.exportLink(selectedNode);
                break;
            }

            case R.id.file_list_option_rename_layout:{
                log("Rename option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                ((ManagerActivityLollipop) context).showRenameDialog(selectedNode, selectedNode.getName());
                break;
            }

            case R.id.file_list_option_share_layout:{
                log("Share option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                nC.selectContactToShareFolder(selectedNode);
                break;
            }

            case R.id.file_list_option_copy_layout:{
                log("Copy option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(selectedNode.getHandle());
                nC.chooseLocationToCopyNodes(handleList);
                break;
            }
            case R.id.file_list_option_clear_share_layout:{
                log("Clear shares");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                ArrayList<MegaShare> shareList = megaApi.getOutShares(selectedNode);
                nC.removeAllSharingContacts(shareList, selectedNode);
                break;
            }

            case R.id.file_list_option_permissions_layout: {
                log("Share with");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                Intent i = new Intent(context, FileContactListActivityLollipop.class);
                i.putExtra("name", selectedNode.getHandle());
                context.startActivity(i);
                break;
            }
            case R.id.file_list_option_leave_share_layout:{
                log("Leave share option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                ((ManagerActivityLollipop) context).showConfirmationLeaveIncomingShare(selectedNode);
                break;
            }
            case R.id.file_list_option_open_folder_layout: {
                log("Open folder option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                nC.openFolderFromSearch(selectedNode.getHandle());
                break;
            }
        }

    }

    public static void log(String message) {
        Util.log("NodeOptionsPanelListener", message);
    }
}
