package mega.privacy.android.app.snackbarListeners;

import android.content.Context;
import android.view.View;

import mega.privacy.android.app.lollipop.ManagerActivityLollipop;

public class SnackbarNavigateOption implements View.OnClickListener{

    Context context;

    public SnackbarNavigateOption(Context context) {

        this.context=context;
    }

    @Override
    public void onClick(View v) {
        //Intent to Settings

        if(context instanceof ManagerActivityLollipop){
            ((ManagerActivityLollipop)context).moveToSettingsSection();
        }
        else{

        }
    }
}
