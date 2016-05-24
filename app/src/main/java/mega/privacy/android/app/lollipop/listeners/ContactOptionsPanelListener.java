package mega.privacy.android.app.lollipop.listeners;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactPropertiesActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaUser;

public class ContactOptionsPanelListener implements View.OnClickListener {

    Context context;
    ManagerActivityLollipop.DrawerItem drawerItem;
    MegaApiAndroid megaApi;

    public ContactOptionsPanelListener(Context context){
        log("NodeOptionsPanelListener created");
        this.context = context;
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    @Override
    public void onClick(View v) {
        log("onClick ContactOptionsPanelListener");

        MegaUser selectedUser = ((ManagerActivityLollipop) context).getSelectedUser();

        if(selectedUser==null){
            log("The selected user is NULL");
            return;
        }
        switch(v.getId()){
            case R.id.contact_list_out_options:{
                ((ManagerActivityLollipop) context).hideContactOptionsPanel();
                break;
            }
            case R.id.contact_list_option_send_file_layout:{
                log("optionSendFile");
                ((ManagerActivityLollipop) context).hideContactOptionsPanel();
                List<MegaUser> user = new ArrayList<MegaUser>();
                user.add(selectedUser);
                NodeController nC = new NodeController(context);
                nC.pickFileToSend(user);
                break;
            }
            case R.id.contact_list_option_properties_layout:{
                log("optionProperties");
                ((ManagerActivityLollipop) context).hideContactOptionsPanel();
                Intent i = new Intent(context, ContactPropertiesActivityLollipop.class);
                i.putExtra("name", selectedUser.getEmail());
                context.startActivity(i);
                break;
            }
            case R.id.contact_list_option_share_layout:{
                log("optionShare");
                ((ManagerActivityLollipop) context).hideContactOptionsPanel();
                List<MegaUser> user = new ArrayList<MegaUser>();
                user.add(selectedUser);
                NodeController nC = new NodeController(context);
                nC.pickFolderToShare(user);
                break;
            }
            case R.id.contact_list_option_remove_layout:{
                log("Remove contact");
                ((ManagerActivityLollipop) context).hideContactOptionsPanel();
                ((ManagerActivityLollipop) context).removeContact(selectedUser);
                break;
            }
        }
    }

    public static void log(String message) {
        Util.log("ContactOptionsPanelListener", message);
    }
}
