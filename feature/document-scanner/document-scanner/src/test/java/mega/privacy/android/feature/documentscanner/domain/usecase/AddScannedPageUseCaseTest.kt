package mega.privacy.android.feature.documentscanner.domain.usecase

import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.documentscanner.domain.entity.PageQuality
import mega.privacy.android.feature.documentscanner.domain.entity.ScannedPage
import mega.privacy.android.feature.documentscanner.domain.repository.ScanSessionRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddScannedPageUseCaseTest {

    private val scanSessionRepository = mock<ScanSessionRepository>()
    private lateinit var underTest: AddScannedPageUseCase

    @BeforeEach
    fun setUp() {
        reset(scanSessionRepository)
        underTest = AddScannedPageUseCase(scanSessionRepository)
    }

    @Test
    fun `test that invoke delegates to the repository addPage`() = runTest {
        val page = ScannedPage(
            id = "p1",
            imageUri = "p1.jpg",
            thumbnailUri = "p1_thumb.jpg",
            order = 0,
            capturedAt = 0L,
            quality = PageQuality.GOOD,
            boundary = null,
        )

        underTest(page)

        verify(scanSessionRepository).addPage(page)
    }
}
