package mega.privacy.android.data.mapper.transfer.completed

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.entity.CompletedTransferEntity
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class CompletedTransferEntityMapperTest {
    private lateinit var underTest: CompletedTransferEntityMapper

    private val encryptData: EncryptData = mock()

    @Before
    fun setUp() {
        underTest = CompletedTransferEntityMapper(encryptData)
    }

    @Test
    fun `test that mapper returns model correctly when invoke function`() = runTest {
        val model = CompletedTransfer(
            fileName = "2023-03-24 00.13.20_1.jpg",
            type = 1,
            state = 6,
            size = "3.57 MB",
            handle = 27169983390750L,
            path = "Cloud drive/Camera uploads",
            isOffline = false,
            timestamp = 1684228012974L,
            error = "No error",
            originalPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
            parentHandle = 11622336899311L,
        )
        val expected = CompletedTransferEntity(
            fileName = "2023-03-24 00.13.20_1.jpg",
            type = "1",
            state = "6",
            size = "3.57 MB",
            handle = "27169983390750",
            path = "Cloud drive/Camera uploads",
            isOffline = "false",
            timestamp = "1684228012974",
            error = "No error",
            originalPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
            parentHandle = "11622336899311",
        )
        whenever(encryptData(model.fileName)).thenReturn(model.fileName)
        whenever(encryptData(model.type.toString())).thenReturn(model.type.toString())
        whenever(encryptData(model.state.toString())).thenReturn(model.state.toString())
        whenever(encryptData(model.size)).thenReturn(model.size)
        whenever(encryptData(model.handle.toString())).thenReturn(model.handle.toString())
        whenever(encryptData(model.path)).thenReturn(model.path)
        whenever(encryptData(model.isOffline.toString())).thenReturn(model.isOffline.toString())
        whenever(encryptData(model.timestamp.toString())).thenReturn(model.timestamp.toString())
        whenever(encryptData(model.error)).thenReturn(model.error)
        whenever(encryptData(model.originalPath)).thenReturn(model.originalPath)
        whenever(encryptData(model.parentHandle.toString())).thenReturn(model.parentHandle.toString())

        Truth.assertThat(underTest(model)).isEqualTo(expected)
    }
}
