package mega.privacy.android.app.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ActivityManageChatHistoryBinding
import mega.privacy.android.app.lollipop.PinActivityLollipop
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.MegaChatRoom

class ManageChatHistoryActivity : PinActivityLollipop(), View.OnClickListener {

    private var screenOrientation = 0
    private lateinit var binding: ActivityManageChatHistoryBinding
    private var chat: MegaChatRoom? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent == null || intent.extras == null) {
            logError("Cannot init view, Intent is null")
            finish()
        }

        val email = intent.extras!!.getString(Constants.EMAIL)

        if (TextUtil.isTextEmpty(email)) {
            logError("Cannot init view, contact' email is empty")
            finish()
        }

        var contact = megaApi.getContact(email)
        if (contact == null) {
            logError("Cannot init view, contact is null")
            finish()
        }

        chat = megaChatApi.getChatRoomByUser(contact.handle)
        if (chat == null) {
            logError("Cannot init view, chat is null")
            finish()
        }

        binding = ActivityManageChatHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(
            applicationContext,
            R.color.status_bar_red_alert
        )

        setSupportActionBar(binding.manageChatToolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setHomeButtonEnabled(true)
        actionBar?.title = getString(R.string.title_properties_manage_chat).toUpperCase()
        screenOrientation = resources.configuration.orientation

        binding.clearChatHistoryLayout?.setOnClickListener(this)
        binding.historyRetentionLayout?.setOnClickListener(this)


    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.clear_chat_history_layout -> {
                logDebug("**************** click in clear_chat_history_layout")
                ChatUtil.showConfirmationClearChat(this, chat)
            }

            R.id.history_retention_layout -> {
                logDebug("**************** click in history_retention_layout")

            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)
            onBackPressed()

        return super.onOptionsItemSelected(item)
    }

}