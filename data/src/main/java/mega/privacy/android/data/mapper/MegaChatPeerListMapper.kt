package mega.privacy.android.data.mapper

import nz.mega.sdk.MegaChatPeerList

/**
 * Mapper to convert a list of use handles in a [MegaChatPeerList] with standard privileges.
 */
typealias MegaChatPeerListMapper = (@JvmSuppressWildcards List<Long>) -> @JvmSuppressWildcards MegaChatPeerList

internal fun toMegaChatPeerList(usersList: List<Long>) =
    usersList.fold(MegaChatPeerList.createInstance()) { list, handle ->
        list.apply { addPeer(handle, MegaChatPeerList.PRIV_STANDARD) }
    }