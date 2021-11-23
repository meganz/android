package mega.privacy.android.app.lollipop.listeners;


import android.content.Context;
import android.graphics.Bitmap;

import java.io.File;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaRichLinkMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.getFolderInfo;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;

public class ChatLinkInfoListener implements MegaRequestListenerInterface, MegaChatRequestListenerInterface {

    Context context;
    long msgId;
    MegaApiAndroid megaApi;
    MegaApiAndroid megaApiFolder;
    AndroidMegaRichLinkMessage richLinkMessage = null;

    public ChatLinkInfoListener(Context context, long msgId, MegaApiAndroid megaApi) {
        this.context = context;
        this.msgId = msgId;
        this.megaApi = megaApi;
        this.megaApiFolder = null;
    }

    public ChatLinkInfoListener(Context context, long msgId, MegaApiAndroid megaApi, MegaApiAndroid megaApiFolder) {
        this.context = context;
        this.msgId = msgId;
        this.megaApi = megaApi;
        this.megaApiFolder = megaApiFolder;
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        logDebug("onRequestFinish()");
        if (e.getErrorCode() == MegaError.API_OK){

            if (request.getType() == MegaRequest.TYPE_GET_ATTR_FILE){
                if (e.getErrorCode() == MegaError.API_OK){
                    ((ChatActivityLollipop) context).setRichLinkImage(msgId);
                }
            }
            else if (request.getType() == MegaRequest.TYPE_GET_PUBLIC_NODE) {
                MegaNode document = request.getPublicMegaNode();
                String link = request.getLink();

                richLinkMessage = new AndroidMegaRichLinkMessage(link, document);

                ((ChatActivityLollipop) context).setRichLinkInfo(msgId, richLinkMessage);

                //Get preview of file
                if (document.isFile()) {
                    Bitmap thumb = null;
                    thumb = getThumbnailFromCache(document);
                    if (thumb == null) {
                        thumb = getThumbnailFromFolder(document, context);
                        if (thumb == null) {
                            if (document.hasThumbnail()) {
                                File previewFile = new File(getThumbFolder(context), document.getBase64Handle() + ".jpg");
                                megaApi.getThumbnail(document, previewFile.getAbsolutePath(), this);
                            }
                        }
                    }
                }
            }
            else if (request.getType() == MegaRequest.TYPE_LOGIN) {

                String link = request.getLink();

                richLinkMessage = new AndroidMegaRichLinkMessage(link, null);

                logDebug("Logged in. Setting account auth token for folder links.");
                megaApiFolder.setAccountAuth(megaApi.getAccountAuth());

                megaApiFolder.fetchNodes(this);

                // Get cookies settings after login.
                MegaApplication.getInstance().checkEnabledCookies();
            } else if (request.getType() == MegaRequest.TYPE_FETCH_NODES) {
                MegaNode rootNode = megaApiFolder.getRootNode();

                if (rootNode != null && !request.getFlag()) {
                    richLinkMessage.setNode(rootNode);
                    String content = getFolderInfo(megaApiFolder.getNumChildFolders(rootNode),
                            megaApiFolder.getNumChildFiles(rootNode));

                    richLinkMessage.setFolderContent(content);

                    ((ChatActivityLollipop) context).setRichLinkInfo(msgId, richLinkMessage);
                }

                megaApiFolder.logout();
            }
        }
        else{
            logError("ERROR - Info of the public node not recovered");

            if (request.getType() == MegaRequest.TYPE_LOGIN) {
                if(e.getErrorCode() == MegaError.API_EINCOMPLETE){
                    String link = request.getLink();

                    richLinkMessage = new AndroidMegaRichLinkMessage(link, null);
                    richLinkMessage.setFile(false);
                    ((ChatActivityLollipop) context).setRichLinkInfo(msgId, richLinkMessage);
                }
                else if(e.getErrorCode() == MegaError.API_EARGS) {
                    String link = request.getLink();

                    richLinkMessage = new AndroidMegaRichLinkMessage(link, null);
                    richLinkMessage.setFile(false);
                    ((ChatActivityLollipop) context).setRichLinkInfo(msgId, richLinkMessage);
                }
                megaApiFolder.logout();
            }
            else if (request.getType() == MegaRequest.TYPE_GET_PUBLIC_NODE) {
                if(e.getErrorCode() == MegaError.API_EINCOMPLETE){
                    String link = request.getLink();

                    richLinkMessage = new AndroidMegaRichLinkMessage(link, null);
                    richLinkMessage.setFile(true);
                    ((ChatActivityLollipop) context).setRichLinkInfo(msgId, richLinkMessage);
                }
                else if(e.getErrorCode() == MegaError.API_EARGS) {
                    String link = request.getLink();

                    richLinkMessage = new AndroidMegaRichLinkMessage(link, null);
                    richLinkMessage.setFile(true);
                    ((ChatActivityLollipop) context).setRichLinkInfo(msgId, richLinkMessage);
                }
            }
            else{
                if(megaApiFolder!=null){
                    megaApiFolder.logout();
                }
            }
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
        if (request.getType() == MegaChatRequest.TYPE_LOAD_PREVIEW){
            String link = request.getLink();
            if (e.getErrorCode() == MegaError.API_OK){

                richLinkMessage = new AndroidMegaRichLinkMessage(link, request.getText(), request.getNumber());

                ((ChatActivityLollipop) context).setRichLinkInfo(msgId, richLinkMessage);
            }
            else{
                //Invalid link
                richLinkMessage = new AndroidMegaRichLinkMessage(link, "", -1);
                ((ChatActivityLollipop) context).setRichLinkInfo(msgId, richLinkMessage);

            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }
}
