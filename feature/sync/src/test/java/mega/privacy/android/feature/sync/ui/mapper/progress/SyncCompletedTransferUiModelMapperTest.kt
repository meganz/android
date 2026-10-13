package mega.privacy.android.feature.sync.ui.mapper.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.ui.progress.entry
import mega.privacy.android.feature.sync.ui.progress.finished
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncCompletedTransferUiModelMapperTest {

    private lateinit var underTest: SyncCompletedTransferUiModelMapper

    @BeforeEach
    fun setUp() {
        underTest = SyncCompletedTransferUiModelMapper()
    }

    @Test
    fun `test that invoke keeps the finish time in milliseconds`() {
        val item = finished(entry(tag = 1), finishedAtMillis = 1_727_700_000_123L)

        assertThat(underTest(item.transfer).finishedAtMillis).isEqualTo(1_727_700_000_123L)
    }

    @Test
    fun `test that invoke copies the location from the entry`() {
        val path = "content://com.android.externalstorage.documents/tree/primary%3ASync/Trip/IMG_1.jpg"

        val result = underTest(finished(entry(tag = 1, path = path, location = "Camera")).transfer)

        assertThat(result.location).isEqualTo("Camera")
    }

    @Test
    fun `test that invoke maps a missing location to empty`() {
        val path = "/storage/emulated/0/MEGA Sync/Photos/a.jpg"

        val result = underTest(finished(entry(tag = 1, path = path, location = null)).transfer)

        assertThat(result.location).isEmpty()
    }

    @Test
    fun `test that invoke maps the size and the average speed`() {
        val withoutMeanSpeed = underTest(finished(entry(tag = 1, totalBytes = 2_048L, meanSpeed = 0L)).transfer)
        val withMeanSpeed = underTest(finished(entry(tag = 2, totalBytes = 2_048L, meanSpeed = 512L)).transfer)

        assertThat(withoutMeanSpeed.sizeBytes).isEqualTo(2_048L)
        assertThat(withoutMeanSpeed.averageSpeed).isEqualTo(2_048L)
        assertThat(withMeanSpeed.sizeBytes).isEqualTo(2_048L)
        assertThat(withMeanSpeed.averageSpeed).isEqualTo(512L)
    }

    @Test
    fun `test that invoke keeps the direction and the file name`() {
        val item = finished(
            entry(tag = 1, direction = SyncTransferDirection.Download, fileName = "clip.mp4"),
        )

        val result = underTest(item.transfer)

        assertThat(result.direction).isEqualTo(SyncTransferDirection.Download)
        assertThat(result.fileName).isEqualTo("clip.mp4")
    }
}
