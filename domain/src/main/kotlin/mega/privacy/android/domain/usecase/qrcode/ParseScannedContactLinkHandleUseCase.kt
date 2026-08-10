package mega.privacy.android.domain.usecase.qrcode

import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase.Companion.MEGA_APP_DOMAIN_NAME
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase.Companion.MEGA_NZ_DOMAIN_NAME
import javax.inject.Inject

/**
 * Use case for extracting the contact link handle from a raw scanned QR code value
 */
class ParseScannedContactLinkHandleUseCase @Inject constructor() {

    /**
     * Invoke
     *
     * @param scannedCode Raw string decoded from the scanned QR code
     * @return Base 64 handle of the contact link, or null if the scanned code is not a valid
     * MEGA contact link
     */
    operator fun invoke(scannedCode: String): String? {
        val segments = scannedCode.split(CONTACT_LINK_PREFIX)
        return segments.getOrNull(1)?.takeIf { segments.first() in contactLinkBaseUrls }
    }

    companion object {
        private const val CONTACT_LINK_PREFIX = "C!"
        private val contactLinkBaseUrls = listOf(
            "https://$MEGA_NZ_DOMAIN_NAME/",
            "https://$MEGA_APP_DOMAIN_NAME/",
        )
    }
}
