package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.AdsGateway
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.preferences.UIPreferencesGateway
import mega.privacy.android.data.mapper.MegaStringListMapper
import mega.privacy.android.data.mapper.advertisements.AdDetailsMapper
import mega.privacy.android.domain.entity.advertisements.AdDetails
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AdsRepository
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * [AdsRepository] implementation
 */
internal class AdsRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val adsGateway: AdsGateway,
    private val adDetailsMapper: AdDetailsMapper,
    private val megaStringListMapper: MegaStringListMapper,
    private val uiPreferencesGateway: UIPreferencesGateway,
    private val appEventGateway: AppEventGateway,
) : AdsRepository {

    override suspend fun fetchAdDetails(
        adSlots: List<String>,
        linkHandle: Long?,
    ): List<AdDetails> = withContext(ioDispatcher) {
        val adUnits = megaStringListMapper(adSlots)
        requireNotNull(adUnits) { "MegaStringListProvider is null, unable to get map adUnits" }
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("fetchAds") { request ->
                adDetailsMapper(request.megaStringMap)
            }

            adsGateway.fetchAds(
                adFlags = MegaApiJava.ADS_DEFAULT,
                adUnits = adUnits,
                publicHandle = linkHandle ?: MegaApiJava.INVALID_HANDLE,
                listener = listener
            )
        }
    }

    override suspend fun queryAds(linkHandle: Long): Boolean = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("queryAds") { request ->
                request.numDetails == 0
            }

            adsGateway.queryAds(
                adFlags = MegaApiJava.ADS_DEFAULT,
                publicHandle = linkHandle,
                listener = listener
            )
        }
    }

    override fun monitorAdsClosingTimestamp() = uiPreferencesGateway.monitorAdsClosingTimestamp()
        .flowOn(ioDispatcher)

    override suspend fun setAdsClosingTimestamp(timestamp: Long) = withContext(ioDispatcher) {
        uiPreferencesGateway.setAdsClosingTimestamp(timestamp)
    }

    override fun setGoogleConsentLoaded(isLoaded: Boolean) {
        appEventGateway.setGoogleConsentLoaded(isLoaded)
    }

    override fun monitorGoogleConsentLoaded() = appEventGateway.monitorGoogleConsentLoaded()
}