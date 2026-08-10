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
class ReplaceScannedPageUseCaseTest {

    private val scanSessionRepository = mock<ScanSessionRepository>()
    private lateinit var underTest: ReplaceScannedPageUseCase

    @BeforeEach
    fun setUp() {
        reset(scanSessionRepository)
        underTest = ReplaceScannedPageUseCase(scanSessionRepository)
    }

    @Test
    fun `test that invoke delegates to the repository replacePage`() = runTest {
        val newPage = ScannedPage(
            id = "new",
            imageUri = "new.jpg",
            thumbnailUri = "new_thumb.jpg",
            order = 0,
            capturedAt = 0L,
            quality = PageQuality.GOOD,
            boundary = null,
        )

        underTest("old", newPage)

        verify(scanSessionRepository).replacePage("old", newPage)
    }
}
