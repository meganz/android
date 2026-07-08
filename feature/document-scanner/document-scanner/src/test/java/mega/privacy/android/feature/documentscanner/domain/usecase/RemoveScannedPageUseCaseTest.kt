package mega.privacy.android.feature.documentscanner.domain.usecase

import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.documentscanner.domain.repository.ScanSessionRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoveScannedPageUseCaseTest {

    private val scanSessionRepository = mock<ScanSessionRepository>()
    private lateinit var underTest: RemoveScannedPageUseCase

    @BeforeEach
    fun setUp() {
        reset(scanSessionRepository)
        underTest = RemoveScannedPageUseCase(scanSessionRepository)
    }

    @Test
    fun `test that invoke delegates to the repository removePage`() = runTest {
        underTest("page-1")

        verify(scanSessionRepository).removePage("page-1")
    }
}
