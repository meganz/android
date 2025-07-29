package mega.privacy.android.domain.usecase.domainmigration

import mega.privacy.android.domain.featuretoggle.DomainFeatures
import mega.privacy.android.domain.repository.DomainNameMigrationRepository
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import javax.inject.Inject

/**
 * Set the domain name to the repository checking the feature flag
 */
class UpdateDomainNameUseCase @Inject constructor(
    private val domainNameRepository: DomainNameMigrationRepository,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() {
        domainNameRepository.isDomainNameMegaDotApp() //update the cached value in case feature flag fails
        domainNameRepository.setDomainNameMegaDotApp(
            getFeatureFlagValueUseCase(DomainFeatures.MegaDotAppDomain)
        )
    }
}