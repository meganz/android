package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.fileservice.FileServiceReclaimOptionsMapper
import mega.privacy.android.data.mapper.fileservice.MegaFileServiceReclaimOptionsMapper
import mega.privacy.android.domain.entity.fileservice.FileServiceReclaimOptions
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FileServiceRepository
import javax.inject.Inject

internal class FileServiceRepositoryImpl @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val fileServiceReclaimOptionsMapper: FileServiceReclaimOptionsMapper,
    private val megaFileServiceReclaimOptionsMapper: MegaFileServiceReclaimOptionsMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FileServiceRepository {

    override suspend fun getReclaimOptions(): FileServiceReclaimOptions? =
        withContext(ioDispatcher) {
            megaApiGateway.fileServiceGetReclaimOptions()
                ?.let { fileServiceReclaimOptionsMapper(it) }
        }

    override suspend fun setReclaimOptions(options: FileServiceReclaimOptions) =
        withContext(ioDispatcher) {
            megaApiGateway.fileServiceSetReclaimOptions(
                megaFileServiceReclaimOptionsMapper(options)
            )
        }

    override suspend fun reclaim(options: FileServiceReclaimOptions?): Long =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("fileServiceReclaim") {
                    it.totalBytes
                }
                val megaOptions = options?.let(megaFileServiceReclaimOptionsMapper::invoke)
                megaApiGateway.fileServiceReclaim(megaOptions, listener)
            }
        }
}
