package mega.privacy.android.app.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.NumberPicker.OnValueChangeListener
import androidx.annotation.IntegerRes
import androidx.core.content.ContextCompat
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_RETENTION_TIME
import mega.privacy.android.app.databinding.ActivityManageChatHistoryBinding
import mega.privacy.android.app.listeners.RetentionTimeListener
import mega.privacy.android.app.listeners.SetRetentionTimeListener
import mega.privacy.android.app.lollipop.PinActivityLollipop
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ChatUtil.createHistoryRetentionAlertDialog
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.DISABLED_RETENTION_TIME
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatRoom


class ManageChatHistoryActivity : PinActivityLollipop(), View.OnClickListener {

    private val OPTION_HOURS = 0
    private val OPTION_DAYS = 1
    private val OPTION_WEEKS = 2
    private val OPTION_MONTHS = 3
    private val OPTION_YEARS = 4

    private var screenOrientation = 0
    private lateinit var binding: ActivityManageChatHistoryBinding
    private var chat: MegaChatRoom? = null
    private var chatId = MEGACHAT_INVALID_HANDLE
    private var contactHandle = INVALID_HANDLE

    private var listener: RetentionTimeListener? = null

    private val retentionTimeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == null || intent.action != ACTION_UPDATE_RETENTION_TIME) {
                return
            }

            val seconds = intent.getLongExtra(BroadcastConstants.RETENTION_TIME, 0)
            updateRetentionTimeUI(seconds)
        }
    }

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

        contactHandle = contact?.handle!!

        registerReceiver(
            retentionTimeReceiver,
            IntentFilter(ACTION_UPDATE_RETENTION_TIME)
        )

        binding = ActivityManageChatHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(
            applicationContext,
            R.color.dark_primary_color
        )

        setSupportActionBar(binding.manageChatToolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setHomeButtonEnabled(true)
        actionBar?.title = getString(R.string.title_properties_manage_chat).toUpperCase()
        screenOrientation = resources.configuration.orientation

        chat = megaChatApi.getChatRoomByUser(contactHandle)
        binding.historyRetentionSwitch.isClickable = false;
        binding.historyRetentionSwitch.isChecked = false
        binding.pickerLayout.visibility= View.GONE

        if(chat == null){
            logDebug("The chat does not exist")
            binding.historyRetentionSwitchLayout.setOnClickListener(null)
            binding.clearChatHistoryLayout.setOnClickListener(null)
            binding.retentionTimeTextLayout.setOnClickListener(null)

            binding.retentionTimeTitle.text =  getString(R.string.title_properties_history_retention)
            binding.retentionTimeSubtitle.text =  getString(R.string.subtitle_properties_history_retention)
            binding.retentionTime.visibility = View.GONE
        }else{
            logDebug("The chat exists")
            binding.historyRetentionSwitchLayout.setOnClickListener(this)
            binding.clearChatHistoryLayout.setOnClickListener(this)
            chatId = chat?.chatId!!
            listener = RetentionTimeListener(this)
            megaChatApi.closeChatRoom(chatId, listener)
            if (megaChatApi.openChatRoom(chatId, listener)) {
                logDebug("Successful open chat");
            }

            val seconds = chat!!.retentionTime

            updateRetentionTimeUI(seconds)
        }
    }

    fun showInitPicker(){
        binding.pickerLayout.visibility = View.VISIBLE
        binding.pickerButton.setOnClickListener(this)

        binding.pickerNumber.minValue = 1
        binding.pickerNumber.maxValue = 24
        binding.pickerNumber.wrapSelectorWheel = true
        binding.pickerNumber.value = 1
        binding.pickerNumber.setOnValueChangedListener(onValueChangeListenerPickerNumber)

        val arrayString = arrayOf(
            app.getString(R.string.retention_time_picker_hours),
            app.getString(R.string.hint_days),
            app.getString(R.string.retention_time_picker_weeks),
            app.getString(R.string.retention_time_picker_months),
            app.getString(R.string.retention_time_picker_years)
        )

        binding.pickerText.minValue = 0;
        binding.pickerText.maxValue = 4

        binding.pickerText.setFormatter { value ->
            arrayString[value]
        }

        binding.pickerText.setOnValueChangedListener(onValueChangeListenerPickerText)

        binding.pickerNumber.value
    }

    private fun updateNumberPicker(typePicketTextOption: IntegerRes){

    }

    var onValueChangeListenerPickerText =
        OnValueChangeListener { textPicker, i, i1 ->
        }

    var onValueChangeListenerPickerNumber =
        OnValueChangeListener { numberPicker, i, i1 ->
            when (numberPicker.value) {
                OPTION_HOURS -> {
                }
                OPTION_DAYS -> {
                }
                OPTION_WEEKS -> {
                }
                OPTION_MONTHS -> {
                }
                OPTION_YEARS -> {
                }
            }
        }

    /**
     * Method for updating the UI when the retention time is updated.
     */
    private fun updateRetentionTimeUI(seconds: Long) {
        val timeFormatted = ChatUtil.transformSecondsInString(seconds)
        if(TextUtil.isTextEmpty(timeFormatted)){
            binding.retentionTimeTextLayout.setOnClickListener(null)

            binding.historyRetentionSwitch.isChecked = false
            binding.retentionTimeTitle.text =  getString(R.string.title_properties_history_retention)
            binding.retentionTimeSubtitle.text =  getString(R.string.subtitle_properties_history_retention)
            binding.retentionTime.visibility = View.GONE
        }else{
            binding.retentionTimeTextLayout.setOnClickListener(this)

            binding.historyRetentionSwitch.isChecked = true
            binding.retentionTimeTitle.text =  getString(R.string.title_properties_history_retention_activated)
            binding.retentionTimeSubtitle.text =  getString(R.string.subtitle_properties_manage_chat)
            binding.retentionTime.text = timeFormatted
            binding.retentionTime.visibility = View.VISIBLE
        }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.clear_chat_history_layout -> {
                ChatUtil.showConfirmationClearChat(this, chat)
            }

            R.id.history_retention_switch_layout -> {
                if (binding.historyRetentionSwitch.isChecked) {
                    MegaApplication.getInstance().megaChatApi.setChatRetentionTime(
                        chat!!.chatId,
                        DISABLED_RETENTION_TIME,
                        SetRetentionTimeListener(this)
                    )

                } else {
                    createHistoryRetentionAlertDialog(this, chatId, true)
                }
            }

            R.id.retention_time_text_layout -> {
                createHistoryRetentionAlertDialog(this, chatId, false)
            }

            R.id.picker_button -> {
                binding.pickerLayout?.visibility = View.GONE
            }

        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)
            onBackPressed()

        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        if (chatId != MEGACHAT_INVALID_HANDLE && listener != null) {
            megaChatApi.closeChatRoom(chatId, listener)
        }

        unregisterReceiver(retentionTimeReceiver)
        super.onDestroy()
    }
}