package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.gateway.AdsGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.MegaStringListMapper
import mega.privacy.android.data.mapper.advertisements.AdDetailMapper
import mega.privacy.android.domain.entity.advertisements.AdDetail
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AdsRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import javax.inject.Inject

/**
 * [AdsRepository] implementation
 */
internal class AdsRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val adsGateway: AdsGateway,
    private val megaApiGateway: MegaApiGateway,
    private val adDetailMapper: AdDetailMapper,
    private val megaStringListMapper: MegaStringListMapper,
) : AdsRepository {
    override suspend fun fetchAdDetails(
        adSlots: List<String>,
        linkHandle: Long?,
    ): List<AdDetail> =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        if (error.errorCode == MegaError.API_OK) {
                            continuation.resumeWith(Result.success(adDetailMapper(request.megaStringMap)))
                        } else {
                            continuation.failWithError(error, "fetchAds")
                        }
                    }
                )
                adsGateway.fetchAds(
                    adFlags = MegaApiJava.ADS_FORCE_ADS,
                    adUnits = megaStringListMapper(adSlots),
                    publicHandle = linkHandle ?: MegaApiJava.INVALID_HANDLE,
                    listener = listener
                )
                continuation.invokeOnCancellation {
                    megaApiGateway.removeRequestListener(listener)
                }
            }
        }
}