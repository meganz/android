package mega.privacy.android.domain.usecase.environment

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class IsFirstLaunchUseCaseTest {
    private lateinit var underTest: IsFirstLaunchUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @Before
    fun setUp() {
        underTest = IsFirstLaunchUseCase(settingsRepository = settingsRepository)
    }

    @Test
    fun `test that true is returned if repository returns true`() = runTest {
        settingsRepository.stub {
            onBlocking { getIsFirstLaunch() }.thenReturn(true)
        }

        Truth.assertThat(underTest()).isTrue()
    }

    @Test
    fun `test that false is returned if repository returns false`() = runTest {
        settingsRepository.stub {
            onBlocking { getIsFirstLaunch() }.thenReturn(false)
        }

        Truth.assertThat(underTest()).isFalse()
    }

    @Test
    fun `test that true is returned if repository returns null`() = runTest {
        settingsRepository.stub {
            onBlocking { getIsFirstLaunch() }.thenReturn(null)
        }

        Truth.assertThat(underTest()).isTrue()
    }
}