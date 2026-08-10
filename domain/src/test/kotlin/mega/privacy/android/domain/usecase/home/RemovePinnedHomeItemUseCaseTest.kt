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
class RemovePinnedHomeItemUseCaseTest {
    private lateinit var underTest: RemovePinnedHomeItemUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = RemovePinnedHomeItemUseCase(settingsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(settingsRepository)
    }

    @Test
    fun `test that invoke removes the item from the repository`() = runTest {
        val nodeId = NodeId(123L)

        underTest(nodeId)

        verify(settingsRepository).removePinnedHomeItem(nodeId)
    }
}
