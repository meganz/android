package mega.privacy.android.data.mapper.transfer.sd

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.entity.SdTransferEntity
import mega.privacy.android.data.mapper.transfer.TransferAppDataStringMapper
import mega.privacy.android.domain.entity.SdTransfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
class SdTransferEntityMapperTest {
    private lateinit var underTest: SdTransferEntityMapper

    private val encryptData: EncryptData = mock()
    private val transferAppDataStringMapper = mock<TransferAppDataStringMapper>()


    @BeforeAll
    fun setup() {
        underTest = SdTransferEntityMapper(encryptData, transferAppDataStringMapper)
    }

    @BeforeEach
    fun reset() {
        reset(encryptData, transferAppDataStringMapper)
    }

    @Test
    fun `test that mapper returns entity correctly when invoke function`() = runTest {
        val appDataRaw = "appData"
        val appData = mock<List<TransferAppData>>()
        val sdTransfer = SdTransfer(
            tag = 0,
            nodeHandle = "2716998339075",
            name = "2023-03-24 00.13.20_1.jpg",
            size = "3.57 MB",
            path = "Cloud drive/Camera uploads",
            appData = appData,
        )
        val encryptedHandle = "encryptedHandle"
        val encryptedName = "encryptedName"
        val encryptedSize = "encryptedSize"
        val encryptedPath = "encryptedPath"
        val encryptedAppData = "encryptedAppData"
        whenever(transferAppDataStringMapper(appData)).thenReturn(appDataRaw)
        whenever(encryptData(sdTransfer.nodeHandle)).thenReturn(encryptedHandle)
        whenever(encryptData(sdTransfer.name)).thenReturn(encryptedName)
        whenever(encryptData(sdTransfer.size)).thenReturn(encryptedSize)
        whenever(encryptData(sdTransfer.path)).thenReturn(encryptedPath)
        whenever(encryptData(appDataRaw)).thenReturn(encryptedAppData)
        val expected = SdTransferEntity(
            tag = 0,
            encryptedHandle = encryptedHandle,
            encryptedName = encryptedName,
            encryptedSize = encryptedSize,
            encryptedPath = encryptedPath,
            encryptedAppData = encryptedAppData,
        )
        Truth.assertThat(underTest(sdTransfer)).isEqualTo(expected)
    }
}