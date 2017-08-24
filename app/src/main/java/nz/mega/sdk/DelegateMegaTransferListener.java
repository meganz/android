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
import nz.mega.sdk.MegaTransfer;

/**
 * Interface to receive information about transfers.
 * <p>
 * All transfers are able to pass a pointer to an implementation of this interface in the singleListener parameter.
 * You can also get information about all transfers using MegaApi.addTransferListener().
 * MegaListener objects can also receive information about transfers.
 *
 * @see MegaTransferListenerInterface
 * @see MegaTransferListener
 */
class DelegateMegaTransferListener extends MegaTransferListener {
    MegaApiJava megaApi;
    MegaTransferListenerInterface listener;
    boolean singleListener;

    DelegateMegaTransferListener(MegaApiJava megaApi, MegaTransferListenerInterface listener, boolean singleListener) {
        this.megaApi = megaApi;
        this.listener = listener;
        this.singleListener = singleListener;
    }

    MegaTransferListenerInterface getUserListener() {
        return listener;
    }

    /**
     * This function is called when a transfer is about to start being processed.
     * <p>
     * The SDK retains the ownership of the transfer parameter. Do not use it after this functions returns.
     * The api object is the one created by the application, it will be valid until the application deletes it.
     *
     * @param api
     *              MegaApi object that started the transfer.
     * @param transfer
     *              Information about the transfer.
     * @see MegaTransferListenerInterface#onTransferStart(MegaApiJava api, MegaTransfer transfer)
     * @see MegaTransferListener#onTransferStart(MegaApi api, MegaTransfer transfer)
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
     * The SDK retains the ownership of the transfer and error parameters. Do not use them after this functions returns.
     * The api object is the one created by the application, it will be valid until the application deletes it.
     * There will not be further callbacks relating to this transfer. The last parameter provides the result of the
     * transfer. If the transfer finished without problems, the error code will be API_OK.
     *
     * @param api
     *          MegaApi object that started the transfer.
     * @param transfer
     *          Information about the transfer.
     * @param e
     *          Error information.
     * @see MegaTransferListenerInterface#onTransferFinish(MegaApiJava api, MegaTransfer transfer, MegaError e)
     * @see MegaTransferListener#onTransferFinish(MegaApi api, MegaTransfer transfer, MegaError error)
     */
    @Override
    public void onTransferFinish(MegaApi api, MegaTransfer transfer, MegaError e) {
        if (listener != null) {
            final MegaTransfer megaTransfer = transfer.copy();
            final MegaError megaError = e.copy();
            megaApi.runCallback(new Runnable() {
                public void run() {
                    listener.onTransferFinish(megaApi, megaTransfer, megaError);
                    
                    if (singleListener) {
                        megaApi.privateFreeTransferListener(DelegateMegaTransferListener.this);
                    }
                }
            });
        }
    }

    /**
     * This function is called to get details about the progress of a transfer.
     * <p>
     * The SDK retains the ownership of the transfer parameter. Do not use it after this functions returns.
     * The api object is the one created by the application, it will be valid until the application deletes it.
     *
     * @param api
     *          MegaApi object that started the transfer.
     * @param transfer
     *          Information about the transfer.
     * @see MegaTransferListenerInterface#onTransferUpdate(MegaApiJava api, MegaTransfer transfer)
     * @see MegaTransferListener#onTransferUpdate(MegaApi api, MegaTransfer transfer)
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
     *          MegaApi object that started the transfer.
     * @param transfer
     *          Information about the transfer.
     * @param e
     *          Error information.
     * @see MegaTransferListenerInterface#onTransferTemporaryError(MegaApiJava api, MegaTransfer transfer, MegaError e)
     * @see MegaTransferListener#onTransferTemporaryError(MegaApi api, MegaTransfer transfer, MegaError error)
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
     * This function is called to provide the last read bytes of streaming downloads.
     * <p>
     * This function will not be called for non streaming downloads. You can get the same buffer provided by this
     * function in MegaTransferListener.onTransferUpdate, using MegaTransfer.getLastBytes() and
     * MegaTransfer.getDeltaSize(). The SDK retains the ownership of the transfer and buffer parameters.
     * Do not use them after this functions returns. This callback is mainly provided for compatibility with other
     * programming languages.
     *
     * @param api
     *          MegaApi object that started the transfer.
     * @param transfer
     *          Information about the transfer.
     * @param buffer
     *          Buffer with the last read bytes.
     * @return
     *          Size of the buffer.
     * @see MegaTransferListenerInterface#onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer)
     * @see MegaTransferListener#onTransferData(MegaApi api, MegaTransfer transfer, byte[] buffer)
     */
    public boolean onTransferData(MegaApi api, MegaTransfer transfer, byte[] buffer) {
        if (listener != null) {
            final MegaTransfer megaTransfer = transfer.copy();
            return listener.onTransferData(megaApi, megaTransfer, buffer);
        }
        return false;
    }
}
