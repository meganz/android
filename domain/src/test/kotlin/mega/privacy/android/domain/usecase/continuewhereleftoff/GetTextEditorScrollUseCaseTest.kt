package mega.privacy.android.domain.usecase.continuewhereleftoff

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.continuewhereleftoff.TextEditorScroll
import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetTextEditorScrollUseCaseTest {

    private lateinit var underTest: GetTextEditorScrollUseCase

    private val repository = mock<ContinueWhereLeftOffRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetTextEditorScrollUseCase(repository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(repository)
    }

    @Test
    fun `test that invoke returns scroll state when it exists`() = runTest {
        val expected = TextEditorScroll(
            nodeHandle = 1L,
            cursorPosition = 100,
            scrollFraction = 0.5f,
        )
        whenever(repository.getTextEditorScroll(1L)).thenReturn(expected)

        assertThat(underTest(1L)).isEqualTo(expected)
    }

    @Test
    fun `test that invoke returns null when no saved state exists`() = runTest {
        whenever(repository.getTextEditorScroll(1L)).thenReturn(null)

        assertThat(underTest(1L)).isNull()
    }
}
