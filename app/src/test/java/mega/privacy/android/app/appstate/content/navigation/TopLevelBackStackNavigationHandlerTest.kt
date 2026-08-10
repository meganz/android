package mega.privacy.android.app.appstate.content.navigation

import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.contract.navOptions
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TopLevelBackStackNavigationHandlerTest {

    private lateinit var backStack: TopLevelBackStack<NavKey, MainNavItemNavKey>
    private lateinit var underTest: TopLevelBackStackNavigationHandler

    private data object StartKey : MainNavItemNavKey
    private data object TopLevelKey1 : MainNavItemNavKey

    @Serializable
    private data object Destination1 : NavKey

    @Serializable
    private data object Destination2 : NavKey

    @Serializable
    private data object Destination3 : NavKey

    private data object DialogDestination1 : DialogNavKey

    private data object DialogDestination2 : DialogNavKey

    @Serializable
    private data class ParameterizedDialogDestination(val value: String) : DialogNavKey

    @Serializable
    private data class ParameterizedDestination(val value: String) : NavKey

    @BeforeEach
    fun setUp() {
        backStack = TopLevelBackStack(StartKey)
        underTest = TopLevelBackStackNavigationHandler(
            backStack = backStack,
            navigationResultManager = NavigationResultManager(),
        )
    }

    @Test
    fun `test that navigate skips when destinations are already at the back stack tail`() {
        underTest.navigate(Destination1)
        underTest.navigate(Destination2)

        underTest.navigate(listOf(Destination1, Destination2))

        assertThat(backStack.backStack).containsExactly(
            StartKey,
            Destination1,
            Destination2,
        ).inOrder()
    }

    @Test
    fun `test that navigate removes duplicate dialog keys before re-adding`() {
        underTest.navigate(Destination1)
        underTest.navigate(DialogDestination1)
        underTest.navigate(Destination2)
        underTest.navigate(DialogDestination1)

        assertThat(backStack.backStack.count { it == DialogDestination1 }).isEqualTo(1)
        assertThat(backStack.backStack).containsExactly(
            StartKey,
            Destination1,
            Destination2,
            DialogDestination1,
        ).inOrder()
    }

    @Test
    fun `test that navigate removes equal parameterized dialog keys before re-adding`() {
        underTest.navigate(ParameterizedDialogDestination("A"))
        underTest.navigate(Destination1)
        underTest.navigate(ParameterizedDialogDestination("A"))

        assertThat(backStack.backStack.count { it == ParameterizedDialogDestination("A") }).isEqualTo(1)
        assertThat(backStack.backStack).containsExactly(
            StartKey,
            Destination1,
            ParameterizedDialogDestination("A"),
        ).inOrder()
    }

    @Test
    fun `test that navigate with launchSingleTop replaces top destination of same type`() {
        underTest.navigate(Destination1)

        val options = navOptions {
            launchSingleTop = true
        }
        underTest.navigate(Destination1, options)

        assertThat(backStack.backStack).containsExactly(StartKey, Destination1)
        assertThat(backStack.backStack.count { it == Destination1 }).isEqualTo(1)
    }

    @Test
    fun `test that navigate list with launchSingleTop replaces matching top destinations`() {
        underTest.navigate(listOf(Destination1, Destination2))

        val options = navOptions {
            launchSingleTop = true
        }
        underTest.navigate(listOf(Destination1, Destination2), options)

        assertThat(backStack.backStack).containsExactly(
            StartKey,
            Destination1,
            Destination2,
        ).inOrder()
        assertThat(backStack.backStack.count { it == Destination1 }).isEqualTo(1)
        assertThat(backStack.backStack.count { it == Destination2 }).isEqualTo(1)
    }

    @Test
    fun `test that navigate deduplicates dialog keys across tab stacks`() {
        underTest.navigate(Destination1)
        underTest.navigate(DialogDestination1)
        backStack.switchTopLevel(TopLevelKey1)
        underTest.navigate(Destination2)
        underTest.navigate(DialogDestination1)

        assertThat(backStack.backStack.count { it == DialogDestination1 }).isEqualTo(1)
        assertThat(backStack.topLevelBackStacks[StartKey]).doesNotContain(DialogDestination1)
    }

    @Test
    fun `test that navigate does not remove different dialog keys when re-adding one dialog`() {
        underTest.navigate(DialogDestination1)
        underTest.navigate(DialogDestination2)
        underTest.navigate(DialogDestination1)

        assertThat(backStack.backStack.count { it == DialogDestination1 }).isEqualTo(1)
        assertThat(backStack.backStack.count { it == DialogDestination2 }).isEqualTo(1)
    }

    @Test
    fun `test that navigate after remove is not skipped by stale tail`() {
        underTest.navigate(Destination1)
        underTest.remove(Destination1)

        underTest.navigate(Destination1)

        assertThat(backStack.backStack).containsExactly(StartKey, Destination1).inOrder()
    }

    @Test
    fun `test that navigateAndClearBackStack with navOptions replaces the stack with the destination`() {
        underTest.navigate(Destination1)
        underTest.navigate(Destination2)

        val options = navOptions {
            launchSingleTop = true
        }
        underTest.navigateAndClearBackStack(Destination3, options)

        assertThat(backStack.backStack).containsExactly(StartKey, Destination3).inOrder()
    }

    @Test
    fun `test that navigateAndClearTo clears back to the new parent and adds the destination`() {
        underTest.navigate(Destination1)
        underTest.navigate(Destination2)

        underTest.navigateAndClearTo(Destination3, newParent = Destination1, inclusive = false)

        assertThat(backStack.backStack).containsExactly(
            StartKey,
            Destination1,
            Destination3,
        ).inOrder()
    }

    @Test
    fun `test that navigateAndClearTo with launchSingleTop replaces remaining top destination of same type`() {
        underTest.navigate(ParameterizedDestination("A"))
        underTest.navigate(Destination1)

        val options = navOptions {
            launchSingleTop = true
        }
        underTest.navigateAndClearTo(
            ParameterizedDestination("B"),
            newParent = ParameterizedDestination("A"),
            inclusive = false,
            navOptions = options,
        )

        assertThat(backStack.backStack).containsExactly(
            StartKey,
            ParameterizedDestination("B"),
        ).inOrder()
    }

    @Test
    fun `test that navigateAndClearTo with popUpTo pops back stack beyond the new parent`() {
        underTest.navigate(Destination1)
        underTest.navigate(Destination2)
        underTest.navigate(Destination3)

        val options = navOptions {
            popUpTo<Destination1> {
                inclusive = true
            }
        }
        underTest.navigateAndClearTo(
            ParameterizedDestination("new"),
            newParent = Destination3,
            inclusive = true,
            navOptions = options,
        )

        assertThat(backStack.backStack).containsExactly(
            StartKey,
            ParameterizedDestination("new"),
        ).inOrder()
    }
}
