package mega.privacy.android.app.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.DomainNameMigrationRepository
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase

/**
 * Entry point for domain name related dependencies.
 * This allows access to domain name functionality from classes that cannot use dependency injection.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DomainNameEntryPoint {

    /**
     * Domain name migration repository
     */
    val domainNameMigrationRepository: DomainNameMigrationRepository

    /**
     * Get domain name use case
     */
    val getDomainNameUseCase: GetDomainNameUseCase
}
