package mega.privacy.android.app.appstate.content.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class PendingBackStackTest {

    @Serializable
    private data object Key1 : NavKey

    @Serializable
    private data object Key2 : NavKey

    @Test
    fun `test that pending setter removes duplicate keys`() {
        val underTest = PendingBackStack<NavKey>(NavBackStack())

        underTest.pending = listOf(Key1, Key2, Key1, Key1)

        assertThat(underTest.pending).containsExactly(Key1, Key2).inOrder()
    }

    @Test
    fun `test that plusAssign does not create duplicate pending keys`() {
        val underTest = PendingBackStack<NavKey>(NavBackStack())

        underTest.pending += Key1
        underTest.pending += Key1

        assertThat(underTest.pending).containsExactly(Key1)
    }

    @Test
    fun `test that prepending a list does not create duplicate pending keys`() {
        val underTest = PendingBackStack<NavKey>(NavBackStack())
        underTest.pending = listOf(Key1)

        underTest.pending = listOf(Key2, Key1) + underTest.pending

        assertThat(underTest.pending).containsExactly(Key2, Key1).inOrder()
    }
}
