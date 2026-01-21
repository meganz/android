package mega.privacy.android.core.nodecomponents.action.eventhandler.mapper

import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.AddToAlbumMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.AvailableOfflineMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.CopyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.FavouriteMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.InfoMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.LabelMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ManageLinkMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RenameMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareFolderMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.SyncMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.mobile.analytics.event.AddToAlbumMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveCopyMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveDownloadMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveFavouriteMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveHideMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveInfoMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveLabelMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveMoveMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveMoveToRubbishBinMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveRenameMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveShareFolderMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveShareLinkMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveSyncMenuItemEvent
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CloudDriveActionEventMapperTest {

    private lateinit var underTest: CloudDriveActionEventMapper

    @BeforeAll
    fun setUp() {
        underTest = CloudDriveActionEventMapper()
    }

    @Test
    fun `test that InfoMenuAction maps to CloudDriveInfoMenuItemEvent`() {
        val action = mock<InfoMenuAction>()
        val result = underTest(action)
        assertThat(result).isEqualTo(CloudDriveInfoMenuItemEvent)
    }

    @Test
    fun `test that FavouriteMenuAction maps to CloudDriveFavouriteMenuItemEvent`() {
        val action = mock<FavouriteMenuAction>()
        val result = underTest(action)
        assertThat(result).isEqualTo(CloudDriveFavouriteMenuItemEvent)
    }

    @Test
    fun `test that LabelMenuAction maps to CloudDriveLabelMenuItemEvent`() {
        val action = mock<LabelMenuAction>()
        val result = underTest(action)
        assertThat(result).isEqualTo(CloudDriveLabelMenuItemEvent)
    }

    @Test
    fun `test that AvailableOfflineMenuAction maps to CloudDriveDownloadMenuItemEvent`() {
        val action = mock<AvailableOfflineMenuAction>()
        val result = underTest(action)
        assertThat(result).isEqualTo(CloudDriveDownloadMenuItemEvent)
    }

    @Test
    fun `test that ManageLinkMenuAction maps to CloudDriveShareLinkMenuItemEvent`() {
        val action = mock<ManageLinkMenuAction>()
        val result = underTest(action)
        assertThat(result).isEqualTo(CloudDriveShareLinkMenuItemEvent)
    }

    @Test
    fun `test that ShareFolderMenuAction maps to CloudDriveShareFolderMenuItemEvent`() {
        val action = mock<ShareFolderMenuAction>()
        val result = underTest(action)
        assertThat(result).isEqualTo(CloudDriveShareFolderMenuItemEvent)
    }

    @Test
    fun `test that RenameMenuAction maps to CloudDriveRenameMenuItemEvent`() {
        val action = mock<RenameMenuAction>()
        val result = underTest(action)
        assertThat(result).isEqualTo(CloudDriveRenameMenuItemEvent)
    }

    @Test
    fun `test that HideMenuAction maps to CloudDriveHideMenuItemEvent`() {
        val action = mock<HideMenuAction>()
        val result = underTest(action)
        assertThat(result).isEqualTo(CloudDriveHideMenuItemEvent)
    }

    @Test
    fun `test that AddToAlbumMenuAction maps to AddToAlbumMenuItemEvent`() {
        val action = mock<AddToAlbumMenuAction>()
        val result = underTest(action)
        assertThat(result).isEqualTo(AddToAlbumMenuItemEvent)
    }

    @Test
    fun `test that MoveMenuAction maps to CloudDriveMoveMenuItemEvent`() {
        val action = mock<MoveMenuAction>()
        val result = underTest(action)
        assertThat(result).isEqualTo(CloudDriveMoveMenuItemEvent)
    }

    @Test
    fun `test that CopyMenuAction maps to CloudDriveCopyMenuItemEvent`() {
        val action = mock<CopyMenuAction>()
        val result = underTest(action)
        assertThat(result).isEqualTo(CloudDriveCopyMenuItemEvent)
    }

    @Test
    fun `test that TrashMenuAction maps to CloudDriveMoveToRubbishBinMenuItemEvent`() {
        val action = mock<TrashMenuAction>()
        val result = underTest(action)
        assertThat(result).isEqualTo(CloudDriveMoveToRubbishBinMenuItemEvent)
    }

    @Test
    fun `test that SyncMenuAction maps to CloudDriveSyncMenuItemEvent`() {
        val action = mock<SyncMenuAction>()
        val result = underTest(action)
        assertThat(result).isEqualTo(CloudDriveSyncMenuItemEvent)
    }

    @Test
    fun `test that unmapped MenuAction returns null`() {
        val action = mock<MenuAction>()
        val result = underTest(action)
        assertThat(result).isNull()
    }
}
