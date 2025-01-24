package mega.privacy.android.domain.usecase.transfers

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

class DeleteCompletedTransfersInCacheUseCaseTest {

    private val cacheRepository: CacheRepository = mock()
    private val transferRepository: TransferRepository = mock()

    private lateinit var underTest: DeleteCompletedTransfersInCacheUseCase

    @BeforeEach
    fun setUp() {
        underTest = DeleteCompletedTransfersInCacheUseCase(cacheRepository, transferRepository)
    }

    @Test
    fun `that last separator of path is removed when invoked`() = runTest {
        val path = "/some/path/"
        whenever(cacheRepository.getPreviewDownloadPathForNode()).thenReturn(path)

        underTest.invoke()

        verify(transferRepository).deleteCompletedTransfersByPath("/some/path")
    }

    @AfterEach
    fun tearDown() {
        reset(cacheRepository, transferRepository)
    }
}