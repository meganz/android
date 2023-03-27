package mega.privacy.android.app.presentation.testpassword

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.ActivityTestPasswordBinding
import mega.privacy.android.app.main.FileStorageActivity
import mega.privacy.android.app.main.controllers.AccountController
import mega.privacy.android.app.main.controllers.AccountController.Companion.logout
import mega.privacy.android.app.main.controllers.AccountController.Companion.saveRkToFileSystem
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.RecoveryKeyBottomSheetDialogFragment
import mega.privacy.android.app.presentation.changepassword.ChangePasswordActivity
import mega.privacy.android.app.presentation.testpassword.model.PasswordState
import mega.privacy.android.app.presentation.testpassword.model.TestPasswordUIState
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.qualifier.ApplicationScope
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Test Password Activity
 */
@AndroidEntryPoint
class TestPasswordActivity : PasscodeActivity(), MegaRequestListenerInterface {
    /**
     * Application Scope
     */
    @ApplicationScope
    @Inject
    lateinit var sharingScope: CoroutineScope

    /**
     * Account Controller
     */
    @Inject
    lateinit var accountController: AccountController

    private val viewModel: TestPasswordViewModel by viewModels()

    /**
     * Checks whether screen is in Logout Mode
     */
    var isLogout = false
        private set

    private lateinit var binding: ActivityTestPasswordBinding
    private var passwordCorrect = false
    private var counter = 0
    private var testingPassword = false
    private var dismissPasswordReminder = false
    private var numRequests = 0
    private val backupRecoveryKeyAction = object : BackupRecoveryKeyAction {
        override fun print() {
            accountController.printRK()
        }

        override fun copyToClipboard() {
            accountController.copyRkToClipboard(sharingScope)
        }

        override fun saveToFile() {
            saveRkToFileSystem(this@TestPasswordActivity)
        }
    }
    private val recoveryKeyBottomSheetDialogFragment by lazy(LazyThreadSafetyMode.NONE) {
        RecoveryKeyBottomSheetDialogFragment(backupRecoveryKeyAction)
    }
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (psaWebBrowser != null && psaWebBrowser?.consumeBack() == true) return
            dismissActivity(false)
            finish()
        }
    }
    private val activity = this@TestPasswordActivity

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        if (intent == null) {
            Timber.w("Intent NULL")
            return
        }
        if (savedInstanceState != null) {
            counter = savedInstanceState.getInt("counter", 0)
            testingPassword = savedInstanceState.getBoolean("testingPassword", false)
        }
        isLogout = intent.getBooleanExtra("logout", false)

        collectUIState()
        bindView()
    }

    private fun collectUIState() {
        collectFlow(viewModel.uiState) {
            handleCheckPasswordState(it)
            handleNotifiedPasswordReminderState(it)
        }
    }

    private fun handleCheckPasswordState(uiState: TestPasswordUIState) {
        if (uiState.isCurrentPassword != PasswordState.Initial) {
            showError(uiState.isCurrentPassword)
            viewModel.resetCurrentPasswordState()
        }
    }

    private fun handleNotifiedPasswordReminderState(uiState: TestPasswordUIState) {
        if (uiState.isPasswordReminderNotified != PasswordState.Initial) {
            numRequests--

            if (uiState.isPasswordReminderNotified == PasswordState.True) {
                // numRequests is temporarily changed to 1 because copy, print, export Recovery Key hasn't been implemented yet in this activity. This is a quick fix for 7.8
                if (dismissPasswordReminder && isLogout && numRequests <= 1) {
                    logout(this, megaApi, sharingScope)
                }
            }

            viewModel.resetPasswordReminderState()
        }
    }

    private fun bindView() {
        with(binding) {
            passwordReminderCloseImageButton.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            passwordReminderCheckbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    Timber.d("Block CheckBox checked!")
                } else {
                    Timber.d("Block CheckBox does NOT checked!")
                }
            }
            passwordReminderTestButton.setOnClickListener {
                shouldBlockPasswordReminder()
                testingPassword = true
                setTestPasswordLayout()
            }
            passwordReminderRecoverykeyButton.setOnClickListener {
                onBackupRecoveryClick()
            }
            passwordReminderDismissButton.setOnClickListener {
                if (activity.isLogout) {
                    dismissActivity(true)
                } else {
                    onBackPressedDispatcher.onBackPressed()
                }
                dismissActivity(true)
            }
            testPasswordBackupButton.setOnClickListener {
                onBackupRecoveryClick()
            }
            testPasswordConfirmButton.setOnClickListener {
                viewModel.checkForCurrentPassword(testPasswordEdittext.text.toString())
            }
            testPasswordDismissButton.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            proceedToLogoutButton.setOnClickListener {
                dismissActivity(true)
            }

            progressBar.visibility = View.GONE
            passwordReminderCloseImageButton.isVisible = isLogout
            testPasswordDismissButton.isVisible = isLogout.not()
            proceedToLogoutButton.isVisible = isLogout

            if (isLogout) {
                passwordReminderText.setText(R.string.remember_pwd_dialog_text_logout)
                passwordReminderDismissButton.setText(R.string.proceed_to_logout)
                passwordReminderDismissButton.setTextColor(
                    ContextCompat.getColor(activity, R.color.red_600_red_300)
                )
            } else {
                passwordReminderText.setText(R.string.remember_pwd_dialog_text)
                passwordReminderDismissButton.setText(R.string.general_dismiss)
                passwordReminderDismissButton.setTextColor(
                    getThemeColor(activity, R.attr.colorSecondary)
                )
            }

            testPasswordEdittext.background.clearColorFilter()
            testPasswordEdittext.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    viewModel.checkForCurrentPassword(testPasswordEdittext.text.toString())
                    return@setOnEditorActionListener true
                }
                false
            }
            testPasswordEdittext.doAfterTextChanged {
                if (!passwordCorrect) {
                    quitError()
                }
            }
            testPasswordTextLayout.isEndIconVisible = false
            testPasswordEdittext.onFocusChangeListener =
                View.OnFocusChangeListener { _, hasFocus ->
                    testPasswordTextLayout.isEndIconVisible = hasFocus
                }
            if (testingPassword) {
                setTestPasswordLayout()
            } else {
                passwordReminderLayout.visibility = View.VISIBLE
                testPasswordLayout.visibility = View.GONE
            }
        }
    }

    private fun onBackupRecoveryClick() {
        if (recoveryKeyBottomSheetDialogFragment.isBottomSheetDialogShown()) return
        recoveryKeyBottomSheetDialogFragment.show(
            supportFragmentManager,
            recoveryKeyBottomSheetDialogFragment.tag
        )
    }

    /**
     * onSaveInstanceState
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("counter", counter)
        outState.putBoolean("testingPassword", testingPassword)
    }

    private fun setTestPasswordLayout() {
        binding.toolbar.visibility = View.VISIBLE
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.remember_pwd_dialog_button_test)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
        binding.passwordReminderLayout.visibility = View.GONE
        binding.testPasswordLayout.visibility = View.VISIBLE
    }

    /**
     * onOptionsItemSelected
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun quitError() {
        with(binding) {
            testPasswordTextLayout.error = null
            testPasswordTextLayout.setHintTextAppearance(R.style.TextAppearance_Design_Hint)
            testPasswordTextErrorIcon.visibility = View.GONE
            testPasswordBackupButton.setTextColor(getThemeColor(activity, R.attr.colorSecondary))
            testPasswordConfirmButton.isEnabled = true
            testPasswordConfirmButton.alpha = 1f
        }
    }

    private fun showError(state: PasswordState) {
        Util.hideKeyboard(this, 0)
        val icon: Drawable?
        with(binding) {
            when (state) {
                PasswordState.Initial -> return@with
                PasswordState.True -> {
                    testPasswordTextLayout.error = getString(R.string.test_pwd_accepted)
                    testPasswordTextLayout.setHintTextAppearance(R.style.TextAppearance_InputHint_Medium)
                    testPasswordTextLayout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Medium)
                    icon = ContextCompat.getDrawable(activity, R.drawable.ic_accept_test)
                    icon?.colorFilter = PorterDuffColorFilter(
                        ContextCompat.getColor(activity, R.color.green_500_green_400),
                        PorterDuff.Mode.SRC_ATOP
                    )
                    testPasswordTextErrorIcon.setImageDrawable(icon)
                    testPasswordBackupButton.setTextColor(
                        getThemeColor(activity, R.attr.colorSecondary)
                    )
                    testPasswordEdittext.isEnabled = false
                    passwordReminderSucceeded()
                }
                PasswordState.False -> {
                    counter++
                    testPasswordTextLayout.error = getString(R.string.test_pwd_wrong)
                    testPasswordTextLayout.setHintTextAppearance(R.style.TextAppearance_InputHint_Error)
                    testPasswordTextLayout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Error)
                    icon = ContextCompat.getDrawable(activity, R.drawable.ic_input_warning)
                    icon?.colorFilter = PorterDuffColorFilter(
                        ContextCompat.getColor(activity, R.color.red_600_red_300),
                        PorterDuff.Mode.SRC_ATOP
                    )
                    testPasswordTextErrorIcon.setImageDrawable(icon)
                    testPasswordBackupButton.setTextColor(
                        ContextCompat.getColor(
                            activity,
                            R.color.red_600_red_300
                        )
                    )
                    if (counter == 3) {
                        val intent = Intent(activity, ChangePasswordActivity::class.java)
                        intent.putExtra(ChangePasswordActivity.KEY_IS_LOGOUT, isLogout)
                        startActivity(intent)
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
            testPasswordTextErrorIcon.visibility = View.VISIBLE
            testPasswordConfirmButton.isEnabled = false
            testPasswordConfirmButton.alpha = 0.3f
        }
    }

    /**
     * onActivityResult
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == Constants.REQUEST_DOWNLOAD_FOLDER && resultCode == RESULT_OK) {
            Timber.d("REQUEST_DOWNLOAD_FOLDER")
            var parentPath = intent?.getStringExtra(FileStorageActivity.EXTRA_PATH)
            if (parentPath != null) {
                Timber.d("parentPath no NULL")
                parentPath = parentPath + File.separator + FileUtil.getRecoveryKeyFileName()
                val ac = AccountController(this)
                ac.exportMK(parentPath)
            }
        }
    }

    private fun disableUI() {
        with(binding) {
            if (passwordReminderLayout.visibility == View.VISIBLE) {
                passwordReminderLayout.isEnabled = false
                passwordReminderLayout.alpha = 0.3f
            } else if (testPasswordLayout.visibility == View.VISIBLE) {
                testPasswordLayout.isEnabled = false
                testPasswordLayout.alpha = 0.3f
            }
            progressBar.visibility = View.VISIBLE
        }
    }

    /**
     * Change UI to Password Reminder Succeeded Mode
     * Can be called from [AccountController]
     */
    fun passwordReminderSucceeded() {
        shouldBlockPasswordReminder()
        enableDismissPasswordReminder()
        incrementRequests()
        viewModel.notifyPasswordReminderSucceed()
        if (isLogout) {
            disableUI()
        } else {
            finish()
        }
    }

    private fun dismissActivity(enableDismissPasswordReminder: Boolean) {
        if (enableDismissPasswordReminder) {
            enableDismissPasswordReminder()
            if (isLogout) {
                disableUI()
            }
        }
        incrementRequests()
        viewModel.notifyPasswordReminderSkipped()
        shouldBlockPasswordReminder()
    }

    private fun shouldBlockPasswordReminder() {
        if (binding.passwordReminderCheckbox.isChecked) {
            incrementRequests()
            viewModel.notifyPasswordReminderBlocked()
        }
    }

    /**
     * Show Snackbar from [AccountController]
     */
    fun showSnackbar(s: String) {
        Timber.d("showSnackbar")
        showSnackbar(binding.root, s)
    }

    /**
     * onRequestPermissionsResult
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.REQUEST_WRITE_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("REQUEST_WRITE_STORAGE PERMISSIONS GRANTED")
                }
            }
        }
    }

    private fun enableDismissPasswordReminder() {
        dismissPasswordReminder = true
    }

    /**
     * Increment requests from [AccountController]
     */
    fun incrementRequests() {
        numRequests++
    }

    /**
     * onRequestStart
     */
    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {}

    /**
     * onRequestUpdate
     */
    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {}

    /**
     * onRequestFinish
     */
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type == MegaRequest.TYPE_LOGOUT) {
            Timber.d("END logout sdk request - wait chat logout")
        }
    }

    /**
     * onRequestTemporaryError
     */
    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {}
}