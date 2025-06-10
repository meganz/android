package mega.privacy.android.data.mapper.transfer.completed

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.CompletedTransferEntity
import mega.privacy.android.data.mapper.transfer.TransferAppDataMapper
import mega.privacy.android.data.mapper.transfer.TransferStateMapper
import mega.privacy.android.data.mapper.transfer.TransferTypeMapper
import mega.privacy.android.data.wrapper.StringWrapper
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import nz.mega.sdk.MegaError
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CompletedTransferModelMapperTest {
    private lateinit var underTest: CompletedTransferModelMapper

    private val stringWrapper = mock<StringWrapper>()
    private val transferTypeMapper = mock<TransferTypeMapper>()
    private val transferStateMapper = mock<TransferStateMapper>()
    private val transferAppDataMapper = mock<TransferAppDataMapper>()

    @BeforeAll
    fun setup() {
        underTest = CompletedTransferModelMapper(
            stringWrapper,
            transferTypeMapper,
            transferStateMapper,
            transferAppDataMapper,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            stringWrapper,
            transferTypeMapper,
            transferStateMapper,
            transferAppDataMapper,
        )
        whenever(
            transferTypeMapper(transferType.second, transferAppData.first)
        ) doReturn transferType.first
        whenever(transferStateMapper(transferState.second)) doReturn transferState.first
        whenever(transferAppDataMapper(transferAppData.second)) doReturn transferAppData.first
    }

    private val transferAppData = listOf(TransferAppData.CameraUpload) to "appData"
    private val transferType = TransferType.DOWNLOAD to 56
    private val transferState = TransferState.STATE_COMPLETED to 54


    private val entity = CompletedTransferEntity(
        id = 0,
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
        errorCode = null,
        originalPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
        parentHandle = 11622336899311L,
        appData = transferAppData.second,
    )

    @Test
    fun `test that mapper returns model correctly when invoke function`() = runTest {
        val expected = CompletedTransfer(
            id = entity.id,
            fileName = entity.fileName,
            type = transferType.first,
            state = transferState.first,
            size = entity.size,
            handle = entity.handle,
            path = entity.path,
            displayPath = entity.displayPath,
            isOffline = entity.isOffline,
            timestamp = entity.timestamp,
            error = entity.error,
            errorCode = entity.errorCode,
            originalPath = entity.originalPath,
            parentHandle = entity.parentHandle,
            appData = transferAppData.first,
        )

        Truth.assertThat(underTest(entity)).isEqualTo(expected)
    }

    @Test
    fun `test error is localized when there's an error code`() = runTest {
        val expected = "Localized message"
        val errorCode = MegaError.API_EEXPIRED
        val entityWithErrorCode = entity.copy(errorCode = errorCode)
        whenever(stringWrapper.getErrorStringResource(argThat { this.errorCode == errorCode })) doReturn expected

        Truth.assertThat(underTest(entityWithErrorCode).error).isEqualTo(expected)
    }

    @Test
    fun `test loclized error is returned when error code is API_EOVERQUOTA_FOREIGN`() = runTest {
        val expected = "Localized message 2"
        val errorCode = API_EOVERQUOTA_FOREIGN
        val entityWithErrorCode = entity.copy(errorCode = errorCode)
        whenever(stringWrapper.getErrorStorageQuota()) doReturn expected

        Truth.assertThat(underTest(entityWithErrorCode).error).isEqualTo(expected)
    }
}
