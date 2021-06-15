package mega.privacy.android.app.meeting.activity

import android.content.Intent
import android.os.Bundle
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.databinding.ActivityGuestLeaveMeetingBinding
import mega.privacy.android.app.lollipop.LoginActivityLollipop
import mega.privacy.android.app.utils.Constants

class LeftMeetingActivity : BaseActivity() {
    private lateinit var binding: ActivityGuestLeaveMeetingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
    fun createAccount(){
        val createAccountIntent = Intent(this, LoginActivityLollipop::class.java)
        createAccountIntent.putExtra(Constants.VISIBLE_FRAGMENT, Constants.CREATE_ACCOUNT_FRAGMENT)
        startActivity(createAccountIntent)
        finish()
    }
}