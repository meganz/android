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
    operator fun invoke(isForEmail: Boolean) =
        if (!domainNameRepository.isDomainNameMegaDotAppFromCache()) {
            MEGA_NZ_DOMAIN_NAME
        } else if (isForEmail) {
            MEGA_IO_DOMAIN_NAME
        } else {
            MEGA_APP_DOMAIN_NAME
        }

    companion object {
        private const val MEGA_APP_DOMAIN_NAME = "mega.app"
        private const val MEGA_IO_DOMAIN_NAME = "mega.io"
        private const val MEGA_NZ_DOMAIN_NAME = "mega.nz"
    }
}