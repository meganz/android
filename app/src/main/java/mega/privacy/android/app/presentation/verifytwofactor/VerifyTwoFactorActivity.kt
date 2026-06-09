package mega.privacy.android.app.presentation.verifytwofactor

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.presentation.container.MegaAppContainer
import mega.privacy.android.app.presentation.verifytwofactor.view.VerifyTwoFactorScreen
import mega.privacy.android.app.utils.Constants.ACTION_PASS_CHANGED
import mega.privacy.android.app.utils.Constants.RESULT
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.destination.MyAccountNavKey
import javax.inject.Inject


@AndroidEntryPoint
internal class VerifyTwoFactorActivity : PasscodeActivity() {

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    private val viewModel by viewModels<VerifyTwoFactorViewModel>()

    /**
     * Initializes the activity and hosts [VerifyTwoFactorScreen].
     *
     * @param savedInstanceState Standard Android saved state bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by monitorThemeModeUseCase()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            MegaAppContainer(themeMode = themeMode) {
                VerifyTwoFactorScreen(
                    viewModel = viewModel,
                    onFinish = ::finish,
                    onDisableSuccess = { setResult(RESULT_OK) },
                    onLogout = viewModel::logout,
                    onNavigateToMyAccount = ::navigateToMyAccount,
                )
            }
        }
    }

    /**
     * Launches the MyAccount destination after a successful password change.
     *
     * @param resultCode SDK error code surfaced to MyAccount (typically `API_OK`).
     */
    private fun navigateToMyAccount(resultCode: Int) {
        val intent = MegaActivity.getIntentWithExtraDestinations(
            context = this,
            navKeys = listOf(
                MyAccountNavKey(
                    action = ACTION_PASS_CHANGED,
                    resultCode = resultCode,
                )
            ),
        ).apply {
            action = ACTION_PASS_CHANGED
            putExtra(RESULT, resultCode)
        }
        startActivity(intent)
    }

    companion object {
        /** Specifies intent data for verification type value. */
        const val KEY_VERIFY_TYPE = "key_verify_type"

        /** Specifies intent data for the new email value. */
        const val KEY_NEW_EMAIL = "key_new_email"

        /** Specifies intent data for the new password value. */
        const val KEY_NEW_PASSWORD = "key_new_password"
    }
}
