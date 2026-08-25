package mega.privacy.android.navigation.contract.shared

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.metadata.buildMetadata
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the guarantee features rely on when they put flow state in a shared scope: the scope
 * outlives its children and dies with the entry that owns it.
 */
@RunWith(AndroidJUnit4::class)
class SharedViewModelStoreNavEntryDecoratorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val backStack = mutableStateListOf<NavKey>(Owner)
    private val ownerScoped = mutableListOf<ScopedViewModel>()
    private val childScoped = mutableListOf<ScopedViewModel>()
    private val childEntryScoped = mutableListOf<ScopedViewModel>()

    @Test
    fun `test that both entries of a flow are given the same shared ViewModel`() {
        setContent()

        backStack.add(Child)
        composeTestRule.waitForIdle()

        assertThat(childScoped.last()).isSameInstanceAs(ownerScoped.last())
    }

    @Test
    fun `test that the shared ViewModel survives a child entry being popped`() {
        setContent()
        backStack.add(Child)
        composeTestRule.waitForIdle()
        val shared = ownerScoped.last()

        backStack.removeAt(backStack.lastIndex)
        composeTestRule.waitForIdle()

        assertThat(shared.cleared).isFalse()
        assertThat(ownerScoped.last()).isSameInstanceAs(shared)
    }

    @Test
    fun `test that an entry scoped ViewModel is cleared when its own entry is popped`() {
        setContent()
        backStack.add(Child)
        composeTestRule.waitForIdle()
        val entryScoped = childEntryScoped.last()

        backStack.removeAt(backStack.lastIndex)
        composeTestRule.waitForIdle()

        assertThat(entryScoped.cleared).isTrue()
    }

    @Test
    fun `test that the shared ViewModel is cleared when the entry owning the scope is popped`() {
        // What a feature scoping flow state to the nav flow depends on: leaving the flow discards
        // everything the flow put in the shared scope.
        backStack.add(0, Root)
        setContent()
        val shared = ownerScoped.last()

        backStack.remove(Owner)
        composeTestRule.waitForIdle()

        assertThat(shared.cleared).isTrue()
    }

    @Test
    fun `test that re-entering the flow after it was left starts a new shared ViewModel`() {
        backStack.add(0, Root)
        setContent()
        val first = ownerScoped.last()

        backStack.remove(Owner)
        composeTestRule.waitForIdle()
        backStack.add(Owner)
        composeTestRule.waitForIdle()

        assertThat(ownerScoped.last()).isNotSameInstanceAs(first)
    }

    private fun setContent() {
        composeTestRule.setContent {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeAt(backStack.lastIndex) },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberSharedViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<Root> { Text("root") }
                    entry<Owner>(
                        metadata = buildMetadata { provideSharedViewModelScope(SCOPE) },
                    ) {
                        ownerScoped += sharedScopedViewModel()
                        Text("owner")
                    }
                    entry<Child>(
                        metadata = buildMetadata { withSharedViewModelStoreKey(SCOPE) },
                    ) {
                        childScoped += sharedScopedViewModel()
                        childEntryScoped += entryScopedViewModel()
                        Text("child")
                    }
                },
            )
        }
        composeTestRule.waitForIdle()
    }

    @Composable
    private fun sharedScopedViewModel(): ScopedViewModel {
        val owner = checkNotNull(LocalSharedViewModelStoreOwner.current)
        return owner.scopedViewModel()
    }

    @Composable
    private fun entryScopedViewModel(): ScopedViewModel {
        val owner = checkNotNull(LocalViewModelStoreOwner.current)
        return owner.scopedViewModel()
    }

    /**
     * Reads the ViewModel straight out of the store rather than through `sharedViewModel`, which
     * goes via Hilt and would need a Hilt-injected host activity. The store is what is under test.
     */
    private fun androidx.lifecycle.ViewModelStoreOwner.scopedViewModel(): ScopedViewModel =
        ViewModelProvider.create(
            store = viewModelStore,
            factory = viewModelFactory { initializer { ScopedViewModel() } },
        )[ScopedViewModel::class]

    private class ScopedViewModel : ViewModel() {
        var cleared = false
            private set

        override fun onCleared() {
            cleared = true
        }
    }

    @Serializable
    private data object Root : NavKey

    @Serializable
    private data object Owner : NavKey

    @Serializable
    private data object Child : NavKey

    private companion object {
        const val SCOPE = "test_flow"
    }
}
