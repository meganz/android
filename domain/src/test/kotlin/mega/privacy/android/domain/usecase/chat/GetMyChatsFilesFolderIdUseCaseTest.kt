package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetMyChatsFilesFolderIdUseCaseTest {
    private lateinit var underTest: GetMyChatsFilesFolderIdUseCase

    val fileSystemRepository = mock<FileSystemRepository>()
    val isNodeInRubbishOrDeletedUseCase = mock<IsNodeInRubbishOrDeletedUseCase>()

    @BeforeAll
    fun setup() {
        underTest = GetMyChatsFilesFolderIdUseCase(
            fileSystemRepository,
            isNodeInRubbishOrDeletedUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(
            fileSystemRepository,
            isNodeInRubbishOrDeletedUseCase,
        )

    @Test
    fun `test that repository folder id is returned when the folder exists and is not in the rubbish bin or deleted`() =
        runTest {
            val folderId = NodeId(11L)
            whenever(fileSystemRepository.getMyChatsFilesFolderId()).thenReturn(folderId)
            whenever(isNodeInRubbishOrDeletedUseCase(folderId.longValue)).thenReturn(false)

            val actual = underTest()

            assertThat(actual).isEqualTo(folderId)
        }

    @Test
    fun `test that null is returned when existing folder id is in rubbish bin or deleted`() =
        runTest {
            val folderId = NodeId(12L)
            whenever(fileSystemRepository.getMyChatsFilesFolderId()).thenReturn(folderId)
            whenever(isNodeInRubbishOrDeletedUseCase(folderId.longValue)).thenReturn(true)

            val actual = underTest()

            assertThat(actual).isNull()
        }

    @Test
    fun `test that null is returned when the folder does not exist`() =
        runTest {
            whenever(fileSystemRepository.getMyChatsFilesFolderId()).thenReturn(null)

            val actual = underTest()

            assertThat(actual).isNull()
            verifyNoInteractions(isNodeInRubbishOrDeletedUseCase)
        }
}