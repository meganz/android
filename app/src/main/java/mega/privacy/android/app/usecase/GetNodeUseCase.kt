package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.di.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import javax.inject.Inject

class GetNodeUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    fun get(nodeHandle: Long): Single<MegaNode> =
        Single.fromCallable {
            megaApi.getNodeByHandle(nodeHandle)
        }

    fun markAsFavorite(nodeHandle: Long, isFavorite: Boolean): Completable =
        Completable.fromCallable {
            val node = megaApi.getNodeByHandle(nodeHandle)
            megaApi.setNodeFavourite(node, isFavorite)
        }
}
