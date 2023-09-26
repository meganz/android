package mega.privacy.android.data.mapper.transfer.sd

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.database.entity.SdTransferEntity
import mega.privacy.android.data.mapper.transfer.TransferAppDataMapper
import mega.privacy.android.domain.entity.SdTransfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SdTransferModelMapperTest {
    private lateinit var underTest: SdTransferModelMapper
    private val decryptData: DecryptData = mock()
    private val transferAppDataMapper = mock<TransferAppDataMapper>()

    @BeforeAll
    fun setup() {
        underTest = SdTransferModelMapper(decryptData, transferAppDataMapper)
    }

    @BeforeEach
    fun reset() {
        reset(decryptData, transferAppDataMapper)
    }

    @Test
    fun `test that mapper returns model correctly when invoke function`() = runTest {
        val appDataRaw = "appData"
        val appData = org.mockito.kotlin.mock<List<TransferAppData>>()
        val entity = SdTransferEntity(
            tag = 0,
            encryptedHandle = "encryptedHandle",
            encryptedName = "encryptedName",
            encryptedSize = "encryptedSize",
            encryptedPath = "encryptedPath",
            encryptedAppData = "encryptedAppData",
        )
        val expected = SdTransfer(
            tag = 0,
            nodeHandle = "2716998339075",
            name = "2023-03-24 00.13.20_1.jpg",
            size = "3.57 MB",
            path = "Cloud drive/Camera uploads",
            appData = appData,
        )
        whenever(transferAppDataMapper(appDataRaw)).thenReturn(appData)
        whenever(decryptData(entity.encryptedHandle)).thenReturn(expected.nodeHandle)
        whenever(decryptData(entity.encryptedName)).thenReturn(expected.name)
        whenever(decryptData(entity.encryptedSize)).thenReturn(expected.size)
        whenever(decryptData(entity.encryptedPath)).thenReturn(expected.path)
        whenever(decryptData(entity.encryptedAppData)).thenReturn(appDataRaw)
        Truth.assertThat(underTest(entity)).isEqualTo(expected)
    }
}