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
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaUser;

public class ContactOptionsPanelListener implements View.OnClickListener {

    Context context;
    ManagerActivityLollipop.DrawerItem drawerItem;
    MegaApiAndroid megaApi;
    ContactController cC;

    public ContactOptionsPanelListener(Context context){
        log("ContactOptionsPanelListener created");
        this.context = context;
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
        cC = new ContactController(context);
    }

    @Override
    public void onClick(View v) {
        log("onClick ContactOptionsPanelListener");

        switch(v.getId()){
            case R.id.contact_list_out_options:{
                log("contact_list_out_options");
                ((ManagerActivityLollipop) context).hideContactOptionsPanel();
                break;
            }
            case R.id.contact_list_option_send_file_layout:{
                log("optionSendFile");
                MegaUser selectedUser = ((ManagerActivityLollipop) context).getSelectedUser();

                if(selectedUser==null){
                    log("The selected user is NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).hideContactOptionsPanel();
                List<MegaUser> user = new ArrayList<MegaUser>();
                user.add(selectedUser);
                ContactController cC = new ContactController(context);
                cC.pickFileToSend(user);
                break;
            }
            case R.id.contact_list_option_properties_layout:{
                log("optionProperties");
                MegaUser selectedUser = ((ManagerActivityLollipop) context).getSelectedUser();

                if(selectedUser==null){
                    log("The selected user is NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).hideContactOptionsPanel();
                Intent i = new Intent(context, ContactPropertiesActivityLollipop.class);
                i.putExtra("name", selectedUser.getEmail());
                context.startActivity(i);
                break;
            }
            case R.id.contact_list_option_share_layout:{
                log("optionShare");
                MegaUser selectedUser = ((ManagerActivityLollipop) context).getSelectedUser();

                if(selectedUser==null){
                    log("The selected user is NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).hideContactOptionsPanel();
                List<MegaUser> user = new ArrayList<MegaUser>();
                user.add(selectedUser);
                ContactController cC = new ContactController(context);
                cC.pickFolderToShare(user);
                break;
            }
            case R.id.contact_list_option_remove_layout:{
                log("Remove contact");
                MegaUser selectedUser = ((ManagerActivityLollipop) context).getSelectedUser();

                if(selectedUser==null){
                    log("The selected user is NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).hideContactOptionsPanel();
                ((ManagerActivityLollipop) context).showConfirmationRemoveContact(selectedUser);
                break;
            }
            case R.id.contact_list_option_reinvite_layout:{
                log("optionReinvite");
                MegaContactRequest selectedRequest = ((ManagerActivityLollipop) context).getSelectedRequest();
                if(selectedRequest==null){
                    log("The selected request is NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).hideContactOptionsPanel();
                cC.reinviteContact(selectedRequest);
                break;
            }
            case R.id.contact_list_option_delete_request_layout:{
                log("Remove Invitation");
                MegaContactRequest selectedRequest = ((ManagerActivityLollipop) context).getSelectedRequest();
                if(selectedRequest==null){
                    log("The selected request is NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).hideContactOptionsPanel();
                cC.removeInvitationContact(selectedRequest);
                break;
            }
            case R.id.contact_list_option_accept_layout:{
                log("option Accept");
                MegaContactRequest selectedRequest = ((ManagerActivityLollipop) context).getSelectedRequest();
                if(selectedRequest==null){
                    log("The selected request is NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).hideContactOptionsPanel();
                cC.acceptInvitationContact(selectedRequest);
                break;
            }
            case R.id.contact_list_option_decline_layout:{
                log("Remove Invitation");
                MegaContactRequest selectedRequest = ((ManagerActivityLollipop) context).getSelectedRequest();
                if(selectedRequest==null){
                    log("The selected request is NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).hideContactOptionsPanel();
                cC.declineInvitationContact(selectedRequest);
                break;
            }
            case R.id.contact_list_option_ignore_layout:{
                log("Ignore Invitation");
                MegaContactRequest selectedRequest = ((ManagerActivityLollipop) context).getSelectedRequest();
                if(selectedRequest==null){
                    log("The selected request is NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).hideContactOptionsPanel();
                cC.ignoreInvitationContact(selectedRequest);
                break;
            }
        }
    }

    public static void log(String message) {
        Util.log("ContactOptionsPanelListener", message);
    }
}
