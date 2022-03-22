package mega.privacy.android.app.meeting.activity

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.databinding.ActivityGuestLeaveMeetingBinding
import mega.privacy.android.app.main.LoginActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaChatCall

class LeftMeetingActivity : BaseActivity() {
    private lateinit var binding: ActivityGuestLeaveMeetingBinding

    private val callStatusObserver = Observer<MegaChatCall> {
        if (it.status == MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION&&
            it.termCode == MegaChatCall.TERM_CODE_TOO_MANY_PARTICIPANTS
        ) {
            Util.showSnackbar(
                this,
                StringResourcesUtils.getString(R.string.call_error_too_many_participants)
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