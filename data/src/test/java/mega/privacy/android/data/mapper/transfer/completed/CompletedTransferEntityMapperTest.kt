package mega.privacy.android.data.mapper.transfer.completed

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.CompletedTransferEntity
import mega.privacy.android.data.mapper.transfer.TransferAppDataStringMapper
import mega.privacy.android.data.mapper.transfer.TransferStateIntMapper
import mega.privacy.android.data.mapper.transfer.TransferTypeIntMapper
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class CompletedTransferEntityMapperTest {
    private lateinit var underTest: CompletedTransferEntityMapper

    private val transferTypeMapper = mock<TransferTypeIntMapper>()
    private val transferStateMapper = mock<TransferStateIntMapper>()
    private val transferAppDataMapper = mock<TransferAppDataStringMapper>()

    @Before
    fun setUp() {
        underTest = CompletedTransferEntityMapper(
            transferTypeMapper,
            transferStateMapper,
            transferAppDataMapper,
        )
    }

    @Test
    fun `test that mapper returns model correctly when invoke function`() = runTest {
        val transferType = TransferType.DOWNLOAD to 56
        whenever(transferTypeMapper(transferType.first)) doReturn transferType.second
        val transferState = TransferState.STATE_COMPLETED to 54
        whenever(transferStateMapper(transferState.first)) doReturn transferState.second
        val transferAppData = listOf(TransferAppData.CameraUpload) to "appData"
        whenever(transferAppDataMapper(transferAppData.first)) doReturn transferAppData.second
        val model = CompletedTransfer(
            fileName = "2023-03-24 00.13.20_1.jpg",
            type = transferType.first,
            state = transferState.first,
            size = "3.57 MB",
            handle = 27169983390750L,
            path = "Cloud drive/Camera uploads",
            displayPath = "display path",
            isOffline = false,
            timestamp = 1684228012974L,
            error = "No error",
            errorCode = 3,
            originalPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
            parentHandle = 11622336899311L,
            appData = transferAppData.first,
        )
        val expected = CompletedTransferEntity(
            fileName = "2023-03-24 00.13.20_1.jpg",
            type = transferType.second,
            state = transferState.second,
            size = "3.57 MB",
            handle = 27169983390750L,
            path = "Cloud drive/Camera uploads",
            displayPath = "display path",
            isOffline = false,
            timestamp = 1684228012974L,
            error = "No error",
            errorCode = 3,
            originalPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
            parentHandle = 11622336899311L,
            appData = transferAppData.second,
        )

        Truth.assertThat(underTest(model)).isEqualTo(expected)
    }
}
