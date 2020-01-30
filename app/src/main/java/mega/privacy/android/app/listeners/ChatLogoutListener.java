package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;

import static mega.privacy.android.app.utils.Util.*;

public class ChatLogoutListener extends ChatBaseListener{

    public ChatLogoutListener(Context context) {
        super(context);

        if (context instanceof LoginActivityLollipop) {
            MegaApplication.getInstance().setIsLogginRunning(true);
        }
    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        if (request.getType() != MegaChatRequest.TYPE_LOGOUT) return;

//        Code for LoginFragment
        MegaApplication.getInstance().disableMegaChatApi();
        resetAndroidLogger();
    }
}
