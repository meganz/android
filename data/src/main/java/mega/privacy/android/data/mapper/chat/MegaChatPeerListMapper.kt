package mega.privacy.android.data.mapper.chat

import nz.mega.sdk.MegaChatPeerList
import javax.inject.Inject

/**
 * Mapper to convert data into [MegaChatPeerList].
 */
internal class MegaChatPeerListMapper @Inject constructor() {

    /**
     * Invoke.
     *
     * @param usersList List of peer handles.
     * @return [MegaChatPeerList]
     */
    operator fun invoke(usersList: List<Long>): MegaChatPeerList =
        usersList.fold(MegaChatPeerList.createInstance()) { list, handle ->
            list.apply { addPeer(handle, MegaChatPeerList.PRIV_STANDARD) }
        }
}