package mega.privacy.android.feature.sync.navigation

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncDeeplinkProcessorTest {

    private lateinit var syncDeeplinkProcessor: SyncDeeplinkProcessor

    @BeforeEach
    fun init() {
        syncDeeplinkProcessor = SyncDeeplinkProcessor(mock())
    }

    @ParameterizedTest
    @MethodSource("urlsProvider")
    fun `test that the sync deep link processor matches sync URLs`(url: String) = runTest {
        assert(syncDeeplinkProcessor.matches(url))
    }

    private fun urlsProvider() =
        listOf("mega.nz", "mega.app").flatMap { domain ->
            listOf(
                "https://$domain/${getSyncRoute()}",
                "https://$domain/${getSyncListRoute()}",
            )
        }
}
