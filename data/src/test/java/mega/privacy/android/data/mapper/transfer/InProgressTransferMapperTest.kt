package mega.privacy.android.data.mapper.transfer

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.math.BigInteger
import kotlin.random.Random

internal class InProgressTransferMapperTest {

    @Test
    fun `test that mapper returns correctly`() {
        val transfer = mock<Transfer> {
            on { transferType } doReturn TransferType.DOWNLOAD
            on { transferredBytes } doReturn Random.nextLong()
            on { totalBytes } doReturn Random.nextLong()
            on { fileName } doReturn "fileName"
            on { tag } doReturn Random.nextInt()
            on { speed } doReturn Random.nextLong()
            on { state } doReturn TransferState.STATE_COMPLETING
            on { priority } doReturn BigInteger.ONE
            on { isPaused } doReturn false
            on { progress } doReturn Progress(1f)
        }
        val expected = with(transfer) {
            if (transferType.isDownloadType()) {
                InProgressTransfer.Download(
                    tag = tag,
                    totalBytes = totalBytes,
                    fileName = fileName,
                    speed = speed,
                    state = state,
                    priority = priority,
                    isPaused = isPaused,
                    progress = progress,
                    nodeId = NodeId(nodeHandle),
                )
            } else {
                InProgressTransfer.Upload(
                    tag = tag,
                    totalBytes = totalBytes,
                    fileName = fileName,
                    speed = speed,
                    state = state,
                    priority = priority,
                    isPaused = isPaused,
                    progress = progress,
                    localPath = localPath,
                )
            }
        }
        assertThat(InProgressTransferMapper().invoke(transfer)).isEqualTo(expected)
    }
}
