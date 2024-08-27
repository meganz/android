package mega.privacy.android.feature.sync.navigation

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DeeplinkProcessorTest {

    private lateinit var syncDeeplinkProcessor: SyncDeeplinkProcessor

    @BeforeEach
    fun init() {
        syncDeeplinkProcessor = SyncDeeplinkProcessor()
    }

    @Test
    fun `test that the sync deep link processor matches sync URLs`() = runTest {
        val urls = listOf(
            "https://mega.nz/$syncRoute",
            "https://mega.nz/$syncListRoute",
        )
        urls.forEach { url ->
            assert(syncDeeplinkProcessor.matches(url))
        }
    }
}