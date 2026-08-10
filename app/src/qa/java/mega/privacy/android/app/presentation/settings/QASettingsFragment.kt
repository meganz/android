package mega.privacy.android.app.presentation.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.EditText
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.presentation.account.QAAccountViewModel
import mega.privacy.android.app.presentation.account.model.QAAccountSwitchEvent
import mega.privacy.android.app.presentation.account.model.SimulateLastActiveDateResult
import mega.privacy.android.app.presentation.featureflag.FeatureFlagActivity
import mega.privacy.android.app.presentation.login.QALoginFragment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.sharedcomponents.canBeHandled
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * QA settings fragment
 */
@AndroidEntryPoint
class QASettingsFragment : PreferenceFragmentCompat() {

    /**
     * Mega navigator
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    private val settingViewModel by viewModels<QASettingViewModel>()
    private val accountViewModel by viewModels<QAAccountViewModel>()

    private val checkForUpdatesPreferenceKey = "settings_qa_check_update"
    private val exportLogsPreferenceKey = "settings_qa_export_logs"
    private val saveLogsPreferenceKey = "settings_qa_save_logs"
    private val featureFlagsPreferenceKey = "settings_qa_feature_flags"
    private val saveAccountPreferenceKey = "settings_qa_save_account"
    private val openLoginPreferenceKey = "settings_qa_open_login"
    private val accountCacheCategoryKey = "settings_qa_account_cache_category"
    private val simulateUserLastActiveDatePreferenceKey = "settings_qa_simulate_user_last_active_date"

    private var accountCacheCategory: PreferenceCategory? = null
    private var saveAccountPreference: Preference? = null
    private var openLoginPreference: Preference? = null
    private var materialDatePicker: MaterialDatePicker<Long>? = null
    private var isPreparingDatePicker = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_qa, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAccountManagement()
        observeAccountList()
        observeAccountSwitchSuccess()
        observeSimulateLastActiveDateResult()
        observePrepareSimulateDate()
    }

    override fun onDestroyView() {
        accountCacheCategory?.removeAll()
        materialDatePicker?.dismiss()
        materialDatePicker = null
        isPreparingDatePicker = false
        super.onDestroyView()
    }


    private fun setupAccountManagement() {
        // Add Tools category
        val simulatorCategory = PreferenceCategory(requireContext()).apply {
            title = getString(R.string.settings_qa_tools_category)
        }
        preferenceScreen.addPreference(simulatorCategory)

        // Add simulate user last active date item
        val simulateUserLastActiveDatePref = Preference(requireContext()).apply {
            key = simulateUserLastActiveDatePreferenceKey
            title = getString(R.string.settings_qa_simulate_user_last_active_date)
            summary = getString(R.string.settings_qa_simulate_user_last_active_date_summary)
        }
        simulatorCategory.addPreference(simulateUserLastActiveDatePref)

        // Add switch account category
        val switchAccountCategory = PreferenceCategory(requireContext()).apply {
            title = getString(R.string.settings_qa_switch_account_category)
        }
        preferenceScreen.addPreference(switchAccountCategory)

        // Add open login button
        val openLoginPref = Preference(requireContext()).apply {
            key = openLoginPreferenceKey
            title = getString(R.string.settings_qa_open_login)
            summary = getString(R.string.settings_qa_open_login_summary)
        }
        openLoginPreference = openLoginPref
        switchAccountCategory.addPreference(openLoginPref)

        // Add save account button
        val saveAccountPref = Preference(requireContext()).apply {
            key = saveAccountPreferenceKey
            title = getString(R.string.settings_qa_save_current_account)
            summary = getString(R.string.settings_qa_save_current_account_summary)
        }
        saveAccountPreference = saveAccountPref
        switchAccountCategory.addPreference(saveAccountPref)

        // Add account cache category
        val accountCacheCat = PreferenceCategory(requireContext()).apply {
            key = accountCacheCategoryKey
            title = getString(R.string.settings_qa_cached_accounts)
        }
        accountCacheCategory = accountCacheCat
        preferenceScreen.addPreference(accountCacheCat)
    }

    private fun observeAccountList() {
        collectFlow(accountViewModel.uiState.map { it.cachedAccounts }
            .distinctUntilChanged()) { accounts ->
            updateAccountList(accounts)
        }
    }

    private fun updateAccountList(accounts: List<UserCredentials>) {
        accountCacheCategory?.removeAll()

        if (accounts.isEmpty()) {
            val emptyPreference = Preference(requireContext()).apply {
                title = getString(R.string.settings_qa_no_cached_accounts)
                isSelectable = false
            }
            accountCacheCategory?.addPreference(emptyPreference)
        } else {
            // Load last login times, remarks and current account asynchronously
            viewLifecycleOwner.lifecycleScope.launch {
                val currentEmail = accountViewModel.getCurrentAccountEmail()
                val accountPreferences = accounts.map { credentials ->
                    async {
                        val lastLoginTime = accountViewModel.getLastLoginTime(credentials.email)
                        val remark = accountViewModel.getRemark(credentials.email)
                        val isCurrentAccount = credentials.email == currentEmail
                        val title = formatAccountTitle(credentials.email)
                        val summary = formatAccountSummary(lastLoginTime, remark, isCurrentAccount)
                        Triple(credentials, title, summary)
                    }
                }.awaitAll()

                accountPreferences.forEach { (credentials, title, summary) ->
                    val accountPreference = Preference(requireContext()).apply {
                        this.title = title
                        this.summary = summary
                        key = "account_${credentials.email}"
                    }
                    accountPreference.setOnPreferenceClickListener {
                        showAccountOptionsDialog(credentials)
                        true
                    }
                    accountCacheCategory?.addPreference(accountPreference)
                }
            }
        }
    }

    private fun formatAccountTitle(email: String?): String {
        return email ?: "Unknown"
    }

    private fun formatAccountSummary(
        timestamp: Long?,
        remark: String?,
        isCurrentAccount: Boolean,
    ): String {
        val loginTimeText = if (timestamp != null) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = Date(timestamp)
            "Last login: ${dateFormat.format(date)}"
        } else {
            "Last login: Never"
        }

        val secondLine = if (isCurrentAccount) {
            "$loginTimeText (Current)"
        } else {
            loginTimeText
        }

        return if (remark.isNullOrBlank()) {
            secondLine
        } else {
            "$secondLine\n$remark"
        }
    }

    private suspend fun updateAccountPreference(credentials: UserCredentials) {
        val preferenceKey = "account_${credentials.email}"
        val accountPreference = accountCacheCategory?.findPreference<Preference>(preferenceKey)

        if (accountPreference != null) {
            val currentEmail = accountViewModel.getCurrentAccountEmail()
            val lastLoginTime = accountViewModel.getLastLoginTime(credentials.email)
            val remark = accountViewModel.getRemark(credentials.email)
            val isCurrentAccount = credentials.email == currentEmail

            accountPreference.title = formatAccountTitle(credentials.email)
            accountPreference.summary =
                formatAccountSummary(lastLoginTime, remark, isCurrentAccount)
        }
    }

    private fun showAccountOptionsDialog(credentials: UserCredentials) {
        val options = arrayOf(
            getString(R.string.settings_qa_switch_to_account),
            getString(R.string.settings_qa_add_remark),
            getString(R.string.settings_qa_remove_account)
        )

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(credentials.email)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Switch to account
                        accountViewModel.switchToAccount(credentials)
                        showSwitchingDialog()
                    }

                    1 -> {
                        // Add remark
                        showAddRemarkDialog(credentials)
                    }

                    2 -> {
                        // Remove account
                        accountViewModel.removeAccount(credentials)
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showAddRemarkDialog(credentials: UserCredentials) {
        viewLifecycleOwner.lifecycleScope.launch {
            val currentRemark = accountViewModel.getRemark(credentials.email)
            val paddingInPx = (20 * resources.displayMetrics.density).toInt()
            val inputLayout = TextInputLayout(requireContext()).apply {
                setPadding(paddingInPx, paddingTop, paddingInPx, paddingBottom)
                isHintEnabled = false
            }
            val editText = EditText(requireContext()).apply {
                setText(currentRemark)
                hint = getString(R.string.settings_qa_remark_hint)
                setSingleLine()
            }
            inputLayout.addView(editText)

            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.settings_qa_add_remark))
                .setView(inputLayout)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val remark = editText.text.toString().trim()
                    viewLifecycleOwner.lifecycleScope.launch {
                        accountViewModel.saveRemark(
                            credentials.email,
                            if (remark.isEmpty()) null else remark
                        )
                        // Manually update the preference to reflect the new remark
                        updateAccountPreference(credentials)
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()

            dialog.show()

            // Show keyboard and focus on EditText
            editText.requestFocus()
            editText.post {
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun showSwitchingDialog() {
        var dialog: android.app.AlertDialog? = null
        collectFlow(accountViewModel.uiState.map { it.isSwitchingAccount }
            .distinctUntilChanged()) { isSwitching ->
            if (isSwitching && dialog == null) {
                dialog = android.app.AlertDialog.Builder(requireContext())
                    .setMessage(getString(R.string.settings_qa_switching_account))
                    .setCancelable(false)
                    .show()
            } else if (!isSwitching && dialog != null) {
                dialog?.dismiss()
                dialog = null
            }
        }
    }

    private fun observeAccountSwitchSuccess() {
        collectFlow(accountViewModel.uiState.map { it.accountSwitchEvent }
            .distinctUntilChanged()) {
            (it as? StateEventWithContentTriggered)?.content?.let { content ->
                when (content) {
                    is QAAccountSwitchEvent.Success -> {
                        Timber.d("Account switch successful, navigating to MegaActivity")
                        navigateToMainActivity()
                    }

                    is QAAccountSwitchEvent.Failure -> {
                        Timber.e(content.error, "Account switch failed")
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.general_error_word)
                            .setMessage(content.error.message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                    }
                }
                accountViewModel.consumeAccountSwitchEvent()
            }
        }
    }

    private fun observeSimulateLastActiveDateResult() {
        collectFlow(accountViewModel.uiState.map { it.simulateLastActiveDateResultEvent }
            .distinctUntilChanged()) { event ->
            if (event is StateEventWithContentTriggered) {
                val (message, length) = when (event.content) {
                    SimulateLastActiveDateResult.Success ->
                        R.string.settings_qa_operation_success to Snackbar.LENGTH_SHORT

                    SimulateLastActiveDateResult.Invalid ->
                        R.string.settings_qa_simulate_date_must_differ to Snackbar.LENGTH_LONG

                    SimulateLastActiveDateResult.Failure ->
                        R.string.settings_qa_operation_failed to Snackbar.LENGTH_LONG
                }
                Snackbar.make(listView, getString(message), length).show()
                accountViewModel.consumeSimulateLastActiveDateResultEvent()
            }
        }
    }

    private fun navigateToMainActivity() {
        runCatching {
            context?.let {
                megaNavigator.openHomeScreen(it)
                activity?.finish()
            }
        }.onFailure {
            Timber.e(it, "Failed to navigate to MegaActivity")
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return when (preference.key) {
            checkForUpdatesPreferenceKey -> {
                settingViewModel.checkUpdatePressed()
                true
            }

            exportLogsPreferenceKey -> {
                settingViewModel.exportLogs(::sendShareLogFileIntent)
                true
            }

            saveLogsPreferenceKey -> {
                settingViewModel.exportLogs(::sendViewLogFileIntent)
                true
            }

            featureFlagsPreferenceKey -> {
                startActivity(Intent(requireContext(), FeatureFlagActivity::class.java))
                true
            }

            saveAccountPreferenceKey -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    val savedCredentials = accountViewModel.saveCurrentAccount()
                    savedCredentials?.let {
                        updateAccountPreference(it)
                    }
                }
                true
            }

            openLoginPreferenceKey -> {
                openLoginScreen()
                true
            }

            simulateUserLastActiveDatePreferenceKey -> {
                showSimulateUserLastActiveDateDialog()
                true
            }

            else -> super.onPreferenceTreeClick(preference)
        }
    }

    private fun sendShareLogFileIntent(logFile: File) = Intent(Intent.ACTION_SEND).apply {
        val uri = getLogFileUri(logFile)
        putExtra(Intent.EXTRA_TITLE, "Send log file")
        putExtra(Intent.EXTRA_SUBJECT, "Mega Log")
        uri?.let<Uri, Unit> {
            type = getMimeType(logFile)
            putExtra(Intent.EXTRA_STREAM, it)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }.let {
        if (it.canBeHandled(requireContext())) {
            startActivity(it)
        }
    }

    private fun sendViewLogFileIntent(logFile: File) = Intent(Intent.ACTION_VIEW).apply {
        val uri = getLogFileUri(logFile)
        uri?.let {
            type = getMimeType(logFile)
            putExtra(Intent.EXTRA_STREAM, it)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }.let {
        if (it.canBeHandled(requireContext())) {
            startActivity(it)
        }
    }

    private fun getMimeType(logFile: File) = MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(logFile.extension)

    private fun getLogFileUri(
        file: File,
    ) = context?.let {
        FileProvider.getUriForFile(
            it,
            Constants.AUTHORITY_STRING_FILE_PROVIDER,
            file
        )
    }

    /**
     * Open login screen without logging out
     */
    private fun openLoginScreen() {
        val fragment = QALoginFragment()
        fragment.show(parentFragmentManager, "qa_login")
    }

    /**
     * Ask the ViewModel to prepare the simulate-last-active-date picker. The previously simulated
     * date is read off the main thread and delivered back through
     * [QAAccountUiState.prepareSimulateDateEvent], which [observePrepareSimulateDate] turns into the
     * actual picker. Keeping the read in the ViewModel preserves the unidirectional data flow.
     */
    private fun showSimulateUserLastActiveDateDialog() {
        // Guard set synchronously so rapid taps cannot each request a picker and open multiple ones.
        if (isPreparingDatePicker || materialDatePicker != null) return
        isPreparingDatePicker = true
        accountViewModel.prepareSimulateUserLastActiveDate()
    }

    private fun observePrepareSimulateDate() {
        collectFlow(accountViewModel.uiState.map { it.prepareSimulateDateEvent }
            .distinctUntilChanged()) { event ->
            if (event is StateEventWithContentTriggered) {
                showSimulateDatePicker(event.content)
                accountViewModel.consumePrepareSimulateDateEvent()
            }
        }
    }

    /**
     * Show a date picker to simulate the user's last active date. On confirm, the selected date is
     * sent to the SDK. If a date was previously simulated (derived from the last acknowledged
     * purge), the picker defaults to one day before it; otherwise it defaults to the current day.
     * Picking the exact same date as last time is rejected, because it produces an
     * already-acknowledged purge timestamp that the SDK would suppress.
     *
     * @param previousLastActive the previously simulated last active time (epoch seconds), or null.
     */
    private fun showSimulateDatePicker(previousLastActive: Long?) {
        if (materialDatePicker != null) {
            isPreparingDatePicker = false
            return
        }
        val defaultSelectionMillis = if (previousLastActive != null) {
            TimeUnit.SECONDS.toMillis(previousLastActive) - TimeUnit.DAYS.toMillis(1)
        } else {
            MaterialDatePicker.todayInUtcMilliseconds() - TimeUnit.DAYS.toMillis(1)
        }

        materialDatePicker = MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.MaterialCalendarTheme)
            .setPositiveButtonText(getString(sharedR.string.general_ok))
            .setNegativeButtonText(getString(sharedR.string.general_dialog_cancel_button))
            .setTitleText(getString(R.string.settings_qa_simulate_user_last_active_date))
            .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
            .setSelection(defaultSelectionMillis)
            .build()
            .apply {
                addOnDismissListener {
                    materialDatePicker = null
                    isPreparingDatePicker = false
                }
                addOnPositiveButtonClickListener { selection ->
                    val epochSeconds = TimeUnit.MILLISECONDS.toSeconds(selection)
                    accountViewModel.onSimulateUserLastActiveDateSelected(epochSeconds)
                }
                show(
                    this@QASettingsFragment.parentFragmentManager,
                    "qa_simulate_user_last_active_date"
                )
            }
    }
}
