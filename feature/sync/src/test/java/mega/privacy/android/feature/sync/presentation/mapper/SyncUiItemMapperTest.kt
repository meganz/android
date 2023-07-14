package mega.privacy.android.feature.sync.presentation.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.FolderPairState
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.ui.mapper.SyncUiItemMapper
import mega.privacy.android.feature.sync.ui.model.SyncStatus
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncUiItemMapperTest {

    private val underTest = SyncUiItemMapper()

    private val folderPairs = listOf(
        FolderPair(
            3L,
            "folderPair",
            "DCIM",
            RemoteFolder(233L, "photos"),
            FolderPairState.RUNNING
        )
    )

    @Test
    fun `test on correct conversion`() {
        val syncUiItems = listOf(
            SyncUiItem(
                id = 3L,
                folderPairName = "folderPair",
                status = SyncStatus.SYNCING,
                deviceStoragePath = "DCIM",
                megaStoragePath = "photos",
                method = "Two-way sync",
                expanded = false
            )
        )

        assertThat(underTest(folderPairs)).isEqualTo(syncUiItems)
    }

    @Test
    fun `test on incorrect conversion`() {
        val syncUiItems = listOf(
            SyncUiItem(
                id = 4L,
                folderPairName = "folderPair",
                status = SyncStatus.SYNCING,
                deviceStoragePath = "DCIM",
                megaStoragePath = "photos",
                method = "Two-way sync",
                expanded = false
            )
        )

        assertThat(underTest(folderPairs)).isNotEqualTo(syncUiItems)
    }
}