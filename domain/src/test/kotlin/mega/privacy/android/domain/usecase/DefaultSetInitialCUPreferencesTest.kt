package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSetInitialCUPreferencesTest {
    private lateinit var underTest: SetInitialCUPreferences

    private val settingsRepository = mock<SettingsRepository>()

    private val accountRepository = mock<AccountRepository>()

    @Before
    fun setUp() {
        underTest = DefaultSetInitialCUPreferences(settingsRepository = settingsRepository,
            accountRepository = accountRepository)
    }

    @Test
    fun `test that initial value for accountRepository setUserHasLoggedIn is set`() = runTest {
        underTest()
        verify(accountRepository).setUserHasLoggedIn()
    }

    @Test
    fun `test that initial value for settingsRepository setStorageAskAlways is set`() = runTest {
        underTest()
        verify(settingsRepository).setStorageAskAlways(true)
    }

    @Test
    fun `test that initial value for settingsRepository setDefaultStorageDownloadLocation is set`() =
        runTest {
            underTest()
            verify(settingsRepository).setDefaultStorageDownloadLocation()
        }

    @Test
    fun `test that initial value for settingsRepository setPasscodeLockEnabled is set`() =
        runTest {
            underTest()
            verify(settingsRepository).setPasscodeLockEnabled(false)
        }

    @Test
    fun `test that initial value for settingsRepository setPasscodeLockCode is set`() = runTest {
        underTest()
        verify(settingsRepository).setPasscodeLockCode("")
    }

    @Test
    fun `test that initial value for settingsRepository setShowCopyright is set`() = runTest {
        underTest()
        verify(settingsRepository).setShowCopyright()
    }
}