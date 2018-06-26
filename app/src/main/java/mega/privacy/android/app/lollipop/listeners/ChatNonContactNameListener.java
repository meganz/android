package mega.privacy.android.app.lollipop.listeners;


import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaChatLollipopAdapter;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaListChatLollipopAdapter;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;

public class ChatNonContactNameListener implements MegaChatRequestListenerInterface {

    Context context;
    RecyclerView.ViewHolder holder;
    RecyclerView.Adapter adapter;
    boolean isUserHandle;
    DatabaseHandler dbH;
    String firstName;
    String lastName;
    String mail;
    long userHandle;
    boolean receivedFirstName = false;
    boolean receivedLastName = false;
    boolean receivedEmail = false;
    MegaApiAndroid megaApi;

    public ChatNonContactNameListener(Context context, RecyclerView.ViewHolder holder, RecyclerView.Adapter adapter, long userHandle) {
        this.context = context;
        this.holder = holder;
        //MegaChatLollipopAdapter.ViewHolderMessageChat holder
        //MegaChatLollipopAdapter adapter
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

        if (e.getErrorCode() == MegaError.API_OK){
            if(request.getType()==MegaChatRequest.TYPE_GET_FIRSTNAME){
                log("->First name received");
                firstName = request.getText();
                receivedFirstName = true;
                if(firstName!=null){
                    if(!firstName.trim().isEmpty()){
                        dbH.setNonContactFirstName(firstName, request.getUserHandle()+"");
                    }
                }
                if(holder instanceof MegaListChatLollipopAdapter.ViewHolderChatList){
                    updateAdapter(((MegaListChatLollipopAdapter.ViewHolderChatList)holder).currentPosition);
                }
                else if(holder instanceof MegaChatLollipopAdapter.ViewHolderMessageChat){
                    log("Update holder: "+((MegaChatLollipopAdapter.ViewHolderMessageChat) holder).getUserHandle());
                    updateAdapter(((MegaChatLollipopAdapter.ViewHolderMessageChat) holder).getCurrentPosition());
                }
            }
            else if(request.getType()==MegaChatRequest.TYPE_GET_LASTNAME){
                log("->Last name received");
                lastName = request.getText();
                receivedLastName = true;
                if(lastName!=null){
                    if(!lastName.trim().isEmpty()){
                        dbH.setNonContactFirstName(lastName, request.getUserHandle()+"");
                    }
                }

                if(holder instanceof MegaListChatLollipopAdapter.ViewHolderChatList){
                    updateAdapter(((MegaListChatLollipopAdapter.ViewHolderChatList)holder).currentPosition);
                }
                else if(holder instanceof MegaChatLollipopAdapter.ViewHolderMessageChat){
                    log("Update holder: "+((MegaChatLollipopAdapter.ViewHolderMessageChat) holder).getUserHandle());
                    updateAdapter(((MegaChatLollipopAdapter.ViewHolderMessageChat) holder).getCurrentPosition());
                }
            }
            else if(request.getType()==MegaChatRequest.TYPE_GET_EMAIL){
                log("->Email received");
                mail = request.getText();
                receivedEmail = true;
                if(mail!=null){
                    if(!mail.trim().isEmpty()){
                        dbH.setNonContactEmail(mail, request.getUserHandle()+"");
                    }
                }
                if(holder instanceof MegaListChatLollipopAdapter.ViewHolderChatList){
                    updateAdapter(((MegaListChatLollipopAdapter.ViewHolderChatList)holder).currentPosition);
                }
                else if(holder instanceof MegaChatLollipopAdapter.ViewHolderMessageChat){
                    log("Update holder: "+((MegaChatLollipopAdapter.ViewHolderMessageChat) holder).getUserHandle());
                    updateAdapter(((MegaChatLollipopAdapter.ViewHolderMessageChat) holder).getCurrentPosition());
                }
            }
        }
        else{
            log("ERROR: requesting: "+request.getRequestString());
        }
    }

    public void updateAdapter(int position) {
        log("updateAdapter: "+position);
        if(receivedFirstName&&receivedLastName&&receivedEmail){

            if(adapter instanceof MegaChatLollipopAdapter){
                ((MegaChatLollipopAdapter)adapter).notifyItemChanged(position);
            }
            else if(adapter instanceof MegaListChatLollipopAdapter){
                ((MegaListChatLollipopAdapter)adapter).updateNonContactName(position, this.userHandle);
            }

            receivedFirstName = false;
            receivedLastName = false;
            receivedEmail = false;
        }
        else{
            log("NOT updateAdapter:"+receivedFirstName+":"+receivedLastName+":"+receivedEmail);
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }
}