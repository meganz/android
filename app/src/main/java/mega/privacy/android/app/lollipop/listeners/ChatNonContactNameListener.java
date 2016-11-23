package mega.privacy.android.app.lollipop.listeners;


import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaChatLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.MegaListChatLollipopAdapter;
import mega.privacy.android.app.lollipop.megachat.NonContactInfo;
import mega.privacy.android.app.lollipop.megachat.RecentChatsFragmentLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

public class ChatNonContactNameListener implements MegaChatRequestListenerInterface {

    Context context;
    MegaChatLollipopAdapter.ViewHolderMessageChatList holder;
    MegaChatLollipopAdapter adapter;
    boolean isUserHandle;
    DatabaseHandler dbH;
    String firstName;
    String lastName;
    String fullName;
    long userHandle;
    boolean receivedFirstName = false;
    boolean receivedLastName = false;
    MegaApiAndroid megaApi;

    public ChatNonContactNameListener(Context context, MegaChatLollipopAdapter.ViewHolderMessageChatList holder, MegaChatLollipopAdapter adapter, long userHandle) {
        this.context = context;
//        this.holder = holder;
        this.adapter = adapter;
        this.isUserHandle = true;
        this.userHandle = userHandle;
        dbH = DatabaseHandler.getDbHandler(context);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    public ChatNonContactNameListener(Context context) {
        this.context = context;
        dbH = DatabaseHandler.getDbHandler(context);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    private static void log(String log) {
        Util.log("ChatNonContactNameListener", log);
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("onRequestFinish()");

        if(context instanceof ManagerActivityLollipop){
            if (e.getErrorCode() == MegaError.API_OK){
                if(request.getType()==MegaChatRequest.TYPE_GET_FIRSTNAME){
                    log("ManagerActivityLollipop->First name received");
                    firstName = request.getText();
                    receivedFirstName = true;
                    addToDB(request.getUserHandle());
                }
                else if(request.getType()==MegaChatRequest.TYPE_GET_LASTNAME){
                    log("ManagerActivityLollipop->Last name received");
                    lastName = request.getText();
                    receivedLastName = true;
                    addToDB(request.getUserHandle());
                }
            }
            else{
                log("ManagerActivityLollipop->From REcentChatsFragment Error asking for name!");
            }
        }
        else{
            if (e.getErrorCode() == MegaError.API_OK){
                if(request.getType()==MegaChatRequest.TYPE_GET_FIRSTNAME){
                    log("MEgaChatAdapter->First name received");
                    if(userHandle==request.getUserHandle()){
                        log("Match!");
                        firstName = request.getText();
                        receivedFirstName = true;
                        addToDB(request.getUserHandle());
                        log("Update holder: "+holder.getUserHandle());
                        updateAdapter(holder.getCurrentPosition());
                    }
                }
                else if(request.getType()==MegaChatRequest.TYPE_GET_LASTNAME){
                    log("MEgaChatAdapter->Update lastname: "+holder.getCurrentPosition());

                    if(userHandle==request.getUserHandle()) {
                        log("MEgaChatAdapter->Match!");
                        lastName = request.getText();
                        receivedLastName = true;
                        addToDB(request.getUserHandle());
                        log("Update holder: "+holder.getUserHandle());
                        updateAdapter(holder.getCurrentPosition());
                    }
                }
            }
            else{
                log("Error asking for name!");
            }
        }

    }

    public void addToDB(long userHandle){
        log("addTODB: "+userHandle);
        if(receivedFirstName&&receivedLastName){

            if(lastName==null){
                lastName="";
            }
            if(lastName == null){
                lastName="";
            }

            if (firstName.trim().length() <= 0){
                fullName = lastName;
            }
            else{
                fullName = firstName + " " + lastName;
            }

//            String userHandleString = megaApi.userHandleToBase64(userHandle);
            NonContactInfo check = dbH.findNonContactByHandle(userHandle+"");

            if(check==null){
                NonContactInfo nonContact = new NonContactInfo(userHandle+"", fullName);
                log("Insert into NON contant table: "+fullName);
                dbH.setNonContact(nonContact);
            }
            else{
                log("The non contact already exists");
//                dbH.setNonContactFullName(fullName, userHandle+"");
            }

            receivedFirstName = false;
            receivedLastName = false;
        }
    }

    public void updateAdapter(int position) {
        log("updateAdapter: "+position);
        adapter.notifyItemChanged(position);
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }
}