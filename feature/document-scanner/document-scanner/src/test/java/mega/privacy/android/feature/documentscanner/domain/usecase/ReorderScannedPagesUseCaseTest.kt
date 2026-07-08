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
class ReorderScannedPagesUseCaseTest {

    private val scanSessionRepository = mock<ScanSessionRepository>()
    private lateinit var underTest: ReorderScannedPagesUseCase

    @BeforeEach
    fun setUp() {
        reset(scanSessionRepository)
        underTest = ReorderScannedPagesUseCase(scanSessionRepository)
    }

    @Test
    fun `test that invoke delegates to the repository reorderPages`() = runTest {
        underTest(fromIndex = 2, toIndex = 0)

        verify(scanSessionRepository).reorderPages(2, 0)
    }
}
