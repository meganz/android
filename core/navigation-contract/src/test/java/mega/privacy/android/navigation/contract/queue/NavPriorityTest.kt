package mega.privacy.android.navigation.contract.queue

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class NavPriorityTest {

    @Test
    fun `test that priority item is higher priority than default`() {
        val input = listOf(NavPriority.Default, NavPriority.Priority(0))

        assertThat(input.sorted()).containsExactly(NavPriority.Priority(0), NavPriority.Default)
    }

    @Test
    fun `test that priority items are ordered from biggest to smallest`() {
        val input = listOf(NavPriority.Priority(0), NavPriority.Priority(1))

        assertThat(input.sorted()).containsExactly(NavPriority.Priority(1), NavPriority.Priority(0))
    }
}