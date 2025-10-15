package mega.privacy.android.app.presentation.testpassword.navigation

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.testpassword.TestPasswordActivity
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
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
fun EntryProviderBuilder<NavKey>.testPasswordLegacyDestination(removeDestination: () -> Unit) {
    entry<TestPasswordNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current

        LaunchedEffect(key) {
            val intent = Intent(context, TestPasswordActivity::class.java).apply {
                putExtra(TestPasswordActivity.WRONG_PASSWORD_COUNTER, key.wrongPasswordCounter)
                putExtra(TestPasswordActivity.KEY_TEST_PASSWORD_MODE, key.isTestPasswordMode)
                putExtra(TestPasswordActivity.KEY_IS_LOGOUT, key.isLogoutMode)
            }
            context.startActivity(intent)
            removeDestination()
        }
    }
}
