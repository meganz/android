package mega.privacy.android.app.domain.usecase.environment

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.EnvironmentRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class IsFirstLaunchTest {
    private lateinit var underTest: IsFirstLaunch

    private val environmentRepository = mock<EnvironmentRepository>()

    @Before
    fun setUp() {
        underTest = IsFirstLaunch(environmentRepository = environmentRepository)
    }

    @Test
    fun `test that true is returned if repository returns true`() = runTest {
        environmentRepository.stub {
            onBlocking { getIsFirstLaunch() }.thenReturn(true)
        }

        assertThat(underTest()).isTrue()
    }

    @Test
    fun `test that false is returned if repository returns false`() = runTest {
        environmentRepository.stub {
            onBlocking { getIsFirstLaunch() }.thenReturn(false)
        }

        assertThat(underTest()).isFalse()
    }

    @Test
    fun `test that true is returned if repository returns null`() = runTest {
        environmentRepository.stub {
            onBlocking { getIsFirstLaunch() }.thenReturn(null)
        }

        assertThat(underTest()).isTrue()
    }
}