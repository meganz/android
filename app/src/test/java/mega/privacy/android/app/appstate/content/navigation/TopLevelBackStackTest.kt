package mega.privacy.android.app.appstate.content.navigation

import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TopLevelBackStackTest {

    private lateinit var underTest: TopLevelBackStack<NavKey>

    private data object StartKey : NavKey
    private data object TopLevelKey1 : NavKey
    private data object TopLevelKey2 : NavKey
    private data object Destination1 : NavKey
    private data object Destination2 : NavKey
    private data object Destination3 : NavKey
    private data object Destination4 : NavKey

    @BeforeEach
    fun setUp() {
        underTest = TopLevelBackStack(StartKey)
    }

    @Test
    fun `test that initialization sets startKey correctly`() {
        assertThat(underTest.startKey).isEqualTo(StartKey)
    }

    @Test
    fun `test that initialization sets topLevelKey to startKey`() {
        assertThat(underTest.topLevelKey).isEqualTo(StartKey)
    }

    @Test
    fun `test that initialization creates backStack with startKey`() {
        assertThat(underTest.backStack).containsExactly(StartKey)
    }

    @Test
    fun `test that add adds key to current top level stack`() {
        underTest.add(Destination1)
        assertThat(underTest.backStack).containsExactly(StartKey, Destination1)
    }

    @Test
    fun `test that add adds multiple keys sequentially`() {
        underTest.add(Destination1)
        underTest.add(Destination2)
        underTest.add(Destination3)
        assertThat(underTest.backStack).containsExactly(
            StartKey,
            Destination1,
            Destination2,
            Destination3
        )
    }

    @Test
    fun `test that backstack contains root and current top level`() {
        underTest.add(Destination1)
        underTest.switchTopLevel(TopLevelKey1)
        underTest.add(Destination2)
        assertThat(underTest.topLevelKey).isEqualTo(TopLevelKey1)
        assertThat(underTest.backStack).containsExactly(
            StartKey,
            Destination1,
            TopLevelKey1,
            Destination2
        )
    }

    @Test
    fun `test that switchTopLevel creates new top level if it does not exist`() {
        assertThat(underTest.topLevelBackStacks).hasSize(1)
        underTest.switchTopLevel(TopLevelKey1)
        assertThat(underTest.topLevelKey).isEqualTo(TopLevelKey1)
        assertThat(underTest.topLevelBackStacks).hasSize(2)
    }

    @Test
    fun `test that switchTopLevel preserves startKey stack when switching`() {
        underTest.add(Destination1)
        underTest.switchTopLevel(TopLevelKey1)
        underTest.add(Destination2)
        assertThat(underTest.topLevelBackStacks[StartKey]).containsExactly(StartKey, Destination1)
    }

    @Test
    fun `test that switchTopLevel to startKey only shows startKey stack`() {
        underTest.add(Destination1)
        underTest.switchTopLevel(TopLevelKey1)
        underTest.add(Destination2)
        underTest.switchTopLevel(StartKey)
        assertThat(underTest.backStack).containsExactly(StartKey, Destination1)
    }

    @Test
    fun `test that switchAndAdd creates new top level and adds keys`() {
        assertThat(underTest.topLevelBackStacks).hasSize(1)
        underTest.switchAndAdd(TopLevelKey1, Destination1, Destination2)
        assertThat(underTest.topLevelKey).isEqualTo(TopLevelKey1)
        assertThat(underTest.topLevelBackStacks).hasSize(2)
        assertThat(underTest.topLevelBackStacks[TopLevelKey1]).containsExactly(
            TopLevelKey1,
            Destination1,
            Destination2
        )
    }

    @Test
    fun `test that switchAndAdd switches to existing top level and adds keys`() {
        underTest.add(Destination1)
        underTest.switchTopLevel(TopLevelKey1)
        assertThat(underTest.topLevelBackStacks).hasSize(2)
        underTest.add(Destination2)
        underTest.switchAndAdd(StartKey, Destination3, Destination4)
        assertThat(underTest.topLevelBackStacks).hasSize(2)
        assertThat(underTest.topLevelKey).isEqualTo(StartKey)
        assertThat(underTest.backStack).containsExactly(
            StartKey,
            Destination1,
            Destination3,
            Destination4
        )
    }

    @Test
    fun `test that removeLast removes last item when stack has more than one item`() {
        underTest.add(Destination1)
        underTest.add(Destination2)
        underTest.removeLast()
        assertThat(underTest.backStack).containsExactly(StartKey, Destination1)
    }

    @Test
    fun `test that removeLast switches to startKey when only one item in non-startKey stack`() {
        underTest.switchTopLevel(TopLevelKey1)
        underTest.removeLast()
        assertThat(underTest.topLevelKey).isEqualTo(StartKey)
        assertThat(underTest.backStack).containsExactly(StartKey)
    }

    @Test
    fun `test that removeLast does nothing when stack is at startKey with only startKey`() {
        underTest.removeLast()
        assertThat(underTest.topLevelKey).isEqualTo(StartKey)
        assertThat(underTest.backStack).containsExactly(StartKey)
    }

    @Test
    fun `test that replaceStack replaces current stack with new keys`() {
        underTest.add(Destination1)
        underTest.add(Destination2)
        underTest.replaceStack(Destination3, Destination4)
        assertThat(underTest.backStack).containsExactly(StartKey, Destination3, Destination4)
    }

    @Test
    fun `test that replaceStack with empty vararg creates a stack with only the start key`() {
        underTest.add(Destination1)
        underTest.add(Destination2)
        underTest.replaceStack()
        assertThat(underTest.backStack).containsExactly(StartKey)
    }

    @Test
    fun `test that addAll adds multiple destinations to current stack`() {
        underTest.addAll(listOf(Destination1, Destination2, Destination3))
        assertThat(underTest.backStack).containsExactly(
            StartKey,
            Destination1,
            Destination2,
            Destination3
        )
    }

    @Test
    fun `test that addAll adds to existing stack`() {
        underTest.add(Destination1)
        underTest.addAll(listOf(Destination2, Destination3))
        assertThat(underTest.backStack).containsExactly(
            StartKey,
            Destination1,
            Destination2,
            Destination3
        )
    }

    @Test
    fun `test that switching between top levels preserves each stack independently`() {
        underTest.add(Destination1)
        underTest.switchTopLevel(TopLevelKey1)
        underTest.add(Destination2)
        underTest.switchTopLevel(TopLevelKey2)
        underTest.add(Destination3)
        underTest.switchTopLevel(StartKey)
        val startStack = arrayOf(StartKey, Destination1)
        assertThat(underTest.backStack).containsExactly(*startStack)
        underTest.switchTopLevel(TopLevelKey1)
        assertThat(underTest.backStack).containsExactly(*startStack, TopLevelKey1, Destination2)
        underTest.switchTopLevel(TopLevelKey2)
        assertThat(underTest.backStack).containsExactly(*startStack, TopLevelKey2, Destination3)
    }

    @Test
    fun `test that backStack updates correctly when topLevelKey is startKey`() {
        underTest.add(Destination1)
        underTest.add(Destination2)
        assertThat(underTest.backStack).containsExactly(StartKey, Destination1, Destination2)
    }

    @Test
    fun `test that backStack updates correctly when topLevelKey is not startKey`() {
        underTest.add(Destination1)
        underTest.switchTopLevel(TopLevelKey1)
        underTest.add(Destination2)
        assertThat(underTest.backStack).containsExactly(
            StartKey,
            Destination1,
            TopLevelKey1,
            Destination2
        )
    }

    @Test
    fun `test that multiple switchTopLevel calls maintain correct state`() {
        underTest.add(Destination1)
        underTest.switchTopLevel(TopLevelKey1)
        underTest.add(Destination2)
        underTest.switchTopLevel(TopLevelKey2)
        underTest.add(Destination3)
        underTest.switchTopLevel(StartKey)
        assertThat(underTest.topLevelKey).isEqualTo(StartKey)
        assertThat(underTest.backStack).containsExactly(StartKey, Destination1)
    }

    @Test
    fun `test that removeLast on startKey stack with multiple items removes last item`() {
        underTest.add(Destination1)
        underTest.add(Destination2)
        underTest.add(Destination3)
        underTest.removeLast()
        assertThat(underTest.backStack).containsExactly(StartKey, Destination1, Destination2)
    }

    @Test
    fun `test that replaceStack on non-startKey top level updates backStack correctly`() {
        underTest.add(Destination1)
        underTest.switchTopLevel(TopLevelKey1)
        underTest.add(Destination2)
        underTest.replaceStack(Destination3, Destination4)
        assertThat(underTest.backStack).containsExactly(
            StartKey,
            Destination1,
            TopLevelKey1,
            Destination3,
            Destination4
        )
    }

    @Test
    fun `test that addAll on non-startKey top level updates backStack correctly`() {
        underTest.add(Destination1)
        underTest.switchTopLevel(TopLevelKey1)
        underTest.addAll(listOf(Destination2, Destination3))
        assertThat(underTest.backStack).containsExactly(
            StartKey,
            Destination1,
            TopLevelKey1,
            Destination2,
            Destination3
        )
    }
}