package mega.privacy.android.app.domain.usecase

import android.content.Context
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNodeLocationInfoTest {
    private lateinit var underTest: DefaultGetNodeLocationInfo
    private val context = mock<Context>()
    private val nodeRepository = mock<NodeRepository>()

    @BeforeEach
    fun setUp() {
        reset(context, nodeRepository)
        whenever(context.getString(R.string.section_cloud_drive)).thenReturn(CLOUD_DRIVE_STRING)
        whenever(context.getString(R.string.home_side_menu_backups_title)).thenReturn(BACKUPS_STRING)
        whenever(context.getString(sharedR.string.shares_screen_incoming_shares_tab_title))
            .thenReturn(INCOMING_SHARES_STRING)
        whenever(context.getString(R.string.shared_items_verify_credentials_undecrypted_folder))
            .thenReturn(UNDECRYPTED_STRING)
        whenever(context.getString(eq(R.string.location_label), anyVararg<Any>())).thenAnswer {
            val args = it.arguments
            "${args[1]} (${args[2]})"
        }
        underTest = DefaultGetNodeLocationInfo(context, nodeRepository)
    }

    @Test
    fun `test that invoke returns null when root parent cannot be resolved`() = runTest {
        whenever(nodeRepository.getOwnerIdFromInShare(nodeId, true)).thenReturn(null)
        whenever(nodeRepository.getParentNode(nodeId)).thenReturn(null)
        whenever(nodeRepository.getRootParentNode(nodeId)).thenReturn(null)

        val result = underTest(node)

        assertThat(result).isNull()
    }

    @Test
    fun `test that invoke returns incoming shares location when node is from incoming share with parent`() =
        runTest {
            val parent = mockNode(handle = 2L, name = "ParentName", isNodeKeyDecrypted = true)
            val root = mockNode(handle = 3L, name = "RootName")
            whenever(nodeRepository.getOwnerIdFromInShare(nodeId, true)).thenReturn(UserId(1L))
            whenever(nodeRepository.getParentNode(nodeId)).thenReturn(parent)
            whenever(nodeRepository.getRootParentNode(nodeId)).thenReturn(root)
            whenever(nodeRepository.getRootNode()).thenReturn(null)
            whenever(nodeRepository.getRubbishNode()).thenReturn(null)
            whenever(nodeRepository.getBackupsNode()).thenReturn(null)

            val result = underTest(node)

            assertThat(result?.location).isEqualTo("ParentName ($INCOMING_SHARES_STRING)")
            assertThat(result?.parentHandle).isEqualTo(2L)
            assertThat(result?.fragmentHandle).isEqualTo(INVALID_HANDLE)
        }

    @Test
    fun `test that invoke uses undecrypted placeholder when parent node key is not decrypted`() =
        runTest {
            val parent = mockNode(handle = 2L, name = "ParentName", isNodeKeyDecrypted = false)
            val root = mockNode(handle = 3L, name = "RootName")
            whenever(nodeRepository.getOwnerIdFromInShare(nodeId, true)).thenReturn(UserId(1L))
            whenever(nodeRepository.getParentNode(nodeId)).thenReturn(parent)
            whenever(nodeRepository.getRootParentNode(nodeId)).thenReturn(root)
            whenever(nodeRepository.getRootNode()).thenReturn(null)
            whenever(nodeRepository.getRubbishNode()).thenReturn(null)
            whenever(nodeRepository.getBackupsNode()).thenReturn(null)

            val result = underTest(node)

            assertThat(result?.location).isEqualTo("$UNDECRYPTED_STRING ($INCOMING_SHARES_STRING)")
        }

    @Test
    fun `test that invoke returns cloud drive label when node parent is the cloud drive root`() =
        runTest {
            val root = mockNode(handle = 3L, name = "ignored")
            whenever(nodeRepository.getOwnerIdFromInShare(nodeId, true)).thenReturn(null)
            whenever(nodeRepository.getParentNode(nodeId)).thenReturn(root)
            whenever(nodeRepository.getRootParentNode(nodeId)).thenReturn(root)
            whenever(nodeRepository.getRootNode()).thenReturn(root)
            whenever(nodeRepository.getRubbishNode()).thenReturn(null)
            whenever(nodeRepository.getBackupsNode()).thenReturn(null)

            val result = underTest(node)

            assertThat(result?.location).isEqualTo(CLOUD_DRIVE_STRING)
            assertThat(result?.fragmentHandle).isEqualTo(3L)
        }

    @Test
    fun `test that invoke returns parent label inside cloud drive when node is nested`() =
        runTest {
            val parent = mockNode(handle = 4L, name = "Folder")
            val root = mockNode(handle = 3L, name = "ignored")
            whenever(nodeRepository.getOwnerIdFromInShare(nodeId, true)).thenReturn(null)
            whenever(nodeRepository.getParentNode(nodeId)).thenReturn(parent)
            whenever(nodeRepository.getRootParentNode(nodeId)).thenReturn(root)
            whenever(nodeRepository.getRootNode()).thenReturn(root)
            whenever(nodeRepository.getRubbishNode()).thenReturn(null)
            whenever(nodeRepository.getBackupsNode()).thenReturn(null)

            val result = underTest(node)

            assertThat(result?.location).isEqualTo("Folder ($CLOUD_DRIVE_STRING)")
            assertThat(result?.parentHandle).isEqualTo(4L)
            assertThat(result?.fragmentHandle).isEqualTo(3L)
        }

    @Test
    fun `test that invoke returns backups label when node parent is the backups root`() =
        runTest {
            val backups = mockNode(handle = 5L, name = "ignored")
            whenever(nodeRepository.getOwnerIdFromInShare(nodeId, true)).thenReturn(null)
            whenever(nodeRepository.getParentNode(nodeId)).thenReturn(backups)
            whenever(nodeRepository.getRootParentNode(nodeId)).thenReturn(backups)
            whenever(nodeRepository.getRootNode()).thenReturn(null)
            whenever(nodeRepository.getRubbishNode()).thenReturn(null)
            whenever(nodeRepository.getBackupsNode()).thenReturn(backups)

            val result = underTest(node)

            assertThat(result?.location).isEqualTo(BACKUPS_STRING)
            assertThat(result?.fragmentHandle).isEqualTo(INVALID_HANDLE)
        }

    @Test
    fun `test that invoke returns nested backups label when node is deep inside backups`() =
        runTest {
            val parent = mockNode(handle = 6L, name = "Device")
            val backups = mockNode(handle = 5L, name = "ignored")
            whenever(nodeRepository.getOwnerIdFromInShare(nodeId, true)).thenReturn(null)
            whenever(nodeRepository.getParentNode(nodeId)).thenReturn(parent)
            whenever(nodeRepository.getRootParentNode(nodeId)).thenReturn(backups)
            whenever(nodeRepository.getRootNode()).thenReturn(null)
            whenever(nodeRepository.getRubbishNode()).thenReturn(null)
            whenever(nodeRepository.getBackupsNode()).thenReturn(backups)

            val result = underTest(node)

            assertThat(result?.location).isEqualTo("Device ($BACKUPS_STRING)")
            assertThat(result?.parentHandle).isEqualTo(6L)
        }

    @Test
    fun `test that invoke falls back to incoming shares label when top ancestor is unknown`() =
        runTest {
            val parent = mockNode(handle = 7L, name = "Folder")
            val stranger = mockNode(handle = 8L, name = "Stranger")
            whenever(nodeRepository.getOwnerIdFromInShare(nodeId, true)).thenReturn(null)
            whenever(nodeRepository.getParentNode(nodeId)).thenReturn(parent)
            whenever(nodeRepository.getRootParentNode(nodeId)).thenReturn(stranger)
            whenever(nodeRepository.getRootNode()).thenReturn(null)
            whenever(nodeRepository.getRubbishNode()).thenReturn(null)
            whenever(nodeRepository.getBackupsNode()).thenReturn(null)

            val result = underTest(node)

            assertThat(result?.location).isEqualTo("Folder ($INCOMING_SHARES_STRING)")
            assertThat(result?.fragmentHandle).isEqualTo(INVALID_HANDLE)
        }

    private fun mockNode(
        handle: Long,
        name: String,
        isNodeKeyDecrypted: Boolean = true,
    ): FolderNode = mock {
        on { id }.thenReturn(NodeId(handle))
        on { this.name }.thenReturn(name)
        on { this.isNodeKeyDecrypted }.thenReturn(isNodeKeyDecrypted)
    }

    private val node: TypedFolderNode = mock {
        on { id }.thenReturn(nodeId)
    }

    private companion object {
        const val handle = 1L
        val nodeId = NodeId(handle)

        const val CLOUD_DRIVE_STRING = "Cloud drive"
        const val BACKUPS_STRING = "Backups"
        const val INCOMING_SHARES_STRING = "Incoming shares"
        const val UNDECRYPTED_STRING = "[Undecrypted folder]"
    }
}
