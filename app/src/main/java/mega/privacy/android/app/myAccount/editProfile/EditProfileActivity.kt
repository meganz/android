package mega.privacy.android.app.myAccount.editProfile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.AppBarStateChangeListener
import mega.privacy.android.app.components.twemoji.EmojiEditText
import mega.privacy.android.app.databinding.ActivityEditProfileBinding
import mega.privacy.android.app.databinding.DialogChangeEmailBinding
import mega.privacy.android.app.databinding.DialogChangeNameBinding
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.PhoneNumberBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.PhotoBottomSheetDialogFragment
import mega.privacy.android.app.myAccount.MyAccountViewModel
import mega.privacy.android.app.myAccount.MyAccountViewModel.Companion.CHECKING_2FA
import mega.privacy.android.app.presentation.changepassword.ChangePasswordActivity
import mega.privacy.android.app.presentation.editProfile.EditProfileViewModel
import mega.privacy.android.app.presentation.settings.exportrecoverykey.ExportRecoveryKeyActivity
import mega.privacy.android.app.presentation.verification.SMSVerificationActivity
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.AlertDialogUtil.quitEditTextError
import mega.privacy.android.app.utils.AlertDialogUtil.setEditTextError
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ChatUtil.StatusIconLocation
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.Constants.BUSINESS
import mega.privacy.android.app.utils.Constants.MAX_WIDTH_APPBAR_LAND
import mega.privacy.android.app.utils.Constants.MAX_WIDTH_APPBAR_PORT
import mega.privacy.android.app.utils.Constants.REQUEST_CAMERA
import mega.privacy.android.app.utils.Constants.TAKE_PICTURE_PROFILE_CODE
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.canVoluntaryVerifyPhoneNumber
import mega.privacy.android.app.utils.Util.checkTakePicture
import mega.privacy.android.app.utils.Util.dp2px
import mega.privacy.android.app.utils.Util.existOngoingTransfers
import mega.privacy.android.app.utils.Util.showAlert
import mega.privacy.android.app.utils.Util.showKeyboardDelayed
import mega.privacy.android.app.utils.ViewUtils.showSoftKeyboard
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.domain.exception.ChangeEmailException
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import nz.mega.sdk.MegaChatApi
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class EditProfileActivity : PasscodeActivity(), PhotoBottomSheetDialogFragment.PhotoCallback,
    PhoneNumberBottomSheetDialogFragment.PhoneNumberCallback, SnackbarShower {

    companion object {
        private const val PADDING_BOTTOM_APP_BAR = 19F
        private const val PADDING_COLLAPSED_BOTTOM_APP_BAR = 9F
        private const val NAME_SIZE = 16F
        private const val EMAIL_SIZE = 12F
        private const val PADDING_LEFT_STATE = 8F

        private const val CHANGE_NAME_SHOWN = "CHANGE_NAME_SHOWN"
        private const val FIRST_NAME_TYPED = "FIRST_NAME_TYPED"
        private const val LAST_NAME_TYPED = "LAST_NAME_TYPED"
        private const val CHANGE_EMAIL_SHOWN = "CHANGE_EMAIL_SHOWN"
        private const val DELETE_PHOTO_SHOWN = "DELETE_PHOTO_SHOWN"
        private const val EMAIL_TYPED = "EMAIL_TYPED"
        private const val REMOVE_OR_MODIFY_PHONE_SHOWN = "REMOVE_OR_MODIFY_PHONE_SHOWN"
        private const val IS_MODIFY = "IS_MODIFY"
    }

    private val viewModel by viewModels<MyAccountViewModel>()
    private val editProfileViewModel by viewModels<EditProfileViewModel>()

    private lateinit var binding: ActivityEditProfileBinding

    private var maxWidth = 0
    private var firstLineTextMaxWidthExpanded = 0
    private var stateToolbar = AppBarStateChangeListener.State.IDLE

    private var photoBottomSheet: PhotoBottomSheetDialogFragment? = null

    private var phoneNumberBottomSheetOld: PhoneNumberBottomSheetDialogFragment? = null

    private var isModify = false
    private var removeOrModifyPhoneNumberDialog: AlertDialog? = null
    private var changeNameDialog: AlertDialog? = null
    private var changeEmailDialog: AlertDialog? = null
    private var deletePhotoDialog: AlertDialog? = null

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (shouldRefreshSessionDueToSDK(true)) return

        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            setupView()
            setupObservers()
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(CHANGE_NAME_SHOWN, false)) {
                showChangeNameDialog(
                    savedInstanceState.getString(FIRST_NAME_TYPED),
                    savedInstanceState.getString(LAST_NAME_TYPED)
                )
            }

            if (savedInstanceState.getBoolean(CHANGE_EMAIL_SHOWN, false)) {
                showChangeEmailDialog(savedInstanceState.getString(EMAIL_TYPED))
            }

            if (savedInstanceState.getBoolean(DELETE_PHOTO_SHOWN, false)) {
                deletePhoto()
            }

            if (savedInstanceState.getBoolean(REMOVE_OR_MODIFY_PHONE_SHOWN, false)) {
                showRemovePhoneNumberConfirmation(savedInstanceState.getBoolean(IS_MODIFY, false))
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (isAlertDialogShown(changeNameDialog)) {
            outState.putBoolean(CHANGE_NAME_SHOWN, true)
            outState.putString(
                FIRST_NAME_TYPED,
                changeNameDialog?.findViewById<EmojiEditText>(R.id.first_name_field)?.text.toString()
            )

            outState.putString(
                LAST_NAME_TYPED,
                changeNameDialog?.findViewById<EmojiEditText>(R.id.last_name_field)?.text.toString()
            )
        }

        if (isAlertDialogShown(changeEmailDialog)) {
            outState.putBoolean(CHANGE_EMAIL_SHOWN, true)

            outState.putString(
                EMAIL_TYPED,
                changeEmailDialog?.findViewById<EmojiEditText>(R.id.email_field)?.text.toString()
            )
        }

        outState.putBoolean(DELETE_PHOTO_SHOWN, isAlertDialogShown(deletePhotoDialog))

        if (isAlertDialogShown(removeOrModifyPhoneNumberDialog)) {
            outState.putBoolean(REMOVE_OR_MODIFY_PHONE_SHOWN, true)
            outState.putBoolean(IS_MODIFY, isModify)
        }

        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()

        changeNameDialog?.dismiss()
        changeEmailDialog?.dismiss()
        deletePhotoDialog?.dismiss()
        removeOrModifyPhoneNumberDialog?.dismiss()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }

    @Suppress("deprecation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        viewModel.manageActivityResult(this, requestCode, resultCode, intent, this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isEmpty()) {
            Timber.w("Permissions ${permissions[0]} not granted")
            return
        }

        // Take profile picture scenario
        if (requestCode == REQUEST_CAMERA && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                checkTakePicture(this, TAKE_PICTURE_PROFILE_CODE)
            } else {
                showSnackbar(getString(R.string.denied_write_permissions))
            }
        }
    }

    private fun setupView() {
        setUpActionBar()
        setUpHeader()

        binding.changeName.setOnClickListener { showChangeNameDialog(null, null) }

        binding.addPhoto.setOnClickListener {
            if (photoBottomSheet.isBottomSheetDialogShown())
                return@setOnClickListener

            photoBottomSheet = PhotoBottomSheetDialogFragment()
            photoBottomSheet?.show(supportFragmentManager, photoBottomSheet?.tag)
        }

        binding.changeEmail.setOnClickListener {
            viewModel.check2FA()
            showChangeEmailDialog(null)
        }

        allowNameAndEmailEdition()

        binding.changePassword.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        binding.recoveryKeyButton.setOnClickListener {
            startActivity(Intent(this, ExportRecoveryKeyActivity::class.java))
        }

        binding.logoutButton.setOnClickListener { viewModel.logout(this@EditProfileActivity) }

        setupLogoutWarnings()
    }

    private fun allowNameAndEmailEdition() {
        val permitEditNameAndEmail =
            !(megaApi.isBusinessAccount && myAccountInfo.accountType == BUSINESS) || megaApi.isMasterBusinessAccount
        binding.changeName.isVisible = permitEditNameAndEmail
        binding.changeNameSeparator.separator.isVisible = permitEditNameAndEmail
        binding.changeEmail.isVisible = permitEditNameAndEmail
        binding.changeEmailSeparator.separator.isVisible = permitEditNameAndEmail
    }

    private fun setupObservers() {
        collectFlow(viewModel.state) { state ->
            binding.headerLayout.firstLineToolbar.text = state.name
            binding.headerLayout.secondLineToolbar.text = state.email
            binding.progressBar.isVisible = state.isLoading

            state.changeEmailResult?.let {
                showChangeEmailResult(it)
                viewModel.markHandleChangeEmailResult()
            }

            state.changeUserNameResult?.let {
                updateName(it.isSuccess)
                viewModel.markHandleChangeUserNameResult()
            }

            setupPhoneNumber(
                alreadyRegistered = state.verifiedPhoneNumber != null,
                canVerify = state.canVerifyPhoneNumber && state.verifiedPhoneNumber == null,
            )
        }

        lifecycleScope.launch {
            editProfileViewModel.state.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect {
                    setUpAvatar(it.avatarFile, it.avatarColor)
                }
        }
    }

    /**
     * Shows the result of a name change.
     *
     * @param success True if the name was changed successfully, false otherwise.
     */
    private fun updateName(success: Boolean) {
        showSnackbar(
            getString(
                if (success) R.string.success_changing_user_attributes
                else R.string.error_changing_user_attributes
            )
        )
    }

    /**
     * Shows the result of an email change.
     *
     * @param result result of the email change.
     */
    private fun showChangeEmailResult(result: Result<String>) {
        binding.progressBar.isVisible = false
        val firstMessageId: Int
        val secondMessageId: Int

        when {
            result.isSuccess -> {
                firstMessageId = R.string.email_verification_text_change_mail
                secondMessageId = R.string.email_verification_title
            }
            result.exceptionOrNull() is ChangeEmailException.EmailInUse -> {
                firstMessageId = R.string.mail_already_used
                secondMessageId = R.string.email_verification_title
            }
            result.exceptionOrNull() is ChangeEmailException.AlreadyRequested -> {
                firstMessageId = R.string.mail_changed_confirm_requested
                secondMessageId = R.string.email_verification_title
            }
            else -> {
                firstMessageId = R.string.general_text_error
                secondMessageId = R.string.general_error_word
            }
        }

        showAlert(
            this,
            getString(firstMessageId),
            getString(secondMessageId)
        )
    }

    private fun setUpActionBar() {
        setSupportActionBar(binding.headerLayout.toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = null

        var drawableArrow = ContextCompat.getDrawable(
            applicationContext,
            R.drawable.ic_arrow_back_white
        )

        drawableArrow = drawableArrow?.mutate()

        val statusBarColor =
            getColorForElevation(this, resources.getDimension(R.dimen.toolbar_elevation))

        binding.headerLayout.collapseToolbar.apply {
            if (Util.isDarkMode(this@EditProfileActivity)) {
                setContentScrimColor(statusBarColor)
            }

            setStatusBarScrimColor(statusBarColor)
        }

        val white = ContextCompat.getColor(this, R.color.white_alpha_087)
        val black = ContextCompat.getColor(this, R.color.grey_087_white_087)

        binding.headerLayout.appBar.addOnOffsetChangedListener(object :
            AppBarStateChangeListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout, state: State) {
                stateToolbar = state

                if (state == State.EXPANDED || state == State.COLLAPSED) {
                    val color: Int = if (state == State.EXPANDED) white else black

                    binding.headerLayout.firstLineToolbar.setTextColor(color)
                    binding.headerLayout.secondLineToolbar.setTextColor(color)

                    drawableArrow?.colorFilter =
                        PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)

                    supportActionBar?.setHomeAsUpIndicator(drawableArrow)
                    visibilityStateIcon()
                }

                binding.headerLayout.secondLineToolbar.setPadding(
                    0, 0, 0,
                    dp2px(if (state == State.COLLAPSED) PADDING_COLLAPSED_BOTTOM_APP_BAR else PADDING_BOTTOM_APP_BAR)
                )
            }
        })
    }

    /**
     * Checks online status and shows it if needed.
     */
    private fun visibilityStateIcon() {
        val status = megaChatApi.onlineStatus
        val showStatus = stateToolbar == AppBarStateChangeListener.State.EXPANDED
                && (status == MegaChatApi.STATUS_ONLINE || status == MegaChatApi.STATUS_AWAY
                || status == MegaChatApi.STATUS_BUSY || status == MegaChatApi.STATUS_OFFLINE)

        binding.headerLayout.firstLineToolbar.apply {
            maxLines =
                if (showStatus || stateToolbar == AppBarStateChangeListener.State.EXPANDED) 2
                else 1

            if (showStatus) {
                setTrailingIcon(
                    ChatUtil.getIconResourceIdByLocation(
                        this@EditProfileActivity,
                        status,
                        StatusIconLocation.STANDARD
                    ), dp2px(PADDING_LEFT_STATE)
                )
            }

            updateMaxWidthAndIconVisibility(
                if (stateToolbar == AppBarStateChangeListener.State.EXPANDED) firstLineTextMaxWidthExpanded else maxWidth,
                showStatus
            )
        }
    }

    /**
     * load avatar and color
     */
    private fun setUpAvatar(avatarFile: File?, color: Int) {
        binding.headerLayout.toolbarImage.load(avatarFile)
        binding.headerLayout.imageLayout.setBackgroundColor(color)
    }

    private fun setUpHeader() {
        firstLineTextMaxWidthExpanded = outMetrics.widthPixels - dp2px(108f, outMetrics)
        binding.headerLayout.firstLineToolbar.apply {
            setMaxWidthEmojis(firstLineTextMaxWidthExpanded)
            textSize = NAME_SIZE
        }

        maxWidth = dp2px(
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) MAX_WIDTH_APPBAR_PORT
            else MAX_WIDTH_APPBAR_LAND,
            outMetrics
        )

        binding.headerLayout.secondLineToolbar.apply {
            maxWidth = maxWidth
            textSize = EMAIL_SIZE
        }
    }

    private fun setupPhoneNumber(
        alreadyRegistered: Boolean = viewModel.isAlreadyRegisteredPhoneNumber(),
        canVerify: Boolean = canVoluntaryVerifyPhoneNumber(),
    ) {

        binding.addPhoneNumber.text = getString(
            if (alreadyRegistered) R.string.title_modify_phone_number
            else R.string.add_phone_number_action
        )

        binding.addPhoneNumberLayout.apply {
            isVisible = alreadyRegistered || canVerify
            setOnClickListener {
                if (canVerify) {
                    startActivity(
                        Intent(
                            this@EditProfileActivity,
                            SMSVerificationActivity::class.java
                        )
                    )
                } else if (!phoneNumberBottomSheetOld.isBottomSheetDialogShown()) {
                    phoneNumberBottomSheetOld = PhoneNumberBottomSheetDialogFragment()
                    phoneNumberBottomSheetOld!!.show(
                        supportFragmentManager,
                        phoneNumberBottomSheetOld!!.tag
                    )
                }
            }
        }
    }

    /**
     * Checks if there are offline files and transfers.
     * If yes, shows the corresponding warning text at the end. If not, hides the text.
     */
    fun setupLogoutWarnings() {
        val existOfflineFiles = OfflineUtils.existsOffline(this)
        val existOutgoingTransfers = existOngoingTransfers(megaApi)

        binding.logoutWarningText.apply {
            isVisible = existOfflineFiles || existOutgoingTransfers
            text = getString(
                when {
                    existOfflineFiles && existOutgoingTransfers -> R.string.logout_warning_offline_and_transfers
                    existOfflineFiles -> R.string.logout_warning_offline
                    else -> R.string.logout_warning_transfers
                }
            )
        }
    }

    /**
     * Shows a dialog to change the name of the account.
     *
     * @param firstName Current typed first name if any, null otherwise.
     * @param lastName  Current typed last name if any, null otherwise.
     */
    private fun showChangeNameDialog(firstName: String?, lastName: String?) {
        val dialogBinding = DialogChangeNameBinding.inflate(layoutInflater)

        changeNameDialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.change_name_action))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.save_action), null)
            .setNegativeButton(getString(R.string.button_cancel), null)
            .create()

        changeNameDialog?.apply {
            setOnShowListener {
                quitEditTextError(dialogBinding.firstNameLayout, dialogBinding.firstNameErrorIcon)
                quitEditTextError(dialogBinding.lastNameLayout, dialogBinding.lastNameErrorIcon)

                dialogBinding.firstNameField.apply {
                    setText(firstName ?: editProfileViewModel.getFirstName())
                    setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                        }

                        false
                    }

                    doAfterTextChanged {
                        quitEditTextError(
                            dialogBinding.firstNameLayout,
                            dialogBinding.firstNameErrorIcon
                        )
                    }

                    post {
                        requestFocus()
                        setSelection(length())
                        showSoftKeyboard()
                    }
                }

                dialogBinding.lastNameField.apply {
                    setText(lastName ?: editProfileViewModel.getLastName())
                    setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                        }

                        false
                    }

                    doAfterTextChanged {
                        quitEditTextError(
                            dialogBinding.lastNameLayout,
                            dialogBinding.lastNameErrorIcon
                        )
                    }
                }

                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    var errorShown = false

                    if (dialogBinding.firstNameField.text.isNullOrEmpty()) {
                        errorShown = true
                        setEditTextError(
                            getString(R.string.error_enter_username),
                            dialogBinding.firstNameLayout, dialogBinding.firstNameErrorIcon
                        )
                    }

                    if (dialogBinding.lastNameField.text.isNullOrEmpty()) {
                        errorShown = true
                        setEditTextError(
                            getString(R.string.error_enter_userlastname),
                            dialogBinding.lastNameLayout, dialogBinding.lastNameErrorIcon
                        )
                    }

                    if (!errorShown) {
                        viewModel.changeName(
                            editProfileViewModel.getFirstName(),
                            editProfileViewModel.getLastName(),
                            dialogBinding.firstNameField.text.toString(),
                            dialogBinding.lastNameField.text.toString()
                        )

                        dismiss()
                    }
                }
            }
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            show()
        }
    }

    /**
     * Shows a dialog to change the email of the account.
     *
     * @param email Current typed email if any, null otherwise.
     */
    private fun showChangeEmailDialog(email: String?) {
        val dialogBinding = DialogChangeEmailBinding.inflate(layoutInflater)

        changeEmailDialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.change_mail_title_last_step))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.save_action), null)
            .setNegativeButton(getString(R.string.button_cancel), null)
            .create()

        changeEmailDialog?.apply {
            setOnShowListener {
                quitEditTextError(dialogBinding.emailLayout, dialogBinding.emailErrorIcon)

                dialogBinding.emailField.apply {
                    setText(email ?: viewModel.getEmail())
                    requestFocus()
                    setSelection(0, text.toString().length)
                    showKeyboardDelayed(this)
                    setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                        }

                        false
                    }

                    doAfterTextChanged {
                        quitEditTextError(dialogBinding.emailLayout, dialogBinding.emailErrorIcon)
                    }
                }

                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    if (dialogBinding.emailField.text.isNullOrEmpty()) {
                        setEditTextError(
                            getString(R.string.error_enter_email),
                            dialogBinding.emailLayout, dialogBinding.emailErrorIcon
                        )
                    } else {
                        val error = viewModel.changeEmail(
                            this@EditProfileActivity,
                            dialogBinding.emailField.text.toString()
                        )

                        binding.progressBar.isVisible = error == null

                        if (error != null && error != CHECKING_2FA) {
                            setEditTextError(
                                error,
                                dialogBinding.emailLayout,
                                dialogBinding.emailErrorIcon
                            )
                        } else {
                            dismiss()
                        }
                    }
                }
            }

            show()
        }
    }

    override fun capturePhoto() {
        viewModel.capturePhoto(this)
    }

    override fun choosePhoto() {
        viewModel.launchChoosePhotoIntent(this)
    }

    override fun deletePhoto() {
        deletePhotoDialog = MaterialAlertDialogBuilder(this)
            .setMessage(R.string.confirmation_delete_avatar)
            .setPositiveButton(R.string.context_delete) { _, _ ->
                viewModel.deleteProfileAvatar(this, this)
            }
            .setNegativeButton(R.string.general_cancel, null)
            .show()
    }

    override fun showRemovePhoneNumberConfirmation(isModify: Boolean) {
        this.isModify = isModify

        val title = getString(
            if (isModify) R.string.title_modify_phone_number
            else R.string.title_remove_phone_number
        )

        val message = getString(
            if (isModify) R.string.modify_phone_number_message
            else R.string.remove_phone_number_message
        )

        removeOrModifyPhoneNumberDialog =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.general_ok) { _, _ ->
                    viewModel.resetPhoneNumber(isModify, this) {
                        startActivity(Intent(this, SMSVerificationActivity::class.java))
                    }
                }
                .setNegativeButton(getString(R.string.general_cancel), null)
                .show()
    }

    private fun showSnackbar(message: String) {
        showSnackbar(binding.root, message)
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        content?.let { showSnackbar(it) }
    }
}
