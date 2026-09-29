package mega.privacy.android.core.nodecomponents.menu.menuitem

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.menu.menuaction.AvailableOfflineMenuAction
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.offline.IsNodeUnderOfflineRootUseCase
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.foldernode.IsFolderEmptyUseCase
import mega.privacy.android.domain.usecase.transfers.offline.IsOfflineDownloadInProgressUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AvailableOfflineBottomSheetMenuItemTest {

    private val isFolderEmptyUseCase = mock<IsFolderEmptyUseCase>()

    /**
     * Only folders consult the offline database, and only once they already have an offline copy;
     * these tests exercise files, which answer from the node itself.
     */
    private val isNodeUnderOfflineRootUseCase = mock<IsNodeUnderOfflineRootUseCase>()

    private val isOfflineDownloadInProgressUseCase = mock<IsOfflineDownloadInProgressUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val underTest = AvailableOfflineBottomSheetMenuItem(
        menuAction = mock<AvailableOfflineMenuAction>(),
        isFolderEmptyUseCase = isFolderEmptyUseCase,
        isNodeUnderOfflineRootUseCase = isNodeUnderOfflineRootUseCase,
        isOfflineDownloadInProgressUseCase = isOfflineDownloadInProgressUseCase,
        getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
    )

    @BeforeEach
    fun commonStubs() = runTest {
        whenever(isOfflineDownloadInProgressUseCase(any())).thenReturn(false)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.OfflineDownloadInProgressState)).thenReturn(true)
    }

    @Test
    fun `test that shouldDisplay returns true when node is not available offline and folder is not empty`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { isAvailableOffline } doReturn false
                on { isTakenDown } doReturn false
                on { isNodeKeyDecrypted } doReturn true
            }
            whenever(isFolderEmptyUseCase(node)).thenReturn(false)
            val result = underTest.shouldDisplay(
                isNodeInRubbish = false,
                accessPermission = null,
                isInBackups = false,
                node = node,
                isConnected = true,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE
            )
            assertThat(result).isTrue()
        }

    @Test
    fun `test that shouldDisplay returns false when node is available offline`() = runTest {
        val node = mock<TypedFileNode> {
            on { isAvailableOffline } doReturn true
            on { isTakenDown } doReturn false
            on { isNodeKeyDecrypted } doReturn true
        }
        whenever(isFolderEmptyUseCase(node)).thenReturn(false)
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when node is taken down`() = runTest {
        val node = mock<TypedFileNode> {
            on { isAvailableOffline } doReturn false
            on { isTakenDown } doReturn true
            on { isNodeKeyDecrypted } doReturn true
        }
        whenever(isFolderEmptyUseCase(node)).thenReturn(false)
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when node is in rubbish`() = runTest {
        val node = mock<TypedFileNode> {
            on { isAvailableOffline } doReturn false
            on { isTakenDown } doReturn false
            on { isNodeKeyDecrypted } doReturn true
        }
        whenever(isFolderEmptyUseCase(node)).thenReturn(false)
        val result = underTest.shouldDisplay(
            isNodeInRubbish = true,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when folder is empty`() = runTest {
        val node = mock<TypedFileNode> {
            on { isAvailableOffline } doReturn false
            on { isTakenDown } doReturn false
            on { isNodeKeyDecrypted } doReturn true
        }
        whenever(isFolderEmptyUseCase(node)).thenReturn(true)
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when node is S4 container`() = runTest {
        val folderNode = mock<TypedFolderNode> {
            on { isAvailableOffline } doReturn false
            on { isTakenDown } doReturn false
            on { isS4Container } doReturn true
            on { isNodeKeyDecrypted } doReturn true
        }
        whenever(isFolderEmptyUseCase(folderNode)).thenReturn(false)
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = folderNode,
            isConnected = true,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when node key is not decrypted`() = runTest {
        val node = mock<TypedFileNode> {
            on { isAvailableOffline } doReturn false
            on { isTakenDown } doReturn false
            on { isNodeKeyDecrypted } doReturn false
        }
        whenever(isFolderEmptyUseCase(node)).thenReturn(false)
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when an offline download is in progress for the node`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { isAvailableOffline } doReturn false
                on { isTakenDown } doReturn false
                on { isNodeKeyDecrypted } doReturn true
            }
            whenever(isFolderEmptyUseCase(node)).thenReturn(false)
            whenever(isOfflineDownloadInProgressUseCase(node)).thenReturn(true)
            val result = underTest.shouldDisplay(
                isNodeInRubbish = false,
                accessPermission = null,
                isInBackups = false,
                node = node,
                isConnected = true,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE
            )
            assertThat(result).isFalse()
        }

    @Test
    fun `test that shouldDisplay returns true when an offline download is in progress and the feature flag is disabled`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { isAvailableOffline } doReturn false
                on { isTakenDown } doReturn false
                on { isNodeKeyDecrypted } doReturn true
            }
            whenever(isFolderEmptyUseCase(node)).thenReturn(false)
            whenever(isOfflineDownloadInProgressUseCase(node)).thenReturn(true)
            whenever(getFeatureFlagValueUseCase(ApiFeatures.OfflineDownloadInProgressState)).thenReturn(false)
            val result = underTest.shouldDisplay(
                isNodeInRubbish = false,
                accessPermission = null,
                isInBackups = false,
                node = node,
                isConnected = true,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE
            )
            assertThat(result).isTrue()
        }
}
