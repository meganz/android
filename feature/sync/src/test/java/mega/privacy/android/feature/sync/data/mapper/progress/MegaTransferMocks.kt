package mega.privacy.android.feature.sync.data.mapper.progress

import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.math.BigInteger

internal fun mockSyncMegaTransfer(
    tag: Int = 1,
    type: Int = MegaTransfer.TYPE_UPLOAD,
    state: Int = MegaTransfer.STATE_ACTIVE,
    fileName: String = "photo.jpg",
    path: String = "content://com.android.externalstorage.documents/tree/primary%3ASync/photo.jpg",
    totalBytes: Long = 100L,
    transferredBytes: Long = 0L,
    speed: Long = 0L,
    meanSpeed: Long = 0L,
    priority: BigInteger = BigInteger.valueOf(0x800000000000L),
    notificationNumber: Long = 1L,
    lastError: MegaError? = null,
    isSyncTransfer: Boolean = true,
    isFolderTransfer: Boolean = false,
    isForeignOverquota: Boolean = false,
): MegaTransfer = mock {
    on { this.tag } doReturn tag
    on { this.type } doReturn type
    on { this.state } doReturn state
    on { this.fileName } doReturn fileName
    on { this.path } doReturn path
    on { this.totalBytes } doReturn totalBytes
    on { this.transferredBytes } doReturn transferredBytes
    on { this.speed } doReturn speed
    on { this.meanSpeed } doReturn meanSpeed
    on { this.priority } doReturn priority
    on { this.notificationNumber } doReturn notificationNumber
    on { this.lastErrorExtended } doReturn lastError
    on { this.isSyncTransfer } doReturn isSyncTransfer
    on { this.isFolderTransfer } doReturn isFolderTransfer
    on { this.isForeignOverquota } doReturn isForeignOverquota
}

internal fun mockMegaError(errorCode: Int, value: Long = 0L): MegaError = mock {
    on { this.errorCode } doReturn errorCode
    on { this.value } doReturn value
}
