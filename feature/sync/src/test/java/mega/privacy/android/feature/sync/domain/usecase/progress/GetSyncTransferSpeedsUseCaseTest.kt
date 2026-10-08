package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentMap
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferCounts
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferSpeeds
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferTally
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetSyncTransferSpeedsUseCaseTest {

    private lateinit var underTest: GetSyncTransferSpeedsUseCase

    @BeforeAll
    fun setUp() {
        underTest = GetSyncTransferSpeedsUseCase()
    }

    @Test
    fun `test that invoke hides the upload speed when no upload is pending`() {
        val state = store(
            rows = listOf(
                row(1, SyncTransferDirection.Upload, 300),
                row(2, SyncTransferDirection.Download, 100),
            ),
            pendingDownloads = 1,
            completedUploads = 2,
        )

        assertThat(underTest(state, sdkUploadSpeed = 1_000L, sdkDownloadSpeed = 200L))
            .isEqualTo(SyncTransferSpeeds(uploadSpeed = null, downloadSpeed = 100L))
    }

    @Test
    fun `test that invoke hides the download speed when no download is pending`() {
        val state = store(
            rows = listOf(
                row(1, SyncTransferDirection.Upload, 300),
                row(2, SyncTransferDirection.Download, 100),
            ),
            pendingUploads = 1,
            completedDownloads = 2,
        )

        assertThat(underTest(state, sdkUploadSpeed = 1_000L, sdkDownloadSpeed = 200L))
            .isEqualTo(SyncTransferSpeeds(uploadSpeed = 300L, downloadSpeed = null))
    }

    @Test
    fun `test that invoke hides both speeds when nothing is pending`() {
        val state = store(
            rows = listOf(
                row(1, SyncTransferDirection.Upload, 300),
                row(2, SyncTransferDirection.Download, 100),
            ),
            completedUploads = 3,
            completedDownloads = 3,
        )

        assertThat(underTest(state, sdkUploadSpeed = 1_000L, sdkDownloadSpeed = 200L))
            .isEqualTo(SyncTransferSpeeds(uploadSpeed = null, downloadSpeed = null))
    }

    @ParameterizedTest(name = "rows {0}, sdk {1} -> {2}")
    @MethodSource("directionSpeedCases")
    fun `test that invoke returns the smaller of the row sum and the sdk speed`(
        rowSpeed: Long,
        sdkSpeed: Long?,
        expected: Long,
    ) {
        val uploads = store(
            rows = listOf(row(1, SyncTransferDirection.Upload, rowSpeed)),
            pendingUploads = 1,
        )
        val downloads = store(
            rows = listOf(row(1, SyncTransferDirection.Download, rowSpeed)),
            pendingDownloads = 1,
        )

        assertThat(underTest(uploads, sdkUploadSpeed = sdkSpeed, sdkDownloadSpeed = null).uploadSpeed)
            .isEqualTo(expected)
        assertThat(underTest(downloads, sdkUploadSpeed = null, sdkDownloadSpeed = sdkSpeed).downloadSpeed)
            .isEqualTo(expected)
    }

    private fun directionSpeedCases(): Stream<Arguments> = Stream.of(
        Arguments.of(500L, 400L, 400L),
        Arguments.of(500L, 900L, 500L),
        Arguments.of(500L, null, 500L),
        Arguments.of(500L, 0L, 0L),
        Arguments.of(0L, 900L, 0L),
    )

    @Test
    fun `test that invoke returns the row sum when the sdk speed is higher`() {
        val state = store(
            rows = listOf(
                row(1, SyncTransferDirection.Upload, 300),
                row(2, SyncTransferDirection.Upload, 200),
            ),
            pendingUploads = 2,
        )

        assertThat(underTest(state, sdkUploadSpeed = 900L, sdkDownloadSpeed = null).uploadSpeed)
            .isEqualTo(500L)
    }

    @Test
    fun `test that invoke returns the row sum when the sdk speed is not known`() {
        val state = store(
            rows = listOf(
                row(1, SyncTransferDirection.Upload, 300),
                row(2, SyncTransferDirection.Download, 100),
            ),
            pendingUploads = 1,
            pendingDownloads = 1,
        )

        assertThat(underTest(state, sdkUploadSpeed = null, sdkDownloadSpeed = null))
            .isEqualTo(SyncTransferSpeeds(uploadSpeed = 300L, downloadSpeed = 100L))
    }

    @Test
    fun `test that invoke counts only rows whose sdk state is active`() {
        val state = store(
            rows = listOf(
                row(1, SyncTransferDirection.Upload, 900, TransferState.STATE_QUEUED),
                row(2, SyncTransferDirection.Upload, 100),
                row(3, SyncTransferDirection.Upload, 700, TransferState.STATE_PAUSED),
                row(4, SyncTransferDirection.Download, 800, TransferState.STATE_RETRYING),
                row(5, SyncTransferDirection.Download, 50),
            ),
            pendingUploads = 3,
            pendingDownloads = 2,
        )

        assertThat(underTest(state, sdkUploadSpeed = null, sdkDownloadSpeed = null))
            .isEqualTo(SyncTransferSpeeds(uploadSpeed = 100L, downloadSpeed = 50L))
    }

    @Test
    fun `test that invoke sums the rows of each direction separately`() {
        val state = store(
            rows = listOf(
                row(1, SyncTransferDirection.Upload, 300),
                row(2, SyncTransferDirection.Download, 40),
                row(3, SyncTransferDirection.Upload, 200),
                row(4, SyncTransferDirection.Download, 60),
            ),
            pendingUploads = 2,
            pendingDownloads = 2,
        )

        assertThat(underTest(state, sdkUploadSpeed = null, sdkDownloadSpeed = null))
            .isEqualTo(SyncTransferSpeeds(uploadSpeed = 500L, downloadSpeed = 100L))
    }

    @Test
    fun `test that invoke caps each direction by its own sdk speed`() {
        val state = store(
            rows = listOf(
                row(1, SyncTransferDirection.Upload, 500),
                row(2, SyncTransferDirection.Download, 500),
            ),
            pendingUploads = 1,
            pendingDownloads = 1,
        )

        assertThat(underTest(state, sdkUploadSpeed = 100L, sdkDownloadSpeed = 900L))
            .isEqualTo(SyncTransferSpeeds(uploadSpeed = 100L, downloadSpeed = 500L))
        assertThat(underTest(state, sdkUploadSpeed = 900L, sdkDownloadSpeed = 100L))
            .isEqualTo(SyncTransferSpeeds(uploadSpeed = 500L, downloadSpeed = 100L))
    }

    @Test
    fun `test that invoke returns zero when a direction is pending without active rows`() {
        val state = store(rows = emptyList(), pendingUploads = 1, pendingDownloads = 1)

        assertThat(underTest(state, sdkUploadSpeed = 300L, sdkDownloadSpeed = 300L))
            .isEqualTo(SyncTransferSpeeds(uploadSpeed = 0L, downloadSpeed = 0L))
    }

    private fun row(
        tag: Int,
        direction: SyncTransferDirection,
        speed: Long,
        state: TransferState = TransferState.STATE_ACTIVE,
    ) = SyncTransferRow(
        entry = SyncTransferEntry(
            tag = tag,
            direction = direction,
            state = state,
            fileName = "file$tag.jpg",
            totalBytes = 100,
            transferredBytes = 10,
            speed = speed,
            meanSpeed = 0,
            priority = tag.toLong(),
            notificationNumber = tag.toLong(),
            lastError = null,
            isForeignOverQuota = false,
            path = null,
        ),
        mediaType = SyncMediaType.Image,
        optimisticPause = null,
    )

    private fun store(
        rows: List<SyncTransferRow>,
        pendingUploads: Int = 0,
        pendingDownloads: Int = 0,
        completedUploads: Int = 0,
        completedDownloads: Int = 0,
    ) = SyncTransferStoreState(
        isSeeded = true,
        active = rows.associateBy { it.entry.tag }.toPersistentMap(),
        overflow = persistentMapOf(),
        completed = persistentListOf(),
        counts = SyncTransferCounts.Empty.copy(
            lifetimeUploads = SyncTransferTally(
                total = pendingUploads + completedUploads,
                pending = pendingUploads,
            ),
            lifetimeDownloads = SyncTransferTally(
                total = pendingDownloads + completedDownloads,
                pending = pendingDownloads,
            ),
        ),
        reseedRemoved = persistentSetOf(),
    )
}
