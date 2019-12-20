package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.ContactsFragmentLollipop;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaStringList;
import nz.mega.sdk.MegaStringMap;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static nz.mega.sdk.MegaApiJava.USER_ATTR_ALIAS;
import static nz.mega.sdk.MegaApiJava.USER_ATTR_FIRSTNAME;
import static nz.mega.sdk.MegaApiJava.USER_ATTR_LASTNAME;

public class ContactNameListener implements MegaRequestListenerInterface {
    Context context;
    DatabaseHandler dbH;

    public ContactNameListener(Context context) {
        this.context = context;
        dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        switch (request.getParamType()) {
            case USER_ATTR_FIRSTNAME: {
                if (e.getErrorCode() == MegaError.API_OK) {
                    String firstName = request.getText();
                    if(request.getType() == MegaRequest.TYPE_SET_ATTR_USER) {
                        int rows = dbH.setContactName(firstName, request.getEmail());
                        logDebug("Rows affected: " + rows);
                    }
                    if (context != null && context instanceof ManagerActivityLollipop) {
                        ContactsFragmentLollipop cFLol = ((ManagerActivityLollipop) context).getContactsFragment();
                        if (cFLol != null) {
                            cFLol.updateView();
                        }
                    }
                }
                break;
            }
            case USER_ATTR_LASTNAME: {
                if (e.getErrorCode() == MegaError.API_OK) {
                    String lastName = request.getText();

                    if(request.getType() == MegaRequest.TYPE_SET_ATTR_USER) {
                        int rows = dbH.setContactLastName(lastName, request.getEmail());
                        logDebug("Rows affected: " + rows);
                    }
                    if (context != null && context instanceof ManagerActivityLollipop) {
                        ContactsFragmentLollipop cFLol = ((ManagerActivityLollipop) context).getContactsFragment();
                        if (cFLol != null) {
                            cFLol.updateView();
                        }
                    }
                }
                break;
            }
            case USER_ATTR_ALIAS: {
                if (e.getErrorCode() == MegaError.API_OK) {
                    String nickname = null;
                    if(request.getType() == MegaRequest.TYPE_SET_ATTR_USER){
                        nickname = request.getText();
                        int rows = dbH.setContactNickname(nickname, request.getNodeHandle());
                        logDebug("Rows affected: " + rows);
                    }else if(request.getType() == MegaRequest.TYPE_GET_ATTR_USER){
                        nickname = request.getName();
                        if(nickname == null){
                            updateDBNickname(request.getMegaStringMap());
                            break;
                        }
                    }

                    if (request.getType() == MegaRequest.TYPE_SET_ATTR_USER && context != null && context instanceof ContactInfoActivityLollipop) {
                        ContactInfoActivityLollipop contactInfoActivityLollipop = (ContactInfoActivityLollipop) context;
                        if (request.getText() == null) {
                            contactInfoActivityLollipop.showSnackbar(SNACKBAR_TYPE, context.getString(R.string.snackbar_nickname_removed), -1);
                        } else {
                            contactInfoActivityLollipop.showSnackbar(SNACKBAR_TYPE, context.getString(R.string.snackbar_nickname_added), -1);
                        }
                    }

                    logDebug("notify of a nickname updated");
                    notifyNicknameUpdate(request.getNodeHandle(), nickname);

                } else if (e.getErrorCode() == MegaError.API_ENOENT) {
                    logDebug("USER_ATTR_ALIAS::API_ENOENT");

                    if (request.getType() == MegaRequest.TYPE_SET_ATTR_USER) {
                        int rows = dbH.setContactNickname(null, request.getNodeHandle());
                        logDebug("Rows affected: " + rows);
                    }

                    logDebug("notify of a nickname removed");
                    notifyNicknameUpdate(request.getNodeHandle(), null);

                } else {
                    logDebug("Error recovering, updating or removing the alias" + e.getErrorCode());
                }
                break;
            }
        }
    }

    private void updateDBNickname(MegaStringMap map){
        if(map == null || map.size() == 0){
            return;
        }
        MegaStringList listHandles = map.getKeys();
        for (int i = 0; i < listHandles.size(); i++) {
            String nickname = map.get(listHandles.get(i));
            logDebug("*******************************************************");
//            try {
//                String nickString = Base64.decode(nickname, 0).toString();
//                logDebug("******************************** nickString =  "+nickString);
//            }catch (java.lang.Exception e){
//                logError("*************Error updating the nickname");
//                return;
//            }


            long userHandle = MegaApiJava.base64ToUserHandle(listHandles.get(i));
            logDebug("update dbh - nickname = "+nickname+", with userhandle = "+userHandle);
            int rows = dbH.setContactNickname(nickname, userHandle);
        }

        if (context != null && context instanceof ManagerActivityLollipop) {
            ContactsFragmentLollipop cFLol = ((ManagerActivityLollipop) context).getContactsFragment();
            if (cFLol != null) {
                logDebug("update view");
                cFLol.updateView();
            }
        }
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
    }

    private void notifyNicknameUpdate(long userHandle, String nickname) {
        logDebug("notifyNicknameUpdate = "+nickname);

        if (context != null) {
            if (context instanceof ManagerActivityLollipop) {
                ContactsFragmentLollipop cFLol = ((ManagerActivityLollipop) context).getContactsFragment();
                if (cFLol != null) {
                    cFLol.updateView();
                }
            }
        }

        Intent intent = new Intent(BROADCAST_ACTION_INTENT_FILTER_ALIAS);
        intent.putExtra(EXTRA_USER_HANDLE, userHandle);
        intent.putExtra(EXTRA_USER_NICKNAME, nickname);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
