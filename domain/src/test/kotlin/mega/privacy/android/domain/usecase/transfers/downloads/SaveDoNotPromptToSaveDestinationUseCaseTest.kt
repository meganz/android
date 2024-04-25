package mega.privacy.android.domain.usecase.transfers.downloads

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaveDoNotPromptToSaveDestinationUseCaseTest {
    private lateinit var underTest: SaveDoNotPromptToSaveDestinationUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setup() {
        underTest = SaveDoNotPromptToSaveDestinationUseCase(settingsRepository)
    }

    @BeforeEach
    fun resetMocks() = reset(settingsRepository)

    @Test
    fun `test that settings repository setAskSetDownloadLocation is set to false when this use case is invoked`() =
        runTest {
            underTest()
            verify(settingsRepository).setAskSetDownloadLocation(false)
        }

}