package mega.privacy.android.feature.sync.navigation

import kotlinx.coroutines.test.runTest
import mega.privacy.android.shared.sync.domain.IsSyncFeatureEnabledUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class SyncDeeplinkProcessorTest {

    private lateinit var syncDeeplinkProcessor: SyncDeeplinkProcessor
    private val isSyncFeatureEnabledUseCase: IsSyncFeatureEnabledUseCase = mock {
        on { invoke() }.thenReturn(true)
    }

    @BeforeEach
    fun init() {
        syncDeeplinkProcessor = SyncDeeplinkProcessor(isSyncFeatureEnabledUseCase)
    }

    @Test
    fun `test that the sync deep link processor matches sync URLs`() = runTest {
        val urls = listOf(
            "https://mega.nz/${getSyncRoute()}",
            "https://mega.nz/${getSyncListRoute()}",
        )
        urls.forEach { url ->
            assert(syncDeeplinkProcessor.matches(url))
        }
    }
}