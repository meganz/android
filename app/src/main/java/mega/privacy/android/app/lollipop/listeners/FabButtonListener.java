package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.view.View;

import mega.privacy.android.app.lollipop.ContactPropertiesActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Util;

public class FabButtonListener implements FloatingActionButton.OnClickListener{

    Context context;
    ManagerActivityLollipop.DrawerItem drawerItem;

    public FabButtonListener(Context context){
        log("FabButtonListener created");
        this.context = context;
    }

    @Override
    public void onClick(View v) {
        log("onClick FabButtonListener");

        if(context instanceof ManagerActivityLollipop){
            drawerItem = ((ManagerActivityLollipop)context).getDrawerItem();
            switch (drawerItem){
                case CLOUD_DRIVE:
                case SHARED_ITEMS:{
                    log("Cloud Drive SECTION");
                    ((ManagerActivityLollipop)context).showUploadPanel();
                    break;
                }
                case CONTACTS:{
                    ((ManagerActivityLollipop)context).chooseAddContactDialog();
                    break;
                }
            }
        }
        else if(context instanceof ContactPropertiesActivityLollipop){
            ((ContactPropertiesActivityLollipop)context).showUploadPanel();
        }

    }

    public static void log(String message) {
        Util.log("FabButtonListener", message);
    }
}
