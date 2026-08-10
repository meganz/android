package mega.privacy.android.domain.usecase.home

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AppVersion
import mega.privacy.android.domain.repository.EnvironmentRepository
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MarkNewFeatureDisplayedUseCaseTest {
    private val settingsRepository = mock<SettingsRepository>()
    private val environmentRepository = mock<EnvironmentRepository>()

    private lateinit var underTest: MarkNewFeatureDisplayedUseCase

    @BeforeAll
    fun setUp() {
        underTest = MarkNewFeatureDisplayedUseCase(
            settingsRepository = settingsRepository,
            environmentRepository = environmentRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(settingsRepository, environmentRepository)
    }

    @Test
    fun `test that the current app version is stored when invoked`() = runTest {
        val currentVersion = AppVersion(major = 14, minor = 2, patch = 1)
        whenever(environmentRepository.getAppVersion()).thenReturn(currentVersion)

        underTest()

        verify(settingsRepository).setLastVersionNewFeatureShown(currentVersion)
    }
}
