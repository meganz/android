package mega.privacy.android.app.lollipop.listeners;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.utils.Constants;
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
            case R.id.avatar_list_out_options:{
                log("contact_list_out_options");
                ((ManagerActivityLollipop) context).hideAvatarOptionsPanel();
                break;
            }
            case R.id.avatar_list_choose_photo_layout:{
                log("option choose photo avatar");
                ((ManagerActivityLollipop) context).hideAvatarOptionsPanel();
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setType("image/*");
                ((ManagerActivityLollipop)context).startActivityForResult(Intent.createChooser(intent, null), Constants.CHOOSE_PICTURE_PROFILE_CODE);

                break;
            }
            case R.id.avatar_list_take_photo_layout:{
                log("option take photo avatar");
                ((ManagerActivityLollipop) context).hideAvatarOptionsPanel();
                AccountController aC = new AccountController(context);
                aC.takeProfilePicture();
                break;
            }
            case R.id.avatar_list_delete_layout:{
                log("option delete avatar");

                ((ManagerActivityLollipop) context).hideAvatarOptionsPanel();
                ((ManagerActivityLollipop) context).showConfirmationDeleteAvatar();

                break;
            }
        }
    }

    public static void log(String message) {
        Util.log("AvatarOptionsPanelListener", message);
    }
}
