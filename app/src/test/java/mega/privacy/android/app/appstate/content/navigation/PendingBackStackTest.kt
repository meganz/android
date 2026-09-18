package mega.privacy.android.app.appstate.content.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mega.privacy.android.navigation.contract.navOptions
import org.junit.jupiter.api.Test

class PendingBackStackTest {

    @Serializable
    private data object Key1 : NavKey

    @Serializable
    private data object Key2 : NavKey

    @Serializable
    private sealed interface SavedKey : NavKey {

        @Serializable
        @SerialName("first")
        data object First : SavedKey

        @Serializable
        @SerialName("second")
        data object Second : SavedKey

        @Serializable
        @SerialName("third")
        data object Third : SavedKey
    }

    private val savedKeySerializer = PendingBackStackSerializer(SavedKey.serializer())

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

    @Test
    fun `test that pendingNavigations setter drops destinations queued by an earlier navigation`() {
        val underTest = PendingBackStack<NavKey>(NavBackStack())
        val clearBackStack = PendingNavigation.ClearBackStack<NavKey>(listOf(Key1), null)

        underTest.pendingNavigations = listOf(
            clearBackStack,
            PendingNavigation.Navigate(listOf(Key1, Key2), null),
            PendingNavigation.Navigate(listOf(Key1), null),
        )

        assertThat(underTest.pendingNavigations).containsExactly(
            clearBackStack,
            PendingNavigation.Navigate(listOf(Key2), null),
        ).inOrder()
    }

    @Test
    fun `test that pendingNavigations setter keeps a navigation whose destinations are all already queued`() {
        val underTest = PendingBackStack<NavKey>(NavBackStack())
        val navigate = PendingNavigation.Navigate<NavKey>(listOf(Key1), null)
        val clearBackStack = PendingNavigation.ClearBackStack<NavKey>(listOf(Key1), null)

        underTest.pendingNavigations = listOf(navigate, clearBackStack)

        assertThat(underTest.pendingNavigations).containsExactly(navigate, clearBackStack).inOrder()
        assertThat(underTest.pending).containsExactly(Key1)
    }

    @Test
    fun `test that serialization preserves the operation and options of queued navigations`() {
        val underTest = PendingBackStack(NavBackStack<SavedKey>(SavedKey.First))
        underTest.pendingNavigations = listOf(
            PendingNavigation.ClearBackStack(listOf(SavedKey.Second), navOptions = null),
            PendingNavigation.ClearTo(
                destinations = listOf(SavedKey.Third),
                newParent = SavedKey.First,
                inclusive = true,
                navOptions = navOptions {
                    launchSingleTop = true
                    popUpTo(SavedKey.Second::class) {
                        inclusive = true
                        firstOccurrence = true
                    }
                }.toPendingNavOptions(),
            ),
        )

        val restored = Json.decodeFromString(
            savedKeySerializer,
            Json.encodeToString(savedKeySerializer, underTest)
        )

        assertThat(restored).containsExactly(SavedKey.First)
        assertThat(restored.pendingNavigations).isEqualTo(underTest.pendingNavigations)
    }

    @Test
    fun `test that deserialization restores queued destinations when the saved data has no navigations`() {
        val saved = """{"base":[{"type":"first"}],"pending":[{"type":"second"},{"type":"third"}]}"""

        val restored = Json.decodeFromString(savedKeySerializer, saved)

        assertThat(restored).containsExactly(SavedKey.First)
        assertThat(restored.pending).containsExactly(SavedKey.Second, SavedKey.Third).inOrder()
    }
}
