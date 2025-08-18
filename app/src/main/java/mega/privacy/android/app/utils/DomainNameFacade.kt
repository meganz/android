package mega.privacy.android.app.utils

import dagger.hilt.android.EntryPointAccessors
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.di.DomainNameEntryPoint
import mega.privacy.android.domain.repository.DomainNameMigrationRepository
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase.Companion.MEGA_NZ_DOMAIN_NAME
import javax.inject.Singleton

/**
 * Static wrapper to access domain name repository where dependency injection is not possible.
 * Note: Please use [GetDomainNameUseCase] or [DomainNameMigrationRepository] when possible.
 */
@Singleton
object DomainNameFacade {

    private val domainNameEntryPoint: DomainNameEntryPoint by lazy {
        val context = MegaApplication.getInstance()
        EntryPointAccessors.fromApplication(context, DomainNameEntryPoint::class.java)
    }

    /**
     * Get the domain name
     */
    fun getDomainName() = runCatching { domainNameEntryPoint.getDomainNameUseCase() }
        .getOrElse { MEGA_NZ_DOMAIN_NAME }

    /**
     * Get the domain name flag
     */
    fun isDomainNameMegaDotApp() =
        runCatching { domainNameEntryPoint.domainNameMigrationRepository.isDomainNameMegaDotAppFromCache() }
            .getOrElse { false }
}