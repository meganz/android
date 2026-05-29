package mega.privacy.android.domain.usecase.fileservice

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.fileservice.FileServiceReclaimOptions
import mega.privacy.android.domain.repository.FileServiceRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetFileServiceReclaimOptionsUseCaseTest {

    private lateinit var underTest: SetFileServiceReclaimOptionsUseCase

    private val fileServiceRepository = mock<FileServiceRepository>()

    @BeforeEach
    fun setUp() {
        reset(fileServiceRepository)
        underTest = SetFileServiceReclaimOptionsUseCase(
            fileServiceRepository = fileServiceRepository,
        )
    }

    @Test
    fun `test that invoke applies the same options to both main and public link file services`() =
        runTest {
            val options = mock<FileServiceReclaimOptions>()

            underTest(options)

            inOrder(fileServiceRepository) {
                verify(fileServiceRepository).setReclaimOptions(options)
                verify(fileServiceRepository).setPublicLinkReclaimOptions(options)
            }
        }
}
