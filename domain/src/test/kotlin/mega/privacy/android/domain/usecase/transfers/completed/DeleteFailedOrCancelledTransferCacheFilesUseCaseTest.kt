package mega.privacy.android.domain.usecase.transfers.completed

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.repository.CacheRepository
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
    private val cacheRepository = mock<CacheRepository>()

    @BeforeAll
    fun setup() {
        underTest = DeleteFailedOrCancelledTransferCacheFilesUseCase(
            getFailedOrCanceledTransfersUseCase = getFailedOrCanceledTransfersUseCase,
            cacheRepository = cacheRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getFailedOrCanceledTransfersUseCase, cacheRepository)
    }

    @Test
    fun `test that use case invokes use case but not repository if no failed or cancelled transfers`() =
        runTest {
            whenever(getFailedOrCanceledTransfersUseCase()).thenReturn(emptyList())

            underTest()

            verify(getFailedOrCanceledTransfersUseCase).invoke()
            verifyNoInteractions(cacheRepository)
        }

    @Test
    fun `test that use case invokes use case and repository`() = runTest {
        val completed1 = CompletedTransfer(
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
        val failedOrCanceledTransfers = listOf(completed1)

        whenever(getFailedOrCanceledTransfersUseCase()).thenReturn(failedOrCanceledTransfers)
        whenever(cacheRepository.isFileInCacheDirectory(any())).thenReturn(true)

        underTest()

        verify(getFailedOrCanceledTransfersUseCase).invoke()
        verify(cacheRepository).isFileInCacheDirectory(any())
    }
}