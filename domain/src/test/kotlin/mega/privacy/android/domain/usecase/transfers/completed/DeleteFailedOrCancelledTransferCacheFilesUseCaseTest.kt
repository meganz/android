package mega.privacy.android.domain.usecase.transfers.completed

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.usecase.transfers.DeleteCacheFilesUseCase
import mega.privacy.android.domain.usecase.transfers.GetFailedOrCanceledTransfersUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteFailedOrCancelledTransferCacheFilesUseCaseTest {

    private lateinit var underTest: DeleteFailedOrCancelledTransferCacheFilesUseCase

    private val getFailedOrCanceledTransfersUseCase = mock<GetFailedOrCanceledTransfersUseCase>()
    private val deleteCacheFilesUseCase = mock<DeleteCacheFilesUseCase>()

    @BeforeAll
    fun setup() {
        underTest = DeleteFailedOrCancelledTransferCacheFilesUseCase(
            getFailedOrCanceledTransfersUseCase = getFailedOrCanceledTransfersUseCase,
            deleteCacheFilesUseCase = deleteCacheFilesUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getFailedOrCanceledTransfersUseCase, deleteCacheFilesUseCase)
    }

    @Test
    fun `test that invokes correctly if getFailedOrCanceledTransfersUseCase returns empty list`() =
        runTest {
            whenever(getFailedOrCanceledTransfersUseCase()).thenReturn(emptyList())

            underTest()

            verify(getFailedOrCanceledTransfersUseCase).invoke()
            verifyNoInteractions(deleteCacheFilesUseCase)
        }

    @Test
    fun `test that invokes correctly if getFailedOrCanceledTransfersUseCase does not return an empty list`() =
        runTest {
            val completed = CompletedTransfer(
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
                appData = "appData",
            )
            val failedOrCanceledTransfers = listOf(completed)

            whenever(getFailedOrCanceledTransfersUseCase()).thenReturn(failedOrCanceledTransfers)
            whenever(deleteCacheFilesUseCase(any())).thenReturn(Unit)

            underTest()

            verify(getFailedOrCanceledTransfersUseCase).invoke()
            verify(deleteCacheFilesUseCase).invoke(any())
        }
}