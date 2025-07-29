package mega.privacy.android.domain.usecase.domainmigration

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.featuretoggle.DomainFeatures
import mega.privacy.android.domain.repository.DomainNameMigrationRepository
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateDomainNameUseCaseTest {
    private lateinit var underTest: UpdateDomainNameUseCase

    private val domainNameRepository = mock<DomainNameMigrationRepository>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    @BeforeAll
    fun setup() {
        underTest = UpdateDomainNameUseCase(
            domainNameRepository,
            getFeatureFlagValueUseCase
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(domainNameRepository, getFeatureFlagValueUseCase)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that value is saved to the repository`(featureFlag: Boolean) = runTest {
        whenever(getFeatureFlagValueUseCase(DomainFeatures.MegaDotAppDomain)) doReturn featureFlag

        underTest()

        verify(domainNameRepository).setDomainNameMegaDotApp(featureFlag)
    }

    @Test
    fun `test that cached value is updated before checking the feature flag`() = runTest {
        whenever(getFeatureFlagValueUseCase(DomainFeatures.MegaDotAppDomain)) doThrow RuntimeException()

        runCatching {
            underTest()
        }

        verify(domainNameRepository).isDomainNameMegaDotApp()
    }
}