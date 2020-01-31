package mega.privacy.android.app.lollipop.listeners;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.ContactsFragmentLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaStringList;
import nz.mega.sdk.MegaStringMap;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static nz.mega.sdk.MegaApiJava.USER_ATTR_ALIAS;
import static nz.mega.sdk.MegaApiJava.USER_ATTR_FIRSTNAME;
import static nz.mega.sdk.MegaApiJava.USER_ATTR_LASTNAME;

public class ContactNameListener implements MegaRequestListenerInterface {
    Context context;
    DatabaseHandler dbH;
    MegaApiAndroid megaApi;

    public ContactNameListener(Context context) {
        this.context = context;
        dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }
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
                    dbH.setContactName(firstName, request.getEmail());
                    updateView();
                }
                break;
            }
            case USER_ATTR_LASTNAME: {
                if (e.getErrorCode() == MegaError.API_OK) {
                    String lastName = request.getText();
                    dbH.setContactLastName(lastName, request.getEmail());
                    updateView();
                }
                break;
            }
            case USER_ATTR_ALIAS: {
                if (e.getErrorCode() == MegaError.API_OK) {
                    String nickname;

                    if (request.getType() == MegaRequest.TYPE_SET_ATTR_USER) {
                        nickname = request.getText();
                        dbH.setContactNickname(nickname, request.getNodeHandle());

                    } else if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER) {
                        nickname = request.getName();
                        if (nickname == null) {
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

                    notifyNicknameUpdate(request.getNodeHandle());

                } else if (e.getErrorCode() == MegaError.API_ENOENT) {
                    if (request.getType() == MegaRequest.TYPE_SET_ATTR_USER) {
                        dbH.setContactNickname(null, request.getNodeHandle());
                        notifyNicknameUpdate(request.getNodeHandle());
                    }
                } else {
                    logDebug("Error recovering, updating or removing the alias" + e.getErrorCode());
                }
                break;
            }
        }
    }

    private ArrayList<MegaContactAdapter> getContactsDBList() {
        ArrayList<MegaContactAdapter> visibleContacts = new ArrayList<>();
        ArrayList<MegaUser> contacts = megaApi.getContacts();
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                long contactHandle = contacts.get(i).getHandle();
                String fullName = getContactNameDB(megaApi, context, contactHandle);
                MegaContactAdapter megaContactAdapter = new MegaContactAdapter(getContactDB(context, contactHandle), contacts.get(i), fullName);
                visibleContacts.add(megaContactAdapter);
            }
        }
        return visibleContacts;

    }

    private void updateDBNickname(MegaStringMap map) {
        ArrayList<MegaContactAdapter> contactsDB = getContactsDBList();

        //No nicknames
        if (map == null || map.size() == 0) {
            if (contactsDB != null && !contactsDB.isEmpty()) {
                for (int i = 0; i < contactsDB.size(); i++) {
                    long contactDBHandle = contactsDB.get(i).getMegaUser().getHandle();
                    String nickname = getNicknameContact(context, contactDBHandle);
                    if (nickname != null) {
                        dbH.setContactNickname(null, contactDBHandle);
                        notifyNicknameUpdate(contactDBHandle);
                    }
                }
            }
            return;
        }

        //Some nicknames
        MegaStringList listHandles = map.getKeys();
        if (contactsDB != null && !contactsDB.isEmpty()) {
            for (int i = 0; i < contactsDB.size(); i++) {
                long contactDBHandle = contactsDB.get(i).getMegaUser().getHandle();
                String newNickname = null;
                for (int j = 0; j < listHandles.size(); j++) {
                    long userHandle = MegaApiJava.base64ToUserHandle(listHandles.get(j));
                    if (contactDBHandle == userHandle) {
                        newNickname = getNewNickname(map, listHandles.get(j));
                        break;
                    }
                }
                String oldNickname = contactsDB.get(i).getMegaContactDB().getNickname();
                if ((newNickname == null && oldNickname == null) || (newNickname != null && oldNickname != null && newNickname.equals(oldNickname))) {
                    continue;
                } else {
                    dbH.setContactNickname(newNickname, contactDBHandle);
                    notifyNicknameUpdate(contactDBHandle);
                }

            }
        }
    }

    private String getNewNickname(MegaStringMap map, String key) {
        String nicknameEncoded = map.get(key);
        try {
            byte[] decrypt = Base64.decode(nicknameEncoded, Base64.DEFAULT);
            return new String(decrypt, StandardCharsets.UTF_8);
        } catch (java.lang.Exception e) {
            return null;
        }
    }

    private void updateView() {
        if (context != null && context instanceof ManagerActivityLollipop) {
            ContactsFragmentLollipop cFLol = ((ManagerActivityLollipop) context).getContactsFragment();
            if (cFLol != null) {
                cFLol.updateView();
            }
        }
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) { }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) { }

    private void notifyNicknameUpdate(long userHandle) {
        Intent intent = new Intent(BROADCAST_ACTION_INTENT_FILTER_ALIAS);
        intent.putExtra(EXTRA_USER_HANDLE, userHandle);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
