package mega.privacy.android.app.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.NumberPicker.OnScrollListener
import android.widget.NumberPicker.OnValueChangeListener
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_RETENTION_TIME
import mega.privacy.android.app.constants.BroadcastConstants.RETENTION_TIME
import mega.privacy.android.app.databinding.ActivityManageChatHistoryBinding
import mega.privacy.android.app.extensions.enableEdgeToEdgeAndConsumeInsets
import mega.privacy.android.app.presentation.chat.model.ChatRoomUiState
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.ClearChatConfirmationDialog
import mega.privacy.android.app.presentation.meeting.managechathistory.ManageChatHistoryViewModel
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ChatHistoryRetentionOption
import mega.privacy.android.app.presentation.meeting.managechathistory.view.dialog.ChatHistoryRetentionConfirmationDialog
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants.DISABLED_RETENTION_TIME
import mega.privacy.android.app.utils.Constants.SECONDS_IN_DAY
import mega.privacy.android.app.utils.Constants.SECONDS_IN_HOUR
import mega.privacy.android.app.utils.Constants.SECONDS_IN_MONTH_30
import mega.privacy.android.app.utils.Constants.SECONDS_IN_WEEK
import mega.privacy.android.app.utils.Constants.SECONDS_IN_YEAR
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ManageChatHistoryActivity : PasscodeActivity(), View.OnClickListener {
    companion object {
        private const val OPTION_HOURS = 0
        private const val OPTION_DAYS = 1
        private const val OPTION_MONTHS = 3
        private const val OPTION_WEEKS = 2
        private const val OPTION_YEARS = 4

        private const val MINIMUM_VALUE_NUMBER_PICKER = 1
        private const val DAYS_IN_A_MONTH_VALUE = 30
        private const val MAXIMUM_VALUE_NUMBER_PICKER_HOURS = 24
        private const val MAXIMUM_VALUE_NUMBER_PICKER_DAYS = 31
        private const val MAXIMUM_VALUE_NUMBER_PICKER_WEEKS = 4
        private const val MAXIMUM_VALUE_NUMBER_PICKER_MONTHS = 12
        private const val MINIMUM_VALUE_TEXT_PICKER = 0
        private const val MAXIMUM_VALUE_TEXT_PICKER = 4

        private const val IS_HISTORY_RETENTION_CONFIRMATION_SHOWN =
            "IS_HISTORY_RETENTION_CONFIRMATION_SHOWN"
        private const val IS_CLEAR_CHAT_CONFIRMATION_SHOWN =
            "IS_CLEAR_CHAT_CONFIRMATION_SHOWN"
    }

    /**
     * Current theme
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    internal val viewModel: ManageChatHistoryViewModel by viewModels()

    private lateinit var binding: ActivityManageChatHistoryBinding

    private val onBackPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            retryConnectionsAndSignalPresence()
            finish()
        }
    }

    private val retentionTimeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == null || intent.action != ACTION_UPDATE_RETENTION_TIME) {
                return
            }

            val seconds =
                intent.getLongExtra(RETENTION_TIME, DISABLED_RETENTION_TIME)
            viewModel.updateRetentionTimeState(seconds)
            updateRetentionTimeUI(seconds)
        }
    }

    private var shouldShowHistoryRetentionConfirmation by mutableStateOf(false)
    private var shouldShowClearChatConfirmation by mutableStateOf(false)

    /**
     * Called to retrieve per-instance state from an activity before being killed
     * so that the state can be restored in onCreate or onRestoreInstanceState
     * (the Bundle populated by this method will be passed to both).
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(
            IS_HISTORY_RETENTION_CONFIRMATION_SHOWN,
            shouldShowHistoryRetentionConfirmation
        )
        outState.putBoolean(
            IS_CLEAR_CHAT_CONFIRMATION_SHOWN,
            shouldShowClearChatConfirmation
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeAndConsumeInsets()
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            shouldShowHistoryRetentionConfirmation =
                it.getBoolean(IS_HISTORY_RETENTION_CONFIRMATION_SHOWN)
            shouldShowClearChatConfirmation = it.getBoolean(IS_CLEAR_CHAT_CONFIRMATION_SHOWN)
        }

        if (intent == null || intent.extras == null) {
            Timber.e("Cannot init view, Intent is null")
            finish()
        }

        viewModel.initializeChatRoom()

        collectFlows()

        registerReceiver(
            retentionTimeReceiver,
            IntentFilter(ACTION_UPDATE_RETENTION_TIME)
        )

        binding = ActivityManageChatHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.composeView.setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                LaunchedEffect(uiState.statusMessageResId) {
                    uiState.statusMessageResId?.let {
                        showSnackbar(binding.root, getString(it))
                        viewModel.onStatusMessageDisplayed()
                    }
                }

                if (shouldShowClearChatConfirmation) {
                    uiState.chatRoom?.apply {
                        ClearChatConfirmationDialog(
                            isMeeting = isMeeting,
                            onConfirm = {
                                shouldShowClearChatConfirmation = false
                                viewModel.clearChatHistory(chatId)
                            },
                            onDismiss = {
                                shouldShowClearChatConfirmation = false
                            }
                        )
                    }
                }

                if (shouldShowHistoryRetentionConfirmation) {
                    ChatHistoryRetentionConfirmationDialog(
                        currentRetentionTime = uiState.retentionTime,
                        onDismissRequest = {
                            shouldShowHistoryRetentionConfirmation = false
                        },
                        onConfirmClick = {
                            if (it == ChatHistoryRetentionOption.Custom) {
                                showPickers(uiState.retentionTime)
                            } else {
                                viewModel.setChatRetentionTime(getSecondsFromRetentionTimeOption(it))
                            }
                            shouldShowHistoryRetentionConfirmation = false
                        }
                    )
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, onBackPressCallback)
    }

    private fun getSecondsFromRetentionTimeOption(option: ChatHistoryRetentionOption) =
        when (option) {
            ChatHistoryRetentionOption.OneDay -> SECONDS_IN_DAY.toLong()
            ChatHistoryRetentionOption.OneWeek -> SECONDS_IN_WEEK.toLong()
            ChatHistoryRetentionOption.OneMonth -> SECONDS_IN_MONTH_30.toLong()
            else -> DISABLED_RETENTION_TIME
        }

    private fun collectFlows() {
        collectFlow(viewModel.uiState) { uiState ->
            if (uiState.shouldNavigateUp) {
                finish()
                viewModel.onNavigatedUp()
            }
        }

        collectFlow(viewModel.uiState.map { it.chatRoom }.distinctUntilChanged()) {
            setupUI(it)
        }

        collectFlow(viewModel.uiState.map { it.retentionTime }.distinctUntilChanged()) {
            updateRetentionTimeUI(it)
        }
    }

    private fun setupUI(chat: ChatRoomUiState?) {
        setSupportActionBar(binding.manageChatToolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setHomeButtonEnabled(true)
        actionBar?.title = if (chat?.isMeeting == true) {
            getString(R.string.meetings_manage_history_view_title)
        } else {
            getString(R.string.title_properties_manage_chat)
        }

        binding.historyRetentionSwitch.isClickable = false
        binding.historyRetentionSwitch.isChecked = false
        binding.pickerLayout.visibility = View.GONE
        binding.separator.visibility = View.GONE

        if (chat == null) {
            Timber.d("The chat does not exist")
            binding.historyRetentionSwitchLayout.setOnClickListener(null)
            binding.clearChatHistoryLayout.setOnClickListener(null)
            binding.retentionTimeTextLayout.setOnClickListener(null)
            binding.retentionTimeSubtitle.text =
                getString(R.string.subtitle_properties_history_retention)
            binding.retentionTime.visibility = View.GONE
        } else {
            Timber.d("The chat exists")
            binding.historyRetentionSwitchLayout.setOnClickListener(this)
            binding.clearChatHistoryLayout.setOnClickListener(this)
            binding.clearChatHistoryLayoutTitle.text = if (chat.isMeeting) {
                getString(R.string.meetings_manage_history_clear)
            } else {
                getString(R.string.title_properties_clear_chat_history)
            }

            val seconds = chat.retentionTime
            updateRetentionTimeUI(seconds)

            binding.numberPicker.setOnScrollListener(onScrollListenerPickerNumber)
            binding.numberPicker.setOnValueChangedListener(onValueChangeListenerPickerNumber)
            binding.textPicker.setOnScrollListener(onScrollListenerPickerText)
            binding.textPicker.setOnValueChangedListener(onValueChangeListenerPickerText)
            binding.pickerButton.setOnClickListener(this)
        }
    }

    private var onValueChangeListenerPickerNumber =
        OnValueChangeListener { _, oldValue, newValue ->
            updateTextPicker(oldValue, newValue)
        }

    private var onValueChangeListenerPickerText =
        OnValueChangeListener { textPicker, _, _ ->
            updateNumberPicker(textPicker.value)
        }

    private var onScrollListenerPickerNumber =
        OnScrollListener { _, scrollState ->
            if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                updateOptionsAccordingly()
            }
        }

    private var onScrollListenerPickerText =
        OnScrollListener { _, scrollState ->
            if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                updateOptionsAccordingly()
            }
        }

    /**
     * Method that controls and shows the initial UI of the picket elements.
     *
     * @param seconds The time the retention time is enabled.
     */
    fun showPickers(seconds: Long) {
        Timber.d("Show the pickers")
        binding.pickerLayout.visibility = View.VISIBLE
        binding.separator.visibility = View.VISIBLE

        binding.numberPicker.wrapSelectorWheel = true
        binding.textPicker.wrapSelectorWheel = true

        binding.textPicker.minimumWidth = MAXIMUM_VALUE_TEXT_PICKER
        binding.numberPicker.minValue = MINIMUM_VALUE_NUMBER_PICKER
        binding.textPicker.minValue = MINIMUM_VALUE_TEXT_PICKER
        binding.textPicker.maxValue = MAXIMUM_VALUE_TEXT_PICKER

        if (seconds == DISABLED_RETENTION_TIME) {
            updatePickersValues(
                MINIMUM_VALUE_TEXT_PICKER,
                MAXIMUM_VALUE_NUMBER_PICKER_HOURS,
                MINIMUM_VALUE_NUMBER_PICKER
            )
        } else {
            checkPickersValues(seconds)
        }
    }

    /**
     * Method for filling the text picker array from the value of the picker number value.
     *
     * @param value The current value of number picker.
     */
    private fun fillPickerText(value: Int) {
        binding.textPicker.displayedValues = null
        val arrayString: Array<String> = arrayOf(
            resources.getQuantityString(
                R.plurals.retention_time_picker_hours,
                value
            ).lowercase(Locale.getDefault()),
            resources.getQuantityString(
                R.plurals.retention_time_picker_days,
                value
            ).lowercase(Locale.getDefault()),
            resources.getQuantityString(
                R.plurals.retention_time_picker_weeks,
                value
            ).lowercase(Locale.getDefault()),
            resources.getQuantityString(
                R.plurals.retention_time_picker_months,
                value
            ).lowercase(Locale.getDefault()),
            getString(R.string.retention_time_picker_year)
                .lowercase(Locale.getDefault())
        )
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
        if (maximumValue < numberValue) {
            binding.textPicker.value = OPTION_HOURS
            binding.numberPicker.maxValue = MAXIMUM_VALUE_NUMBER_PICKER_HOURS
            binding.numberPicker.value = MINIMUM_VALUE_NUMBER_PICKER

        } else {
            binding.textPicker.value = textValue
            binding.numberPicker.maxValue = maximumValue
            binding.numberPicker.value = numberValue
        }

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

        val numberMonths = seconds / SECONDS_IN_MONTH_30
        val months = seconds - numberMonths * SECONDS_IN_MONTH_30

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
        if ((oldValue == 1 && newValue == 1) || (oldValue > 1 && newValue > 1))
            return

        if ((oldValue == 1 && newValue > 1) || (newValue == 1 && oldValue > 1)) {
            fillPickerText(newValue)
            binding.textPicker.minimumWidth = MAXIMUM_VALUE_TEXT_PICKER
        }
    }

    /**
     * Method that transforms the chosen option into the most correct form:
     * - If the option selected is 24 hours, it becomes 1 day.
     * - If the selected option is 31 days, it becomes 1 month.
     * - If the selected option is 4 weeks, it becomes 1 month.
     * - If the selected option is 12 months, it becomes 1 year.
     */
    private fun updateOptionsAccordingly() {
        if (binding.textPicker.value == OPTION_HOURS &&
            binding.numberPicker.value == MAXIMUM_VALUE_NUMBER_PICKER_HOURS
        ) {
            updatePickersValues(
                OPTION_DAYS,
                getMaximumValueOfNumberPicker(OPTION_DAYS),
                MINIMUM_VALUE_NUMBER_PICKER
            )
            return
        }

        if (binding.textPicker.value == OPTION_DAYS &&
            binding.numberPicker.value == DAYS_IN_A_MONTH_VALUE
        ) {
            updatePickersValues(
                OPTION_MONTHS,
                getMaximumValueOfNumberPicker(OPTION_MONTHS),
                MINIMUM_VALUE_NUMBER_PICKER
            )
            return
        }

        if (binding.textPicker.value == OPTION_WEEKS &&
            binding.numberPicker.value == MAXIMUM_VALUE_NUMBER_PICKER_WEEKS
        ) {
            updatePickersValues(
                OPTION_MONTHS,
                getMaximumValueOfNumberPicker(OPTION_MONTHS),
                MINIMUM_VALUE_NUMBER_PICKER
            )
            return
        }

        if (binding.textPicker.value == OPTION_MONTHS &&
            binding.numberPicker.value == MAXIMUM_VALUE_NUMBER_PICKER_MONTHS
        ) {
            updatePickersValues(
                OPTION_YEARS,
                getMaximumValueOfNumberPicker(OPTION_YEARS),
                MINIMUM_VALUE_NUMBER_PICKER
            )
            return
        }
    }

    /**
     *
     * Method for getting the maximum value of the picker number from a value.
     * @param value the value
     */
    private fun getMaximumValueOfNumberPicker(value: Int): Int {
        when (value) {
            OPTION_HOURS -> {
                return MAXIMUM_VALUE_NUMBER_PICKER_HOURS
            }

            OPTION_DAYS -> {
                return MAXIMUM_VALUE_NUMBER_PICKER_DAYS
            }

            OPTION_WEEKS -> {
                return MAXIMUM_VALUE_NUMBER_PICKER_WEEKS
            }

            OPTION_MONTHS -> {
                return MAXIMUM_VALUE_NUMBER_PICKER_MONTHS
            }

            OPTION_YEARS -> {
                return MINIMUM_VALUE_NUMBER_PICKER
            }

            else -> {
                return 0
            }
        }
    }

    /**
     * Method that updates the values of the number picker according to the current value of the text picker.
     *
     * @param value the current value of the text picker
     */
    private fun updateNumberPicker(value: Int) {
        val maximumValue = getMaximumValueOfNumberPicker(value)

        if (binding.numberPicker.value > maximumValue) {
            updateTextPicker(binding.numberPicker.value, MINIMUM_VALUE_NUMBER_PICKER)
            binding.numberPicker.value = MINIMUM_VALUE_NUMBER_PICKER
        }

        binding.numberPicker.maxValue = maximumValue
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

    override fun onClick(v: View) {
        when (v.id) {
            R.id.clear_chat_history_layout -> {
                shouldShowClearChatConfirmation = true
            }

            R.id.history_retention_switch_layout -> {
                if (binding.historyRetentionSwitch.isChecked) {
                    viewModel.setChatRetentionTime(period = DISABLED_RETENTION_TIME)
                } else {
                    shouldShowHistoryRetentionConfirmation = true
                }
            }

            R.id.retention_time_text_layout -> {
                shouldShowHistoryRetentionConfirmation = true
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
                        secondInOption = SECONDS_IN_MONTH_30
                    }

                    OPTION_YEARS -> {
                        secondInOption = SECONDS_IN_YEAR
                    }
                }

                val totalSeconds = binding.numberPicker.value * secondInOption
                viewModel.setChatRetentionTime(period = totalSeconds.toLong())
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)
            onBackPressedDispatcher.onBackPressed()

        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        unregisterReceiver(retentionTimeReceiver)
        super.onDestroy()
    }
}
