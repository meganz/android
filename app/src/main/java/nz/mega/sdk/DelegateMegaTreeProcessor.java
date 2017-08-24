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
 * Interface to process node trees.
 * <p>
 * An implementation of this class can be used to process a node tree passing a pointer to MegaApi.processMegaTree().
 *
 * @see MegaTreeProcessorInterface
 * @see MegaTreeProcessor
 */
class DelegateMegaTreeProcessor extends MegaTreeProcessor {
    MegaApiJava megaApi;
    MegaTreeProcessorInterface listener;

    DelegateMegaTreeProcessor(MegaApiJava megaApi, MegaTreeProcessorInterface listener) {
        this.megaApi = megaApi;
        this.listener = listener;
    }

    /**
     * Function that will be called for all nodes in a node tree.
     *
     * @param node
     *          Node to be processed.
     * @return
     *          true to continue processing nodes, false to stop.
     * @see MegaTreeProcessorInterface#processMegaNode(MegaApiJava megaApi, MegaNode node)
     * @see MegaTreeProcessor#processMegaNode(MegaNode node)
     */
    public boolean processMegaNode(MegaNode node) {
        if (listener != null)
            return listener.processMegaNode(megaApi, node);
        return false;
    }
}
