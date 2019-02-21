package mega.privacy.android.app.lollipop.listeners;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import mega.privacy.android.app.lollipop.megachat.calls.InfoPeerGroupCall;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaChatLollipopAdapter;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaListChatLollipopAdapter;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;


public class CallNonContactNameListener implements MegaChatRequestListenerInterface {

    Context context;
    ArrayList<InfoPeerGroupCall> peers;
    boolean isUserHandle;
    DatabaseHandler dbH;
    String firstName;
    String lastName;
    String mail;
    long peerId;
    boolean receivedFirstName = false;
    boolean receivedLastName = false;
    boolean receivedEmail = false;
    MegaApiAndroid megaApi;

    public CallNonContactNameListener(Context context, long peerId) {
        this.context = context;
        this.isUserHandle = true;
        this.peerId = peerId;
        dbH = DatabaseHandler.getDbHandler(context);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    public CallNonContactNameListener(Context context) {
        this.context = context;
        dbH = DatabaseHandler.getDbHandler(context);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    private static void log(String log) {
        Util.log("CallNonContactNameListener", log);
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

        if (e.getErrorCode() == MegaError.API_OK){
            if(request.getType()==MegaChatRequest.TYPE_GET_FIRSTNAME){
                firstName = request.getText();
                receivedFirstName = true;
                if(firstName!=null){
                    if(!firstName.trim().isEmpty()){
                        dbH.setNonContactFirstName(firstName, request.getUserHandle()+"");
                    }
                }
                log("onRequestFinish(): firstName = "+firstName);

                updateName(firstName);

            }else if(request.getType()==MegaChatRequest.TYPE_GET_LASTNAME){
                log("->Last name received");
                lastName = request.getText();
                receivedLastName = true;
                if(lastName!=null){
                    if(!lastName.trim().isEmpty()){
                        dbH.setNonContactFirstName(lastName, request.getUserHandle()+"");
                    }
                }
                log("onRequestFinish(): lastName = "+lastName);

                updateName(lastName);

            }else if(request.getType()==MegaChatRequest.TYPE_GET_EMAIL){
                mail = request.getText();
                receivedEmail = true;
                if(mail!=null){
                    if(!mail.trim().isEmpty()){
                        dbH.setNonContactEmail(mail, request.getUserHandle()+"");
                    }
                }
                log("onRequestFinish(): mail = "+mail);

                updateName(mail);
            }
        }
        else{
            log("ERROR: requesting: "+request.getRequestString());
        }
    }

    public void updateName(String name){
         if(receivedFirstName&&receivedLastName&&receivedEmail){
             ((ChatCallActivity)context).updateNonContactName(this.peerId, name);
            receivedFirstName = false;
            receivedLastName = false;
            receivedEmail = false;
         }

    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }
}