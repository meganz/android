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

/**
 * Interface to get all information related to a MEGA account.
 * <p>
 * Implementations of this interface can receive all events (request, transfer, global) and two additional
 * events related to the synchronization engine. The SDK will provide a new interface to get synchronization
 * events separately in future updates.
 * Multiple inheritance is not used for compatibility with other programming languages.
 *
 * @see MegaGlobalListenerInterface
 * @see MegaRequestListenerInterface
 * @see MegaTransferListenerInterface
 */
public interface MegaListenerInterface extends MegaRequestListenerInterface, MegaGlobalListenerInterface,
        MegaTransferListenerInterface {
}
