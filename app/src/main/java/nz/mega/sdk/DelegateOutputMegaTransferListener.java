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

import java.io.IOException;
import java.io.OutputStream;

/**
 * The listener interface for receiving delegateOutputMegaTransfer events.
 * <p>
 * The class that is interested in processing a delegateOutputMegaTransfer event implements this interface.
 * The object created with that class is registered with a component using the component's
 * addDelegateOutputMegaTransferListener method. When the delegateOutputMegaTransfer event occurs,
 * that object's appropriate method is invoked.
 *
 * @see DelegateMegaTransferListener
 */
public class DelegateOutputMegaTransferListener extends DelegateMegaTransferListener {
    OutputStream outputStream;

    /**
     * Instantiates a new delegate output mega transfer listener.
     *
     * @param megaApi
     *              the mega api.
     * @param outputStream
     *              the output stream.
     * @param listener
     *              the listener.
     * @param singleListener
     *              the single listener.
     */
    public DelegateOutputMegaTransferListener(MegaApiJava megaApi, OutputStream outputStream, MegaTransferListenerInterface listener,
            boolean singleListener) {
        super(megaApi, listener, singleListener);
        this.outputStream = outputStream;
    }

    /**
     * Provides the last read bytes of streaming downloads.
     *
     * @param api
     *              MegaApi object that started the transfer.
     * @param transfer
     *              Information about the transfer.
     * @param buffer
     *              Buffer with the last read bytes.
     * @return
     *              true, if successful.
     * @see DelegateMegaTransferListener#onTransferData(MegaApi api, MegaTransfer transfer, byte[] buffer)
     */
    public boolean onTransferData(MegaApi api, MegaTransfer transfer, byte[] buffer) {
        if (outputStream != null) {
            try {
                outputStream.write(buffer);
                return true;
            } catch (IOException e) {
            }
        }
        return false;
    }
}
