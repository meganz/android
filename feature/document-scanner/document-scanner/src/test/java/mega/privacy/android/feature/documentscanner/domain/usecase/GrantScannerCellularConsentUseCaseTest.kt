package mega.privacy.android.feature.documentscanner.domain.usecase

import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.documentscanner.domain.repository.ScannerPreferencesRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GrantScannerCellularConsentUseCaseTest {

    private lateinit var underTest: GrantScannerCellularConsentUseCase

    private val scannerPreferences = mock<ScannerPreferencesRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GrantScannerCellularConsentUseCase(scannerPreferences)
    }

    @BeforeEach
    fun resetMocks() {
        reset(scannerPreferences)
    }

    @Test
    fun `test that invoke grants cellular consent on the repository`() = runTest {
        underTest()

        verify(scannerPreferences).grantCellularConsent()
    }
}
