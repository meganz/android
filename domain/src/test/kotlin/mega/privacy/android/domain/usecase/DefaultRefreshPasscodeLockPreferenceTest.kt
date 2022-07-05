package mega.privacy.android.domain.usecase


import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class DefaultRefreshPasscodeLockPreferenceTest {

    private lateinit var underTest: RefreshPasscodeLockPreference

    private val settingsRepository = mock<SettingsRepository>()

    @Before
    fun setUp() {
        underTest = DefaultRefreshPasscodeLockPreference(settingsRepository)
    }

    @Test
    fun `test that passcodeLockPreference is set to false if preferences is null`() {
        underTest()
        verify(settingsRepository, times(1)).setPasscodeLockEnabled(false)
    }
}