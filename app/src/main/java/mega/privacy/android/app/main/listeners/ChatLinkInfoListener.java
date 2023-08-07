package mega.privacy.android.app.main.listeners;


import android.content.Context;

import mega.privacy.android.data.model.chat.AndroidMegaRichLinkMessage;
import mega.privacy.android.app.main.megachat.ChatActivity;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import timber.log.Timber;

public class ChatLinkInfoListener implements MegaRequestListenerInterface, MegaChatRequestListenerInterface {

    Context context;
    long msgId;
    MegaApiAndroid megaApi;
    AndroidMegaRichLinkMessage richLinkMessage = null;

    public ChatLinkInfoListener(Context context, long msgId, MegaApiAndroid megaApi) {
        this.context = context;
        this.msgId = msgId;
        this.megaApi = megaApi;
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        Timber.d("onRequestFinish()");
        if (e.getErrorCode() == MegaError.API_OK) {

            if (request.getType() == MegaRequest.TYPE_GET_ATTR_FILE) {
                if (e.getErrorCode() == MegaError.API_OK) {
                    ((ChatActivity) context).setRichLinkImage(msgId);
                }
            }
        } else {
            Timber.e("ERROR - Info of the public node not recovered");
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        if (request.getType() == MegaChatRequest.TYPE_LOAD_PREVIEW) {
            String link = request.getLink();
            if (e.getErrorCode() == MegaError.API_OK) {

                richLinkMessage = new AndroidMegaRichLinkMessage(link, request.getText(), request.getNumber());

                ((ChatActivity) context).setRichLinkInfo(msgId, richLinkMessage);
            } else {
                //Invalid link
                richLinkMessage = new AndroidMegaRichLinkMessage(link, "", -1);
                ((ChatActivity) context).setRichLinkInfo(msgId, richLinkMessage);

            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }
}
