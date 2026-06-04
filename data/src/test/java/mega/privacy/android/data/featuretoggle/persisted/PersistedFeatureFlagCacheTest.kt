package mega.privacy.android.data.featuretoggle.persisted

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mega.privacy.android.data.gateway.CacheGateway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PersistedFeatureFlagCacheTest {

    @TempDir
    lateinit var tempDir: File

    private val cacheFile: File get() = File(tempDir, "persisted_feature_flags.json")

    private lateinit var cacheGateway: CacheGateway
    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var underTest: PersistedFeatureFlagCache

    @BeforeEach
    fun setUp() {
        cacheGateway = mock {
            on { getCacheFile(any(), any()) } doReturn cacheFile
        }
        underTest = PersistedFeatureFlagCache(
            ioDispatcher = UnconfinedTestDispatcher(),
            cacheGateway = cacheGateway,
        )
    }

    @Test
    fun `test that read returns an empty map when the cache file does not exist`() = runTest {
        assertThat(underTest.read()).isEmpty()
    }

    @Test
    fun `test that read returns an empty map when the gateway cannot locate the file`() =
        runTest {
            val nullGateway = mock<CacheGateway> {
                on { getCacheFile(any(), any()) }.thenReturn(null)
            }
            underTest = PersistedFeatureFlagCache(
                ioDispatcher = UnconfinedTestDispatcher(),
                cacheGateway = nullGateway,
            )
            assertThat(underTest.read()).isEmpty()
        }

    @Test
    fun `test that read returns the persisted map when the file contains valid JSON`() = runTest {
        val expected = mapOf("a" to true, "b" to false)
        cacheFile.writeText(json.encodeToString(expected))

        assertThat(underTest.read()).isEqualTo(expected)
    }

    @Test
    fun `test that read returns an empty map when the file contains invalid JSON`() = runTest {
        cacheFile.writeText("not valid json")

        assertThat(underTest.read()).isEmpty()
    }

    @Test
    fun `test that write persists the map as JSON readable by read`() = runTest {
        val expected = mapOf("a" to true, "b" to false)

        underTest.write(expected)

        assertThat(json.decodeFromString<Map<String, Boolean>>(cacheFile.readText()))
            .isEqualTo(expected)
        assertThat(underTest.read()).isEqualTo(expected)
    }

    @Test
    fun `test that write is a no-op when the gateway cannot locate the file`() = runTest {
        val nullGateway = mock<CacheGateway> {
            on { getCacheFile(any(), any()) }.thenReturn(null)
        }
        underTest = PersistedFeatureFlagCache(
            ioDispatcher = UnconfinedTestDispatcher(),
            cacheGateway = nullGateway,
        )

        underTest.write(mapOf("a" to true))

        assertThat(cacheFile.exists()).isFalse()
    }

    @Test
    fun `test that write overwrites a previously persisted map`() = runTest {
        underTest.write(mapOf("a" to true, "b" to true))
        underTest.write(mapOf("a" to false))

        assertThat(underTest.read()).isEqualTo(mapOf("a" to false))
    }
}
