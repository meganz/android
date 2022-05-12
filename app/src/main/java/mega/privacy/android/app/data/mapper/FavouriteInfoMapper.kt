package mega.privacy.android.app.data.mapper

import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.domain.entity.FavouriteInfo
import nz.mega.sdk.MegaNode

typealias FavouriteInfoMapper = (@JvmSuppressWildcards MegaNode, @JvmSuppressWildcards MegaApiGateway) -> @JvmSuppressWildcards FavouriteInfo

internal fun toFavouriteInfo(megaNode: MegaNode, megaApiGateway: MegaApiGateway) =
    FavouriteInfo(
        node = megaNode,
        hasVersion = megaApiGateway.hasVersion(megaNode),
        numChildFolders = megaApiGateway.getNumChildFolders(megaNode),
        numChildFiles = megaApiGateway.getNumChildFiles(megaNode)
    )