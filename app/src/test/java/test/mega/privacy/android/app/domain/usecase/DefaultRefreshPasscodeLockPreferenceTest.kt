package test.mega.privacy.android.app.domain.usecase


import mega.privacy.android.app.domain.repository.SettingsRepository
import mega.privacy.android.app.domain.usecase.DefaultRefreshPasscodeLockPreference
import mega.privacy.android.app.domain.usecase.RefreshPasscodeLockPreference
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DefaultRefreshPasscodeLockPreferenceTest {

private lateinit var underTest: RefreshPasscodeLockPreference

    private val settingsRepository = mock<SettingsRepository>()

    @Before
    fun setUp() {
        underTest = DefaultRefreshPasscodeLockPreference(settingsRepository)
    }

    @Test
    fun `test that passcodeLockPreference is set to false if preferences is null`() {
        whenever(settingsRepository.getPreferences()).thenReturn(null)
        underTest()
        verify(settingsRepository, times(1)).setPasscodeLockEnabled(false)
    }
}