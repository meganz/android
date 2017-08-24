/*
 * (c) 2013-2015 by Mega Limited, Auckland, New Zealand
 *
 * This file is part of the MEGA SDK - Client Access Engine.
 *
 * Applications using the MEGA API must present a valid application key
 * and comply with the the rules set forth in the Terms of Service.
 *
 * The MEGA SDK is distributed in the hope that it will be useful,\
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * @copyright Simplified (2-clause) BSD License.
 * You should have received a copy of the license along with this
 * program.
 */
package nz.mega.sdk;

import nz.mega.sdk.MegaApi;
import nz.mega.sdk.MegaRequest;

import java.util.ArrayList;

/**
 * Listener to receive and send events to the app.
 *
 * @see MegaListenerInterface
 * @see MegaListener
 */
class DelegateMegaListener extends MegaListener {
    MegaApiJava megaApi;
    MegaListenerInterface listener;

    DelegateMegaListener(MegaApiJava megaApi, MegaListenerInterface listener) {
        this.megaApi = megaApi;
        this.listener = listener;
    }

    MegaListenerInterface getUserListener() {
        return listener;
    }

    /**
     * This function is called when a request is about to start being processed.
     * <p>
     * The SDK retains the ownership of the request parameter. Do not use it after this function returns.
     * The api object is the one created by the application, it will be valid until the application deletes it.
     *
     * @param api
     *            API object that started the request.
     * @param request
     *            Information about the request.
     * @see MegaRequestListenerInterface#onRequestStart(MegaApiJava api, MegaRequest request)
     * @see MegaListener#onRequestStart(MegaApi api, MegaRequest request)
     */
    @Override
    public void onRequestStart(MegaApi api, MegaRequest request) {
        if (listener != null) {
            final MegaRequest megaRequest = request.copy();
            megaApi.runCallback(new Runnable() {
                public void run() {
                    listener.onRequestStart(megaApi, megaRequest);
                }
            });
        }
    }

    /**
     * This function is called when a request has finished.
     * <p>
     * There will not be more callbacks about this request. The last parameter provides the result of the request.
     * If the request finished without problems, the error code will be API_OK. The SDK retains the ownership of
     * the request and error parameters. Do not use them after this functions returns.
     * The api object is the one created by the application, it will be valid until the application deletes it.
     *  
     * @param api
     *            API object that started the request.
     * @param request
     *            The MegaRequestType that has finished.
     * @param e
     *            Error Information.
     * @see MegaRequestListenerInterface#onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e)
     * @see MegaListener#onRequestFinish(MegaApi api, MegaRequest request, MegaError e)
     */
    @Override
    public void onRequestFinish(MegaApi api, MegaRequest request, MegaError e) {
        if (listener != null) {
            final MegaRequest megaRequest = request.copy();
            final MegaError megaError = e.copy();
            megaApi.runCallback(new Runnable() {
                public void run() {
                    listener.onRequestFinish(megaApi, megaRequest, megaError);
                }
            });
        }
    }

    /**
     * This function is called when there is a temporary error processing a request.
     * <p>
     * The request continues after this callback, so expect more MegaRequestListener.onRequestTemporaryError
     * or a MegaRequestListener.onRequestFinish callback. The SDK retains the ownership of the request and error
     * parameters.
     * Do not use them after this functions returns.
     * The api object is the one created by the application, it will be valid until the application deletes it.
     *  
     * @param api
     *            API object that started the request.
     * @param request
     *            Information about the request.
     * @param e
     *            Error Information.
     * @see MegaRequestListenerInterface#onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e)
     * @see MegaListener#onRequestTemporaryError(MegaApi api, MegaRequest request, MegaError error)
     */
    @Override
    public void onRequestTemporaryError(MegaApi api, MegaRequest request, MegaError e) {
        if (listener != null) {
            final MegaRequest megaRequest = request.copy();
            final MegaError megaError = e.copy();
            megaApi.runCallback(new Runnable() {
                public void run() {
                    listener.onRequestTemporaryError(megaApi, megaRequest, megaError);
                }
            });
        }
    }

    /**
     * This function is called when a transfer is about to start being processed.
     * <p>
     * The SDK retains the ownership of the transfer parameter. 
     * Do not use it after this functions returns.
     * The api object is the one created by the application, it will be valid until the application deletes it.
     *  
     * @param api
     *            API object that started the request.
     * @param transfer
     *            Information about the transfer.
     * @see MegaTransferListenerInterface#onTransferStart(MegaApiJava api, MegaTransfer transfer)
     * @see MegaListener#onTransferStart(MegaApi api, MegaTransfer transfer)
     */
    @Override
    public void onTransferStart(MegaApi api, MegaTransfer transfer) {
        if (listener != null) {
            final MegaTransfer megaTransfer = transfer.copy();
            megaApi.runCallback(new Runnable() {
                public void run() {
                    listener.onTransferStart(megaApi, megaTransfer);
                }
            });
        }
    }

    /**
     * This function is called when a transfer has finished.
     * <p>
     * The SDK retains the ownership of the transfer and error parameters.
     * Do not use them after this functions returns.
     * The api object is the one created by the application, it will be valid until the application deletes it.
     * There will not be more callbacks about this transfer.
     * The last parameter provides the result of the transfer. 
     * If the transfer finishes without errors, the error code will be API_OK.
     *  
     * @param api
     *            API object that started the request.
     * @param transfer
     *            Information about the transfer.
     * @param e
     *            Error Information.
     * @see MegaTransferListenerInterface#onTransferFinish(MegaApiJava api, MegaTransfer transfer, MegaError e)
     * @see MegaListener#onTransferFinish(MegaApi api, MegaTransfer transfer, MegaError error)
     */
    @Override
    public void onTransferFinish(MegaApi api, MegaTransfer transfer, MegaError e) {
        if (listener != null) {
            final MegaTransfer megaTransfer = transfer.copy();
            final MegaError megaError = e.copy();
            megaApi.runCallback(new Runnable() {
                public void run() {
                    listener.onTransferFinish(megaApi, megaTransfer, megaError);
                }
            });
        }
    }

    /**
     * This function is called to inform about the progress of a transfer.
     * <p>
     * The SDK retains the ownership of the transfer parameter. Do not use it after this functions returns.
     * The api object is the one created by the application, it will be valid until the application deletes it.
     *  
     * @param api
     *            API object that started the request.
     * @param transfer
     *            Information about the transfer.
     * @see MegaTransferListenerInterface#onTransferUpdate(MegaApiJava api, MegaTransfer transfer)
     * @see MegaListener#onTransferUpdate(MegaApi api, MegaTransfer transfer)
     */
    @Override
    public void onTransferUpdate(MegaApi api, MegaTransfer transfer) {
        if (listener != null) {
            final MegaTransfer megaTransfer = transfer.copy();
            megaApi.runCallback(new Runnable() {
                public void run() {
                    listener.onTransferUpdate(megaApi, megaTransfer);
                }
            });
        }
    }
    
    /**
     * This function is called when there is a temporary error processing a transfer.
     * <p>
     * The transfer continues after this callback, so expect more MegaTransferListener.onTransferTemporaryError
     * or a MegaTransferListener.onTransferFinish callback. The SDK retains the ownership of the transfer and
     * error parameters. Do not use them after this function returns.
     *  
     * @param api
     *            API object that started the request.
     * @param transfer
     *            Information about the transfer.
     * @param e
     *            Error Information.
     * @see MegaTransferListenerInterface#onTransferTemporaryError(MegaApiJava api, MegaTransfer transfer, MegaError e)
     * @see MegaListener#onTransferTemporaryError(MegaApi api, MegaTransfer transfer, MegaError error)
     */
    @Override
    public void onTransferTemporaryError(MegaApi api, MegaTransfer transfer, MegaError e) {
        if (listener != null) {
            final MegaTransfer megaTransfer = transfer.copy();
            final MegaError megaError = e.copy();
            megaApi.runCallback(new Runnable() {
                public void run() {
                    listener.onTransferTemporaryError(megaApi, megaTransfer, megaError);
                }
            });
        }
    }

    /**
     * This function is called when there are new or updated contacts in the account.
     * <p>
     * The SDK retains the ownership of the MegaUserList in the second parameter.
     * The list and all the MegaUser objects that it contains will be valid until this function returns.
     * If you want to save the list, use MegaUserList.copy().
     * If you want to save only some of the MegaUser objects, use MegaUser.copy() for those objects.
     *
     * @param api
     *            API object that started the request.
     * @param userList
     *            List that contains new or updated contacts.
     * @see MegaGlobalListenerInterface#onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users)
     * @see MegaListener#onUsersUpdate(MegaApi api, MegaUserList users)
     */
    @Override
    public void onUsersUpdate(MegaApi api, MegaUserList userList) {
        if (listener != null) {
            final ArrayList<MegaUser> users = MegaApiJava.userListToArray(userList);
            megaApi.runCallback(new Runnable() {
                public void run() {
                    listener.onUsersUpdate(megaApi, users);
                }
            });
        }
    }

    /**
     * This function is called when there are new or updated nodes in the account.
     * <p>
     * When the full account is reloaded or a large number of server notifications arrives at once,
     * the second parameter will be null.
     * The SDK retains the ownership of the MegaNodeList in the second parameter.
     * The list and all the MegaNode objects that it contains will be valid until this function returns.
     * If you want to save the list, use MegaNodeList.copy().
     * If you want to save only some of the MegaNode objects, use MegaNode.copy() for those nodes.
     *
     * @param api
     *            API object that started the request.
     * @param nodeList
     *            List that contains new or updated nodes.
     * @see MegaGlobalListenerInterface#onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodes)
     * @see MegaListener#onNodesUpdate(MegaApi api, MegaNodeList nodes)
     */
    @Override
    public void onNodesUpdate(MegaApi api, MegaNodeList nodeList) {
        if (listener != null) {
            final ArrayList<MegaNode> nodes = MegaApiJava.nodeListToArray(nodeList);
            megaApi.runCallback(new Runnable() {
                public void run() {
                    listener.onNodesUpdate(megaApi, nodes);
                }
            });
        }
    }
    
    /**
     * This function is called when an inconsistency is detected in the local cache.
     * <p>
     * You should call MegaApiJava.fetchNodes() when this callback is received.
     *
     * @param api
     *            API object that started the request.
     * @see MegaGlobalListenerInterface#onReloadNeeded(MegaApiJava api)
     * @see MegaListener#onReloadNeeded(MegaApi api)
     */
    @Override
    public void onReloadNeeded(MegaApi api) {
        if (listener != null) {
            megaApi.runCallback(new Runnable() {
                public void run() {
                    listener.onReloadNeeded(megaApi);
                }
            });
        }
    }

    @Override
    public void onAccountUpdate(MegaApi api) {
        if (listener != null) {
            megaApi.runCallback(new Runnable() {
                public void run() {
                    listener.onAccountUpdate(megaApi);
                }
            });
        }
    }

    @Override
    public void onContactRequestsUpdate(MegaApi api, MegaContactRequestList contactRequestList) {
        if (listener != null) {
            final ArrayList<MegaContactRequest> requests = MegaApiJava.contactRequestListToArray(contactRequestList);
            megaApi.runCallback(new Runnable() {
                public void run() {
                    listener.onContactRequestsUpdate(megaApi, requests);
                }
            });
        }
    }
}
