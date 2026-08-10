package mega.privacy.android.domain.usecase.home

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
class ClearPinnedHomeItemsUseCaseTest {
    private lateinit var underTest: ClearPinnedHomeItemsUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ClearPinnedHomeItemsUseCase(settingsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(settingsRepository)
    }

    @Test
    fun `test that invoke clears the pinned items in the repository`() = runTest {
        underTest()

        verify(settingsRepository).clearPinnedHomeItems()
    }
}
