package mega.privacy.android.app.meeting.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.databinding.ActivityGuestLeaveMeetingBinding
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.meeting.LeftMeetingViewModel
import mega.privacy.android.app.presentation.meeting.view.dialog.FreePlanLimitParticipantsDialog
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.shared.theme.MegaAppTheme
import nz.mega.sdk.MegaChatCall

class LeftMeetingActivity : BaseActivity() {
    private lateinit var binding: ActivityGuestLeaveMeetingBinding
    private val viewModel by viewModels<LeftMeetingViewModel>()

    private val callStatusObserver = Observer<MegaChatCall> {
        if (it.status == MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION &&
            it.termCode == MegaChatCall.TERM_CODE_TOO_MANY_PARTICIPANTS
        ) {
            Util.showSnackbar(
                this,
                getString(R.string.call_error_too_many_participants)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LiveEventBus.get(EventConstants.EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
            .observeSticky(this, callStatusObserver)

        binding = ActivityGuestLeaveMeetingBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                val isDark = Util.isDarkMode(this@LeftMeetingActivity)
                val state by viewModel.state.collectAsStateWithLifecycle()
                if (state.callEndedDueToFreePlanLimits && state.isCallUnlimitedProPlanFeatureFlagEnabled) {
                    MegaAppTheme(isDark = isDark) {
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