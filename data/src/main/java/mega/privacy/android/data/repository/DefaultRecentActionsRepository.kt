package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.RecentActionBucketMapper
import mega.privacy.android.data.mapper.RecentActionsMapper
import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.RecentActionsRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRecentActionBucket
import nz.mega.sdk.MegaRecentActionBucketList
import nz.mega.sdk.MegaRequest
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

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
            val megaRecentActions = recentActionsMapper(
                getMegaRecentAction(),
                ::copyRecentActionBucket)

            val list = (megaRecentActions.indices).map {
                recentActionBucketMapper.invoke(
                    megaRecentActions[it],
                    cacheFolderGateway::getThumbnailCacheFilePath,
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

    private suspend fun getMegaRecentAction(): MegaRecentActionBucketList? =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaApiGateway.getRecentActionsAsync(DAYS, MAX_NODES,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = { request: MegaRequest, error: MegaError ->
                            if (error.errorCode == MegaError.API_OK) {
                                continuation.resumeWith(Result.success(request.recentActions))
                            } else {
                                continuation.failWithError(error)
                            }
                        }
                    ))
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
