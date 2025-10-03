package mega.privacy.android.app.presentation.testpassword.navigation

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import mega.privacy.android.app.presentation.testpassword.TestPasswordActivity
import mega.privacy.android.navigation.destination.TestPasswordNavKey

/**
 * Navigation destination for TestPasswordActivity that handles password testing and recovery key management.
 * 
 * Usage examples:
 * - Navigate to test password: navController.navigate(TestPasswordNavKey())
 * - Navigate with specific counter: navController.navigate(TestPasswordNavKey(wrongPasswordCounter = 3))
 * - Navigate in test mode: navController.navigate(TestPasswordNavKey(isTestPasswordMode = true))
 * - Navigate in logout mode: navController.navigate(TestPasswordNavKey(isLogoutMode = true))
 */
fun NavGraphBuilder.testPasswordLegacyDestination(removeDestination: () -> Unit) {
    composable<TestPasswordNavKey> {
        val context = LocalContext.current
        val testPasswordNavKey = it.toRoute<TestPasswordNavKey>()

        LaunchedEffect(testPasswordNavKey) {
            val intent = Intent(context, TestPasswordActivity::class.java).apply {
                putExtra(TestPasswordActivity.WRONG_PASSWORD_COUNTER, testPasswordNavKey.wrongPasswordCounter)
                putExtra(TestPasswordActivity.KEY_TEST_PASSWORD_MODE, testPasswordNavKey.isTestPasswordMode)
                putExtra(TestPasswordActivity.KEY_IS_LOGOUT, testPasswordNavKey.isLogoutMode)
            }
            context.startActivity(intent)
            removeDestination()
        }
    }
}
