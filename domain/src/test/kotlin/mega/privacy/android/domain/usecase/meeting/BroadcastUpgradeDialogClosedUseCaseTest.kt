package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BroadcastUpgradeDialogClosedUseCaseTest {

    private val chatRepository = mock<ChatRepository>()
    private lateinit var underTest: BroadcastUpgradeDialogClosedUseCase

    @BeforeEach
    internal fun setUp() {
        underTest = BroadcastUpgradeDialogClosedUseCase(chatRepository)
    }

    @Test
    fun `test that it emits value when invoked `() = runTest {
        underTest()
        verify(chatRepository).broadcastUpgradeDialogClosed()
    }
}
