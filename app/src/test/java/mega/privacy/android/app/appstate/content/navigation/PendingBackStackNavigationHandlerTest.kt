package mega.privacy.android.app.appstate.content.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.appstate.content.destinations.FetchingContentNavKey
import mega.privacy.android.navigation.contract.navkey.NoNodeNavKey
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PendingBackStackNavigationHandlerTest {
    private lateinit var underTest: PendingBackStackNavigationHandler
    private val navigationResultManager = NavigationResultManager()

    private val backStack = PendingBackStack(NavBackStack())

    private data object DefaultLoginDestination : NoSessionNavKey.Mandatory
    private data object InitialLoginDestination : NoSessionNavKey.Mandatory

    private data object NoSessionDestination1 : NoSessionNavKey.Mandatory
    private data object NoSessionDestination2 : NoSessionNavKey.Mandatory

    private data object OptionalNoSessionNavKey1 : NoSessionNavKey.Optional
    private data object OptionalNoSessionNavKey2 : NoSessionNavKey.Optional


    private data object NoNodeDestination1 : NoNodeNavKey
    private data object NoNodeDestination2 : NoNodeNavKey

    private data object Destination1 : NavKey
    private data object Destination2 : NavKey
    private data object Destination3 : NavKey

    private data object PasscodeDestination : NavKey

    private data object DefaultLandingScreen : NavKey

    private val initialSession = "initial"

    private val getFetchNodeDestinationFunction: (String, Boolean) -> NavKey =
        { sessionValue, fromLogin ->
            FetchingContentNavKey(sessionValue, fromLogin)
        }

    @BeforeEach
    fun setUp() {
        underTest = PendingBackStackNavigationHandler(
            backstack = backStack,
            currentAuthStatus = PendingBackStackNavigationHandler.AuthStatus.LoggedIn(initialSession),
            hasRootNode = true,
            defaultLandingScreen = DefaultLandingScreen,
            defaultLoginDestination = DefaultLoginDestination,
            initialLoginDestination = InitialLoginDestination,
            fetchRootNodeDestination = getFetchNodeDestinationFunction,
            isPasscodeLocked = false,
            passcodeDestination = PasscodeDestination,
            navigationResultManager = navigationResultManager,
        )
    }

    @AfterEach
    fun tearDown() {
        backStack.clear()
        backStack.pending = emptyList()
    }

    @Test
    fun `test that back removes last element from backStack`() {
        backStack.add(Destination1)
        underTest.back()
        assertThat(backStack).containsExactly(DefaultLandingScreen)
    }

    @Test
    fun `test that navigate adds multiple destinations to backStack`() {
        val list = listOf(Destination1, Destination2)
        underTest.navigate(list)
        assertThat(backStack).containsExactly(DefaultLandingScreen, Destination1, Destination2)
    }

    @Test
    fun `test that navigateAndClearBackStack clears backStack and adds destination`() {
        backStack.add(Destination1)
        underTest.navigateAndClearBackStack(Destination2)
        assertThat(backStack).containsExactly(Destination2)
    }

    @Test
    fun `test that backTo removes items until target destination`() {
        backStack.addAll(listOf(Destination1, Destination2, Destination3))
        underTest.backTo(Destination1, inclusive = false)
        assertThat(backStack).containsExactly(DefaultLandingScreen, Destination1)
    }

    @Test
    fun `test that navigateAndClearTo clears back to parent and adds new destination`() {
        backStack.addAll(listOf(Destination1, Destination2, Destination3))

        underTest.navigateAndClearTo(Destination3, newParent = Destination2, inclusive = true)
        assertThat(backStack).containsExactly(DefaultLandingScreen, Destination1, Destination3)
    }

    @Test
    fun `test that returnResult sets Value and pops BackStack`() = runTest {
        backStack.add(Destination1)
        val key = "testKey"
        underTest.returnResult(key, "resultValue")

        val result = underTest.monitorResult<String>(key)
        result.test {
            assert(awaitItem() == "resultValue")
        }

        assertThat(backStack).containsExactly(DefaultLandingScreen)
    }

    @Test
    fun `test that monitorResult returns flow that updates on returnResult`() = runTest {
        backStack.add(Destination1)
        val key = "resultKey"
        val testFlow = underTest.monitorResult<String>(key)
        testFlow.test {
            assert(awaitItem() == null) // initial
            underTest.returnResult(key, "Hello")
            assert(awaitItem() == "Hello")
        }
    }

    @Test
    fun `test that clearResult sets value to null`() = runTest {
        backStack.add(Destination1)
        val key = "key"
        underTest.returnResult(key, "value")
        underTest.clearResult(key)

        val result = underTest.monitorResult<String>(key)
        result.test {
            assert(awaitItem() == null)
        }
    }

    @Test
    fun `test that clearAllResults clears all flows`() = runTest {
        backStack.add(Destination1)
        underTest.returnResult("a", 1)
        underTest.returnResult("b", 2)

        underTest.clearAllResults()

        val aFlow = underTest.monitorResult<Int>("a")
        val bFlow = underTest.monitorResult<Int>("b")

        aFlow.test { assert(awaitItem() == null) }
        bFlow.test { assert(awaitItem() == null) }
    }

    @Test
    fun `test that optional no session destination is added even if not logged in`() = runTest {
        underTest.onLoginChange(PendingBackStackNavigationHandler.AuthStatus.NotLoggedIn)
        underTest.navigate(OptionalNoSessionNavKey1)
        assertThat(backStack.last()).isEqualTo(OptionalNoSessionNavKey1)
    }

    @Test
    fun `test that optional no session destinations are added even if not logged in`() = runTest {
        underTest.onLoginChange(PendingBackStackNavigationHandler.AuthStatus.NotLoggedIn)
        underTest.navigate(listOf(OptionalNoSessionNavKey1, OptionalNoSessionNavKey2))
        assertThat(backStack.takeLast(2)).containsExactly(
            OptionalNoSessionNavKey1, OptionalNoSessionNavKey2
        )
    }

    @Test
    fun `test that default landing screen is added instead if not logged in and mandatory no session destination is added`() =
        runTest {
            underTest.navigate(NoSessionDestination1)
            assertThat(backStack.last()).isEqualTo(DefaultLandingScreen)
        }

    @Test
    fun `test that default landing screen is added instead if not logged in and mandatory no session destinations are added`() =
        runTest {
            underTest.navigate(listOf(NoSessionDestination1, NoSessionDestination2))
            assertThat(backStack.last()).isEqualTo(DefaultLandingScreen)
        }

    @Test
    fun `test that no navigation happens if logged in and mandatory no session destination is added`() =
        runTest {
            underTest.navigate(Destination1)
            underTest.navigate(NoSessionDestination1)
            assertThat(backStack.last()).isEqualTo(Destination1)
        }

    @Test
    fun `test that default login destination is added instead if not logged in and destination requires it`() =
        runTest {
            underTest.onLoginChange(PendingBackStackNavigationHandler.AuthStatus.NotLoggedIn)
            underTest.navigate(Destination1)
            assertThat(backStack).containsExactly(DefaultLoginDestination)
        }

    @Test
    fun `test that default login destination is added instead if not logged in and last destination requires it`() =
        runTest {
            underTest.onLoginChange(PendingBackStackNavigationHandler.AuthStatus.NotLoggedIn)
            underTest.navigate(listOf(NoSessionDestination1, Destination2))
            assertThat(backStack).containsExactly(DefaultLoginDestination)
        }

    @Test
    fun `test that non NoSessionNavKey destinations are removed if initialised without a session`() =
        runTest {
            val tempBackStack = PendingBackStack(
                NavBackStack(
                    NoSessionDestination1, NoSessionDestination2, Destination1, Destination2
                )
            )
            PendingBackStackNavigationHandler(
                backstack = tempBackStack,
                currentAuthStatus = PendingBackStackNavigationHandler.AuthStatus.NotLoggedIn,
                hasRootNode = true,
                defaultLandingScreen = DefaultLandingScreen,
                defaultLoginDestination = DefaultLoginDestination,
                initialLoginDestination = InitialLoginDestination,
                fetchRootNodeDestination = getFetchNodeDestinationFunction,
                isPasscodeLocked = false,
                passcodeDestination = PasscodeDestination,
                navigationResultManager = navigationResultManager,
            )

            assertThat(tempBackStack).containsExactly(NoSessionDestination1, NoSessionDestination2)
            assertThat(tempBackStack.pending).containsExactly(Destination1, Destination2)
        }

    @Test
    fun `test that existing destination is replaced if initialised without a session`() = runTest {
        val tempBackStack = PendingBackStack<NavKey>(NavBackStack(Destination1))
        PendingBackStackNavigationHandler(
            backstack = tempBackStack,
            currentAuthStatus = PendingBackStackNavigationHandler.AuthStatus.NotLoggedIn,
            hasRootNode = true,
            defaultLandingScreen = DefaultLandingScreen,
            defaultLoginDestination = DefaultLoginDestination,
            initialLoginDestination = InitialLoginDestination,
            fetchRootNodeDestination = getFetchNodeDestinationFunction,
            isPasscodeLocked = false,
            passcodeDestination = PasscodeDestination,
            navigationResultManager = navigationResultManager,
        )

        assertThat(tempBackStack).containsExactly(InitialLoginDestination)
        assertThat(tempBackStack.pending).containsExactly(Destination1)
    }

    @Test
    fun `test that multiple existing destinations are replaced if initialised without a session`() =
        runTest {
            val tempBackStack =
                PendingBackStack(NavBackStack(Destination1, Destination3, Destination2))
            PendingBackStackNavigationHandler(
                backstack = tempBackStack,
                currentAuthStatus = PendingBackStackNavigationHandler.AuthStatus.NotLoggedIn,
                hasRootNode = true,
                defaultLandingScreen = DefaultLandingScreen,
                defaultLoginDestination = DefaultLoginDestination,
                initialLoginDestination = InitialLoginDestination,
                fetchRootNodeDestination = getFetchNodeDestinationFunction,
                isPasscodeLocked = false,
                passcodeDestination = PasscodeDestination,
                navigationResultManager = navigationResultManager,
            )

            assertThat(tempBackStack).containsExactly(InitialLoginDestination)
            assertThat(tempBackStack.pending).containsExactly(
                Destination1, Destination3, Destination2
            )
        }

    @Test
    fun `test that backstack is replaced by login destination if logged out`() = runTest {
        backStack.addAll(listOf(Destination1, Destination3))

        underTest.onLoginChange(PendingBackStackNavigationHandler.AuthStatus.NotLoggedIn)

        assertThat(backStack).containsExactly(DefaultLoginDestination)
    }

    @Test
    fun `test that default landing screen is added if navigating to a login screen while logged in`() =
        runTest {
            underTest.navigate(DefaultLoginDestination)

            assertThat(backStack.lastOrNull()).isEqualTo(DefaultLandingScreen)
        }

    @Test
    fun `test that no-node destination is added even if not logged in`() = runTest {
        underTest.onRootNodeChange(false)
        underTest.navigate(NoNodeDestination1)
        assertThat(backStack.last()).isEqualTo(NoNodeDestination1)
    }

    @Test
    fun `test that no-node destinations are added even if not logged in`() = runTest {
        underTest.onRootNodeChange(false)
        underTest.navigate(listOf(NoNodeDestination1, NoNodeDestination2))
        assertThat(backStack.takeLast(2)).containsExactly(
            NoNodeDestination1, NoNodeDestination2
        )
    }

    @Test
    fun `test that fetch node destination is added instead if not fetched in and destination requires it`() =
        runTest {
            underTest.onRootNodeChange(false)
            underTest.navigate(Destination1)
            assertThat(backStack).containsExactly(FetchingContentNavKey(initialSession, false))
        }

    @Test
    fun `test that fetch node destination is added instead if not fetched and last destination requires it`() =
        runTest {
            underTest.onRootNodeChange(false)
            underTest.navigate(listOf(NoNodeDestination1, Destination2))
            assertThat(backStack).containsExactly(FetchingContentNavKey(initialSession, false))
        }

    @Test
    fun `test that non NoNodeNavKey destinations are removed if initialised without a root node`() =
        runTest {
            val tempBackStack = PendingBackStack(
                NavBackStack(
                    NoNodeDestination1, NoNodeDestination2, Destination1, Destination2
                )
            )
            PendingBackStackNavigationHandler(
                backstack = tempBackStack,
                currentAuthStatus = PendingBackStackNavigationHandler.AuthStatus.LoggedIn(
                    initialSession
                ),
                hasRootNode = false,
                defaultLandingScreen = DefaultLandingScreen,
                defaultLoginDestination = DefaultLoginDestination,
                initialLoginDestination = InitialLoginDestination,
                fetchRootNodeDestination = getFetchNodeDestinationFunction,
                isPasscodeLocked = false,
                passcodeDestination = PasscodeDestination,
                navigationResultManager = navigationResultManager,
            )

            assertThat(tempBackStack).containsExactly(
                NoNodeDestination1, NoNodeDestination2, FetchingContentNavKey(initialSession, false)
            )
            assertThat(tempBackStack.pending).containsExactly(Destination1, Destination2)
        }

    @Test
    fun `test that existing destination is replaced if initialised without a root node`() =
        runTest {
            val tempBackStack = PendingBackStack<NavKey>(NavBackStack(Destination1))
            PendingBackStackNavigationHandler(
                backstack = tempBackStack,
                currentAuthStatus = PendingBackStackNavigationHandler.AuthStatus.LoggedIn(
                    initialSession
                ),
                hasRootNode = false,
                defaultLandingScreen = DefaultLandingScreen,
                defaultLoginDestination = DefaultLoginDestination,
                initialLoginDestination = InitialLoginDestination,
                fetchRootNodeDestination = getFetchNodeDestinationFunction,
                isPasscodeLocked = false,
                passcodeDestination = PasscodeDestination,
                navigationResultManager = navigationResultManager,
            )

            assertThat(tempBackStack).containsExactly(FetchingContentNavKey(initialSession, false))
            assertThat(tempBackStack.pending).containsExactly(Destination1)
        }

    @Test
    fun `test that multiple existing destinations are replaced if initialised without a root node`() =
        runTest {
            val tempBackStack =
                PendingBackStack(NavBackStack(Destination1, Destination3, Destination2))
            PendingBackStackNavigationHandler(
                backstack = tempBackStack,
                currentAuthStatus = PendingBackStackNavigationHandler.AuthStatus.LoggedIn(
                    initialSession
                ),
                hasRootNode = false,
                defaultLandingScreen = DefaultLandingScreen,
                defaultLoginDestination = DefaultLoginDestination,
                initialLoginDestination = InitialLoginDestination,
                fetchRootNodeDestination = getFetchNodeDestinationFunction,
                isPasscodeLocked = false,
                passcodeDestination = PasscodeDestination,
                navigationResultManager = navigationResultManager,
            )

            assertThat(tempBackStack).containsExactly(FetchingContentNavKey(initialSession, false))
            assertThat(tempBackStack.pending).containsExactly(
                Destination1, Destination3, Destination2
            )
        }

    @Test
    fun `test that backstack is replaced by fetch nodes destination if has root node is set to false`() =
        runTest {
            backStack.addAll(listOf(Destination1, Destination3))

            underTest.onRootNodeChange(false)

            assertThat(backStack).containsExactly(FetchingContentNavKey(initialSession, false))
        }

    @Test
    fun `test that default landing screen is added if navigating to the fetch node destination while node is present`() =
        runTest {
            underTest.navigate(FetchingContentNavKey(initialSession, false))

            assertThat(backStack.lastOrNull()).isEqualTo(DefaultLandingScreen)
        }

    @Test
    fun `test that if session is added, but there is no pending destinations, the default landing screen is added`() =
        runTest {
            underTest.onRootNodeChange(false)
            assertThat(backStack.pending).isEmpty()
            underTest.onRootNodeChange(true)

            assertThat(backStack.lastOrNull()).isEqualTo(DefaultLandingScreen)
        }

    @Test
    fun `test that pending destinations are added to the backstack when root node is fetched`() =
        runTest {
            underTest.onRootNodeChange(false)

            backStack.pending = listOf(Destination2, Destination3)

            underTest.onRootNodeChange(true)

            assertThat(backStack).containsExactly(Destination2, Destination3)
        }

    @Test
    fun `test that if logged in but there are no pending destinations, fetch nodes destination is added to backStack`() =
        runTest {
            underTest.onLoginChange(PendingBackStackNavigationHandler.AuthStatus.NotLoggedIn)
            val newSession = "ANewSession"
            underTest.onLoginChange(PendingBackStackNavigationHandler.AuthStatus.LoggedIn(newSession))
            assertThat(backStack).containsExactly(FetchingContentNavKey(newSession, true))
        }

    @Test
    fun `test that if passcode lock is enabled passcode is the only destination on the stack`() =
        runTest {
            val navTree = listOf(Destination1, Destination2)
            backStack.addAll(navTree)
            underTest.onPasscodeStateChanged(true)
            assertThat(backStack).containsExactly(PasscodeDestination)
            assertThat(backStack.pending).containsExactlyElementsIn(listOf(DefaultLandingScreen) + navTree)
        }

    @Test
    fun `test that when passcode unlocks pending destinations are added and passcode is removed`() =
        runTest {
            val navTree = listOf(Destination1, Destination2)
            backStack.pending = navTree
            underTest.onPasscodeStateChanged(true)

            assertThat(backStack).containsExactly(PasscodeDestination)
            assertThat(backStack.pending).containsExactlyElementsIn(listOf(DefaultLandingScreen) + navTree)

            underTest.onPasscodeStateChanged(false)
            assertThat(backStack).containsExactly(DefaultLandingScreen, Destination1, Destination2)
            assertThat(backStack.pending).isEmpty()
        }

    @Test
    fun `test that passcode is only added after login and fetch nodes is completed`() = runTest {
        val tempBackStack = PendingBackStack<NavKey>(NavBackStack(Destination1))
        val tempHandler = PendingBackStackNavigationHandler(
            backstack = tempBackStack,
            currentAuthStatus = PendingBackStackNavigationHandler.AuthStatus.NotLoggedIn,
            hasRootNode = false,
            defaultLandingScreen = DefaultLandingScreen,
            defaultLoginDestination = DefaultLoginDestination,
            initialLoginDestination = InitialLoginDestination,
            fetchRootNodeDestination = getFetchNodeDestinationFunction,
            isPasscodeLocked = false,
            passcodeDestination = PasscodeDestination,
            navigationResultManager = navigationResultManager,
        )
        val session = "newSession"

        tempHandler.onPasscodeStateChanged(true)
        assertThat(tempBackStack).containsExactly(DefaultLoginDestination)
        tempHandler.onLoginChange(PendingBackStackNavigationHandler.AuthStatus.LoggedIn(session))
        assertThat(tempBackStack).containsExactly(FetchingContentNavKey(session, true))
        tempHandler.onRootNodeChange(true)
        assertThat(tempBackStack).containsExactly(PasscodeDestination)
    }


    @Test
    fun `test that if initial passcode lock is enabled passcode is the only destination on the stack`() =
        runTest {
            val navTree = listOf(Destination1, Destination2)
            val tempBackStack = PendingBackStack<NavKey>(NavBackStack(*navTree.toTypedArray()))
            PendingBackStackNavigationHandler(
                backstack = tempBackStack,
                currentAuthStatus = PendingBackStackNavigationHandler.AuthStatus.LoggedIn(
                    initialSession
                ),
                hasRootNode = true,
                defaultLandingScreen = DefaultLandingScreen,
                defaultLoginDestination = DefaultLoginDestination,
                initialLoginDestination = InitialLoginDestination,
                fetchRootNodeDestination = getFetchNodeDestinationFunction,
                isPasscodeLocked = true,
                passcodeDestination = PasscodeDestination,
                navigationResultManager = navigationResultManager,
            )
            assertThat(tempBackStack).containsExactly(PasscodeDestination)
            assertThat(tempBackStack.pending).containsExactlyElementsIn(navTree)
        }

    @Test
    fun `test that backstack is not empty after initialising handler`() = runTest {
        assertThat(backStack).containsExactly(DefaultLandingScreen)
    }

    @Test
    fun `test that logging out removes fetch nodes destination`() = runTest {
        val tempBackStack = PendingBackStack<NavKey>(NavBackStack(DefaultLandingScreen))
        val tempHandler = PendingBackStackNavigationHandler(
            backstack = tempBackStack,
            currentAuthStatus = PendingBackStackNavigationHandler.AuthStatus.LoggedIn(
                initialSession
            ),
            hasRootNode = false,
            defaultLandingScreen = DefaultLandingScreen,
            defaultLoginDestination = DefaultLoginDestination,
            initialLoginDestination = InitialLoginDestination,
            fetchRootNodeDestination = getFetchNodeDestinationFunction,
            isPasscodeLocked = true,
            passcodeDestination = PasscodeDestination,
            navigationResultManager = navigationResultManager,
        )
        tempHandler.onLoginChange(PendingBackStackNavigationHandler.AuthStatus.NotLoggedIn)

        assertThat(tempBackStack).containsExactly(DefaultLoginDestination)
    }

}