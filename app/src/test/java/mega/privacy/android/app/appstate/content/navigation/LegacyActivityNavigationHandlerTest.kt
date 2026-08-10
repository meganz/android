package mega.privacy.android.app.appstate.content.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.navOptions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class LegacyActivityNavigationHandlerTest {

    private lateinit var underTest: LegacyActivityNavigationHandler

    private val navigationResultManager = NavigationResultManager()
    private val backStack = PendingBackStack(NavBackStack<NavKey>())
    private var onEmptyBackStackCalls = 0

    @Serializable
    private data object Root : NavKey

    @Serializable
    private data object Destination1 : NavKey

    @Serializable
    private data object Destination2 : NavKey

    @Serializable
    private data object Destination3 : NavKey

    @Serializable
    private data class ParameterizedDestination(val value: String) : NavKey

    @BeforeEach
    fun setUp() {
        backStack.clear()
        backStack.add(Root)
        onEmptyBackStackCalls = 0
        underTest = LegacyActivityNavigationHandler(
            backStack = backStack,
            navigationResultManager = navigationResultManager,
            onEmptyBackStack = { onEmptyBackStackCalls++ },
        )
    }

    @AfterEach
    fun tearDown() {
        backStack.clear()
        navigationResultManager.clearAllResults()
    }

    @Test
    fun `test that back removes last destination when stack has more than one entry`() {
        backStack.add(Destination1)

        underTest.back()

        assertThat(backStack.toList()).containsExactly(Root)
        assertThat(onEmptyBackStackCalls).isEqualTo(0)
    }

    @Test
    fun `test that back invokes onEmptyBackStack when only the root entry remains`() {
        underTest.back()

        assertThat(backStack.toList()).containsExactly(Root)
        assertThat(onEmptyBackStackCalls).isEqualTo(1)
    }

    @Test
    fun `test that remove removes the given navKey when stack has more than one entry`() {
        backStack.add(Destination1)
        backStack.add(Destination2)

        underTest.remove(Destination1)

        assertThat(backStack.toList()).containsExactly(Root, Destination2).inOrder()
    }

    @Test
    fun `test that remove invokes onEmptyBackStack when the navKey is the only entry`() {
        underTest.remove(Root)

        assertThat(onEmptyBackStackCalls).isEqualTo(1)
        assertThat(backStack.toList()).containsExactly(Root)
    }

    @Test
    fun `test that navigate adds the destination to the stack`() {
        underTest.navigate(Destination1)

        assertThat(backStack.toList()).containsExactly(Root, Destination1).inOrder()
    }

    @Test
    fun `test that navigate with a list of destinations adds them all`() {
        underTest.navigate(listOf(Destination1, Destination2))

        assertThat(backStack.toList())
            .containsExactly(Root, Destination1, Destination2)
            .inOrder()
    }

    @Test
    fun `test that backTo with inclusive false pops above the destination`() {
        backStack.add(Destination1)
        backStack.add(Destination2)

        underTest.backTo(Destination1, inclusive = false)

        assertThat(backStack.toList()).containsExactly(Root, Destination1).inOrder()
    }

    @Test
    fun `test that backTo with inclusive true pops up to and including the destination`() {
        backStack.add(Destination1)
        backStack.add(Destination2)

        underTest.backTo(Destination1, inclusive = true)

        assertThat(backStack.toList()).containsExactly(Root)
        assertThat(onEmptyBackStackCalls).isEqualTo(0)
    }

    @Test
    fun `test that backTo invokes onEmptyBackStack when it would empty the stack`() {
        backStack.add(Destination1)

        underTest.backTo(Root, inclusive = true)

        assertThat(onEmptyBackStackCalls).isEqualTo(1)
        // Stack remains untouched so NavDisplay does not crash mid-composition.
        assertThat(backStack.toList()).containsExactly(Root, Destination1).inOrder()
    }

    @Test
    fun `test that navigateAndClearBackStack replaces the stack with the destination`() {
        backStack.add(Destination1)

        underTest.navigateAndClearBackStack(Destination2)

        assertThat(backStack.toList()).containsExactly(Destination2)
    }

    @Test
    fun `test that navigateAndClearBackStack with navOptions replaces the stack with the destination`() {
        backStack.add(Destination1)

        val options = navOptions {
            launchSingleTop = true
        }
        underTest.navigateAndClearBackStack(Destination2, options)

        assertThat(backStack.toList()).containsExactly(Destination2)
    }

    @Test
    fun `test that navigateAndClearTo clears back to the new parent and adds the destination`() {
        backStack.add(Destination1)
        backStack.add(Destination2)

        underTest.navigateAndClearTo(Destination3, newParent = Destination1, inclusive = false)

        assertThat(backStack.toList()).containsExactly(Root, Destination1, Destination3).inOrder()
    }

    @Test
    fun `test that navigateAndClearTo with launchSingleTop replaces remaining top destination of same type`() {
        backStack.add(ParameterizedDestination("A"))
        backStack.add(Destination1)

        val options = navOptions {
            launchSingleTop = true
        }
        underTest.navigateAndClearTo(
            ParameterizedDestination("B"),
            newParent = ParameterizedDestination("A"),
            inclusive = false,
            navOptions = options,
        )

        assertThat(backStack.toList()).containsExactly(
            Root,
            ParameterizedDestination("B"),
        ).inOrder()
    }

    @Test
    fun `test that navigateAndClearTo with popUpTo pops back stack beyond the new parent`() {
        backStack.add(Destination1)
        backStack.add(Destination2)

        val options = navOptions {
            popUpTo<Destination1> {
                inclusive = true
            }
        }
        underTest.navigateAndClearTo(
            Destination3,
            newParent = Destination2,
            inclusive = true,
            navOptions = options,
        )

        assertThat(backStack.toList()).containsExactly(Root, Destination3).inOrder()
    }

    @Test
    fun `test that returnResult stores the result and pops the top of the stack`() = runTest {
        backStack.add(Destination1)

        underTest.returnResult("key", "value")

        navigationResultManager.monitorResult<String>("key").test {
            assertThat(awaitItem()).isEqualTo("value")
        }
        assertThat(backStack.toList()).containsExactly(Root)
    }

    @Test
    fun `test that returnResult does not pop when only the root entry remains`() = runTest {
        underTest.returnResult("key", "value")

        navigationResultManager.monitorResult<String>("key").test {
            assertThat(awaitItem()).isEqualTo("value")
        }
        assertThat(backStack.toList()).containsExactly(Root)
    }

    @Test
    fun `test that clearResult removes the stored result`() = runTest {
        navigationResultManager.returnResult("key", "value")

        underTest.clearResult("key")

        navigationResultManager.monitorResult<String>("key").test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `test that default onEmptyBackStack keeps the bottom entry pinned`() {
        underTest = LegacyActivityNavigationHandler(
            backStack = backStack,
            navigationResultManager = navigationResultManager,
        )

        underTest.back()
        underTest.backTo(Root, inclusive = true)

        assertThat(backStack.toList()).containsExactly(Root)
    }
}
