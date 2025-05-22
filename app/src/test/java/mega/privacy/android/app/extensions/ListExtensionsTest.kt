package mega.privacy.android.app.extensions

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ListExtensionsTest {
    @Nested
    inner class MoveElement {
        @Test
        fun `test that move one position forward works correctly`() {
            val list = mutableListOf(1, 2, 3, 4)
            list.moveElement(fromIndex = 1, toIndex = 2)
            val expected = listOf(1, 3, 2, 4)
            assertThat(list).containsExactlyElementsIn(expected).inOrder()
        }

        @Test
        fun `test that move one position backward works correctly`() {
            val list = mutableListOf(1, 2, 3, 4)
            list.moveElement(fromIndex = 2, toIndex = 1)
            val expected = listOf(1, 3, 2, 4)
            assertThat(list).containsExactlyElementsIn(expected).inOrder()
        }

        @Test
        fun `test that move multiple positions forward works correctly`() {
            val list = mutableListOf(1, 2, 3, 4, 5, 6)
            list.moveElement(fromIndex = 1, toIndex = 4)
            val expected = listOf(1, 3, 4, 5, 2, 6)
            assertThat(list).containsExactlyElementsIn(expected).inOrder()
        }

        @Test
        fun `test that move multiple positions backward works correctly`() {
            val list = mutableListOf(1, 2, 3, 4, 5, 6)
            list.moveElement(fromIndex = 4, toIndex = 1)
            val expected = listOf(1, 5, 2, 3, 4, 6)
            assertThat(list).containsExactlyElementsIn(expected).inOrder()
        }

        @Test
        fun `test that move to same position works correctly`() {
            val list = mutableListOf(1, 2, 3, 4)
            list.moveElement(fromIndex = 2, toIndex = 2)
            assertThat(list).containsExactlyElementsIn(list).inOrder()
        }
    }

    data class Item(val id: Int, val name: String)

    @Nested
    inner class MatchOrderWithNewAtEnd {

        private fun reorderWithNewAtEnd(
            currentList: List<Item>,
            updatedList: List<Item>,
        ): List<Item> = updatedList.matchOrderWithNewAtEnd(currentList) { it.id }

        @Test
        fun `test that preserves order of existing items and appends new`() {
            val current = listOf(Item(1, "A"))
            val updated = listOf(Item(3, "C"), Item(2, "B"), Item(1, "A"), Item(4, "D"))

            val expected = listOf(Item(1, "A"), Item(3, "C"), Item(2, "B"), Item(4, "D"))
            val result = reorderWithNewAtEnd(current, updated)

            assertThat(result).containsExactlyElementsIn(expected).inOrder()
        }

        @Test
        fun `test that preserves current order by key but with new values`() {
            val current = listOf(Item(1, "A"), Item(2, "B"), Item(3, "C"))
            val updated = listOf(Item(3, "C2"), Item(2, "B2"), Item(1, "A2"))

            val expected = listOf(Item(1, "A2"), Item(2, "B2"), Item(3, "C2"))
            val result = reorderWithNewAtEnd(current, updated)

            assertThat(result).containsExactlyElementsIn(expected).inOrder()
        }

        @Test
        fun `test that handles empty current list`() {
            val current = emptyList<Item>()
            val updated = listOf(Item(3, "C"), Item(2, "B"))

            val expected = listOf(Item(3, "C"), Item(2, "B"))
            val result = reorderWithNewAtEnd(current, updated)

            assertThat(result).containsExactlyElementsIn(expected).inOrder()
        }

        @Test
        fun `test that handles removed items`() {
            val current = listOf(Item(1, "A"), Item(2, "B"), Item(3, "C"))
            val updated = listOf(Item(3, "C"), Item(2, "B")) // Item(1) removed

            val expected = listOf(Item(2, "B"), Item(3, "C"))
            val result = reorderWithNewAtEnd(current, updated)

            assertThat(result).containsExactlyElementsIn(expected).inOrder()
        }
    }
}