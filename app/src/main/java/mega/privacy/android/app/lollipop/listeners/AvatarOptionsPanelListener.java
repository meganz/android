package mega.privacy.android.app.lollipop.listeners;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

public class AvatarOptionsPanelListener implements View.OnClickListener {

    Context context;
    ManagerActivityLollipop.DrawerItem drawerItem;
    MegaApiAndroid megaApi;
    AccountController aC;

    public AvatarOptionsPanelListener(Context context){
        log("AvatarOptionsPanelListener created");
        this.context = context;
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
        aC = new AccountController(context);
    }

    @Override
    public void onClick(View v) {
        log("onClick AvatarOptionsPanelListener");

        switch(v.getId()){
            case R.id.contact_list_out_options:{
                log("contact_list_out_options");
                ((ManagerActivityLollipop) context).hideContactOptionsPanel();
                break;
            }
            case R.id.avatar_list_choose_photo_layout:{
                log("option choose photo avatar");
//                MegaUser selectedUser = ((ManagerActivityLollipop) context).getSelectedUser();
//
//                if(selectedUser==null){
//                    log("The selected user is NULL");
//                    return;
//                }
//                ((ManagerActivityLollipop) context).hideContactOptionsPanel();
//                List<MegaUser> user = new ArrayList<MegaUser>();
//                user.add(selectedUser);
//                ContactController cC = new ContactController(context);
//                cC.pickFileToSend(user);
                break;
            }
            case R.id.avatar_list_take_photo_layout:{
                log("option take photo avatar");
//                MegaUser selectedUser = ((ManagerActivityLollipop) context).getSelectedUser();
//
//                if(selectedUser==null){
//                    log("The selected user is NULL");
//                    return;
//                }
//                ((ManagerActivityLollipop) context).hideContactOptionsPanel();
//                Intent i = new Intent(context, ContactPropertiesActivityLollipop.class);
//                i.putExtra("name", selectedUser.getEmail());
//                context.startActivity(i);
                break;
            }
            case R.id.avatar_list_delete_layout:{
                log("option delete avatar");
//                MegaUser selectedUser = ((ManagerActivityLollipop) context).getSelectedUser();
//
//                if(selectedUser==null){
//                    log("The selected user is NULL");
//                    return;
//                }
//                ((ManagerActivityLollipop) context).hideContactOptionsPanel();
//                List<MegaUser> user = new ArrayList<MegaUser>();
//                user.add(selectedUser);
//                ContactController cC = new ContactController(context);
//                cC.pickFolderToShare(user);
                break;
            }
        }
    }

    public static void log(String message) {
        Util.log("AvatarOptionsPanelListener", message);
    }
}
