package mega.privacy.android.feature.sync.data.mapper.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.mapper.transfer.TransferStateMapper
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferError
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigInteger

internal class SyncTransferMapperTest {

    private lateinit var underTest: SyncTransferMapper

    @BeforeEach
    fun setUp() {
        underTest = SyncTransferMapper(TransferStateMapper())
    }

    @Test
    fun `test that invoke maps the direction from the transfer type`() {
        val download = underTest(mockSyncMegaTransfer(type = MegaTransfer.TYPE_DOWNLOAD), readPath = false)
        val upload = underTest(mockSyncMegaTransfer(type = MegaTransfer.TYPE_UPLOAD), readPath = false)

        assertThat(download.direction).isEqualTo(SyncTransferDirection.Download)
        assertThat(upload.direction).isEqualTo(SyncTransferDirection.Upload)
    }

    @Test
    fun `test that invoke maps the tag and never reads the unique id`() {
        val transfer = mockSyncMegaTransfer(tag = 7)

        val entry = underTest(transfer, readPath = false)

        assertThat(entry.tag).isEqualTo(7)
        verify(transfer, never()).uniqueId
    }

    @ParameterizedTest(name = "state {0}")
    @ValueSource(
        ints = [
            MegaTransfer.STATE_NONE,
            MegaTransfer.STATE_QUEUED,
            MegaTransfer.STATE_ACTIVE,
            MegaTransfer.STATE_PAUSED,
            MegaTransfer.STATE_RETRYING,
            MegaTransfer.STATE_COMPLETING,
            MegaTransfer.STATE_COMPLETED,
            MegaTransfer.STATE_CANCELLED,
            MegaTransfer.STATE_FAILED,
        ]
    )
    fun `test that invoke maps the sdk state`(state: Int) {
        val entry = underTest(mockSyncMegaTransfer(state = state), readPath = false)

        assertThat(entry.state).isEqualTo(TransferStateMapper()(state))
    }

    @Test
    fun `test that invoke maps transferred and total bytes`() {
        val entry = underTest(
            mockSyncMegaTransfer(transferredBytes = 25L, totalBytes = 100L),
            readPath = false,
        )

        assertThat(entry.transferredBytes).isEqualTo(25L)
        assertThat(entry.totalBytes).isEqualTo(100L)
    }

    @Test
    fun `test that invoke maps file name speed mean speed and priority`() {
        val transfer = mockSyncMegaTransfer(
            fileName = "video.mp4",
            speed = 300L,
            meanSpeed = 200L,
            priority = BigInteger.valueOf(0x800000010000L),
        )

        val entry = underTest(transfer, readPath = false)

        assertThat(entry.fileName).isEqualTo("video.mp4")
        assertThat(entry.speed).isEqualTo(300L)
        assertThat(entry.meanSpeed).isEqualTo(200L)
        assertThat(entry.priority).isEqualTo(0x800000010000L)

        val unnamed = mockSyncMegaTransfer()
        whenever(unnamed.fileName).thenReturn(null)
        assertThat(underTest(unnamed, readPath = false).fileName).isEmpty()
    }

    @Test
    fun `test that invoke converts the sdk priority to a long`() {
        val entry = underTest(
            mockSyncMegaTransfer(priority = BigInteger.valueOf(0x800000000000L)),
            readPath = false,
        )

        assertThat(entry.priority).isEqualTo(140_737_488_355_328L)
    }

    @Test
    fun `test that invoke keeps the notification number`() {
        val entry = underTest(mockSyncMegaTransfer(notificationNumber = 42L), readPath = false)

        assertThat(entry.notificationNumber).isEqualTo(42L)
    }

    @Test
    fun `test that invoke maps the last error code and value`() {
        val entry = underTest(
            mockSyncMegaTransfer(lastError = mockMegaError(MegaError.API_EOVERQUOTA, 3_600L)),
            readPath = false,
        )

        assertThat(entry.lastError).isEqualTo(SyncTransferError(-17, 3_600L))
    }

    @Test
    fun `test that invoke maps no last error when the sdk reports none`() {
        val entry = underTest(mockSyncMegaTransfer(lastError = null), readPath = false)

        assertThat(entry.lastError).isNull()
    }

    @Test
    fun `test that invoke maps no last error when the code is ok`() {
        val entry = underTest(
            mockSyncMegaTransfer(lastError = mockMegaError(MegaError.API_OK)),
            readPath = false,
        )

        assertThat(entry.lastError).isNull()
    }

    @Test
    fun `test that invoke maps foreign over quota for an upload`() {
        val overQuota = mockSyncMegaTransfer(type = MegaTransfer.TYPE_UPLOAD, isForeignOverquota = true)
        val withinQuota = mockSyncMegaTransfer(type = MegaTransfer.TYPE_UPLOAD, isForeignOverquota = false)

        assertThat(underTest(overQuota, readPath = false).isForeignOverQuota).isTrue()
        assertThat(underTest(withinQuota, readPath = false).isForeignOverQuota).isFalse()
        verify(overQuota).isForeignOverquota
        verify(withinQuota).isForeignOverquota
    }

    @Test
    fun `test that invoke does not read foreign over quota for a download`() {
        val transfer = mockSyncMegaTransfer(type = MegaTransfer.TYPE_DOWNLOAD, isForeignOverquota = true)

        val entry = underTest(transfer, readPath = false)

        assertThat(entry.isForeignOverQuota).isFalse()
        verify(transfer, never()).isForeignOverquota
    }

    @Test
    fun `test that invoke reads the path when asked`() {
        val transfer = mockSyncMegaTransfer(path = "content://sync/a.jpg")

        assertThat(underTest(transfer, readPath = true).path).isEqualTo("content://sync/a.jpg")

        whenever(transfer.path).thenReturn(null)
        assertThat(underTest(transfer, readPath = true).path).isNull()
    }

    @ParameterizedTest(name = "state {0}")
    @ValueSource(
        ints = [
            MegaTransfer.STATE_COMPLETED,
            MegaTransfer.STATE_CANCELLED,
            MegaTransfer.STATE_FAILED,
        ]
    )
    fun `test that invoke reads the path of a terminal state without being asked`(state: Int) {
        val transfer = mockSyncMegaTransfer(state = state, path = "content://sync/b.jpg")

        assertThat(underTest(transfer, readPath = false).path).isEqualTo("content://sync/b.jpg")
    }

    @Test
    fun `test that invoke does not read the path of an unfinished transfer`() {
        val transfer = mockSyncMegaTransfer(state = MegaTransfer.STATE_ACTIVE)

        val entry = underTest(transfer, readPath = false)

        assertThat(entry.path).isNull()
        verify(transfer, never()).path
    }

    @Test
    fun `test that invoke never reads the node handle`() {
        val transfer = mockSyncMegaTransfer(state = MegaTransfer.STATE_COMPLETED)

        underTest(transfer, readPath = true)

        verify(transfer, never()).nodeHandle
    }
}
