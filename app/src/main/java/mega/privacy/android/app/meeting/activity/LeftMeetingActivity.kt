package mega.privacy.android.app.meeting.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.ActivityGuestLeaveMeetingBinding
import mega.privacy.android.app.extensions.enableEdgeToEdgeAndConsumeInsets
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.meeting.LeftMeetingViewModel
import mega.privacy.android.app.presentation.meeting.view.dialog.FreePlanLimitParticipantsDialog
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

class LeftMeetingActivity : BaseActivity() {
    private lateinit var binding: ActivityGuestLeaveMeetingBinding
    private val viewModel by viewModels<LeftMeetingViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeAndConsumeInsets()
        super.onCreate(savedInstanceState)

        binding = ActivityGuestLeaveMeetingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        collectFlows()

        binding.btCreateAccount.setOnClickListener {
            createAccount()
        }

        binding.ivRemove.setOnClickListener {
            finish()
        }

        binding.composeView.apply {
            this.isVisible = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val state by viewModel.state.collectAsStateWithLifecycle()
                if (state.callEndedDueToFreePlanLimits) {
                    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
                        FreePlanLimitParticipantsDialog(
                            onConfirm = {
                                viewModel.onConsumeShowFreePlanParticipantsLimitDialogEvent()
                            },
                        )
                    }
                }
            }
        }
    }

    private fun collectFlows() {
        collectFlow(viewModel.state.map { it.callEndedDueToTooManyParticipants }
            .distinctUntilChanged()) { showSnackbar ->
            if (showSnackbar) {
                viewModel.onConsumeShowParticipantsLimitSnackbarEvent()
                Util.showSnackbar(
                    this,
                    getString(R.string.call_error_too_many_participants)
                )
            }
        }
    }

    /**
     * Open Create Mega Account page
     *
     */
    private fun createAccount() {
        val createAccountIntent = Intent(this, LoginActivity::class.java)
        createAccountIntent.putExtra(Constants.VISIBLE_FRAGMENT, Constants.CREATE_ACCOUNT_FRAGMENT)
        startActivity(createAccountIntent)
        finish()
    }
}
