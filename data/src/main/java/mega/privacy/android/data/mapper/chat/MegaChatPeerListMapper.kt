package mega.privacy.android.data.mapper.chat

import nz.mega.sdk.MegaChatPeerList

/**
 * Mapper to convert data into [MegaChatPeerList].
 */
internal fun interface MegaChatPeerListMapper {

    /**
     * Invoke.
     *
     * @param usersList List of peer handles.
     * @return [MegaChatPeerList]
     */
    operator fun invoke(usersList: List<Long>): MegaChatPeerList
}