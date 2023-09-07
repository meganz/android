package mega.privacy.android.data.gateway

import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaStringList

/**
 * Advertisements gateway interface
 */
internal interface AdsGateway {

    /**
     * Fetch ads
     *
     * The associated request type with this request is MegaRequest::TYPE_FETCH_ADS
     * Valid data in the MegaRequest object received on callbacks:
     *  - MegaRequest::getNumber A bitmap flag used to communicate with the API
     *  - MegaRequest::getMegaStringList List of the adslot ids to fetch
     *  - MegaRequest::getNodeHandle  Public handle that the user is visiting
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getMegaStringMap: map with relationship between ids and ius
     *
     * @param adFlags A bitmap flag used to communicate with the API
     * Valid values are:
     *      - ADS_DEFAULT = 0x0
     *      - ADS_FORCE_ADS = 0x200
     *      - ADS_IGNORE_MEGA = 0x400
     *      - ADS_IGNORE_COUNTRY = 0x800
     *      - ADS_IGNORE_IP = 0x1000
     *      - ADS_IGNORE_PRO = 0x2000
     *      - ADS_FLAG_IGNORE_ROLLOUT = 0x4000
     * @param adUnits MegaStringList, a list of the adslot ids to fetch; it cannot be null nor empty
     * @param publicHandle MegaHandle, provide the public handle that the user is visiting file/folder link (Shared Link screen)
     * @param listener MegaRequestListener to track this request
     */
    fun fetchAds(
        adFlags: Int,
        adUnits: MegaStringList,
        publicHandle: Long,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Check if ads should show or not
     *
     * The associated request type with this request is MegaRequest::TYPE_QUERY_ADS
     * Valid data in the MegaRequest object received on callbacks:
     *  - MegaRequest::getNumber A bitmap flag used to communicate with the API
     *  - MegaRequest::getNodeHandle  Public handle that the user is visiting
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getNumDetails Return if ads should be show or not
     *
     * @param adFlags A bitmap flag used to communicate with the API
     * Valid values are:
     *      - ADS_DEFAULT = 0x0
     *      - ADS_FORCE_ADS = 0x200
     *      - ADS_IGNORE_MEGA = 0x400
     *      - ADS_IGNORE_COUNTRY = 0x800
     *      - ADS_IGNORE_IP = 0x1000
     *      - ADS_IGNORE_PRO = 0x2000
     *      - ADS_FLAG_IGNORE_ROLLOUT = 0x4000
     * @param publicHandle MegaHandle, provide the public handle that the user is visiting file/folder link (Shared Link screen)
     * @param listener MegaRequestListener to track this request
     */
    fun queryAds(adFlags: Int, publicHandle: Long, listener: MegaRequestListenerInterface)
}