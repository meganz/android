package mega.privacy.android.domain.usecase.home

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.home.PinnedHomeItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddPinnedHomeItemsUseCaseTest {
    private lateinit var underTest: AddPinnedHomeItemsUseCase

    private val settingsRepository = mock<SettingsRepository>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = AddPinnedHomeItemsUseCase(
            settingsRepository = settingsRepository,
            getNodeByIdUseCase = getNodeByIdUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(settingsRepository, getNodeByIdUseCase)
    }

    @Test
    fun `test that invoke pins resolved nodes with their name and folder snapshots`() =
        runTest {
            val folderId = NodeId(1L)
            val fileId = NodeId(2L)
            val folder = mock<TypedFolderNode> { on { name } doReturn "Clients" }
            val file = mock<TypedFileNode> { on { name } doReturn "notes.txt" }
            whenever(getNodeByIdUseCase(folderId)).thenReturn(folder)
            whenever(getNodeByIdUseCase(fileId)).thenReturn(file)

            underTest(listOf(folderId, fileId))

            val captor = argumentCaptor<List<PinnedHomeItem>>()
            verify(settingsRepository).addPinnedHomeItems(captor.capture())
            val items = captor.firstValue
            assertThat(items.map { it.nodeId }).containsExactly(folderId, fileId).inOrder()
            assertThat(items.map { it.name }).containsExactly("Clients", "notes.txt").inOrder()
            assertThat(items.map { it.isFolder }).containsExactly(true, false).inOrder()
        }

    @Test
    fun `test that invoke skips nodes that can no longer be resolved`() = runTest {
        val missingId = NodeId(1L)
        val fileId = NodeId(2L)
        val file = mock<TypedFileNode> { on { name } doReturn "notes.txt" }
        whenever(getNodeByIdUseCase(missingId)).thenReturn(null)
        whenever(getNodeByIdUseCase(fileId)).thenReturn(file)

        underTest(listOf(missingId, fileId))

        val captor = argumentCaptor<List<PinnedHomeItem>>()
        verify(settingsRepository).addPinnedHomeItems(captor.capture())
        assertThat(captor.firstValue.map { it.nodeId }).containsExactly(fileId)
    }

    @Test
    fun `test that invoke does not touch the repository when no nodes resolve`() = runTest {
        whenever(getNodeByIdUseCase(any())).thenReturn(null)

        underTest(listOf(NodeId(1L)))

        verifyNoInteractions(settingsRepository)
    }
}
