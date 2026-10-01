package mega.privacy.android.domain.usecase.domainmigration

import mega.privacy.android.domain.repository.DomainNameMigrationRepository
import javax.inject.Inject

/**
 * Get the domain name according to the repository cached value flag
 */
class GetDomainNameUseCase @Inject constructor(
    private val domainNameRepository: DomainNameMigrationRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke() =
        if (domainNameRepository.isDomainNameMegaDotAppFromCache()) {
            MEGA_APP_DOMAIN_NAME
        } else {
            MEGA_NZ_DOMAIN_NAME
        }

    companion object {
        /**
         * mega.app domain
         */
        const val MEGA_APP_DOMAIN_NAME = "mega.app"

        /**
         * mega.nz domain
         */
        const val MEGA_NZ_DOMAIN_NAME = "mega.nz"
    }
}