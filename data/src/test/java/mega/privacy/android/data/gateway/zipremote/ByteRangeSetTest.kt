package mega.privacy.android.data.gateway.zipremote

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ByteRangeSetTest {

    @Test
    fun `test that missing returns the whole range when nothing is filled`() {
        assertThat(ByteRangeSet().missing(0, 100)).containsExactly(0L..99L)
    }

    @Test
    fun `test that missing returns nothing once the range is filled`() {
        val set = ByteRangeSet().apply { add(0, 100) }

        assertThat(set.missing(10, 90)).isEmpty()
    }

    @Test
    fun `test that missing returns only the gaps between filled ranges`() {
        val set = ByteRangeSet().apply {
            add(10, 20)
            add(40, 50)
        }

        assertThat(set.missing(0, 60)).containsExactly(0L..9L, 20L..39L, 50L..59L).inOrder()
    }

    @Test
    fun `test that add merges overlapping and touching ranges`() {
        val set = ByteRangeSet().apply {
            add(0, 10)
            add(10, 20)
            add(15, 30)
        }

        assertThat(set.filledBytes).isEqualTo(30)
        assertThat(set.missing(0, 30)).isEmpty()
    }

    @Test
    fun `test that add ignores an empty range`() {
        val set = ByteRangeSet().apply { add(5, 5) }

        assertThat(set.filledBytes).isEqualTo(0)
    }
}
