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
import mega.privacy.android.app.utils.Constants.*
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

    private val MAXIMUM_VALUE_NUMBER_PICKER_HOURS = 24
    private val MAXIMUM_VALUE_NUMBER_PICKER_DAYS = 31
    private val MAXIMUM_VALUE_NUMBER_PICKER_WEEKS = 4
    private val MAXIMUM_VALUE_NUMBER_PICKER_MONTHS = 12
    private val MINIMUM_VALUE_NUMBER_PICKER = 1

    private val MINIMUM_VALUE_TEXT_PICKER = 0
    private val MAXIMUM_VALUE_TEXT_PICKER = 4

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
        binding.separator.visibility = View.GONE

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

    fun showInitPicker(seconds: Long){
        binding.pickerLayout.visibility = View.VISIBLE
        binding.separator.visibility = View.VISIBLE
        binding.pickerButton.setOnClickListener(this)

        binding.pickerNumber.disableTextEditing(true)
        binding.pickerNumber.wrapSelectorWheel = true
        binding.pickerNumber.minValue = MINIMUM_VALUE_NUMBER_PICKER

        binding.pickerText.disableTextEditing(true)
        binding.pickerText.wrapSelectorWheel = true
        binding.pickerText.minValue = MINIMUM_VALUE_TEXT_PICKER;
        binding.pickerText.maxValue = MAXIMUM_VALUE_TEXT_PICKER

        var valueInNumberPicker = MINIMUM_VALUE_NUMBER_PICKER

        if(seconds == DISABLED_RETENTION_TIME){
            binding.pickerNumber.maxValue = MAXIMUM_VALUE_NUMBER_PICKER_HOURS
            binding.pickerNumber.value = MINIMUM_VALUE_NUMBER_PICKER
            binding.pickerText.value = MINIMUM_VALUE_TEXT_PICKER
        }else{


            val numberYears = seconds / SECONDS_IN_YEAR
            val years = seconds - numberYears * SECONDS_IN_YEAR
            if (years == 0L) {
                binding.pickerText.value = OPTION_YEARS
                binding.pickerNumber.maxValue = MINIMUM_VALUE_NUMBER_PICKER
                binding.pickerNumber.value = numberYears.toInt()
            } else {
                val numberMonths = seconds / SECONDS_IN_MONTH_31
                val months = seconds - numberMonths * SECONDS_IN_MONTH_31
                if (months == 0L) {
                    binding.pickerText.value = OPTION_MONTHS
                    binding.pickerNumber.maxValue = MAXIMUM_VALUE_NUMBER_PICKER_MONTHS
                    binding.pickerNumber.value = numberMonths.toInt()
                } else {
                    val numberWeeks = seconds / SECONDS_IN_WEEK
                    val weeks = seconds - numberWeeks * SECONDS_IN_WEEK
                    if (weeks == 0L) {
                        binding.pickerText.value = OPTION_WEEKS
                        binding.pickerNumber.maxValue = MAXIMUM_VALUE_NUMBER_PICKER_WEEKS
                        binding.pickerNumber.value = numberWeeks.toInt()
                    } else {
                        val numberDays = seconds / SECONDS_IN_DAY
                        val days = seconds - numberDays * SECONDS_IN_DAY
                        if (days == 0L) {
                            binding.pickerText.value = OPTION_DAYS
                            binding.pickerNumber.maxValue = MAXIMUM_VALUE_NUMBER_PICKER_DAYS
                            binding.pickerNumber.value = numberDays.toInt()
                        } else {
                            val numberHours = seconds / SECONDS_IN_HOUR
                            val hours = seconds - numberHours * SECONDS_IN_HOUR
                            if (hours == 0L) {
                                binding.pickerText.value = OPTION_HOURS
                                binding.pickerNumber.maxValue = MAXIMUM_VALUE_NUMBER_PICKER_HOURS
                                binding.pickerNumber.value = numberHours.toInt()
                            }
                        }
                    }
                }
            }

            valueInNumberPicker = binding.pickerNumber.value
        }

        val arrayString = arrayOf(
            app.baseContext.resources.getQuantityString(
                R.plurals.retention_time_picker_hours,
                valueInNumberPicker
            ),
            app.baseContext.resources.getQuantityString(
                R.plurals.retention_time_picker_days,
                valueInNumberPicker
            ),
            app.baseContext.resources.getQuantityString(
                R.plurals.retention_time_picker_weeks,
                valueInNumberPicker
            ),
            app.baseContext.resources.getQuantityString(
                R.plurals.retention_time_picker_months,
                valueInNumberPicker
            ),
            app.getString(R.string.retention_time_picker_year)
        )

        binding.pickerText.setFormatter { value ->
            arrayString[value]
        }

        binding.pickerText.setDisplayedValues(arrayString)

        binding.pickerNumber.setOnValueChangedListener(onValueChangeListenerPickerNumber)
        binding.pickerText.setOnValueChangedListener(onValueChangeListenerPickerText)

    }

    /**
     * Method that updates the values of the text picker according to the current value of the number picker.
     */
    private fun updateTextPicker(oldValue: Int, newValue: Int) {
        if (oldValue == 1 && newValue == 1 || oldValue > 1 && newValue > 1 || binding.pickerText.value == OPTION_YEARS)
            return

        if (oldValue == 1 && newValue > 1 || newValue == 1 && oldValue > 1) {
            val newArrayString = arrayOf(
                app.baseContext.resources.getQuantityString(
                    R.plurals.retention_time_picker_hours,
                    newValue
                ),
                app.baseContext.resources.getQuantityString(
                    R.plurals.retention_time_picker_days,
                    newValue
                ),
                app.baseContext.resources.getQuantityString(
                    R.plurals.retention_time_picker_weeks,
                    newValue
                ),
                app.baseContext.resources.getQuantityString(
                    R.plurals.retention_time_picker_months,
                    newValue
                ),
                app.getString(R.string.retention_time_picker_year)
            )

            binding.pickerText.setFormatter { value ->
                newArrayString[value]
            }
            binding.pickerText.displayedValues = newArrayString
        }
    }

    /**
     * Method that updates the values of the number picker according to the current value of the text picker.
     */
    private fun updateNumberPicker(value: Int) {
        var maximoValue = 0
        if (value == OPTION_HOURS) {
            maximoValue = MAXIMUM_VALUE_NUMBER_PICKER_HOURS
        } else if (value == OPTION_DAYS) {
            maximoValue = MAXIMUM_VALUE_NUMBER_PICKER_DAYS
        } else if (value == OPTION_WEEKS) {
            maximoValue = MAXIMUM_VALUE_NUMBER_PICKER_WEEKS
        } else if (value == OPTION_MONTHS) {
            maximoValue = MAXIMUM_VALUE_NUMBER_PICKER_MONTHS
        } else if (value == OPTION_YEARS) {
            maximoValue = MINIMUM_VALUE_NUMBER_PICKER
        }

        if (binding.pickerNumber.value > maximoValue) {
            updateTextPicker(binding.pickerNumber.value, MINIMUM_VALUE_NUMBER_PICKER)
            binding.pickerNumber.value = MINIMUM_VALUE_NUMBER_PICKER
        }

        binding.pickerNumber.maxValue = maximoValue
    }

    fun NumberPicker.disableTextEditing(disable: Boolean) {
        descendantFocusability = if (disable) FOCUS_BLOCK_DESCENDANTS else FOCUS_BEFORE_DESCENDANTS
    }

    var onValueChangeListenerPickerNumber =
        OnValueChangeListener { numberPicker, oldValue, newValue ->
            updateTextPicker(oldValue, newValue)
        }

    var onValueChangeListenerPickerText =
        OnValueChangeListener { textPicker, i, i1 ->
            updateNumberPicker(textPicker.value)
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
                binding.pickerLayout.visibility = View.GONE
                binding.separator.visibility = View.GONE
                var secondInOption = 0
                if (binding.pickerText.value == OPTION_HOURS) {
                    secondInOption = SECONDS_IN_HOUR
                } else if (binding.pickerText.value == OPTION_DAYS) {
                    secondInOption = SECONDS_IN_DAY
                } else if (binding.pickerText.value == OPTION_WEEKS) {
                    secondInOption = SECONDS_IN_WEEK
                } else if (binding.pickerText.value == OPTION_MONTHS) {
                    secondInOption = SECONDS_IN_MONTH_31
                } else if (binding.pickerText.value == OPTION_YEARS) {
                    secondInOption = SECONDS_IN_YEAR
                }
                var totalSeconds = binding.pickerNumber.value * secondInOption

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
        if (chatId != MEGACHAT_INVALID_HANDLE && listener != null) {
            megaChatApi.closeChatRoom(chatId, listener)
        }

        unregisterReceiver(retentionTimeReceiver)
        super.onDestroy()
    }
}