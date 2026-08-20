package mega.privacy.android.app.utils

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
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

    /**
     * Application context set once at app boot by the app-create initialiser tier.
     *
     * This object cannot be Hilt-injected, so its application context is handed to it explicitly
     * during `Application.onCreate` instead of reaching through `MegaApplication.getInstance()`.
     */
    internal lateinit var applicationContext: Context

    private val domainNameEntryPoint: DomainNameEntryPoint by lazy {
        EntryPointAccessors.fromApplication(applicationContext, DomainNameEntryPoint::class.java)
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