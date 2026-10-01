package mega.privacy.android.domain.usecase.continuewhereleftoff

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.continuewhereleftoff.TextEditorScroll
import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaveTextEditorScrollUseCaseTest {

    private lateinit var underTest: SaveTextEditorScrollUseCase

    private val repository = mock<ContinueWhereLeftOffRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SaveTextEditorScrollUseCase(repository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(repository)
    }

    @Test
    fun `test that invoke passes TextEditorScroll to repository`() = runTest {
        val scroll = TextEditorScroll(
            nodeHandle = 1L,
            cursorPosition = 100,
            scrollFraction = 0.5f,
        )

        underTest(scroll)

        verify(repository).saveTextEditorScroll(scroll)
    }
}
