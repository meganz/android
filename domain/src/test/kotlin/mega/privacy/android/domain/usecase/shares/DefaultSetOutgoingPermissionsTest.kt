package mega.privacy.android.domain.usecase.shares

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.exception.ShareAccessNotSetException
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSetOutgoingPermissionsTest {
    private val nodeRepository = mock<NodeRepository>()
    private val underTest: SetOutgoingPermissions =
        DefaultSetOutgoingPermissions(nodeRepository)


    @Test
    fun `test that when share access is set then nodeRepository is called for each user`() =
        runTest {
            val block = mock<((AccessPermission, String) -> Unit)>()
            whenever(nodeRepository.createShareKey(folderNode)).thenReturn(block)
            underTest.invoke(folderNode, accessPermission, *emails)

            emails.forEach {
                verify(block).invoke(accessPermission, it)
            }
        }

    @Test
    fun `test that when an error rises the number of not set users is returned as an exception with correct amount`() =
        runTest {
            val successSet = emails.filterIndexed { index, _ -> index.mod(2) == 0 }
            val failSet = emails.asList() - successSet.toSet()
            val block = mock<((AccessPermission, String) -> Unit)>()
            successSet.forEach {
                whenever(block(accessPermission, it)).thenReturn(Unit)
            }
            failSet.forEach {
                whenever(block(accessPermission, it)).thenThrow(RuntimeException::class.java)
            }
            whenever(nodeRepository.createShareKey(folderNode)).thenReturn(block)
            val exception = runCatching {
                underTest.invoke(folderNode, accessPermission, *emails)
            }.exceptionOrNull()
            Truth.assertThat(exception).isNotNull()
            Truth.assertThat(exception).isInstanceOf(ShareAccessNotSetException::class.java)
            (exception as? ShareAccessNotSetException)?.let {
                Truth.assertThat(it.totalNotSet).isEqualTo(failSet.size)
            }
        }

    private companion object {
        val accessPermission = AccessPermission.READ
        val nodeId = NodeId(1L)
        val folderNode = mock<TypedFolderNode> {
            on { id }.thenReturn(nodeId)
        }
        val emails = Array(10) { "example$it@example.com" }
    }
}