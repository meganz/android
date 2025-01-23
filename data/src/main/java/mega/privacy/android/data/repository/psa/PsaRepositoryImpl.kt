package mega.privacy.android.data.repository.psa

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.cache.Cache
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.psa.PsaPreferenceGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.psa.PsaMapper
import mega.privacy.android.domain.entity.psa.Psa
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.psa.PsaRepository
import nz.mega.sdk.MegaError
import timber.log.Timber
import javax.inject.Inject

internal class PsaRepositoryImpl @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val psaCache: Cache<Psa>,
    private val psaPreferenceGateway: PsaPreferenceGateway,
    private val psaMapper: PsaMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PsaRepository {
    override suspend fun fetchPsa(refreshCache: Boolean) = withContext(ioDispatcher) {
        if (refreshCache) psaCache.set(getLatestPsa())
        return@withContext psaCache.get()
    }

    private suspend fun getLatestPsa(): Psa? = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (isActive) {
                        when (error.errorCode) {
                            MegaError.API_OK -> {
                                continuation.resumeWith(Result.success(psaMapper(request)))
                            }

                            MegaError.API_ENOENT -> continuation.resumeWith(Result.success(null))
                            else -> {
                                Timber.e("Calling getLatestPsa failed with error code ${error.errorCode}")
                                continuation.failWithError(error, "getLatestPsa")
                            }
                        }
                    }
                }
            )
            megaApiGateway.getPsa(listener)
        }
    }

    override suspend fun getLastPsaFetchedTime() =
        withContext(ioDispatcher) { psaPreferenceGateway.getLastRequestedTime() }

    override suspend fun setLastFetchedTime(time: Long?) =
        withContext(ioDispatcher) { psaPreferenceGateway.setLastRequestedTime(time) }

    override suspend fun clearCache() {
        withContext(ioDispatcher) { psaCache.clear() }
    }

    override suspend fun dismissPsa(psaId: Int) =
        withContext(ioDispatcher) { megaApiGateway.setPsaHandled(psaId) }
}