package mega.privacy.android.data.mapper

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import nz.mega.sdk.MegaTransfer
import org.junit.Test
import org.mockito.kotlin.mock
import java.math.BigInteger
import kotlin.random.Random

internal class TransferMapperTest {
    @Test
    fun `test that transfer mapper returns correctly`() {
        val megaTransfer = mock<MegaTransfer> {
            on { tag }.thenReturn(Random.nextInt())
            on { type }.thenReturn(MegaTransfer.TYPE_DOWNLOAD)
            on { state }.thenReturn(MegaTransfer.STATE_COMPLETED)
            on { totalBytes }.thenReturn(Random.nextLong())
            on { transferredBytes }.thenReturn(Random.nextLong())
            on { isFinished }.thenReturn(Random.nextBoolean())
            on { fileName }.thenReturn("myFileName")
            on { nodeHandle }.thenReturn(Random.nextLong())
            on { isFolderTransfer }.thenReturn(Random.nextBoolean())
            on { priority }.thenReturn(BigInteger.ONE)
            on { isStreamingTransfer }.thenReturn(true)
            on { notificationNumber }.thenReturn(Random.nextLong())
            on { speed }.thenReturn(Random.nextLong())
            on { appData }.thenReturn("appData")
            on { isForeignOverquota }.thenReturn(Random.nextBoolean())
        }
        val expected = Transfer(
            tag = megaTransfer.tag,
            transferType = TransferType.TYPE_DOWNLOAD,
            totalBytes = megaTransfer.totalBytes,
            transferredBytes = megaTransfer.transferredBytes,
            isFinished = megaTransfer.isFinished,
            transferState = TransferState.STATE_COMPLETED,
            fileName = megaTransfer.fileName,
            handle = megaTransfer.nodeHandle,
            isFolderTransfer = megaTransfer.isFolderTransfer,
            priority = megaTransfer.priority,
            isStreamingTransfer = megaTransfer.isStreamingTransfer,
            notificationNumber = megaTransfer.notificationNumber,
            speed = megaTransfer.speed,
            appData = megaTransfer.appData.orEmpty(),
            isForeignOverQuota = megaTransfer.isForeignOverquota
        )
        Truth.assertThat(TransferMapper().invoke(megaTransfer)).isEqualTo(expected)
    }
}