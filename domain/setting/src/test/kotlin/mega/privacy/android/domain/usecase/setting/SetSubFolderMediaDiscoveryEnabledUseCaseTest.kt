package mega.privacy.android.domain.usecase.setting

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class SetSubFolderMediaDiscoveryEnabledUseCaseTest {
    private lateinit var underTest: SetSubFolderMediaDiscoveryEnabledUseCase
    private val settingsRepository = mock<SettingsRepository>()

    @Before
    fun setUp() {
        underTest = SetSubFolderMediaDiscoveryEnabledUseCase(
            settingsRepository = settingsRepository
        )
    }

    @Test
    fun `test that preference is updated`() = runTest {
        underTest(true)

        verify(settingsRepository).setSubfolderMediaDiscoveryEnabled(true)
    }
}