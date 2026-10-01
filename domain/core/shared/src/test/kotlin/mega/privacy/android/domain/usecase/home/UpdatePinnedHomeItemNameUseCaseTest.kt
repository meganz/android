package mega.privacy.android.domain.usecase.home

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdatePinnedHomeItemNameUseCaseTest {
    private lateinit var underTest: UpdatePinnedHomeItemNameUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = UpdatePinnedHomeItemNameUseCase(settingsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(settingsRepository)
    }

    @Test
    fun `test that invoke updates the name in the repository`() = runTest {
        val nodeId = NodeId(7L)

        underTest(nodeId, "Renamed")

        verify(settingsRepository).updatePinnedHomeItemName(nodeId, "Renamed")
    }
}
