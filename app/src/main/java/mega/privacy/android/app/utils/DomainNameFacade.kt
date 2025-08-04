package mega.privacy.android.app.utils

import dagger.hilt.android.EntryPointAccessors
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.domain.repository.DomainNameMigrationRepository
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase.Companion.MEGA_APP_DOMAIN_NAME

/**
 * Static wrapper to access domain name repository where dependency injection is not possible.
 * Note: Please use [GetDomainNameUseCase] or [DomainNameMigrationRepository] when possible.
 */
object DomainNameFacade {

    private val domainNameRepository: DomainNameMigrationRepository by lazy {
        val context = MegaApplication.getInstance()
        EntryPointAccessors.fromApplication(context, DomainNameMigrationRepository::class.java)
    }

    private val getDomainNameUseCase: GetDomainNameUseCase by lazy {
        val context = MegaApplication.getInstance()
        EntryPointAccessors.fromApplication(context, GetDomainNameUseCase::class.java)
    }

    /**
     * Get the domain name
     */
    fun getDomainName() = runCatching { getDomainNameUseCase() }.getOrElse { MEGA_APP_DOMAIN_NAME }

    /**
     * Get the domain name flag
     */
    fun isDomainNameMegaDotApp() =
        runCatching { domainNameRepository.isDomainNameMegaDotAppFromCache() }.getOrElse { false }
}