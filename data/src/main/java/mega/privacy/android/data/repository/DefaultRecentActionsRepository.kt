package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.recentactions.RecentActionBucketMapper
import mega.privacy.android.data.mapper.recentactions.RecentActionsMapper
import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.RecentActionsRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRecentActionBucket
import nz.mega.sdk.MegaRequest
import timber.log.Timber
import javax.inject.Inject

/**
 * Default implementation of [RecentActionsRepository]
 */
internal class DefaultRecentActionsRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val cacheFolderGateway: CacheFolderGateway,
    private val recentActionsMapper: RecentActionsMapper,
    private val recentActionBucketMapper: RecentActionBucketMapper,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RecentActionsRepository {

    override suspend fun getRecentActions() = withContext(ioDispatcher) {
        runCatching {
            val list = getMegaRecentAction().map {
                recentActionBucketMapper.invoke(
                    it,
                    cacheFolderGateway::getThumbnailCacheFolder,
                    cacheFolderGateway::getPreviewCacheFolder,
                    cacheFolderGateway::getFullSizeCacheFolder,
                    megaApiGateway::hasVersion,
                    megaApiGateway::getNumChildFolders,
                    megaApiGateway::getNumChildFiles,
                    fileTypeInfoMapper,
                    megaApiGateway::isPendingShare,
                    megaApiGateway::isInRubbish,
                )
            }
            return@withContext list
        }.onFailure {
            Timber.e(it)
        }
        return@withContext emptyList<RecentActionBucketUnTyped>()
    }

    private suspend fun getMegaRecentAction(): List<MegaRecentActionBucket> =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request: MegaRequest, error: MegaError ->
                        if (error.errorCode == MegaError.API_OK) {
                            val result = recentActionsMapper(
                                request.recentActions,
                                this@DefaultRecentActionsRepository::copyRecentActionBucket
                            )
                            continuation.resumeWith(Result.success(result))
                        } else {
                            continuation.failWithError(error, "getMegaRecentAction")
                        }
                    })
                continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
                megaApiGateway.getRecentActionsAsync(DAYS, MAX_NODES, listener)
            }
        }


    /**
     * Provide the [MegaRecentActionBucket] required copy.
     */
    private fun copyRecentActionBucket(recentActionBucket: MegaRecentActionBucket) =
        megaApiGateway.copyBucket(recentActionBucket)

    companion object {
        /**
         * Default and recommended value for getting recent actions in the last days.
         */
        private const val DAYS = 30L

        /**
         * Default and recommended value for getting recent actions for a maximum value of nodes.
         */
        private const val MAX_NODES = 500L
    }
}
