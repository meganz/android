package mega.privacy.android.data.mapper

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferStage
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
            on { type }.thenReturn(MegaTransfer.TYPE_DOWNLOAD)
            on { transferredBytes }.thenReturn(Random.nextLong())
            on { totalBytes }.thenReturn(Random.nextLong())
            on { path }.thenReturn("path")
            on { parentPath }.thenReturn("parentPath")
            on { nodeHandle }.thenReturn(Random.nextLong())
            on { parentHandle }.thenReturn(Random.nextLong())
            on { fileName }.thenReturn("myFileName")
            on { stage }.thenReturn(MegaTransfer.STAGE_SCAN.toLong())
            on { tag }.thenReturn(Random.nextInt())
            on { speed }.thenReturn(Random.nextLong())
            on { isForeignOverquota }.thenReturn(Random.nextBoolean())
            on { isStreamingTransfer }.thenReturn(true)
            on { isFinished }.thenReturn(Random.nextBoolean())
            on { isFolderTransfer }.thenReturn(Random.nextBoolean())
            on { appData }.thenReturn("appData")
            on { state }.thenReturn(MegaTransfer.STATE_COMPLETED)
            on { priority }.thenReturn(BigInteger.ONE)
            on { notificationNumber }.thenReturn(Random.nextLong())
        }
        val expected = Transfer(
            type = TransferType.TYPE_DOWNLOAD,
            transferredBytes = megaTransfer.transferredBytes,
            totalBytes = megaTransfer.totalBytes,
            localPath = megaTransfer.path,
            parentPath = megaTransfer.parentPath,
            nodeHandle = megaTransfer.nodeHandle,
            parentHandle = megaTransfer.parentHandle,
            fileName = megaTransfer.fileName,
            stage = TransferStage.STAGE_SCANNING,
            tag = megaTransfer.tag,
            speed = megaTransfer.speed,
            isForeignOverQuota = megaTransfer.isForeignOverquota,
            isStreamingTransfer = megaTransfer.isStreamingTransfer,
            isFinished = megaTransfer.isFinished,
            isFolderTransfer = megaTransfer.isFolderTransfer,
            appData = megaTransfer.appData.orEmpty(),
            state = TransferState.STATE_COMPLETED,
            priority = megaTransfer.priority,
            notificationNumber = megaTransfer.notificationNumber,
        )
        Truth.assertThat(TransferMapper().invoke(megaTransfer)).isEqualTo(expected)
    }
}