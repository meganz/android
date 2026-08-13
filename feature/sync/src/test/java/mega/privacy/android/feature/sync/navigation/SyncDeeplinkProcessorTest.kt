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

    // Spelled out rather than derived from the production constant, so changing that constant
    // fails here instead of silently redefining what the test checks. This pins the processor's
    // own behaviour only — nothing currently invokes it, so it proves nothing about deeplinks
    // reaching the app.
    private fun urlsProvider() =
        listOf("mega.nz", "mega.app").flatMap { domain ->
            listOf(
                "https://$domain/Sync",
                "https://$domain/Sync/SyncList?selectedChip=SYNC_FOLDERS",
                "https://$domain/Sync/SyncNewFolder",
            )
        }
}
