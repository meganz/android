package mega.privacy.android.domain.extension

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class TimeCacheTest {

    private var currentTime = 0L
    private val timeProvider: () -> Long = { currentTime }

    @BeforeEach
    fun setUp() {
        currentTime = 1000L
    }

    @Test
    fun `test that get should load value when empty`() = runTest {
        val callCount = AtomicInteger(0)
        val cache = TimeCache(timeToLive = 100.milliseconds, loader = {
            callCount.incrementAndGet()
            "value"
        }, timeProvider = timeProvider)

        val result = cache.get()

        assertThat(result).isEqualTo("value")
        assertThat(callCount.get()).isEqualTo(1)
    }

    @Test
    fun `test that get should return cached value if not expired`() = runTest {
        val callCount = AtomicInteger(0)
        val cache = TimeCache(timeToLive = 100.milliseconds, loader = {
            callCount.incrementAndGet()
            "value"
        }, timeProvider = timeProvider)

        cache.get()
        currentTime += 50 // Half of TTL
        val result = cache.get()

        assertThat(result).isEqualTo("value")
        assertThat(callCount.get()).isEqualTo(1)
    }

    @Test
    fun `test that get should reload value if expired`() = runTest {
        val callCount = AtomicInteger(0)
        val cache = TimeCache(timeToLive = 100.milliseconds, loader = {
            val count = callCount.incrementAndGet()
            "value$count"
        }, timeProvider = timeProvider)

        cache.get()
        currentTime += 101 // Past TTL
        val result = cache.get()

        assertThat(result).isEqualTo("value2")
        assertThat(callCount.get()).isEqualTo(2)
    }

    @Test
    fun `test that refresh should always reload value`() = runTest {
        val callCount = AtomicInteger(0)
        val cache = TimeCache(timeToLive = 100.milliseconds, loader = {
            val count = callCount.incrementAndGet()
            "value$count"
        }, timeProvider = timeProvider)

        cache.get()
        val result = cache.refresh()

        assertThat(result).isEqualTo("value2")
        assertThat(callCount.get()).isEqualTo(2)
    }

    @Test
    fun `test that invalidate should make next get reload`() = runTest {
        val callCount = AtomicInteger(0)
        val cache = TimeCache(timeToLive = 100.milliseconds, loader = {
            val count = callCount.incrementAndGet()
            "value$count"
        }, timeProvider = timeProvider)

        cache.get()
        cache.invalidate()
        val result = cache.get()

        assertThat(result).isEqualTo("value2")
        assertThat(callCount.get()).isEqualTo(2)
    }

    @Test
    fun `test that isFresh should return correct state`() = runTest {
        val cache = TimeCache(
            timeToLive = 100.milliseconds,
            loader = { "value" },
            timeProvider = timeProvider
        )

        assertThat(cache.isFresh()).isFalse()
        cache.get()
        assertThat(cache.isFresh()).isTrue()

        currentTime += 101
        assertThat(cache.isFresh()).isFalse()
    }

    @Test
    fun `test that age should return correct duration`() = runTest {
        val cache = TimeCache(
            timeToLive = 100.milliseconds,
            loader = { "value" },
            timeProvider = timeProvider
        )

        assertThat(cache.age()).isNull()
        cache.get()
        assertThat(cache.age()).isEqualTo(0L)

        currentTime += 40
        assertThat(cache.age()).isEqualTo(40L)
    }

    @Test
    fun `test that concurrent gets should only trigger one load`() = runTest {
        val callCount = AtomicInteger(0)
        val cache = TimeCache(timeToLive = 100.milliseconds, loader = {
            kotlinx.coroutines.delay(50)
            callCount.incrementAndGet()
            "value"
        }, timeProvider = timeProvider)

        val deferreds = List(10) {
            async { cache.get() }
        }
        deferreds.awaitAll()

        assertThat(callCount.get()).isEqualTo(1)
    }
}
