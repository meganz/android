package mega.privacy.android.domain.usecase


import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultRefreshPasscodeLockPreferenceTest {

    private lateinit var underTest: RefreshPasscodeLockPreference

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeEach
    fun setUp() {
        underTest = DefaultRefreshPasscodeLockPreference(settingsRepository)
    }

    @Test
    fun `test that passcodeLockPreference is set to false if preferences is null`() = runTest {
        settingsRepository.stub {
            onBlocking { isPasscodeLockPreferenceEnabled() }.thenReturn(null, false)
        }
        underTest()
        verify(settingsRepository, times(1)).setPasscodeLockEnabled(false)
    }

    @Test
    internal fun `test that an exception is thrown if preference remains null after being set`() =
        runTest {
            settingsRepository.stub {
                onBlocking { isPasscodeLockPreferenceEnabled() }.thenReturn(null)
            }
            assertThrows<IllegalStateException> { underTest() }
        }
}