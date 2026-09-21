package mega.privacy.android.app.presentation.settings.passcode.navigation

import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.toRoute
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetupPasscodeDestinationTest {

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
        navController.graph = navController.createGraph(
            startDestination = PasscodeSettingsDestination,
        ) {
            composable(PasscodeSettingsDestination) {}
            setupPasscode(navController)
        }
    }

    @Test
    fun `test that the setup passcode destination is reachable in edit mode`() {
        navController.navigate(SetupPasscodeDestination(isEdit = true))

        assertThat(currentRoute()).isEqualTo(SetupPasscodeDestination(isEdit = true))
    }

    @Test
    fun `test that the setup passcode destination is reachable in set mode`() {
        navController.navigate(SetupPasscodeDestination(isEdit = false))

        assertThat(currentRoute()).isEqualTo(SetupPasscodeDestination(isEdit = false))
    }

    @Test
    fun `test that popping the setup passcode destination returns to the passcode settings`() {
        navController.navigate(SetupPasscodeDestination(isEdit = false))

        navController.popBackStack()

        assertThat(navController.currentDestination?.route).isEqualTo(PasscodeSettingsDestination)
    }

    private fun currentRoute() =
        navController.currentBackStackEntry?.toRoute<SetupPasscodeDestination>()
}
