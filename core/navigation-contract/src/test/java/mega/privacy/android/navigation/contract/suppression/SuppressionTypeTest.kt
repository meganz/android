package mega.privacy.android.navigation.contract.suppression

import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.navigation.contract.queue.QueueEvent
import org.junit.jupiter.api.Test

class SuppressionTypeTest {

    @Test
    fun `test that None does not suppress event with suppressable key`() {
        val event = TestQueueEvent(suppressableKey = TestNavKey("a"))

        assertThat(SuppressionType.None.suppressesEvent(event)).isFalse()
    }

    @Test
    fun `test that None does not suppress event without suppressable key`() {
        val event = TestQueueEvent(suppressableKey = null)

        assertThat(SuppressionType.None.suppressesEvent(event)).isFalse()
    }

    @Test
    fun `test that Complete suppresses event with suppressable key`() {
        val event = TestQueueEvent(suppressableKey = TestNavKey("a"))

        assertThat(SuppressionType.Complete.suppressesEvent(event)).isTrue()
    }

    @Test
    fun `test that Complete does not suppress event without suppressable key`() {
        val event = TestQueueEvent(suppressableKey = null)

        assertThat(SuppressionType.Complete.suppressesEvent(event)).isFalse()
    }

    @Test
    fun `test that WithExceptions suppresses event when suppressable key is not in exceptions`() {
        val exception = TestNavKey("a")
        val event = TestQueueEvent(suppressableKey = TestNavKey("b"))

        val underTest = SuppressionType.WithExceptions(exceptions = listOf(exception))

        assertThat(underTest.suppressesEvent(event)).isTrue()
    }

    @Test
    fun `test that WithExceptions does not suppress event when suppressable key is in exceptions`() {
        val key = TestNavKey("a")
        val event = TestQueueEvent(suppressableKey = key)

        val underTest = SuppressionType.WithExceptions(exceptions = listOf(key))

        assertThat(underTest.suppressesEvent(event)).isFalse()
    }

    @Test
    fun `test that WithExceptions does not suppress event when suppressable key is null`() {
        val event = TestQueueEvent(suppressableKey = null)

        val underTest = SuppressionType.WithExceptions(exceptions = listOf(TestNavKey("a")))

        assertThat(underTest.suppressesEvent(event)).isFalse()
    }

    @Test
    fun `test that WithExceptions does not suppress event when exceptions is empty and key is null`() {
        val event = TestQueueEvent(suppressableKey = null)

        val underTest = SuppressionType.WithExceptions(exceptions = emptyList())

        assertThat(underTest.suppressesEvent(event)).isFalse()
    }

    @Test
    fun `test that WithExceptions suppresses event when exceptions is empty and key is present`() {
        val event = TestQueueEvent(suppressableKey = TestNavKey("a"))

        val underTest = SuppressionType.WithExceptions(exceptions = emptyList())

        assertThat(underTest.suppressesEvent(event)).isTrue()
    }

    private data class TestQueueEvent(override val suppressableKey: NavKey?) : QueueEvent

    private data class TestNavKey(val id: String) : NavKey
}