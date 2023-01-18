package mega.privacy.android.domain.entity.qrcode

/**
 * Enum class defining results of calls to sdk
 */
enum class QRCodeQueryResults {
    /**
     * API_OK result from contactLinkQuery sdk call
     */
    CONTACT_QUERY_OK,

    /**
     * API_EEXIST result from contactLinkQuery sdk call
     */
    CONTACT_QUERY_EEXIST,

    /**
     * default result from contactLinkQuery sdk call
     */
    CONTACT_QUERY_DEFAULT
}