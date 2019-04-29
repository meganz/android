package mega.privacy.android.app.lollipop.listeners;

import android.app.Activity;
import android.content.Context;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;


/**
 * Created by mega on 19/09/18.
 */

public class CopyAndSendToChatListener implements MegaRequestListenerInterface {

    Context context;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    int counter = 0;
    MegaNode parentNode;
    ArrayList<MegaNode> nodesCopied = new ArrayList<>();

    public CopyAndSendToChatListener(Context context) {
        super();
        this.context = context;

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
        }

        counter = 0;
        parentNode = megaApi.getNodeByPath("/" + Constants.CHAT_FOLDER);
    }

    public void copyNode(MegaNode node) {
        megaApi.copyNode(node, parentNode, this);
    }

    public void copyNodes (ArrayList<MegaNode> nodes, ArrayList<MegaNode> ownerNodes) {
        nodesCopied.addAll(ownerNodes);
        counter = nodes.size();
        for (int i=0; i<nodes.size(); i++) {
            copyNode(nodes.get(i));
        }
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        log("onRequestStart");
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        log("onRequestUpdate");
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestFinish");
        if (request.getType() == MegaRequest.TYPE_COPY){
            counter --;
            if (e.getErrorCode() == MegaError.API_OK){
                nodesCopied.add(megaApi.getNodeByHandle(request.getNodeHandle()));
                if (counter == 0){
                    if (nodesCopied != null) {
                        NodeController nC = new NodeController(context);
                        nC.selectChatsToSendNodes(nodesCopied);
                    }
                }
            }
            else {
                log("MegaRequest.TYPE_COPY error: " +e.getErrorString());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestTemporaryError");
    }

    private static void log(String log) {
        Util.log("CopyAndSendToChatListener", log);
    }
}
