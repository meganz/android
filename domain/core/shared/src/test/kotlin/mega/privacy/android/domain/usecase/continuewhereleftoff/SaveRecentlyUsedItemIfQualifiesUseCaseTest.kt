package mega.privacy.android.domain.usecase.continuewhereleftoff

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaveRecentlyUsedItemIfQualifiesUseCaseTest {

    private lateinit var underTest: SaveRecentlyUsedItemIfQualifiesUseCase

    private val saveRecentlyUsedItemUseCase = mock<SaveRecentlyUsedItemUseCase>()
    private val removeRecentlyUsedItemUseCase = mock<RemoveRecentlyUsedItemUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = SaveRecentlyUsedItemIfQualifiesUseCase(
            saveRecentlyUsedItemUseCase = saveRecentlyUsedItemUseCase,
            removeRecentlyUsedItemUseCase = removeRecentlyUsedItemUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(saveRecentlyUsedItemUseCase, removeRecentlyUsedItemUseCase)
    }

    @Test
    fun `test that invoke saves the item and returns true when progress is below the threshold`() =
        runTest {
            val result = underTest(
                nodeHandle = 123L,
                type = RecentlyUsedType.PDF,
                fileName = "doc.pdf",
                progress = 0.89f,
            )

            assertThat(result).isTrue()
            verify(saveRecentlyUsedItemUseCase)(123L, RecentlyUsedType.PDF, "doc.pdf")
            verifyNoInteractions(removeRecentlyUsedItemUseCase)
        }

    @Test
    fun `test that invoke removes the item and returns false when progress reaches the threshold`() =
        runTest {
            val result = underTest(
                nodeHandle = 123L,
                type = RecentlyUsedType.PDF,
                fileName = "doc.pdf",
                progress = 0.9f,
            )

            assertThat(result).isFalse()
            verify(removeRecentlyUsedItemUseCase)(123L)
            verifyNoInteractions(saveRecentlyUsedItemUseCase)
        }

    @Test
    fun `test that invoke removes the item and returns false when progress is above the threshold`() =
        runTest {
            val result = underTest(
                nodeHandle = 123L,
                type = RecentlyUsedType.TextEditor,
                fileName = "notes.txt",
                progress = 1f,
            )

            assertThat(result).isFalse()
            verify(removeRecentlyUsedItemUseCase)(123L)
            verifyNoInteractions(saveRecentlyUsedItemUseCase)
        }
}
