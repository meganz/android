package mega.privacy.android.app.presentation.twofactorauthentication

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.EditTextPIN
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.databinding.ActivityTwoFactorAuthenticationBinding
import mega.privacy.android.app.databinding.Dialog2faHelpBinding
import mega.privacy.android.app.databinding.DialogNoAuthenticationAppsBinding
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.presentation.twofactorauthentication.model.AuthenticationState
import mega.privacy.android.app.presentation.twofactorauthentication.model.TwoFactorAuthenticationUIState
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber
import java.util.EnumMap

/**
 * TwoFactorAuthenticationActivity
 */
class TwoFactorAuthenticationActivity : PasscodeActivity(), MegaRequestListenerInterface {

    private lateinit var binding: ActivityTwoFactorAuthenticationBinding
    private val viewModel: TwoFactorAuthenticationViewModel by viewModels()

    private var seed: String? = null
    private var url: String? = null
    private var pin = ""

    private val arraySeed: ArrayList<String> by lazy { ArrayList() }

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

    private var qr: Bitmap? = null
    private var imm: InputMethodManager? = null

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
            title = getFormattedStringOrDefault(R.string.settings_2fa)
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
                seed = getString("seed", "")
                arraySeed.clear()
                arraySeed.addAll(getStringArrayList("arraySeed") ?: emptyList())
                isNoAppsDialogShown = getBoolean("isNoAppsDialogShown", false)
                isHelpDialogShown = getBoolean("isHelpDialogShown", false)
            }
        }
        val explainQrText =
            SpannableString(getFormattedStringOrDefault(R.string.explain_qr_seed_2fa_2) + "  QM")
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
            fileNameRK.text = FileUtil.getRecoveryKeyFileName()

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
                    megaApi.multiFactorAuthGetCode(this@TwoFactorAuthenticationActivity)
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

        if (scanOrCopyIsShown) {
            Timber.d("scanOrCopyIsShown")
            with(outState) {
                putString("seed", seed)
                putStringArrayList("arraySeed", arraySeed)
            }
        }
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
            if (seed == null) {
                megaApi.multiFactorAuthGetCode(this@TwoFactorAuthenticationActivity)
            } else {
                Timber.d("Seed not null")
                setSeed()
                if (qr != null) {
                    Timber.d("QR not null")
                    qr2fa.setImageBitmap(qr)
                } else {
                    generate2FAQR()
                }
            }

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
        Timber.d("generate2FAQR")
        url = null
        val myEmail = megaApi.myEmail
        if (myEmail != null && seed != null) {
            url = getString(R.string.url_qr_2fa, myEmail, seed)
            setSeed()
        }
        if (url == null) return

        val hints: MutableMap<EncodeHintType, ErrorCorrectionLevel> =
            EnumMap(EncodeHintType::class.java)
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
        val bitMatrix: BitMatrix? = try {
            MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, 40, 40, null)

        } catch (e: WriterException) {
            e.printStackTrace()
            return
        }
        bitMatrix?.apply bitMatrix@{
            val w = bitMatrix.width
            val h = bitMatrix.height
            val pixels = IntArray(w * h)
            val width = w * WIDTH / FACTOR
            qr = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888)
            val colorBackground = ContextCompat.getColor(
                this@TwoFactorAuthenticationActivity,
                R.color.white_grey_700
            )
            val colorCode = ContextCompat.getColor(
                this@TwoFactorAuthenticationActivity,
                R.color.dark_grey
            )
            val paint = Paint()
            paint.isAntiAlias = true
            paint.color = colorBackground
            qr?.apply qrApply@{
                val c = Canvas(this)
                c.drawRect(0f, 0f, width.toFloat(), width.toFloat(), paint)
                paint.color = colorCode
                val size = (w - 12).toFloat()
                for (y in 0 until h) {
                    val offset = y * w
                    for (x in 0 until w) {
                        pixels[offset + x] =
                            if (bitMatrix.get(x, y)) colorCode else colorBackground
                        if (pixels[offset + x] == colorCode) {
                            c.drawCircle(x * RESIZE, y * RESIZE, 3.5f, paint)
                        }
                    }
                }

                //            8.5 width
                paint.color = colorBackground
                c.drawRect(3 * RESIZE, 3 * RESIZE, 11.5f * RESIZE, 11.5f * RESIZE, paint)
                c.drawRect(
                    size * RESIZE,
                    3 * RESIZE,
                    (size + 8.5f) * RESIZE,
                    11.5f * RESIZE,
                    paint
                )
                c.drawRect(
                    3 * RESIZE,
                    size * RESIZE,
                    11.5f * RESIZE,
                    (size + 8.5f) * RESIZE,
                    paint
                )
                paint.color = colorCode
                c.drawRoundRect(
                    3.75f * RESIZE,
                    3.75f * RESIZE,
                    10.75f * RESIZE,
                    10.75f * RESIZE,
                    15f,
                    15f,
                    paint
                )
                //                7 width, 0.75 more than last
                c.drawRoundRect(
                    (size + 0.75f) * RESIZE,
                    3.75f * RESIZE,
                    (size + 0.75f + 7f) * RESIZE,
                    10.75f * RESIZE,
                    15f,
                    15f,
                    paint
                )
                c.drawRoundRect(
                    3.75f * RESIZE,
                    (size + 0.75f) * RESIZE,
                    10.75f * RESIZE,
                    (size + 0.75f + 7f) * RESIZE,
                    15f,
                    15f,
                    paint
                )
                paint.color = colorBackground
                c.drawRoundRect(
                    4.75f * RESIZE,
                    4.75f * RESIZE,
                    9.75f * RESIZE,
                    9.75f * RESIZE,
                    12.5f,
                    12.5f,
                    paint
                )
                //                5 width, 1.75 more than first
                c.drawRoundRect(
                    (size + 1.75f) * RESIZE,
                    4.75f * RESIZE,
                    (size + 1.75f + 5f) * RESIZE,
                    9.75f * RESIZE,
                    12.5f,
                    12.5f,
                    paint
                )
                c.drawRoundRect(
                    4.75f * RESIZE,
                    (size + 1.75f) * RESIZE,
                    9.75f * RESIZE,
                    (size + 1.75f + 5f) * RESIZE,
                    12.5f,
                    12.5f,
                    paint
                )
                paint.color = colorCode
                c.drawCircle(7.25f * RESIZE, 7.25f * RESIZE, 12f, paint)
                //            4.25 more than first
                c.drawCircle((size + 4.25f) * RESIZE, 7.25f * RESIZE, 12f, paint)
                c.drawCircle(7.25f * RESIZE, (size + 4.25f) * RESIZE, 12f, paint)
                if (qr != null) {
                    with(binding) {
                        qr2fa.setImageBitmap(qr)
                        qrProgressBar.visibility = View.GONE
                    }
                } else {
                    showSnackbar(getString(R.string.qr_seed_text_error))
                }
            }


        }
    }

    private fun showSnackbar(s: String) {
        showSnackbar(binding.container2fa, s)
    }

    private fun setSeed() {
        arraySeed.clear()
        seed?.let { it ->
            var index = 0
            for (i in 0 until LENGTH_SEED) {
                arraySeed.add(it.substring(index, index + 4))
                index += 4
            }
        } ?: return

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
                if (url == null) {
                    val myEmail = megaApi.myEmail
                    if (myEmail != null && seed != null) {
                        url = getString(R.string.url_qr_2fa, myEmail, seed)
                    }
                }
                if (url != null) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    Timber.d("URL: $url seed: $seed")
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
                showKeyboard(passFifth)
                clearAllPins()
            }
            listOf(containerRk2fa, buttonExportRk).forEach {
                it.setOnClickListener {
                    update2FASetting()
                    val intent =
                        Intent(this@TwoFactorAuthenticationActivity, ManagerActivity::class.java)
                    intent.action = Constants.ACTION_RECOVERY_KEY_EXPORTED
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                }
            }
            buttonDismissRk.setOnClickListener {
                update2FASetting()
                finish()
            }
            explainQrSeed2fa2.setOnClickListener {
                showAlertHelp()
            }
            seed2fa.setOnLongClickListener {
                copySeed()
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

    private fun addTextChangedListener(
        index: Int,
        currentPin: EditTextPIN,
        isLastPin: Boolean = false,
    ) {
        if (isLastPin) {
            currentPin.doAfterTextChanged {
                if (currentPin.length() != 0) {
                    currentPin.isCursorVisible = true
                    Util.hideKeyboard(this@TwoFactorAuthenticationActivity, 0)
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

    private fun copySeed() {
        Timber.d("Copy seed")
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        seed?.let {
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
                            pin.setText("" + code[index])
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
                Util.hideKeyboard(
                    this@TwoFactorAuthenticationActivity,
                    0
                )
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
        }
    }

    private fun handleEnableMultiFactorAuthState(state: TwoFactorAuthenticationUIState) {
        with(state) {
            if (isPinSubmitted) {
                when (authenticationState) {
                    AuthenticationState.AuthenticationPassed -> {
                        confirm2FAIsShown = false
                        isEnabled2FA = true
                        megaApi.isMasterKeyExported(this@TwoFactorAuthenticationActivity)
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

    override fun onRequestStart(p0: MegaApiJava?, p1: MegaRequest?) {
    }

    override fun onRequestUpdate(p0: MegaApiJava?, p1: MegaRequest?) {
    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.d("onRequestFinish")
        when (request.type) {
            MegaRequest.TYPE_MULTI_FACTOR_AUTH_GET -> {
                Timber.d("MegaRequest.TYPE_MULTI_FACTOR_AUTH_GET")
                if (e.errorCode == MegaError.API_OK) {
                    Timber.d("MegaError.API_OK")
                    seed = request.text
                    if (seed == null) {
                        showSnackbar(getString(R.string.qr_seed_text_error))
                    } else {
                        binding.qrProgressBar.visibility = View.VISIBLE
                        generate2FAQR()
                    }
                } else {
                    Timber.e("e.getErrorCode(): ${e.errorCode}")
                    showSnackbar(getString(R.string.qr_seed_text_error))
                }
            }
            MegaRequest.TYPE_MULTI_FACTOR_AUTH_SET -> {
                Timber.d("TYPE_MULTI_FACTOR_AUTH_SET: ${e.errorCode}")
                if (request.flag && e.errorCode == MegaError.API_OK) {
                    Timber.d("Pin correct: Two-Factor Authentication enabled")
                    confirm2FAIsShown = false
                    isEnabled2FA = true
                    megaApi.isMasterKeyExported(this)
                } else if (e.errorCode == MegaError.API_EFAILED) {
                    Timber.w("Pin not correct: ${request.password}")
                    if (request.flag) {
                        showError()
                    }
                } else {
                    Timber.e("An error ocurred trying to enable Two-Factor Authentication")
                    showSnackbar(getString(R.string.error_enable_2fa))
                }
            }
            MegaRequest.TYPE_MULTI_FACTOR_AUTH_CHECK -> {
                if (e.errorCode == MegaError.API_OK) {
                    Timber.d("TYPE_MULTI_FACTOR_AUTH_CHECK: ${request.flag}")
                }
            }
            MegaRequest.TYPE_GET_ATTR_USER -> {
                if (request.paramType == MegaApiJava.USER_ATTR_PWD_REMINDER) {
                    Timber.d("TYPE_GET_ATTR_USER")
                    if (e.errorCode == MegaError.API_OK || e.errorCode == MegaError.API_ENOENT) {
                        Timber.d("TYPE_GET_ATTR_USER API_OK")
                        with(binding) {
                            scrollContainer2fa.visibility = View.GONE
                            scrollContainerVerify.visibility = View.GONE
                            container2faEnabled.visibility = View.VISIBLE
                        }
                        binding.buttonDismissRk.isVisible =
                            e.errorCode == MegaError.API_OK && request.access == 1
                        rkSaved = binding.buttonDismissRk.isVisible
                    } else {
                        Timber.e("TYPE_GET_ATTR_USER error: ${e.errorString}")
                    }
                }
            }
        }
    }

    override fun onRequestTemporaryError(
        p0: MegaApiJava?,
        p1: MegaRequest?,
        p2: MegaError?,
    ) {
    }

    override fun onDestroy() {
        super.onDestroy()
        megaApi.removeRequestListener(this)
    }

    companion object {
        private const val LENGTH_SEED = 13
        private const val WIDTH = 520
        private const val FACTOR = 65
        private const val RESIZE = 8f
    }
}