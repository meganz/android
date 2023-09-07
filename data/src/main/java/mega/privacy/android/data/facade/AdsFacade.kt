package mega.privacy.android.data.facade

import mega.privacy.android.data.gateway.AdsGateway
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaStringList
import javax.inject.Inject

/**
 * Advertisements Facade implements [AdsGateway]
 *
 * provides methods to fetch ads and to query ads (check if ads should be shown)
 */
internal class AdsFacade @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
) : AdsGateway {

    override fun fetchAds(
        adFlags: Int,
        adUnits: MegaStringList,
        publicHandle: Long,
        listener: MegaRequestListenerInterface,
    ) = megaApi.fetchAds(adFlags, adUnits, publicHandle, listener)

    override fun queryAds(
        adFlags: Int,
        publicHandle: Long,
        listener: MegaRequestListenerInterface,
    ) = megaApi.queryAds(adFlags, publicHandle, listener)
}