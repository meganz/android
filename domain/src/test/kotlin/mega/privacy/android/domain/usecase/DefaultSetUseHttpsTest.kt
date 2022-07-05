package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.NetworkRepository
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class DefaultSetUseHttpsTest {
    private lateinit var underTest: SetUseHttps

    private val settingsRepository = mock<SettingsRepository>()

    private val networkRepository = mock<NetworkRepository>()

    @Before
    fun setUp() {
        underTest = DefaultSetUseHttps(settingsRepository = settingsRepository,
            networkRepository = networkRepository)
    }

    @Test
    fun `test that preference is updated`() = runTest {
        underTest(true)

        verify(settingsRepository).setUseHttpsPreference(true)
    }

    @Test
    fun `test that network setup is updated`() = runTest {
        underTest(true)

        verify(networkRepository).setUseHttps(true)
    }

    @Test
    fun `test that enabled state is returned`() = runTest {
        assertThat(underTest(true)).isTrue()
    }
}