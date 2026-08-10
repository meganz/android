package mega.privacy.android.feature.clouddrive.presentation.clouddrive.model

import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import org.junit.jupiter.api.Test

class CloudDriveUiStateTest {

    @Test
    fun `test that isUploadAllowed returns true when all conditions are met`() {
        val state = createDataState(
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            hasWritePermission = true,
        )

        assertThat(state.isUploadAllowed).isTrue()
    }

    @Test
    fun `test that isUploadAllowed returns false when hasWritePermission is false`() {
        val state = createDataState(
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            hasWritePermission = false,
        )

        assertThat(state.isUploadAllowed).isFalse()
    }

    @Test
    fun `test that isUploadAllowed returns false when nodeSourceType is RUBBISH_BIN`() {
        val state = createDataState(
            nodeSourceType = NodeSourceType.RUBBISH_BIN,
            hasWritePermission = true,
        )

        assertThat(state.isUploadAllowed).isFalse()
    }

    @Test
    fun `test that isUploadAllowed returns false when isNodeInBackups is true`() {
        val state = createDataState(
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            hasWritePermission = true,
            isNodeInBackups = true,
        )

        assertThat(state.isUploadAllowed).isFalse()
    }

    @Test
    fun `test that isMediaDiscoveryAllow returns true when all conditions are met`() {
        val state = createDataState(
            isCloudDriveRoot = false,
            hasMediaItems = true,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        assertThat(state.isMediaDiscoveryAllowed).isTrue()
    }

    @Test
    fun `test that isMediaDiscoveryAllow returns false when nodeSourceType is not CLOUD_DRIVE`() {
        val state = createDataState(
            isCloudDriveRoot = false,
            hasMediaItems = true,
            nodeSourceType = NodeSourceType.RUBBISH_BIN,
        )

        assertThat(state.isMediaDiscoveryAllowed).isFalse()
    }

    @Test
    fun `test that isMediaDiscoveryAllow returns false when hasMediaItems is false`() {
        val state = createDataState(
            isCloudDriveRoot = false,
            hasMediaItems = false,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        assertThat(state.isMediaDiscoveryAllowed).isFalse()
    }

    @Test
    fun `test that isMediaDiscoveryAllow returns false when isCloudDriveRoot is true`() {
        val state = createDataState(
            isCloudDriveRoot = true,
            hasMediaItems = true,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        assertThat(state.isMediaDiscoveryAllowed).isFalse()
    }

    @Test
    fun `test that isMediaDiscoveryAllow returns false when isNodeInBackups is true`() {
        val state = createDataState(
            isCloudDriveRoot = false,
            hasMediaItems = true,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            isNodeInBackups = true,
        )

        assertThat(state.isMediaDiscoveryAllowed).isFalse()
    }

    private fun createDataState(
        nodesLoadingState: NodesLoadingState = NodesLoadingState.FullyLoaded,
        isCloudDriveRoot: Boolean = false,
        hasMediaItems: Boolean = false,
        nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
        hasWritePermission: Boolean = false,
        isNodeInBackups: Boolean = false,
    ) = CloudDriveUiState.Data(
        title = LocalizedText.Literal(""),
        nodesLoadingState = nodesLoadingState,
        currentFolderId = NodeId(-1L),
        isCloudDriveRoot = isCloudDriveRoot,
        items = emptyList(),
        currentViewType = ViewType.LIST,
        navigateBack = consumed,
        hasMediaItems = hasMediaItems,
        selectedSortOrder = SortOrder.ORDER_DEFAULT_ASC,
        selectedSortConfiguration = NodeSortConfiguration.default,
        showContactNotVerifiedBanner = false,
        nodeSourceType = nodeSourceType,
        hasWritePermission = hasWritePermission,
        isNodeInBackups = isNodeInBackups,
    )
}
