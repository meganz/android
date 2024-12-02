package mega.privacy.android.domain.usecase.node.backup

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [GetBackupsNodeUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetBackupsNodeUseCaseTest {

    private lateinit var underTest: GetBackupsNodeUseCase

    private val nodeRepository = mock<NodeRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetBackupsNodeUseCase(nodeRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository)
    }

    @Test
    fun `test that the backups node is retrieved`() = runTest {
        val testNode = mock<FileNode>()
        whenever(nodeRepository.getBackupsNode()).thenReturn(testNode)

        assertThat(underTest()).isEqualTo(testNode)
    }

    @Test
    fun `test that null is returned if the backups node could not be retrieved`() = runTest {
        whenever(nodeRepository.getBackupsNode()).thenReturn(null)

        assertThat(underTest()).isNull()
    }
}