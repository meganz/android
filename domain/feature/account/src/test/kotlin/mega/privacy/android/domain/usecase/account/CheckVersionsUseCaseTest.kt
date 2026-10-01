package mega.privacy.android.domain.usecase.account

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CheckVersionsUseCaseTest {
    private lateinit var underTest: CheckVersionsUseCase
    private val nodeRepository: NodeRepository = mock()

    @BeforeEach
    fun setUp() {
        underTest = CheckVersionsUseCase(nodeRepository = nodeRepository)
    }

    @Test
    fun `test that folder tree info is returned when checking account versions`() = runTest {
        val node: FolderNode = mock()
        val folderTreeInfo: FolderTreeInfo = mock()
        whenever(nodeRepository.getRootNode()).thenReturn(node)
        whenever(nodeRepository.getFolderTreeInfo(node)).thenReturn(folderTreeInfo)
        val result = underTest()
        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result).isEqualTo(folderTreeInfo)
    }
}