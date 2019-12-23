package mega.privacy.android.app.lollipop.listeners;

import android.app.Activity;
import android.content.Context;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.interfaces.MyChatFilesExisitListener;
import mega.privacy.android.app.listeners.SetAttrUserListener;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static nz.mega.sdk.MegaApiJava.USER_ATTR_MY_CHAT_FILES_FOLDER;

public class CopyAndSendToChatListener implements MegaRequestListenerInterface, MyChatFilesExisitListener<ArrayList<MegaNode>> {

    private Context context;

    private MegaApiAndroid megaApi;

    private long idChat = -1;
    private int counter = 0;
    private MegaNode parentNode;
    private ArrayList<MegaNode> nodesCopied = new ArrayList<>();
    // The list to preserve node is not owned by user
    private ArrayList<MegaNode> preservedNotOwnerNode;
    private long[] idChats;

    public CopyAndSendToChatListener(Context context) {
        initListener(context);
    }

    public CopyAndSendToChatListener(Context context, long idChat) {
        initListener(context);
        this.idChat = idChat;
    }

    public CopyAndSendToChatListener(Context context, long[] idChats) {
        super();
        initListener(context);
        this.idChats = idChats;
    }

    void initListener(Context context) {
        this.context = context;

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }

        counter = 0;
    }

    public void copyNode(MegaNode node) {
        megaApi.copyNode(node, parentNode, this);
    }

    public void copyNodes(ArrayList<MegaNode> nodes, ArrayList<MegaNode> ownerNodes) {
        nodesCopied.addAll(ownerNodes);
        counter = nodes.size();
        storedUnhandledData(nodes);
        megaApi.getMyChatFilesFolder(this);
    }

    private long[] getNodeHandles() {
        long[] handles = new long[nodesCopied.size()];
        for (int i = 0; i < nodesCopied.size(); i++) {
            handles[i] = nodesCopied.get(i).getHandle();
        }

        return handles;
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        logDebug("onRequestFinish: " + request.getRequestString());
        if (request.getType() == MegaRequest.TYPE_COPY) {
            counter--;
            if (e.getErrorCode() == MegaError.API_OK) {
                nodesCopied.add(megaApi.getNodeByHandle(request.getNodeHandle()));
                if (counter == 0) {
                    if (idChats != null && idChats.length > 0 && context instanceof ManagerActivityLollipop) {
                        ((ManagerActivityLollipop) context).sendFilesToChats(null, idChats, getNodeHandles());
                    } else if (idChat == -1) {
                        NodeController nC = new NodeController(context);
                        nC.selectChatsToSendNodes(nodesCopied);
                    } else if (context instanceof ChatActivityLollipop) {
                        for (MegaNode node : nodesCopied) {
                            ((ChatActivityLollipop) context).retryNodeAttachment(node.getHandle());
                        }
                    } else if (context instanceof ContactInfoActivityLollipop) {
                        ((ContactInfoActivityLollipop) context).sendFilesToChat(getNodeHandles(), idChat);
                    }
                }
            } else {
                logWarning("TYPE_COPY error: " + e.getErrorString());
            }
        } else if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER && request.getName().equals(context.getString(R.string.my_chat_files_folder))) {
            if (e.getErrorCode() == MegaError.API_OK) {
                megaApi.setMyChatFilesFolder(request.getNodeHandle(), new SetAttrUserListener(context));
                proceedWithHandleStoredData(request.getNodeHandle());
            } else {
                logError("Error creating \"My chat files\" folder");
            }
        } else if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER && request.getParamType() == USER_ATTR_MY_CHAT_FILES_FOLDER) {
            boolean myChatFilesFolderExists = false;

            if (e.getErrorCode() == MegaError.API_OK) {
                myChatFilesFolderExists = true;
            } else if (e.getErrorCode() == MegaError.API_ENOENT) {
                MegaNode myChatFolderNode = api.getNodeByPath(CHAT_FOLDER, api.getRootNode());
                if (myChatFolderNode != null) {
                    megaApi.renameNode(myChatFolderNode, context.getString(R.string.my_chat_files_folder), this);
                    megaApi.setMyChatFilesFolder(myChatFolderNode.getHandle(), new SetAttrUserListener(context));
                    myChatFilesFolderExists = true;
                } else {
                    megaApi.createFolder(context.getString(R.string.my_chat_files_folder), megaApi.getRootNode(), this);
                }
            }
            if (myChatFilesFolderExists) {
                proceedWithHandleStoredData(request.getNodeHandle());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    private void proceedWithHandleStoredData(long handle) {
        parentNode = megaApi.getNodeByHandle(handle);
        handleStoredData();
    }

    @Override
    public void storedUnhandledData(ArrayList<MegaNode> preservedData) {
        preservedNotOwnerNode = preservedData;
    }

    @Override
    public void handleStoredData() {
        if (preservedNotOwnerNode != null) {
            for (MegaNode node : preservedNotOwnerNode) {
                copyNode(node);
            }
        }
        preservedNotOwnerNode = null;
    }
}
