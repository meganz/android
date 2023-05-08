package mega.privacy.android.app.presentation.twofactorauthentication

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.EditTextPIN
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.databinding.ActivityTwoFactorAuthenticationBinding
import mega.privacy.android.app.databinding.Dialog2faHelpBinding
import mega.privacy.android.app.databinding.DialogNoAuthenticationAppsBinding
import mega.privacy.android.app.main.FileStorageActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.settings.exportrecoverykey.ExportRecoveryKeyActivity
import mega.privacy.android.app.presentation.twofactorauthentication.extensions.toSeedArray
import mega.privacy.android.app.presentation.twofactorauthentication.model.AuthenticationState
import mega.privacy.android.app.presentation.twofactorauthentication.model.TwoFactorAuthenticationUIState
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils
import org.jetbrains.anko.toast
import timber.log.Timber

/**
 * TwoFactorAuthenticationActivity
 */
class TwoFactorAuthenticationActivity : PasscodeActivity() {

    private lateinit var binding: ActivityTwoFactorAuthenticationBinding
    private val viewModel: TwoFactorAuthenticationViewModel by viewModels()

    private var pin = ""
    private var seed = ""
    private var url: String? = null
    private val sb = StringBuilder()
    private var pinsViewsList = emptyList<EditTextPIN>()
    private var scanOrCopyIsShown = false
    private var confirm2FAIsShown = false
    private var isEnabled2FA = false
    private var isErrorShown = false
    private var firstTime = true
    private var newAccount = false
    private var pinLongClick = false
    private var rkSaved = false
    private var isHelpDialogShown = false
    private var isNoAppsDialogShown = false
    private var imm: InputMethodManager? = null

    private val downloadFolderActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val dataIntent = result.data
            if (result.resultCode == RESULT_OK && dataIntent != null) {
                exportRecoveryKey(dataIntent)
            }
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty() || requestCode != ExportRecoveryKeyActivity.WRITE_STORAGE_TO_SAVE_RK) {
            Timber.w("Permissions ${permissions[0]} not granted")
        }

        onPermissionAsked()
    }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (psaWebBrowser?.consumeBack() == true) return
                retryConnectionsAndSignalPresence()

                if (confirm2FAIsShown) {
                    confirm2FAIsShown = false
                    showScanOrCopyLayout()
                } else {
                    if (isEnabled2FA) {
                        if (rkSaved) {
                            finish()
                        } else {
                            showSnackbar(getString(R.string.backup_rk_2fa_end))
                        }
                        update2FASetting()
                    } else {
                        finish()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        binding = ActivityTwoFactorAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(onBackPressedCallback)
        imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.settings_2fa)
        }

        if (savedInstanceState == null) {
            rkSaved = false
            confirm2FAIsShown = false
            scanOrCopyIsShown = false
            isEnabled2FA = false
            intent?.let {
                newAccount = it.getBooleanExtra(IntentConstants.EXTRA_NEW_ACCOUNT, false)
            }
            isNoAppsDialogShown = false
            isHelpDialogShown = false
        } else {
            Timber.d("savedInstanceState No null")
            with(savedInstanceState) {
                confirm2FAIsShown = getBoolean("confirm2FAIsShown", false)
                scanOrCopyIsShown = getBoolean("scanOrCopyIsShown", false)
                isEnabled2FA = getBoolean("isEnabled2FA", false)
                isErrorShown = getBoolean("isErrorShown", false)
                firstTime = getBoolean("firstTimeAfterInstallation", true)
                rkSaved = getBoolean("rkSaved", false)
                isNoAppsDialogShown = getBoolean("isNoAppsDialogShown", false)
                isHelpDialogShown = getBoolean("isHelpDialogShown", false)
            }
        }
        val explainQrText =
            SpannableString("${getString(R.string.explain_qr_seed_2fa_2)} QM")
        ContextCompat.getDrawable(this, R.drawable.ic_question_mark)?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                colorFilter =
                    BlendModeColorFilter(
                        ColorUtils.getThemeColor(
                            this@TwoFactorAuthenticationActivity,
                            android.R.attr.textColorPrimary
                        ), BlendMode.SRC_IN
                    )
            } else {
                setColorFilter(
                    ColorUtils.getThemeColor(
                        this@TwoFactorAuthenticationActivity,
                        android.R.attr.textColorPrimary
                    ), PorterDuff.Mode.SRC_IN
                )
            }
            setBounds(
                0,
                0,
                intrinsicWidth,
                intrinsicHeight
            )
            val imageSpan = ImageSpan(this, ImageSpan.ALIGN_BOTTOM)
            explainQrText.setSpan(
                imageSpan,
                explainQrText.length - 2,
                explainQrText.length,
                Spannable.SPAN_EXCLUSIVE_INCLUSIVE
            )
        }
        with(binding) {
            explainQrSeed2fa2.text = explainQrText
            pin2faError.visibility = View.GONE
            pinsViewsList =
                listOf(passFirst, passSecond, passThird, passFourth, passFifth, passSixth)
            pinsViewsList.forEach {
                showKeyboard(it)
            }
            addListeners()

            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
            passSecond.previousDigitEditText = passFirst
            passThird.previousDigitEditText = passSecond
            passFourth.previousDigitEditText = passThird
            passFifth.previousDigitEditText = passFourth
            passSixth.previousDigitEditText = passFifth
            fileNameRK.text = FileUtil.getRecoveryKeyFileName(binding.root.context)

            when {
                scanOrCopyIsShown || newAccount -> {
                    showScanOrCopyLayout()
                }
                confirm2FAIsShown -> {
                    listOf(
                        containerQrSeed2fa,
                        scrollContainer2fa,
                        container2faEnabled
                    ).forEach { it.visibility = View.GONE }
                    listOf(
                        containerConfirm2fa,
                        scrollContainerVerify
                    ).forEach { it.visibility = View.VISIBLE }
                    if (isErrorShown) {
                        showError()
                    }
                }
                isEnabled2FA -> {
                    listOf(
                        scrollContainer2fa,
                        scrollContainerVerify
                    ).forEach { it.visibility = View.GONE }
                    container2faEnabled.visibility = View.VISIBLE
                    buttonDismissRk.isVisible = rkSaved
                }
                else -> {
                    viewModel.getAuthenticationCode()
                    scrollContainer2fa.visibility = View.VISIBLE
                    listOf(
                        scrollContainerVerify,
                        container2faEnabled
                    ).forEach { it.visibility = View.GONE }
                }
            }
        }
        observeUIState()
    }

    private fun showError() {
        firstTime = false
        isErrorShown = true
        with(binding) {
            pin2faError.visibility = View.VISIBLE
            pinsViewsList.forEach {
                it.setTextColor(
                    ContextCompat.getColor(
                        this@TwoFactorAuthenticationActivity,
                        R.color.red_600_red_300
                    )
                )
            }
        }
    }

    /**
     * Action when permission has been asked to the user
     * Will save to storage if permission granted
     * else it will display a Snackbar telling that the user denied the request
     */
    private fun onPermissionAsked() {
        if (PermissionUtils.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            saveRecoveryKeyToStorage()
        } else {
            showSnackbar(getString(R.string.denied_write_permissions))
        }
    }

    /**
     * Open folder selection, where user can select the location the recovery key will be stored.
     */
    private fun saveRecoveryKeyToStorage() {
        val intent = Intent(
            this@TwoFactorAuthenticationActivity,
            FileStorageActivity::class.java
        ).apply {
            action = FileStorageActivity.Mode.PICK_FOLDER.action
            putExtra(FileStorageActivity.EXTRA_SAVE_RECOVERY_KEY, true)
        }
        downloadFolderActivityResult.launch(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Timber.d("onSaveInstanceState")

        with(outState) {
            putBoolean("confirm2FAIsShown", confirm2FAIsShown)
            putBoolean("scanOrCopyIsShown", scanOrCopyIsShown)
            putBoolean("isEnabled2FA", isEnabled2FA)
            putBoolean("isErrorShown", isErrorShown)
            putBoolean("firstTimeAfterInstallation", firstTime)
            putBoolean("rkSaved", rkSaved)
            putBoolean("isNoAppsDialogShown", isNoAppsDialogShown)
            putBoolean("isHelpDialogShown", isHelpDialogShown)
        }
    }

    /**
     * Action when User finished choosing folder to save recovery key
     * Will show SnackBar message from Compose
     */
    private fun exportRecoveryKey(result: Intent) = lifecycleScope.launch {
        val key = viewModel.getRecoveryKey()
        when {
            key.isNullOrBlank() -> {
                showSnackbar(getString(R.string.general_text_error))
            }
            isSaveToTextFileSuccessful(key, result) -> {
                val intent =
                    Intent(
                        this@TwoFactorAuthenticationActivity,
                        ManagerActivity::class.java
                    )
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
                toast(R.string.save_MK_confirmation)
            }
            else -> {
                showSnackbar(getString(R.string.general_text_error))
            }
        }
    }

    /**
     * Saving the recovery key to text file
     * @return is save successful as [Boolean]
     */
    private suspend fun isSaveToTextFileSuccessful(key: String, result: Intent): Boolean =
        withContext(Dispatchers.IO) {
            FileUtil.saveTextOnContentUri(
                this@TwoFactorAuthenticationActivity.contentResolver,
                result.data,
                key
            )
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun update2FASetting() {
        setResult(RESULT_OK)
    }

    private fun showScanOrCopyLayout() {
        hideKeyboard()
        scanOrCopyIsShown = true
        with(binding) {
            listOf(
                containerConfirm2fa,
                scrollContainer2fa,
                container2faEnabled
            ).forEach { it.visibility = View.GONE }
            listOf(
                containerQrSeed2fa,
                scrollContainerVerify
            ).forEach { it.visibility = View.VISIBLE }
            scrollContainerVerify.setBackgroundColor(
                ContextCompat.getColor(
                    this@TwoFactorAuthenticationActivity,
                    R.color.white_grey_700
                )
            )

            if (isNoAppsDialogShown) {
                showAlertNotAppAvailable()
            }

            if (isHelpDialogShown) {
                showAlertHelp()
            }
        }
    }

    private fun showAlertHelp() {
        Timber.d("showAlertHelp")
        val builder = MaterialAlertDialogBuilder(this)
        val twoFactorHelpBinding = Dialog2faHelpBinding.inflate(layoutInflater)
        builder.setView(twoFactorHelpBinding.root)
        val helpDialog = builder.create()
        with(twoFactorHelpBinding) {
            cancelButtonHelp.setOnClickListener {
                helpDialog.dismiss()
                isHelpDialogShown = false
            }
            playStoreButtonHelp.setOnClickListener {
                helpDialog.dismiss()
                isHelpDialogShown = false
                openPlayStore()
            }
        }
        with(helpDialog) {
            setCanceledOnTouchOutside(false)
            setOnDismissListener {
                isHelpDialogShown = false
            }
            setOnShowListener {
                isHelpDialogShown = true
            }
            show()
        }
    }

    private fun showAlertNotAppAvailable() {
        Timber.d("showAlertNotAppAvailable")
        val builder = MaterialAlertDialogBuilder(this)
        val noAppsDialogBinding = DialogNoAuthenticationAppsBinding.inflate(layoutInflater)
        builder.setView(noAppsDialogBinding.root)
        val noAppsDialog = builder.create()
        with(noAppsDialogBinding) {
            cancelButtonNoApp.setOnClickListener {
                noAppsDialog.dismiss()
            }
            openButtonNoApp.setOnClickListener {
                noAppsDialog.dismiss()
                isNoAppsDialogShown = false
            }
        }
        with(noAppsDialog) {
            setCanceledOnTouchOutside(false)
            setOnDismissListener {
                noAppsDialog.dismiss()
                isNoAppsDialogShown = false
                openPlayStore()
            }
            setOnShowListener {
                isNoAppsDialogShown = true
            }
            show()
        }
    }

    private fun openPlayStore() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=authenticator&c=apps"))
        startActivity(intent)
    }

    private fun generate2FAQR() {
        url?.let {
            val colorBackground = ContextCompat.getColor(
                this@TwoFactorAuthenticationActivity,
                R.color.white_grey_700
            )
            val colorCode = ContextCompat.getColor(
                this@TwoFactorAuthenticationActivity,
                R.color.dark_grey
            )
            viewModel.generateQRCodeBitmap(
                qrCodeUrl = it,
                width = 300,
                height = 300,
                bgColor = colorBackground,
                penColor = colorCode
            )
        }
    }

    private fun showSnackbar(s: String) {
        showSnackbar(binding.container2fa, s)
    }

    private fun setSeed(arraySeed: ArrayList<String>) {
        with(binding) {
            listOf(
                seed2fa1,
                seed2fa2,
                seed2fa3,
                seed2fa4,
                seed2fa5,
                seed2fa6,
                seed2fa7,
                seed2fa8,
                seed2fa9,
                seed2fa10,
                seed2fa11,
                seed2fa12,
                seed2fa13
            ).forEachIndexed { index, currentSeed ->
                currentSeed.text = arraySeed[index]
            }
        }
    }

    private fun addListeners() {
        with(binding) {
            buttonEnable2fa.setOnClickListener {
                scanOrCopyIsShown = true
                confirm2FAIsShown = false
                isEnabled2FA = false
                containerQrSeed2fa.visibility = View.VISIBLE
                scrollContainerVerify.visibility = View.VISIBLE
                containerConfirm2fa.visibility = View.GONE
                scrollContainer2fa.visibility = View.GONE
                container2faEnabled.visibility = View.GONE

            }
            buttonOpenWith2fa.setOnClickListener {
                url?.let {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    if (MegaApiUtils.isIntentAvailable(
                            this@TwoFactorAuthenticationActivity,
                            intent
                        )
                    ) {
                        startActivity(intent)
                    } else {
                        showAlertNotAppAvailable()
                    }
                }
            }
            buttonNext2fa.setOnClickListener {
                scanOrCopyIsShown = false
                newAccount = false
                confirm2FAIsShown = true
                isEnabled2FA = false
                containerQrSeed2fa.visibility = View.GONE
                containerConfirm2fa.visibility = View.VISIBLE
                scrollContainer2fa.visibility = View.GONE
                scrollContainerVerify.visibility = View.VISIBLE
                scrollContainerVerify.setBackgroundColor(Color.TRANSPARENT)
                container2faEnabled.visibility = View.GONE
                passFirst.requestFocus()
                showKeyboard(passFirst)
                clearAllPins()
            }
            listOf(containerRk2fa, buttonExportRk).forEach {
                it.setOnClickListener { chooseRecoverySaveLocation() }
            }
            buttonDismissRk.setOnClickListener {
                update2FASetting()
                finish()
            }
            explainQrSeed2fa2.setOnClickListener {
                showAlertHelp()
            }
            seed2fa.setOnLongClickListener {
                copySeed(seed)
                return@setOnLongClickListener true
            }
            pinsViewsList.forEach { currentPin ->
                currentPin.setOnLongClickListener {
                    pinLongClick = true
                    it.requestFocus()
                    return@setOnLongClickListener false
                }
            }
            pinsViewsList.forEach { currentPin ->
                currentPin.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        clearText(currentPin)
                    }
                }
            }
            pinsViewsList.forEachIndexed { index, currentPin ->
                addTextChangedListener(index, currentPin, index == pinsViewsList.lastIndex)
            }
        }
    }

    /**
     * Action when save button is clicked. Will save to storage if permission granted
     * else will ask for permission to write external storage
     */
    private fun chooseRecoverySaveLocation() {
        if (PermissionUtils.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            saveRecoveryKeyToStorage()
        } else {
            PermissionUtils.requestPermission(
                this,
                ExportRecoveryKeyActivity.WRITE_STORAGE_TO_SAVE_RK,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    private fun addTextChangedListener(
        index: Int,
        currentPin: EditTextPIN,
        isLastPin: Boolean = false,
    ) {
        if (isLastPin) {
            currentPin.doAfterTextChanged {
                if (currentPin.length() != 0) {
                    currentPin.isCursorVisible = true
                    hideKeyboard()
                    if (pinLongClick) {
                        pasteClipboard()
                    } else {
                        permitVerify()
                    }
                } else if (isErrorShown) {
                    quitError()
                }
            }
        } else {
            currentPin.doAfterTextChanged {
                if (currentPin.length() != 0) {
                    pinsViewsList[index + 1].apply {
                        requestFocus()
                        isCursorVisible = true
                    }
                    when {
                        firstTime && !pinLongClick -> {
                            clearAllPins(index + 1)
                        }
                        pinLongClick -> pasteClipboard()
                        else -> permitVerify()
                    }
                } else if (isErrorShown) {
                    quitError()
                }
            }
        }
    }

    private fun showKeyboard(view: View) {
        imm?.showSoftInput(view, InputMethodManager.RESULT_UNCHANGED_SHOWN)
    }

    private fun hideKeyboard() {
        Util.hideKeyboard(this, 0)
    }

    private fun quitError() {
        isErrorShown = false
        with(binding) {
            pin2faError.visibility = View.GONE
            pinsViewsList.forEach {
                it.setTextColor(
                    ContextCompat.getColor(
                        this@TwoFactorAuthenticationActivity,
                        R.color.grey_087_white_087
                    )
                )
            }
        }
    }

    private fun copySeed(seed: String) {
        Timber.d("Copy seed")
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        seed.let {
            ClipData.newPlainText("seed", seed)?.let {
                clipboard.setPrimaryClip(it)
                showSnackbar(getString(R.string.messages_copied_clipboard))
            }
        }
    }

    private fun pasteClipboard() {
        Timber.d("pasteClipboard")
        pinLongClick = false
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        if (clipData != null) {
            val code = clipData.getItemAt(0).text.toString()
            Timber.d("Code: $code")
            if (code.length == 6) {
                var areDigits = true
                for (i in 0..5) {
                    if (!Character.isDigit(code[i])) {
                        areDigits = false
                        break
                    }
                }
                if (areDigits) {
                    with(binding) {
                        listOf(
                            passFirst,
                            passSecond,
                            passThird,
                            passFourth,
                            passFifth,
                            passSixth
                        ).forEachIndexed { index, pin ->
                            pin.setText(code[index].toString())
                        }
                    }
                } else {
                    clearAllPins()
                }
            }
        }
    }

    private fun permitVerify() {
        Timber.d("permitVerify")
        with(binding) {
            var allPinsHasOneCharacter = true
            pinsViewsList.forEach {
                if (it.length() != 1) {
                    allPinsHasOneCharacter = false
                    return
                } else allPinsHasOneCharacter = true
            }
            if (confirm2FAIsShown && allPinsHasOneCharacter) {
                hideKeyboard()
                if (sb.isNotEmpty()) {
                    sb.delete(0, sb.length)
                }
                listOf(
                    passFirst,
                    passSecond,
                    passThird,
                    passFourth,
                    passFifth,
                    passSixth
                ).forEach {
                    sb.append(it.text)
                }
                pin = sb.toString().trim()
                if (pin.isNotEmpty()) {
                    viewModel.submitMultiFactorAuthPin(pin)
                }
            }
        }
    }

    private fun observeUIState() {
        this.collectFlow(viewModel.uiState) { state ->
            handleEnableMultiFactorAuthState(state)
            handleGetting2FACode(state)
            handleGettingUserEmail(state)
            handleQRCode(state)
        }
    }

    private fun handleQRCode(state: TwoFactorAuthenticationUIState) {
        with(state) {
            if (isQRCodeGenerationCompleted) {
                if (qrBitmap == null) {
                    showSnackbar(getString(R.string.qr_seed_text_error))
                    return
                }
                with(binding) {
                    qr2fa.setImageBitmap(qrBitmap)
                    qrProgressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun handleGettingUserEmail(state: TwoFactorAuthenticationUIState) {
        with(state) {
            if (userEmail != null && seed != null) {
                url = getString(R.string.url_qr_2fa, userEmail, seed)
                generate2FAQR()
            }
        }
    }

    private fun handleGetting2FACode(state: TwoFactorAuthenticationUIState) {
        if (state.is2FAFetchCompleted) {
            state.seed?.let {
                this@TwoFactorAuthenticationActivity.seed = it
                binding.qrProgressBar.visibility = View.VISIBLE
                setSeed(it.toSeedArray())
            } ?: showSnackbar(getString(R.string.qr_seed_text_error))
        }
    }

    private fun handleEnableMultiFactorAuthState(state: TwoFactorAuthenticationUIState) {
        with(state) {
            if (isPinSubmitted) {
                when (authenticationState) {
                    AuthenticationState.AuthenticationPassed -> {
                        confirm2FAIsShown = false
                        isEnabled2FA = true
                        rkSaved = isMasterKeyExported
                        with(binding) {
                            scrollContainer2fa.isVisible = false
                            scrollContainerVerify.isVisible = false
                            container2faEnabled.isVisible = true
                            buttonDismissRk.isVisible = isMasterKeyExported
                        }
                    }
                    AuthenticationState.AuthenticationFailed -> {
                        showError()
                    }
                    else -> {
                        showSnackbar(getString(R.string.error_enable_2fa))
                    }
                }
            }
        }
    }

    private fun clearAllPins(startIndex: Int = 0) {
        pinsViewsList.subList(startIndex, pinsViewsList.size).forEach {
            clearText(it)
        }
    }

    private fun clearText(editText: EditText) {
        editText.setText("")
    }
}