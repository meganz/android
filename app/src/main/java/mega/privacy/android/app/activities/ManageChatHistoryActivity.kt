package mega.privacy.android.app.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.FOCUS_BEFORE_DESCENDANTS
import android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
import android.widget.NumberPicker
import android.widget.NumberPicker.OnValueChangeListener
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_manage_chat_history.*
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
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatRoom

class ManageChatHistoryActivity : PinActivityLollipop(), View.OnClickListener {

    companion object {
        private const val OPTION_HOURS = 0
        private const val OPTION_DAYS = 1
        private const val OPTION_MONTHS = 3
        private const val OPTION_WEEKS = 2
        private const val OPTION_YEARS = 4

        private const val MINIMUM_VALUE_NUMBER_PICKER = 1
        private const val MAXIMUM_VALUE_NUMBER_PICKER_HOURS = 24
        private const val MAXIMUM_VALUE_NUMBER_PICKER_DAYS = 31
        private const val MAXIMUM_VALUE_NUMBER_PICKER_WEEKS = 4
        private const val MAXIMUM_VALUE_NUMBER_PICKER_MONTHS = 12
        private const val MINIMUM_VALUE_TEXT_PICKER = 0
        private const val MAXIMUM_VALUE_TEXT_PICKER = 4
    }

    private var screenOrientation: Int? = 0
    private var chat: MegaChatRoom? = null
    private var chatId = MEGACHAT_INVALID_HANDLE
    private var contactHandle = INVALID_HANDLE
    private var isFromContacts: Boolean? = false
    private var listener: RetentionTimeListener? = null

    private lateinit var binding: ActivityManageChatHistoryBinding

    private val retentionTimeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == null || intent.action != ACTION_UPDATE_RETENTION_TIME) {
                return
            }

            val seconds =
                intent.getLongExtra(BroadcastConstants.RETENTION_TIME, DISABLED_RETENTION_TIME)
            updateRetentionTimeUI(seconds)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent == null || intent.extras == null) {
            logError("Cannot init view, Intent is null")
            finish()
        }

        chatId = intent.extras!!.getLong(CHAT_ID)
        isFromContacts = intent.extras!!.getBoolean(IS_FROM_CONTACTS)

        if (chatId != MEGACHAT_INVALID_HANDLE) {
            logDebug("Group info")
            chat = megaChatApi.getChatRoom(chatId)
        } else {
            logDebug("Contact info")
            val email = intent.extras!!.getString(EMAIL)

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

            chat = megaChatApi.getChatRoomByUser(contactHandle)
            if (chat != null)
                chatId = chat?.chatId!!
        }

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

        binding.historyRetentionSwitch.isClickable = false
        binding.historyRetentionSwitch.isChecked = false
        binding.pickerLayout.visibility = View.GONE
        binding.separator.visibility = View.GONE

        if (chat == null) {
            logDebug("The chat does not exist")
            binding.historyRetentionSwitchLayout.setOnClickListener(null)
            binding.clearChatHistoryLayout.setOnClickListener(null)
            binding.retentionTimeTextLayout.setOnClickListener(null)
            binding.retentionTimeSubtitle.text =
                getString(R.string.subtitle_properties_history_retention)
            binding.retentionTime.visibility = View.GONE
        } else {
            logDebug("The chat exists")
            binding.historyRetentionSwitchLayout.setOnClickListener(this)
            binding.clearChatHistoryLayout.setOnClickListener(this)

            if (isFromContacts as Boolean) {
                listener = RetentionTimeListener(this)
                megaChatApi.closeChatRoom(chatId, listener)
                if (megaChatApi.openChatRoom(chatId, listener)) {
                    logDebug("Successful open chat")
                }
            }

            val seconds = chat!!.retentionTime
            updateRetentionTimeUI(seconds)
        }
    }

    /**
     * Method that controls and shows the initial UI of the picket elements.
     *
     * @param seconds The time the retention time is enabled.
     */
    fun showPickers(seconds: Long) {
        logDebug("Show the pickers")
        binding.pickerLayout.visibility = View.VISIBLE
        binding.separator.visibility = View.VISIBLE
        binding.pickerButton.setOnClickListener(this)

        binding.numberPicker.wrapSelectorWheel = true
        binding.textPicker.wrapSelectorWheel = true

        binding.numberPicker.minValue = MINIMUM_VALUE_NUMBER_PICKER

        binding.textPicker.minValue = MINIMUM_VALUE_TEXT_PICKER
        binding.textPicker.maxValue = MAXIMUM_VALUE_TEXT_PICKER

        if (seconds == DISABLED_RETENTION_TIME) {
            updatePickersValues(MINIMUM_VALUE_TEXT_PICKER, MAXIMUM_VALUE_NUMBER_PICKER_HOURS, MINIMUM_VALUE_NUMBER_PICKER)
        } else {
            checkPickersValues(seconds)
        }

        binding.numberPicker.setOnValueChangedListener(onValueChangeListenerPickerNumber)
        binding.textPicker.setOnValueChangedListener(onValueChangeListenerPickerText)
    }

    /**
     * Method for filling the text picker array from the value of the picker number value.
     *
     * @param value The current value of number picker.
     */
    private fun fillPickerText(value: Int) {
        binding.textPicker.displayedValues = null
        val arrayString: Array<String> = arrayOf(
            app.baseContext.resources.getQuantityString(
                R.plurals.retention_time_picker_hours,
                value
            ),
            app.baseContext.resources.getQuantityString(
                R.plurals.retention_time_picker_days,
                value
            ),
            app.baseContext.resources.getQuantityString(
                R.plurals.retention_time_picker_weeks,
                value
            ),
            app.baseContext.resources.getQuantityString(
                R.plurals.retention_time_picker_months,
                value
            ),
            app.getString(R.string.year_cc).toLowerCase()
        )

        binding.textPicker.setFormatter { value ->
            arrayString[value]
        }

        binding.textPicker.displayedValues = arrayString
    }

    /**
     * Updates the initial values of the pickers.
     *
     * @param textValue The current value of text picker
     * @param maximumValue The maximum value of numbers picker
     * @param numberValue The current value of number picker
     */
    private fun updatePickersValues(textValue: Int, maximumValue: Int, numberValue: Int) {
        binding.textPicker.value = textValue
        binding.numberPicker.maxValue = maximumValue
        binding.numberPicker.value = numberValue
        fillPickerText(binding.numberPicker.value)
    }

    /**
     * Controls the initial values of the pickers.
     *
     * @param seconds The retention time in seconds.
     */
    private fun checkPickersValues(seconds: Long) {
        val numberYears = seconds / SECONDS_IN_YEAR
        val years = seconds - numberYears * SECONDS_IN_YEAR
        if (years == 0L) {
            updatePickersValues(OPTION_YEARS, MINIMUM_VALUE_NUMBER_PICKER, numberYears.toInt())
            return
        }
        val numberMonths = seconds / SECONDS_IN_MONTH_31
        val months = seconds - numberMonths * SECONDS_IN_MONTH_31

        if (months == 0L) {
            updatePickersValues(
                OPTION_MONTHS,
                MAXIMUM_VALUE_NUMBER_PICKER_MONTHS,
                numberMonths.toInt()
            )
            return
        }
        val numberWeeks = seconds / SECONDS_IN_WEEK
        val weeks = seconds - numberWeeks * SECONDS_IN_WEEK
        if (weeks == 0L) {
            updatePickersValues(
                OPTION_WEEKS,
                MAXIMUM_VALUE_NUMBER_PICKER_WEEKS,
                numberWeeks.toInt()
            )
            return
        }
        val numberDays = seconds / SECONDS_IN_DAY
        val days = seconds - numberDays * SECONDS_IN_DAY
        if (days == 0L) {
            updatePickersValues(OPTION_DAYS, MAXIMUM_VALUE_NUMBER_PICKER_DAYS, numberDays.toInt())
            return
        }
        val numberHours = seconds / SECONDS_IN_HOUR
        val hours = seconds - numberHours * SECONDS_IN_HOUR
        if (hours == 0L) {
            updatePickersValues(
                OPTION_HOURS,
                MAXIMUM_VALUE_NUMBER_PICKER_HOURS,
                numberHours.toInt()
            )
        }
    }

    /**
     * Updates the values of the text picker according to the current value of the number picker.
     *
     * @param oldValue the previous value of the number picker
     * @param newValue the current value of the number picker
     */
    private fun updateTextPicker(oldValue: Int, newValue: Int) {
        if (oldValue == 1 && newValue == 1 || oldValue > 1 && newValue > 1 || binding.textPicker.value == OPTION_YEARS)
            return

        if (oldValue == 1 && newValue > 1 || newValue == 1 && oldValue > 1) {
            fillPickerText(newValue)
        }
    }

    /**
     * Method that transforms the chosen option into the most correct form:
    If the option selected is 24 hours, it becomes 1 day.
    If the selected option is 31 days, it becomes 1 month.
    If the selected option is 12 months, it becomes 1 year.
     */
    private fun updateOptionsAccordingly(){
        if(binding.numberPicker.value == MAXIMUM_VALUE_NUMBER_PICKER_HOURS &&
                binding.textPicker.value == OPTION_HOURS){
                    updatePickersValues(OPTION_DAYS, getMaximumValueOfNumberPicker(OPTION_DAYS), MINIMUM_VALUE_NUMBER_PICKER)
            return
        }

        if(binding.numberPicker.value == MAXIMUM_VALUE_NUMBER_PICKER_DAYS &&
            binding.textPicker.value == OPTION_DAYS){
            updatePickersValues(OPTION_MONTHS, getMaximumValueOfNumberPicker(OPTION_MONTHS), MINIMUM_VALUE_NUMBER_PICKER)
            return
        }

        if(binding.numberPicker.value == MAXIMUM_VALUE_NUMBER_PICKER_MONTHS &&
            binding.textPicker.value == OPTION_MONTHS){
            updatePickersValues(OPTION_YEARS, getMaximumValueOfNumberPicker(OPTION_YEARS), MINIMUM_VALUE_NUMBER_PICKER)
            return
        }
    }

    /**
     *
     * Method for getting the maximum value of the picker number from a value.
     * @param value the value
     */
    private fun getMaximumValueOfNumberPicker(value: Int): Int {
        var maximumValue = 0
        when (value) {
            OPTION_HOURS -> {
                maximumValue = MAXIMUM_VALUE_NUMBER_PICKER_HOURS
            }
            OPTION_DAYS -> {
                maximumValue = MAXIMUM_VALUE_NUMBER_PICKER_DAYS
            }
            OPTION_WEEKS -> {
                maximumValue = MAXIMUM_VALUE_NUMBER_PICKER_WEEKS
            }
            OPTION_MONTHS -> {
                maximumValue = MAXIMUM_VALUE_NUMBER_PICKER_MONTHS
            }
            OPTION_YEARS -> {
                maximumValue = MINIMUM_VALUE_NUMBER_PICKER
            }
        }
        return maximumValue
    }

    /**
     * Method that updates the values of the number picker according to the current value of the text picker.
     *
     * @param value the current value of the text picker
     */
    private fun updateNumberPicker(value: Int) {
        var maximumValue = getMaximumValueOfNumberPicker(value)

        if (binding.numberPicker.value > maximumValue) {
            updateTextPicker(binding.numberPicker.value, MINIMUM_VALUE_NUMBER_PICKER)
            binding.numberPicker.value = MINIMUM_VALUE_NUMBER_PICKER
        }
        binding.numberPicker.maxValue = maximumValue
    }

    private var onValueChangeListenerPickerNumber =
        OnValueChangeListener { numberPicker, oldValue, newValue ->
            updateTextPicker(oldValue, newValue)
            updateOptionsAccordingly()
        }

    private var onValueChangeListenerPickerText =
        OnValueChangeListener { textPicker, i, i1 ->
            updateNumberPicker(textPicker.value)
            updateOptionsAccordingly()
        }

    /**
     * Method for updating the UI when the retention time is updated.
     *
     * @param seconds The retention time in seconds
     */
    private fun updateRetentionTimeUI(seconds: Long) {
        val timeFormatted = ChatUtil.transformSecondsInString(seconds)
        if (TextUtil.isTextEmpty(timeFormatted)) {
            binding.retentionTimeTextLayout.setOnClickListener(null)
            binding.historyRetentionSwitch.isChecked = false
            binding.retentionTimeSubtitle.text =
                getString(R.string.subtitle_properties_history_retention)
            binding.retentionTime.visibility = View.GONE
        } else {
            binding.retentionTimeTextLayout.setOnClickListener(this)
            binding.historyRetentionSwitch.isChecked = true
            binding.retentionTimeSubtitle.text = getString(R.string.subtitle_properties_manage_chat)
            binding.retentionTime.text = timeFormatted
            binding.retentionTime.visibility = View.VISIBLE
        }
        binding.pickerLayout.visibility = View.GONE
        binding.separator.visibility = View.GONE
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
                binding.pickerLayout.visibility = View.GONE
                binding.separator.visibility = View.GONE
                var secondInOption = 0

                when (binding.textPicker.value) {
                    OPTION_HOURS -> {
                        secondInOption = SECONDS_IN_HOUR
                    }
                    OPTION_DAYS -> {
                        secondInOption = SECONDS_IN_DAY
                    }
                    OPTION_WEEKS -> {
                        secondInOption = SECONDS_IN_WEEK
                    }
                    OPTION_MONTHS -> {
                        secondInOption = SECONDS_IN_MONTH_31
                    }
                    OPTION_YEARS -> {
                        secondInOption = SECONDS_IN_YEAR
                    }
                }

                var totalSeconds = binding.numberPicker.value * secondInOption

                megaChatApi.setChatRetentionTime(
                    chatId,
                    totalSeconds.toLong(),
                    SetRetentionTimeListener(this)
                )
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)
            onBackPressed()

        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        if (isFromContacts == true && chatId != MEGACHAT_INVALID_HANDLE && listener != null) {
            logDebug("Successful close chat")
            megaChatApi.closeChatRoom(chatId, listener)
        }

        unregisterReceiver(retentionTimeReceiver)
        super.onDestroy()
    }
}