package mega.privacy.android.data.mapper.transfer.completed

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.database.entity.CompletedTransferEntity
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class CompletedTransferModelMapperTest {
    private lateinit var underTest: CompletedTransferModelMapper

    private val decryptData: DecryptData = mock()


    @Before
    fun setUp() {
        underTest = CompletedTransferModelMapper(decryptData)
    }

    @Test
    fun `test that mapper returns model correctly when invoke function`() = runTest {
        val entity = CompletedTransferEntity(
            id = 0,
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
        val expected = CompletedTransfer(
            id = 0,
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
        whenever(decryptData(entity.fileName)).thenReturn(entity.fileName)
        whenever(decryptData(entity.type)).thenReturn(entity.type)
        whenever(decryptData(entity.state)).thenReturn(entity.state)
        whenever(decryptData(entity.size)).thenReturn(entity.size)
        whenever(decryptData(entity.handle)).thenReturn(entity.handle)
        whenever(decryptData(entity.path)).thenReturn(entity.path)
        whenever(decryptData(entity.isOffline)).thenReturn(entity.isOffline)
        whenever(decryptData(entity.timestamp)).thenReturn(entity.timestamp)
        whenever(decryptData(entity.error)).thenReturn(entity.error)
        whenever(decryptData(entity.originalPath)).thenReturn(entity.originalPath)
        whenever(decryptData(entity.parentHandle)).thenReturn(entity.parentHandle)

        Truth.assertThat(underTest(entity)).isEqualTo(expected)
    }

    @Test
    fun `test that mapper returns default value when decrypt fails`() = runTest {
        val entity = CompletedTransferEntity(
            id = 0,
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
        val expected = CompletedTransfer(
            id = 0,
            fileName = "",
            type = -1,
            state = -1,
            size = "",
            handle = -1L,
            path = "",
            isOffline = null,
            timestamp = -1L,
            error = null,
            originalPath = "",
            parentHandle = -1L,
        )
        whenever(decryptData(entity.fileName)).thenReturn(null)
        whenever(decryptData(entity.type)).thenReturn(null)
        whenever(decryptData(entity.state)).thenReturn(null)
        whenever(decryptData(entity.size)).thenReturn(null)
        whenever(decryptData(entity.handle)).thenReturn(null)
        whenever(decryptData(entity.path)).thenReturn(null)
        whenever(decryptData(entity.isOffline)).thenReturn(null)
        whenever(decryptData(entity.timestamp)).thenReturn(null)
        whenever(decryptData(entity.error)).thenReturn(null)
        whenever(decryptData(entity.originalPath)).thenReturn(null)
        whenever(decryptData(entity.parentHandle)).thenReturn(null)

        Truth.assertThat(underTest(entity)).isEqualTo(expected)
    }
}
