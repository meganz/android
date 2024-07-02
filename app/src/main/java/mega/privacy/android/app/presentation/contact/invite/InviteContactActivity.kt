package mega.privacy.android.app.presentation.contact.invite

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.compose.runtime.getValue
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doBeforeTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.ContactsDividerDecoration
import mega.privacy.android.app.components.scrollBar.FastScrollerScrollListener
import mega.privacy.android.app.databinding.ActivityInviteContactBinding
import mega.privacy.android.app.databinding.SelectedContactItemBinding
import mega.privacy.android.app.main.InvitationContactInfo
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_MANUAL_INPUT_EMAIL
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_MANUAL_INPUT_PHONE
import mega.privacy.android.app.main.adapters.InvitationContactsAdapter
import mega.privacy.android.app.main.model.InviteContactUiState.InvitationStatusMessageUiState
import mega.privacy.android.app.main.model.InviteContactUiState.InvitationStatusMessageUiState.InvitationsSent
import mega.privacy.android.app.main.model.InviteContactUiState.InvitationStatusMessageUiState.NavigateUpWithResult
import mega.privacy.android.app.main.model.InviteContactUiState.MessageTypeUiState.Plural
import mega.privacy.android.app.main.model.InviteContactUiState.MessageTypeUiState.Singular
import mega.privacy.android.app.presentation.contact.invite.component.ContactInfoListDialog
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.qrcode.QRCodeComposeActivity
import mega.privacy.android.app.presentation.view.open.camera.confirmation.OpenCameraConfirmationDialogRoute
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.REQUEST_READ_CONTACTS
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import timber.log.Timber
import javax.inject.Inject

/**
 * Invite contact activity.
 */
@AndroidEntryPoint
class InviteContactActivity : PasscodeActivity(), InvitationContactsAdapter.OnItemClickListener {

    /**
     * Current theme
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel: InviteContactViewModel by viewModels()

    private var displayMetrics: DisplayMetrics? = null
    private var actionBar: ActionBar? = null
    private var invitationContactsAdapter: InvitationContactsAdapter? = null

    private var isPermissionGranted = false
    private var isGetContactCompleted = false

    private lateinit var binding: ActivityInviteContactBinding

    //work around for android bug - https://issuetracker.google.com/issues/37007605#c10
    private class LinearLayoutManagerWrapper(context: Context) : LinearLayoutManager(context) {
        override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
            try {
                super.onLayoutChildren(recycler, state)
            } catch (e: IndexOutOfBoundsException) {
                Timber.d("IndexOutOfBoundsException in RecyclerView happens")
            }
        }
    }

    /**
     * Called when the activity is starting.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        binding = ActivityInviteContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.composeView.setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                if (uiState.showOpenCameraConfirmation) {
                    OpenCameraConfirmationDialogRoute(
                        onConfirm = {
                            initScanQR()
                            viewModel.onQRScannerInitialized()
                            viewModel.onOpenCameraConfirmationShown()
                        },
                        onDismiss = viewModel::onOpenCameraConfirmationShown
                    )
                }

                uiState.invitationContactInfoWithMultipleContacts?.let {
                    ContactInfoListDialog(
                        contactInfo = it,
                        currentSelectedContactInfo = uiState.selectedContactInformation,
                        onConfirm = { newListOfSelectedContact ->
                            viewModel.onDismissContactListContactInfo()
                            viewModel.updateSelectedContactInfoByInfoWithMultipleContacts(
                                newListOfSelectedContact = newListOfSelectedContact,
                                contactInfo = it
                            )
                        },
                        onCancel = viewModel::onDismissContactListContactInfo
                    )
                }
            }
        }

        setSupportActionBar(binding.inviteContactToolbar)

        actionBar = supportActionBar
        actionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
            it.title = getString(R.string.invite_contacts)
            setTitleAB()
        }

        setupListeners()

        binding.inviteContactList.apply {
            setClipToPadding(false)
            setItemAnimator(DefaultItemAnimator())
            setLayoutManager(LinearLayoutManagerWrapper(this@InviteContactActivity))
            addItemDecoration(ContactsDividerDecoration(this@InviteContactActivity))

            invitationContactsAdapter = InvitationContactsAdapter(
                this@InviteContactActivity,
                viewModel.uiState.value.filteredContacts,
                this@InviteContactActivity,
                megaApi
            )
            setAdapter(invitationContactsAdapter)
        }

        binding.fastScroller.setRecyclerView(binding.inviteContactList)

        binding.inviteContactListEmptyText.setText(R.string.contacts_list_empty_text_loading_share)

        refreshInviteContactButton()

        //orientation changes
        if (savedInstanceState != null) {
            isGetContactCompleted =
                savedInstanceState.getBoolean(KEY_IS_GET_CONTACT_COMPLETED, false)
        }

        if (isGetContactCompleted) {
            savedInstanceState?.let {
                isPermissionGranted = it.getBoolean(KEY_IS_PERMISSION_GRANTED, false)
            }
            refreshAddedContactsView(true)
            setRecyclersVisibility()
            setTitleAB()
            if (viewModel.allContacts.isNotEmpty()) {
                setEmptyStateVisibility(false)
            } else if (isPermissionGranted) {
                setEmptyStateVisibility(true)
                showEmptyTextView()
            } else {
                setEmptyStateVisibility(true)
                binding.inviteContactListEmptyText.setText(R.string.no_contacts_permissions)
                binding.inviteContactListEmptyImage.visibility = View.VISIBLE
                binding.noPermissionHeader.visibility = View.VISIBLE
            }
        } else {
            queryIfHasReadContactsPermissions()
        }

        collectFlows()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        binding.fabButtonNext.setOnClickListener {
            enableFabButton(false)
            Timber.d("invite Contacts")
            viewModel.inviteContacts()
            Util.hideKeyboard(this, 0)
        }

        binding.typeMailEditText.apply {
            doBeforeTextChanged { _, _, _, _ -> refreshKeyboard() }

            doOnTextChanged { text, start, before, count ->
                Timber.d("onTextChanged: s is $text start: $start before: $before count: $count")
                if (!text.isNullOrBlank()) {
                    val last = text.last()
                    if (last.isWhitespace()) {
                        val processedString = text.toString().trim()
                        if (isValidEmail(processedString)) {
                            viewModel.addContactInfo(processedString, TYPE_MANUAL_INPUT_EMAIL)
                            binding.typeMailEditText.text.clear()
                        } else if (isValidPhone(processedString)) {
                            viewModel.addContactInfo(processedString, TYPE_MANUAL_INPUT_PHONE)
                            binding.typeMailEditText.text.clear()
                        }

                        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            Util.hideKeyboard(this@InviteContactActivity, 0)
                        }
                    } else {
                        Timber.d("Last character is: $last")
                    }
                }

                refreshInviteContactButton()
                viewModel.onSearchQueryChange(binding.typeMailEditText.text.toString())
                refreshKeyboard()
            }

            doAfterTextChanged { refreshKeyboard() }

            setOnEditorActionListener { textView, actionId, _ ->
                refreshKeyboard()
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val processedStrong = textView.text.toString().trim()
                    if (processedStrong.isNotBlank()) {
                        binding.typeMailEditText.text.clear()
                        viewModel.onSearchQueryChange("")
                        when {
                            isValidEmail(processedStrong) -> {
                                viewModel.validateEmailInput(processedStrong)
                                Util.hideKeyboard(this@InviteContactActivity, 0)
                            }

                            isValidPhone(processedStrong) -> {
                                viewModel.addContactInfo(processedStrong, TYPE_MANUAL_INPUT_PHONE)
                                Util.hideKeyboard(this@InviteContactActivity, 0)
                                viewModel.filterContacts(binding.typeMailEditText.text.toString())
                            }

                            else -> {
                                Toast.makeText(
                                    this@InviteContactActivity,
                                    R.string.invalid_input,
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@setOnEditorActionListener true
                            }
                        }
                    }
                    Util.hideKeyboard(this@InviteContactActivity, 0)
                    refreshInviteContactButton()
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }

            setImeOptions(EditorInfo.IME_ACTION_DONE)
        }

        binding.layoutScanQr.setOnClickListener {
            Timber.d("Scan QR code pressed")
            viewModel.validateCameraAvailability()
        }

        binding.inviteContactList.setOnTouchListener { _: View?, _: MotionEvent ->
            Util.hideKeyboard(this@InviteContactActivity, 0)
            false
        }

        binding.fastScroller.setUpScrollListener(object : FastScrollerScrollListener {
            override fun onScrolled() {
                binding.fabButtonNext.hide()
            }

            override fun onScrolledToTop() {
                binding.fabButtonNext.show()
            }
        })
    }

    private fun collectFlows() {
        collectFlow(
            viewModel
                .uiState
                .map { it.areContactsInitialized }
                .distinctUntilChanged()
        ) { isContactsInitialized ->
            if (isContactsInitialized) {
                onGetContactCompleted()
                viewModel.filterContacts(binding.typeMailEditText.text.toString())
                viewModel.resetOnContactsInitializedState()
            }
        }

        collectFlow(viewModel.uiState.map { it.filteredContacts }.distinctUntilChanged()) {
            refreshList()
            visibilityFastScroller()
        }

        collectFlow(
            viewModel
                .uiState
                .map { it.invitationStatusResult }
                .distinctUntilChanged()
        ) {
            it?.let { showInvitationsResult(it) }
        }

        collectFlow(
            viewModel
                .uiState
                .map { it.shouldInitializeQR }
                .filter { it }
                .distinctUntilChanged()
        ) {
            initScanQR()
            viewModel.onQRScannerInitialized()
        }

        collectFlow(
            viewModel
                .uiState
                .map { it.pendingPhoneNumberInvitations }
                .filter { it.isNotEmpty() }
                .distinctUntilChanged()
        ) {
            if (viewModel.uiState.value.invitationStatusResult == null) {
                invitePhoneContacts(it)
                finish()
            }
        }

        collectFlow(
            viewModel
                .uiState
                .map { it.selectedContactInformation }
                .distinctUntilChanged()
        ) {
            refreshComponents()
        }

        collectFlow(
            viewModel
                .uiState
                .map { it.emailValidationMessage }
                .distinctUntilChanged()
        ) {
            it?.let {
                if (it is Singular) {
                    val message = if (it.argument != null) {
                        resources.getString(
                            it.id,
                            it.argument
                        )
                    } else resources.getString(it.id)
                    showSnackBar(message)
                }
            }
        }
    }

    private fun showInvitationsResult(status: InvitationStatusMessageUiState) {
        val result = Intent()
        when (status) {
            is NavigateUpWithResult -> {
                result.putExtra(status.result.key, status.result.totalInvitationsSent)
            }

            is InvitationsSent -> {
                val message = status.messages.fold("") { acc, messageType ->
                    acc + when (messageType) {
                        is Plural -> {
                            resources.getQuantityString(
                                messageType.id,
                                messageType.quantity,
                                messageType.quantity
                            )
                        }

                        is Singular -> {
                            if (messageType.argument != null) {
                                resources.getString(
                                    messageType.id,
                                    messageType.argument
                                )
                            } else resources.getString(messageType.id)
                        }
                    }
                }
                showSnackBar(message)
            }
        }

        Util.hideKeyboard(this@InviteContactActivity, 0)
        Handler(Looper.getMainLooper()).postDelayed({
            if (viewModel.uiState.value.pendingPhoneNumberInvitations.isNotEmpty()) {
                invitePhoneContacts(viewModel.uiState.value.pendingPhoneNumberInvitations)
            }
            setResult(RESULT_OK, result)
            finish()
        }, 2000)
    }

    /**
     * Initialize the contents of the Activity's standard options menu. You should place your menu items in to menu.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Timber.d("onCreateOptionsMenu")
        menuInflater.inflate(R.menu.activity_invite_contact, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.action_my_qr -> initMyQr()
            R.id.action_more -> {
                Timber.i("more button clicked - share invitation through other app")
                val message = resources.getString(
                    R.string.invite_contacts_to_start_chat_text_message,
                    viewModel.uiState.value.contactLink
                )
                val sendIntent = Intent().apply {
                    setAction(Intent.ACTION_SEND)
                    putExtra(Intent.EXTRA_TEXT, message)
                    setType(Constants.TYPE_TEXT_PLAIN)
                }
                startActivity(
                    Intent.createChooser(
                        sendIntent,
                        getString(R.string.invite_contact_chooser_title)
                    )
                )
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Called to retrieve per-instance state from an activity before being killed
     */
    override fun onSaveInstanceState(outState: Bundle) {
        Timber.d("onSaveInstanceState")
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_PERMISSION_GRANTED, isPermissionGranted)
        outState.putBoolean(KEY_IS_GET_CONTACT_COMPLETED, isGetContactCompleted)
    }

    override fun onBackPressed() {
        Timber.d("onBackPressed")
        val psaWebBrowser = psaWebBrowser
        if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return
        finish()
    }

    private fun setTitleAB() {
        Timber.d("setTitleAB")
        actionBar?.subtitle = if (viewModel.uiState.value.selectedContactInformation.isNotEmpty()) {
            resources.getQuantityString(
                R.plurals.general_selection_num_contacts,
                viewModel.uiState.value.selectedContactInformation.size,
                viewModel.uiState.value.selectedContactInformation.size
            )
        } else {
            null
        }
    }

    private fun setRecyclersVisibility() {
        Timber.d("setRecyclersVisibility ${viewModel.allContacts.size}")
        binding.containerListContacts.visibility = if (viewModel.allContacts.isNotEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun queryIfHasReadContactsPermissions() {
        Timber.d("queryIfHasReadContactsPermissions")
        if (hasPermissions(this, Manifest.permission.READ_CONTACTS)) {
            isPermissionGranted = true
            prepareToGetContacts()
        } else {
            requestPermission(
                this,
                REQUEST_READ_CONTACTS,
                Manifest.permission.READ_CONTACTS
            )
        }
    }

    private fun prepareToGetContacts() {
        Timber.d("prepareToGetContacts")
        setEmptyStateVisibility(true)
        binding.inviteContactProgressBar.visibility = View.VISIBLE
        viewModel.initializeContacts()
    }

    private fun visibilityFastScroller() {
        Timber.d("visibilityFastScroller")
        binding.fastScroller.setRecyclerView(binding.inviteContactList)
        binding.fastScroller.visibility =
            if (viewModel.allContacts.size < MIN_LIST_SIZE_FOR_FAST_SCROLLER) {
                View.GONE
            } else {
                View.VISIBLE
            }
    }

    private fun setPhoneAdapterContacts(contacts: List<InvitationContactInfo>) {
        Timber.d("setPhoneAdapterContacts")
        invitationContactsAdapter?.let {
            binding.inviteContactList.post {
                it.setContactData(contacts)
                it.notifyDataSetChanged()
            }
        }
    }

    private fun showEmptyTextView() {
        Timber.d("showEmptyTextView")
        var textToShow = getString(R.string.context_empty_contacts)
        try {
            textToShow = textToShow.replace(
                "[A]", "<font color=\'"
                        + getColorHexString(this, R.color.grey_900_grey_100)
                        + "\'>"
            ).replace("[/A]", "</font>").replace(
                "[B]", "<font color=\'"
                        + getColorHexString(this, R.color.grey_300_grey_600)
                        + "\'>"
            ).replace("[/B]", "</font>")
        } catch (e: Exception) {
            Timber.e(e)
        }

        val result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY)
        binding.inviteContactListEmptyText.text = result
    }

    private fun setEmptyStateVisibility(visible: Boolean) {
        Timber.d("setEmptyStateVisibility")
        if (visible) {
            binding.inviteContactListEmptyImage.visibility = View.VISIBLE
            binding.inviteContactListEmptyText.visibility = View.VISIBLE
            binding.inviteContactListEmptySubtext.visibility = View.GONE
        } else {
            binding.inviteContactListEmptyImage.visibility = View.GONE
            binding.inviteContactListEmptyText.visibility = View.GONE
            binding.inviteContactListEmptySubtext.visibility = View.GONE
        }
    }

    /**
     * Open the [QRCodeComposeActivity]
     */
    fun initScanQR() {
        Timber.d("initScanQR")
        Intent(this, QRCodeComposeActivity::class.java).apply {
            putExtra(Constants.OPEN_SCAN_QR, true)
            startQRActivity(this)
        }
    }

    private fun initMyQr() {
        startQRActivity(Intent(this, QRCodeComposeActivity::class.java))
    }

    private fun startQRActivity(intent: Intent) {
        startActivityForResult(intent, SCAN_QR_FOR_INVITE_CONTACTS)
    }

    private fun refreshKeyboard() {
        Timber.d("refreshKeyboard")
        val imeOptions = binding.typeMailEditText.imeOptions
        binding.typeMailEditText.setImeOptions(EditorInfo.IME_ACTION_DONE)

        val imeOptionsNew = binding.typeMailEditText.imeOptions
        if (imeOptions != imeOptionsNew) {
            val view = currentFocus
            if (view != null) {
                val inputMethodManager =
                    getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.restartInput(view)
            }
        }
    }

    private fun isValidEmail(target: CharSequence?): Boolean {
        val result = target != null && Constants.EMAIL_ADDRESS.matcher(target).matches()
        Timber.d("isValidEmail%s", result)
        return result
    }

    private fun isValidPhone(target: CharSequence?): Boolean {
        val result = target != null && Constants.PHONE_NUMBER_REGEX.matcher(target).matches()
        Timber.d("isValidPhone%s", result)
        return result
    }

    /**
     * Callback for the result from requesting permissions.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        Timber.d("onRequestPermissionsResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Timber.d("Permission granted")
                isPermissionGranted = true
                prepareToGetContacts()
                binding.noPermissionHeader.visibility = View.GONE
            } else {
                Timber.d("Permission denied")
                setEmptyStateVisibility(true)
                binding.inviteContactListEmptyText.setText(R.string.no_contacts_permissions)
                binding.inviteContactListEmptyImage.visibility = View.VISIBLE
                binding.inviteContactProgressBar.visibility = View.GONE
                binding.noPermissionHeader.visibility = View.VISIBLE
            }
        }
    }

    private var lastSelectedContactsSize = 0
    private fun refreshComponents() {
        val shouldScroll =
            if (lastSelectedContactsSize > viewModel.uiState.value.selectedContactInformation.size) {
                // If there are contact removals, don't scroll.
                lastSelectedContactsSize = viewModel.uiState.value.selectedContactInformation.size
                false
            } else true
        refreshAddedContactsView(shouldScroll)
        refreshInviteContactButton()
        //clear input text view after selection
        binding.typeMailEditText.setText("")
        setTitleAB()
    }

    /**
     * Called when a single contact item.
     *
     * @param position The item position in recycler view.
     */
    override fun onItemClick(position: Int) {
        invitationContactsAdapter?.let {
            val contactInfo = it.getItem(position)
            Timber.d("on Item click at %d name is %s", position, contactInfo.getContactName())
            viewModel.validateContactListItemClick(contactInfo)
        }
    }

    private fun refreshHorizontalScrollView() {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.scroller.fullScroll(View.FOCUS_RIGHT)
        }, 100)
    }

    private fun onGetContactCompleted() {
        isGetContactCompleted = true
        binding.inviteContactProgressBar.visibility = View.GONE
        refreshList()
        setRecyclersVisibility()
        visibilityFastScroller()

        if (viewModel.allContacts.isNotEmpty()) {
            setEmptyStateVisibility(false)
        } else {
            showEmptyTextView()
        }
    }

    private fun createContactTextView(name: String, viewId: Int): View {
        Timber.d("createTextView contact name is %s", name)
        val params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(
            Util.dp2px(ADDED_CONTACT_VIEW_MARGIN_LEFT.toFloat(), displayMetrics),
            0,
            0,
            0
        )

        val rowView = SelectedContactItemBinding.inflate(layoutInflater)
        with(rowView.root) {
            layoutParams = params
            id = viewId
            isClickable = true
            setOnClickListener { v: View ->
                val invitationContactInfo = viewModel.uiState.value.selectedContactInformation[v.id]
                viewModel.removeSelectedContactInformation(invitationContactInfo)
                if (invitationContactInfo.hasMultipleContactInfos()) {
                    viewModel.toggleContactHighlightedInfo(
                        contactInfo = invitationContactInfo,
                        value = viewModel.uiState.value.selectedContactInformation.any { it.id == invitationContactInfo.id }
                    )
                } else {
                    viewModel.toggleContactHighlightedInfo(
                        contactInfo = invitationContactInfo,
                        value = false
                    )
                }
                refreshInviteContactButton()
                refreshList()
                setTitleAB()
            }
        }

        rowView.contactName.text = name
        return rowView.root
    }

    private fun refreshAddedContactsView(shouldScroll: Boolean) {
        Timber.d("refreshAddedContactsView")
        binding.labelContainer.removeAllViews()
        viewModel.uiState.value.selectedContactInformation.forEachIndexed { index, contact ->
            val displayedLabel = contact.getContactName()
            binding.labelContainer.addView(createContactTextView(displayedLabel, index))
        }
        binding.labelContainer.invalidate()
        if (shouldScroll) {
            refreshHorizontalScrollView()
        } else {
            binding.scroller.clearFocus()
        }
    }

    private fun refreshList() {
        Timber.d("refresh list")
        setPhoneAdapterContacts(viewModel.uiState.value.filteredContacts)
    }

    private fun refreshInviteContactButton() {
        Timber.d("refreshInviteContactButton")
        val stringInEditText = binding.typeMailEditText.text.toString()
        val isStringValidNow = (stringInEditText.isEmpty()
                || isValidEmail(stringInEditText)
                || isValidPhone(stringInEditText))
        enableFabButton(viewModel.uiState.value.selectedContactInformation.isNotEmpty() && isStringValidNow)
    }

    private fun invitePhoneContacts(phoneNumbers: List<String>) {
        Timber.d("invitePhoneContacts")
        val recipient = buildString {
            append("smsto:")
            phoneNumbers.forEach { phone ->
                append(phone)
                append(";")
                Timber.d("setResultPhoneContacts: $phone")
            }
        }
        val smsBody = resources.getString(
            R.string.invite_contacts_to_start_chat_text_message,
            viewModel.uiState.value.contactLink
        )
        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse(recipient))
        smsIntent.putExtra("sms_body", smsBody)
        startActivity(smsIntent)
    }

    /**
     * Show a snack bar
     */
    fun showSnackBar(message: String) {
        showSnackbar(Constants.SNACKBAR_TYPE, binding.scroller, message, -1)
    }

    private fun enableFabButton(enableFabButton: Boolean) {
        Timber.d("enableFabButton: $enableFabButton")
        binding.fabButtonNext.isEnabled = enableFabButton
    }

    companion object {
        private const val SCAN_QR_FOR_INVITE_CONTACTS = 1111
        private const val KEY_IS_PERMISSION_GRANTED = "KEY_IS_PERMISSION_GRANTED"
        private const val KEY_IS_GET_CONTACT_COMPLETED = "KEY_IS_GET_CONTACT_COMPLETED"
        private const val MIN_LIST_SIZE_FOR_FAST_SCROLLER = 20
        private const val ADDED_CONTACT_VIEW_MARGIN_LEFT = 10
    }
}
