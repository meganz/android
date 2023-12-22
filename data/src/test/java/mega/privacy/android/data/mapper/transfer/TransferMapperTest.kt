package mega.privacy.android.data.mapper.transfer

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import nz.mega.sdk.MegaTransfer
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import java.math.BigInteger
import kotlin.random.Random

internal class TransferMapperTest {
    @Test
    fun `test that transfer mapper returns correctly`() {
        val appDataList = listOf(TransferAppData.CameraUpload)
        val transferAppDataMapper = mock<TransferAppDataMapper> {
            on { invoke(APP_DATA_RAW) }.thenReturn(appDataList)
        }
        val transferTypeMapper = mock<TransferTypeMapper> {
            on { invoke(MegaTransfer.TYPE_DOWNLOAD, appDataList) }
                .thenReturn(TransferType.DOWNLOAD)
        }

        val transferStateMapper = mock<TransferStateMapper> {
            on { invoke(MegaTransfer.STATE_COMPLETED) }.thenReturn(TransferState.STATE_COMPLETED)
        }
        val megaTransfer = mockMegaTransfer()

        val expected = Transfer(
            transferType = TransferType.DOWNLOAD,
            transferredBytes = megaTransfer.transferredBytes,
            totalBytes = megaTransfer.totalBytes,
            localPath = megaTransfer.path.orEmpty(),
            parentPath = megaTransfer.parentPath.orEmpty(),
            nodeHandle = megaTransfer.nodeHandle,
            parentHandle = megaTransfer.parentHandle,
            fileName = megaTransfer.fileName,
            stage = TransferStage.STAGE_SCANNING,
            tag = megaTransfer.tag,
            folderTransferTag = megaTransfer.folderTransferTag,
            speed = megaTransfer.speed,
            isForeignOverQuota = megaTransfer.isForeignOverquota,
            isStreamingTransfer = megaTransfer.isStreamingTransfer,
            isFinished = megaTransfer.isFinished,
            isFolderTransfer = megaTransfer.isFolderTransfer,
            appData = appDataList,
            state = TransferState.STATE_COMPLETED,
            priority = megaTransfer.priority,
            notificationNumber = megaTransfer.notificationNumber,
        )
        assertThat(
            TransferMapper(transferAppDataMapper, transferTypeMapper, transferStateMapper).invoke(
                megaTransfer
            )
        )
            .isEqualTo(expected)
    }

    @ParameterizedTest
    @ValueSource(ints = [-1, 0])
    fun `test that folderTransferTag is null when original value is a non positive number`(
        folderTransferTag: Int,
    ) {
        val transferAppDataMapper = mock<TransferAppDataMapper> {
            on { invoke(anyOrNull()) }.thenReturn(emptyList())
        }
        val transferTypeMapper = mock<TransferTypeMapper> {
            on { invoke(anyOrNull(), anyOrNull()) }
                .thenReturn(TransferType.NONE)
        }

        val transferStateMapper = mock<TransferStateMapper> {
            on { invoke(MegaTransfer.STATE_COMPLETED) }.thenReturn(TransferState.STATE_COMPLETED)
        }

        assertThat(
            TransferMapper(transferAppDataMapper, transferTypeMapper, transferStateMapper).invoke(
                mockMegaTransfer(folderTransferTag)
            ).folderTransferTag
        ).isNull()
    }

    private fun mockMegaTransfer(folderTransferTag: Int = 11) =
        mock<MegaTransfer> {
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
            on { this.folderTransferTag }.thenReturn(folderTransferTag)
            on { speed }.thenReturn(Random.nextLong())
            on { isForeignOverquota }.thenReturn(Random.nextBoolean())
            on { isStreamingTransfer }.thenReturn(true)
            on { isFinished }.thenReturn(Random.nextBoolean())
            on { isFolderTransfer }.thenReturn(Random.nextBoolean())
            on { appData }.thenReturn(APP_DATA_RAW)
            on { state }.thenReturn(MegaTransfer.STATE_COMPLETED)
            on { priority }.thenReturn(BigInteger.ONE)
            on { notificationNumber }.thenReturn(Random.nextLong())
        }


    companion object {
        const val APP_DATA_RAW = "appData"
    }
}
