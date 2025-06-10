package mega.privacy.android.data.mapper.transfer.completed

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.database.entity.CompletedTransferEntity
import mega.privacy.android.data.database.entity.CompletedTransferEntityLegacy
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CompletedTransferLegacyModelMapperTest {
    private lateinit var underTest: CompletedTransferLegacyModelMapper

    private val decryptData: DecryptData = mock()
    private val completedTransferModelMapper = mock<CompletedTransferModelMapper>()


    @BeforeAll
    fun setup() {
        underTest = CompletedTransferLegacyModelMapper(
            decryptData,
            completedTransferModelMapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            decryptData,
            completedTransferModelMapper,
        )
    }

    @Test
    fun `test that mapper returns model correctly when invoke function`() = runTest {
        val legacy = CompletedTransferEntityLegacy(
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
            appData = "appData",
        )
        val entity = with(legacy) {
            CompletedTransferEntity(
                id = id,
                fileName = fileName.orEmpty(),
                type = 1,
                state = 6,
                size = size.orEmpty(),
                handle = handle?.toLongOrNull() ?: -1L,
                path = path.orEmpty(),
                displayPath = null,
                isOffline = isOffline?.toBoolean(),
                timestamp = timestamp?.toLongOrNull() ?: -1L,
                error = error.orEmpty(),
                errorCode = null,
                originalPath = originalPath.orEmpty(),
                parentHandle = parentHandle?.toLongOrNull() ?: -1,
                appData = appData
            )
        }
        val expected = CompletedTransfer(
            id = 0,
            fileName = "2023-03-24 00.13.20_1.jpg",
            type = TransferType.DOWNLOAD,
            state = TransferState.STATE_FAILED,
            size = "3.57 MB",
            handle = 27169983390750L,
            path = "Cloud drive/Camera uploads",
            isOffline = false,
            timestamp = 1684228012974L,
            error = "No error",
            originalPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
            parentHandle = 11622336899311L,
            appData = null,
            displayPath = null,
            errorCode = null,
        )

        whenever(completedTransferModelMapper(entity)) doReturn expected
        whenever(decryptData(legacy.fileName)).thenReturn(legacy.fileName)
        whenever(decryptData(legacy.type)).thenReturn(legacy.type)
        whenever(decryptData(legacy.state)).thenReturn(legacy.state)
        whenever(decryptData(legacy.size)).thenReturn(legacy.size)
        whenever(decryptData(legacy.handle)).thenReturn(legacy.handle)
        whenever(decryptData(legacy.path)).thenReturn(legacy.path)
        whenever(decryptData(legacy.isOffline)).thenReturn(legacy.isOffline)
        whenever(decryptData(legacy.timestamp)).thenReturn(legacy.timestamp)
        whenever(decryptData(legacy.error)).thenReturn(legacy.error)
        whenever(decryptData(legacy.originalPath)).thenReturn(legacy.originalPath)
        whenever(decryptData(legacy.parentHandle)).thenReturn(legacy.parentHandle)
        whenever(decryptData(legacy.appData)).thenReturn(legacy.appData)

        Truth.assertThat(underTest(legacy)).isEqualTo(expected)
    }

    @Test
    fun `test that mapper returns null when decrypt fails`() = runTest {
        val entity = CompletedTransferEntityLegacy(
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
            appData = "appData",
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
        whenever(decryptData(entity.appData)).thenReturn(null)

        Truth.assertThat(underTest(entity)).isNull()
    }
}
