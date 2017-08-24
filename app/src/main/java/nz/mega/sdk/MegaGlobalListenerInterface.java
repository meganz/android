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

import java.util.ArrayList;

/**
 * Interface to get information about global events.
 * <p>
 * You can implement this interface and start receiving events calling MegaApiJava.addGlobalListener().
 * MegaListener objects can also receive global events.
 */
public interface MegaGlobalListenerInterface {
    /**
     * This function is called when there are new or updated contacts in the account.
     * <p>
     * The SDK retains the ownership of the MegaUserList in the second parameter.
     * The list and all the MegaUser objects that it contains will be valid until this function returns.
     * If you want to save the list, use MegaUserList.copy().
     * If you want to save only some of the MegaUser objects, use MegaUser.copy() for those objects.
     * @param api
     *          Mega Java API connected to account.
     * @param users
     *          List of new or updated Contacts.
     */
    public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users);

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
     *            API connected to account.
     * @param nodeList
     *            List of new or updated Nodes.
     */
    public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodeList);

    /**
     * This function is called when an inconsistency is detected in the local cache.
     * <p>
     * You should call MegaApi.fetchNodes() when this callback is received.
     *  
     * @param api
     *            API connected to account.
     */
    public void onReloadNeeded(MegaApiJava api);
    
    public void onAccountUpdate(MegaApiJava api);
    public void onContactRequestsUpdate(MegaApiJava api, ArrayList<MegaContactRequest> requests);
}
