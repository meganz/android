package mega.privacy.android.data.mapper.chat

import nz.mega.sdk.MegaChatPeerList
import javax.inject.Inject

/**
 * Implementation of [MegaChatPeerListMapper].
 */
internal class MegaChatPeerListMapperImpl @Inject constructor() : MegaChatPeerListMapper {

    override fun invoke(usersList: List<Long>): MegaChatPeerList =
        usersList.fold(MegaChatPeerList.createInstance()) { list, handle ->
            list.apply { addPeer(handle, MegaChatPeerList.PRIV_STANDARD) }
        }
}