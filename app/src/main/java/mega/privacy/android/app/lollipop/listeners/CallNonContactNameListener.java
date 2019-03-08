package mega.privacy.android.app.lollipop.listeners;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.GroupCallAdapter;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;

public class CallNonContactNameListener implements MegaChatRequestListenerInterface {

    Context context;
    DatabaseHandler dbH;
    String mail;
    long peerId;
    boolean isAvatar;
    String name;
    MegaApiAndroid megaApi;
    RecyclerView.ViewHolder holder;
    RecyclerView.Adapter adapter;

    public CallNonContactNameListener(Context context, long peerId, boolean isAvatar) {
        this.context = context;
        this.peerId = peerId;
        this.isAvatar = isAvatar;
        dbH = DatabaseHandler.getDbHandler(context);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    public CallNonContactNameListener(Context context, RecyclerView.ViewHolder holder, RecyclerView.Adapter adapter, long peerId, String fullName) {
        this.context = context;
        this.peerId = peerId;
        this.name = fullName;
        if(adapter instanceof GroupCallAdapter){
            this.holder = (GroupCallAdapter.ViewHolderGroupCall) holder;
            this.adapter = (GroupCallAdapter) adapter;
        }
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) { }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) { }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("onRequestFinish()");
        if(request.getType()==MegaChatRequest.TYPE_GET_EMAIL){
            if (e.getErrorCode() == MegaError.API_OK){
                if(context instanceof ChatCallActivity){
                    mail = request.getText();
                    if(mail!=null){
                        if(!mail.trim().isEmpty()){
                            dbH.setNonContactEmail(mail, request.getUserHandle()+"");
                        }
                    }
                    if(holder instanceof GroupCallAdapter.ViewHolderGroupCall){
                        ((GroupCallAdapter) adapter).setProfile(peerId, name, mail, (GroupCallAdapter.ViewHolderGroupCall)holder);
                    }else{
                        if(isAvatar){
                            ((ChatCallActivity)context).setProfilePeerSelected(peerId, mail);
                        }else{
                            ((ChatCallActivity)context).updateNonContactName(peerId, mail);
                        }
                    }
                }
            }else{
                log("ERROR: TYPE_GET_EMAIL: "+request.getRequestString());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {}

    private static void log(String log) {
        Util.log("CallNonContactNameListener", log);
    }
}