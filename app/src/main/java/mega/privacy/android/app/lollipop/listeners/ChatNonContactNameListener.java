package mega.privacy.android.app.lollipop.listeners;


import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaChatLollipopAdapter;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaListChatLollipopAdapter;
import mega.privacy.android.app.utils.LogUtil;
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
    boolean isPreview = false;
    int pos;

    public ChatNonContactNameListener(Context context, RecyclerView.ViewHolder holder, RecyclerView.Adapter adapter, long userHandle, boolean isPreview) {
        this.context = context;
        this.holder = holder;
        this.adapter = adapter;
        this.isUserHandle = true;
        this.userHandle = userHandle;
        this.isPreview = isPreview;

        dbH = DatabaseHandler.getDbHandler(context);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    public ChatNonContactNameListener(Context context, RecyclerView.ViewHolder holder, RecyclerView.Adapter adapter, long userHandle, boolean isPreview, int pos) {
        this.context = context;
        this.holder = holder;
        this.adapter = adapter;
        this.isUserHandle = true;
        this.userHandle = userHandle;
        this.isPreview = isPreview;
        this.pos = pos;

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

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        LogUtil.logDebug("onRequestFinish()");

        if (e.getErrorCode() == MegaError.API_OK){
            if (adapter == null) {
                return;
            }

            if(adapter instanceof MegaChatLollipopAdapter && holder == null){
                LogUtil.logWarning("holder is NULL");
                holder = ((MegaChatLollipopAdapter)adapter).queryIfHolderNull(pos);
                if (holder == null) {
                    LogUtil.logWarning("holder is NULL");
                    return;
                }
            }
            else {
                LogUtil.logWarning("Other adapter holder is NULL");
            }

            if(request.getType()==MegaChatRequest.TYPE_GET_FIRSTNAME){
                LogUtil.logDebug("First name received");
                firstName = request.getText();
                receivedFirstName = true;
                if(firstName!=null&& !firstName.trim().isEmpty()){
                    dbH.setNonContactFirstName(firstName, request.getUserHandle()+"");
                    updateAdapter(holder.getAdapterPosition());
                }
            }
            else if(request.getType()==MegaChatRequest.TYPE_GET_LASTNAME){
                LogUtil.logDebug("Last name received");
                lastName = request.getText();
                receivedLastName = true;
                if(lastName!=null && !lastName.trim().isEmpty()){
                    dbH.setNonContactLastName(lastName, request.getUserHandle()+"");
                    updateAdapter(holder.getAdapterPosition());
                }
            }
            else if(request.getType()==MegaChatRequest.TYPE_GET_EMAIL){
                LogUtil.logDebug("Email received");
                mail = request.getText();
                receivedEmail = true;
                if(mail!=null && !mail.trim().isEmpty()){
                    dbH.setNonContactEmail(mail, request.getUserHandle()+"");
                    updateAdapter(holder.getAdapterPosition());
                }
            }
        }
        else{
            LogUtil.logError("ERROR: requesting: " + request.getRequestString());
        }
    }

    public void updateAdapter(int position) {
        LogUtil.logDebug("position: " + position);
        if ((!isPreview && receivedFirstName && receivedLastName && receivedEmail)
            || (isPreview && receivedFirstName && receivedLastName)){
            LogUtil.logDebug("updateAdapter");
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
            LogUtil.logWarning("NOT updateAdapter: " + receivedFirstName + ":" + receivedLastName + ":" + receivedEmail);
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }
}