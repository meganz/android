package mega.privacy.android.app.lollipop.listeners;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FilePropertiesActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

public class OptionsPanelListener implements View.OnClickListener {

    Context context;
    ManagerActivityLollipop.DrawerItem drawerItem;
    MegaApiAndroid megaApi;

    public OptionsPanelListener(Context context){
        log("OptionsPanelListener created");
        this.context = context;
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    @Override
    public void onClick(View v) {
        log("onClick OptionsPanelListener");
        MegaNode selectedNode = ((ManagerActivityLollipop) context).getSelectedNode();

        if(selectedNode==null){
            log("The selected node is NULL");
            return;
        }
        switch(v.getId()){

            case R.id.file_list_out_options:
            case R.id.file_grid_out_options:{
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                break;
            }

            case R.id.file_list_option_download_layout:
            case R.id.file_grid_option_download_layout: {
                log("Download option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(selectedNode.getHandle());
                ((ManagerActivityLollipop) context).onFileClick(handleList);
                break;
            }

            case R.id.file_list_option_send_inbox_layout:
            case R.id.file_grid_option_send_inbox_layout: {
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                ((ManagerActivityLollipop) context).sendToInboxLollipop(selectedNode);
//				ArrayList<Long> handleList = new ArrayList<Long>();
//				handleList.add(selectedNode.getHandle());
//				((ManagerActivityLollipop) context).onFileClick(handleList);
                break;
            }
            case R.id.file_list_option_move_layout:
            case R.id.file_grid_option_move_layout: {
                log("Move option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(selectedNode.getHandle());
                ((ManagerActivityLollipop) context).showMoveLollipop(handleList);

                break;
            }

            case R.id.file_list_option_properties_layout:
            case R.id.file_grid_option_properties_layout: {
                log("Properties option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                Intent i = new Intent(context, FilePropertiesActivityLollipop.class);
                i.putExtra("handle", selectedNode.getHandle());

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
            case R.id.file_grid_option_delete_layout: {
                log("Delete option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(selectedNode.getHandle());

                ((ManagerActivityLollipop) context).moveToTrash(handleList);

                break;
            }

            case R.id.file_list_option_public_link_layout:
            case R.id.file_grid_option_public_link_layout: {
                log("Public link option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                ((ManagerActivityLollipop) context).getPublicLinkAndShareIt(selectedNode);

                break;
            }

            case R.id.file_list_option_rename_layout:
            case R.id.file_grid_option_rename_layout: {
                log("Rename option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                ((ManagerActivityLollipop) context).showRenameDialog(selectedNode, selectedNode.getName());
                break;
            }

            case R.id.file_list_option_share_layout:
            case R.id.file_grid_option_share_layout: {
                log("Share option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                ((ManagerActivityLollipop) context).shareFolderLollipop(selectedNode);
                break;
            }

            case R.id.file_list_option_copy_layout:
            case R.id.file_grid_option_copy_layout: {
                log("Copy option");
                ((ManagerActivityLollipop) context).hideOptionsPanel();
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(selectedNode.getHandle());
                ((ManagerActivityLollipop) context).showCopyLollipop(handleList);
                break;
            }
        }

    }

    public static void log(String message) {
        Util.log("OptionsPanelListener", message);
    }
}
