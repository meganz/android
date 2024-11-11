package mega.privacy.android.feature.sync.navigation

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SyncDeeplinkProcessorTest {

    private lateinit var syncDeeplinkProcessor: SyncDeeplinkProcessor

    @BeforeEach
    fun init() {
        syncDeeplinkProcessor = SyncDeeplinkProcessor()
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