package mega.privacy.android.app.presentation.contact.invite.contact

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
import android.os.Parcelable
import android.text.Html
import android.util.DisplayMetrics
import android.view.KeyEvent
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
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doBeforeTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.ContactInfoListDialog
import mega.privacy.android.app.components.ContactInfoListDialog.OnMultipleSelectedListener
import mega.privacy.android.app.components.ContactsDividerDecoration
import mega.privacy.android.app.components.scrollBar.FastScrollerScrollListener
import mega.privacy.android.app.databinding.ActivityInviteContactBinding
import mega.privacy.android.app.databinding.SelectedContactItemBinding
import mega.privacy.android.app.main.InvitationContactInfo
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_MANUAL_INPUT_EMAIL
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_MANUAL_INPUT_PHONE
import mega.privacy.android.app.main.InvitationContactInfo.Companion.createManualInput
import mega.privacy.android.app.main.InviteContactViewModel
import mega.privacy.android.app.main.adapters.InvitationContactsAdapter
import mega.privacy.android.app.presentation.qrcode.QRCodeComposeActivity
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.REQUEST_READ_CONTACTS
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.contacts.ContactsFilter.isEmailInContacts
import mega.privacy.android.app.utils.contacts.ContactsFilter.isEmailInPending
import mega.privacy.android.app.utils.contacts.ContactsFilter.isMySelf
import mega.privacy.android.app.utils.contacts.ContactsFilter.isTheSameContact
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber

/**
 * Invite contact activity.
 */
@AndroidEntryPoint
class InviteContactActivity : PasscodeActivity(), OnMultipleSelectedListener,
    MegaRequestListenerInterface, InvitationContactsAdapter.OnItemClickListener {

    private val viewModel: InviteContactViewModel by viewModels()

    private var displayMetrics: DisplayMetrics? = null
    private var actionBar: ActionBar? = null
    private var invitationContactsAdapter: InvitationContactsAdapter? = null

    private var listDialog: ContactInfoListDialog? = null
    private var currentSelected: InvitationContactInfo? = null

    private var phoneContacts: MutableList<InvitationContactInfo> = mutableListOf()
    private var addedContacts: MutableList<InvitationContactInfo> = mutableListOf()
    private var contactsEmailsSelected: MutableList<String> = mutableListOf()
    private var contactsPhoneSelected: MutableList<String> = mutableListOf()

    private var fromAchievement = false
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
                viewModel.filterUiState.value.filteredContacts,
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
            fromAchievement = savedInstanceState.getBoolean(KEY_FROM, false)
        } else {
            fromAchievement = intent.getBooleanExtra(KEY_FROM, false)
        }
        Timber.d("Request by Achievement: $fromAchievement")
        if (isGetContactCompleted) {
            if (savedInstanceState != null) {
                phoneContacts =
                    savedInstanceState.getParcelableArrayList(KEY_PHONE_CONTACTS) ?: mutableListOf()
                addedContacts =
                    savedInstanceState.getParcelableArrayList(KEY_ADDED_CONTACTS) ?: mutableListOf()
                isPermissionGranted =
                    savedInstanceState.getBoolean(KEY_IS_PERMISSION_GRANTED, false)
                currentSelected = savedInstanceState.getParcelable(CURRENT_SELECTED_CONTACT)
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

            currentSelected?.let { contactInfo ->
                listDialog = ContactInfoListDialog(this, contactInfo, this)
                savedInstanceState?.let {
                    listDialog?.checkedIndex = it.getIntegerArrayList(CHECKED_INDEX)
                    val selectedList =
                        it.getParcelableArrayList<InvitationContactInfo>(SELECTED_CONTACT_INFO)
                    selectedList?.let {
                        listDialog?.selected = HashSet(selectedList)
                    }
                    val unSelectedList =
                        it.getParcelableArrayList<InvitationContactInfo>(UNSELECTED)
                    unSelectedList?.let {
                        listDialog?.unSelected = HashSet(unSelectedList)
                    }
                }
                listDialog?.showInfo(ArrayList(addedContacts), true)
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
            inviteContacts(addedContacts)
            Util.hideKeyboard(this, 0)
        }

        binding.typeMailEditText.apply {
            doBeforeTextChanged { _, _, _, _ -> refreshKeyboard() }

            doOnTextChanged { text, start, before, count ->
                Timber.d("onTextChanged: s is $text start: $start before: $before count: $count")
                if (!text.isNullOrBlank()) {
                    val last = text[text.length - 1]
                    if (last == ' ') {
                        val processedString = text.toString().trim()
                        if (isValidEmail(processedString)) {
                            addContactInfo(processedString, TYPE_MANUAL_INPUT_EMAIL)
                            binding.typeMailEditText.text.clear()
                        } else if (isValidPhone(processedString)) {
                            addContactInfo(processedString, TYPE_MANUAL_INPUT_PHONE)
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

            setOnEditorActionListener { textView, actionId, event ->
                refreshKeyboard()
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val processedStrong = textView.text.toString().trim()
                    if (processedStrong.isNotBlank()) {
                        binding.typeMailEditText.text.clear()
                        val isEmailValid = isValidEmail(processedStrong)
                        val isPhoneValid = isValidPhone(processedStrong)
                        if (isEmailValid) {
                            val result = checkInputEmail(processedStrong)
                            if (result != null) {
                                Util.hideKeyboard(this@InviteContactActivity, 0)
                                showSnackBar(result)
                                return@setOnEditorActionListener true
                            }
                            addContactInfo(processedStrong, TYPE_MANUAL_INPUT_EMAIL)
                        } else if (isPhoneValid) {
                            addContactInfo(processedStrong, TYPE_MANUAL_INPUT_PHONE)
                        }
                        if (isEmailValid || isPhoneValid) {
                            Util.hideKeyboard(this@InviteContactActivity, 0)
                        } else {
                            Toast.makeText(
                                this@InviteContactActivity,
                                R.string.invalid_input,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnEditorActionListener true
                        }
                        if (!Util.isScreenInPortrait(this@InviteContactActivity)) {
                            Util.hideKeyboard(this@InviteContactActivity, 0)
                        }
                        viewModel.filterContacts(binding.typeMailEditText.text.toString())
                    }
                    Util.hideKeyboard(this@InviteContactActivity, 0)
                    refreshInviteContactButton()
                    return@setOnEditorActionListener true
                }
                if ((event != null && (event.keyCode == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_SEND)) {
                    if (addedContacts.isEmpty()) {
                        Util.hideKeyboard(this@InviteContactActivity, 0)
                    } else {
                        inviteContacts(addedContacts)
                    }
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }

            setImeOptions(EditorInfo.IME_ACTION_DONE)
        }

        binding.layoutScanQr.setOnClickListener {
            Timber.d("Scan QR code pressed")
            if (CallUtil.isNecessaryDisableLocalCamera() != MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
                CallUtil.showConfirmationOpenCamera(this, Constants.ACTION_OPEN_QR, true)
            } else {
                initScanQR()
            }
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
        collectFlow(viewModel.uiState) { uiState ->
            if (uiState.onContactsInitialized) {
                onGetContactCompleted()
                viewModel.filterContacts(binding.typeMailEditText.text.toString())
                viewModel.resetOnContactsInitializedState()
            }
        }

        collectFlow(viewModel.filterUiState) {
            refreshList()
            visibilityFastScroller()
        }
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
        outState.putParcelableArrayList(
            KEY_PHONE_CONTACTS,
            ArrayList(phoneContacts)
        )
        outState.putParcelableArrayList(
            KEY_ADDED_CONTACTS,
            ArrayList(addedContacts)
        )
        outState.putBoolean(KEY_IS_PERMISSION_GRANTED, isPermissionGranted)
        outState.putBoolean(KEY_IS_GET_CONTACT_COMPLETED, isGetContactCompleted)
        outState.putBoolean(KEY_FROM, fromAchievement)
        outState.putParcelable(CURRENT_SELECTED_CONTACT, currentSelected)
        listDialog?.let {
            outState.putIntegerArrayList(CHECKED_INDEX, it.checkedIndex)
            outState.putParcelableArrayList(
                SELECTED_CONTACT_INFO,
                ArrayList<Parcelable>(it.selected)
            )
            outState.putParcelableArrayList(UNSELECTED, ArrayList<Parcelable>(it.unSelected))
        }
    }

    override fun onBackPressed() {
        Timber.d("onBackPressed")
        val psaWebBrowser = psaWebBrowser
        if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return
        finish()
    }

    /**
     * Perform any final cleanup before an activity is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        listDialog?.recycle()
    }

    private fun setTitleAB() {
        Timber.d("setTitleAB")
        if (addedContacts.isNotEmpty()) {
            actionBar?.setSubtitle(
                resources.getQuantityString(
                    R.plurals.general_selection_num_contacts,
                    addedContacts.size,
                    addedContacts.size
                )
            )
        } else {
            actionBar?.subtitle = null
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

    private fun checkInputEmail(email: String): String? = when {
        isMySelf(megaApi, email) -> {
            getString(R.string.error_own_email_as_contact)
        }

        isEmailInContacts(megaApi, email) -> {
            getString(R.string.context_contact_already_exists, email)
        }

        isEmailInPending(megaApi, email) -> {
            getString(R.string.invite_not_sent_already_sent, email)
        }

        else -> null
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

    /**
     * Called when the user select/deselect multiple contacts.
     *
     * @param selected Set of selected contacts.
     * @param toRemove Set of contacts that need to be removed.
     */
    override fun onSelect(
        selected: Set<InvitationContactInfo>,
        toRemove: Set<InvitationContactInfo>,
    ) {
        var id = -1L
        cancel()
        for (select in selected) {
            id = select.id
            if (!isContactAdded(select)) {
                addedContacts.add(select)
            }
        }
        for (select in toRemove) {
            id = select.id
            addedContacts.removeIf { isTheSameContact(it, select) }
        }
        controlHighlighted(id)
        refreshComponents(selected.size > toRemove.size)
    }

    /**
     * Clear the current selected contacts.
     */
    override fun cancel() {
        currentSelected = null
    }

    private fun refreshComponents(shouldScroll: Boolean) {
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
            if (contactInfo.hasMultipleContactInfos()) {
                this.currentSelected = contactInfo
                listDialog = ContactInfoListDialog(this, contactInfo, this)
                listDialog?.showInfo(ArrayList(addedContacts), false)
            } else {
                viewModel.toggleContactHighlightedInfo(contactInfo)
                val singleInvitationContactInfo =
                    viewModel.filterUiState.value.filteredContacts[position]
                if (isContactAdded(singleInvitationContactInfo)) {
                    addedContacts.removeIf { isTheSameContact(it, singleInvitationContactInfo) }
                    refreshComponents(false)
                } else {
                    addedContacts.add(singleInvitationContactInfo)
                    refreshComponents(true)
                }
            }
        }
    }

    private fun refreshHorizontalScrollView() {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.scroller.fullScroll(View.FOCUS_RIGHT)
        }, 100)
    }

    private fun onGetContactCompleted() {
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
                val invitationContactInfo = addedContacts[v.id]
                addedContacts.removeAt(viewId)
                if (invitationContactInfo.hasMultipleContactInfos()) {
                    controlHighlighted(invitationContactInfo.id)
                } else {
                    viewModel.toggleContactHighlightedInfo(invitationContactInfo, false)
                }
                refreshAddedContactsView(false)
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
        addedContacts.forEachIndexed { index, contact ->
            val displayedLabel = contact.getContactName().ifBlank { contact.displayInfo }
            binding.labelContainer.addView(createContactTextView(displayedLabel, index))
        }
        binding.labelContainer.invalidate()
        if (shouldScroll) {
            refreshHorizontalScrollView()
        } else {
            binding.scroller.clearFocus()
        }
    }

    private fun controlHighlighted(id: Long) {
        var shouldHighlighted = false
        for ((addedId) in addedContacts) {
            if (addedId == id) {
                shouldHighlighted = true
                break
            }
        }
        invitationContactsAdapter?.data?.forEach {
            if (it.id == id) {
                viewModel.toggleContactHighlightedInfo(it, shouldHighlighted)
            }
        }
    }

    private fun isContactAdded(invitationContactInfo: InvitationContactInfo): Boolean {
        Timber.d("isContactAdded contact name is %s", invitationContactInfo.getContactName())
        for (addedContact in addedContacts) {
            if (isTheSameContact(addedContact, invitationContactInfo)) {
                return true
            }
        }
        return false
    }

    private fun refreshList() {
        Timber.d("refresh list")
        setPhoneAdapterContacts(viewModel.filterUiState.value.filteredContacts)
    }

    private fun refreshInviteContactButton() {
        Timber.d("refreshInviteContactButton")
        val stringInEditText = binding.typeMailEditText.text.toString()
        val isStringValidNow = (stringInEditText.isEmpty()
                || isValidEmail(stringInEditText)
                || isValidPhone(stringInEditText))
        enableFabButton(addedContacts.isNotEmpty() && isStringValidNow)
    }

    private fun addContactInfo(inputString: String, type: Int) {
        Timber.d("addContactInfo inputString is %s type is %d", inputString, type)
        var info: InvitationContactInfo? = null
        if (type == TYPE_MANUAL_INPUT_EMAIL) {
            info = createManualInput(
                inputString,
                TYPE_MANUAL_INPUT_EMAIL,
                R.color.grey_500_grey_400
            )
        } else if (type == TYPE_MANUAL_INPUT_PHONE) {
            info = createManualInput(
                inputString,
                TYPE_MANUAL_INPUT_PHONE,
                R.color.grey_500_grey_400
            )
        }
        if (info != null) {
            val index = isUserEnteredContactExistInList(info)
            val holder = binding.inviteContactList.findViewHolderForAdapterPosition(index)
            if (index >= 0 && holder != null) {
                holder.itemView.performClick()
            } else if (!isContactAdded(info)) {
                addedContacts.add(info)
                refreshAddedContactsView(true)
            }
        }
        setTitleAB()
    }

    private fun inviteContacts(addedContacts: List<InvitationContactInfo>?) {
        // Email/phone contacts to be invited
        contactsEmailsSelected = mutableListOf()
        contactsPhoneSelected = mutableListOf()

        addedContacts?.forEach { contact ->
            if (contact.isEmailContact()) {
                contactsEmailsSelected.add(contact.displayInfo)
            } else {
                contactsPhoneSelected.add(contact.displayInfo)
            }
        }

        if (contactsEmailsSelected.isNotEmpty()) {
            //phone contact will be invited once email done
            inviteEmailContacts(ArrayList(contactsEmailsSelected))
        } else if (contactsPhoneSelected.isNotEmpty()) {
            invitePhoneContacts(ArrayList(contactsPhoneSelected))
            finish()
        } else {
            finish()
        }
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

    private var numberToSend = 0
    private var numberSent = 0
    private var numberNotSent = 0

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) = Unit

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) = Unit

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type == MegaRequest.TYPE_INVITE_CONTACT) {
            Timber.d("MegaRequest.TYPE_INVITE_CONTACT finished: ${request.number}")
            if (e.errorCode == MegaError.API_OK) {
                numberSent++
                Timber.d("OK INVITE CONTACT: ${request.email}")
            } else {
                numberNotSent++
                Timber.d("ERROR: ${e.errorCode}___${e.errorString}")
            }
            if (numberSent + numberNotSent == numberToSend) {
                val result = Intent()
                if (numberToSend == 1 && numberSent == 1) {
                    if (!fromAchievement) {
                        showSnackBar(
                            getString(
                                R.string.context_contact_request_sent,
                                request.email
                            )
                        )
                    } else {
                        result.putExtra(KEY_SENT_EMAIL, request.email)
                    }
                } else {
                    if (numberNotSent > 0 && !fromAchievement) {
                        val requestsSent = resources.getQuantityString(
                            R.plurals.contact_snackbar_invite_contact_requests_sent,
                            numberSent,
                            numberSent
                        )
                        val requestsNotSent = resources.getQuantityString(
                            R.plurals.contact_snackbar_invite_contact_requests_not_sent,
                            numberNotSent,
                            numberNotSent
                        )
                        showSnackBar(requestsSent + requestsNotSent)
                    } else {
                        if (!fromAchievement) {
                            showSnackBar(
                                resources.getQuantityString(
                                    R.plurals.number_correctly_invite_contact_request,
                                    numberToSend,
                                    numberToSend
                                )
                            )
                        } else {
                            result.putExtra(KEY_SENT_NUMBER, numberSent)
                        }
                    }
                }

                Util.hideKeyboard(this@InviteContactActivity, 0)
                Handler(Looper.getMainLooper()).postDelayed({
                    if (contactsPhoneSelected.isNotEmpty()) {
                        invitePhoneContacts(ArrayList(contactsPhoneSelected))
                    }
                    numberSent = 0
                    numberToSend = 0
                    numberNotSent = 0
                    setResult(RESULT_OK, result)
                    finish()
                }, 2000)
            }
        }
    }

    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {}

    private fun inviteEmailContacts(emails: List<String>) {
        numberToSend = emails.size
        emails.forEach {
            Timber.d("setResultEmailContacts: $it")
            megaApi.inviteContact(it, null, MegaContactRequest.INVITE_ACTION_ADD, this)
        }
    }

    /**
     * Show a snack bar
     */
    fun showSnackBar(message: String) {
        showSnackbar(Constants.SNACKBAR_TYPE, binding.scroller, message, -1)
    }

    private fun isUserEnteredContactExistInList(userEnteredInfo: InvitationContactInfo): Int {
        if (invitationContactsAdapter == null) return USER_INDEX_NONE_EXIST

        val list = invitationContactsAdapter!!.data
        for (i in list.indices) {
            if (userEnteredInfo.displayInfo.equals(list[i].displayInfo, ignoreCase = true)) {
                return i
            }
        }

        return USER_INDEX_NONE_EXIST
    }

    private fun enableFabButton(enableFabButton: Boolean) {
        Timber.d("enableFabButton: $enableFabButton")
        binding.fabButtonNext.isEnabled = enableFabButton
    }

    companion object {
        internal const val KEY_FROM: String = "fromAchievement"
        internal const val KEY_SENT_NUMBER: String = "sentNumber"

        private const val SCAN_QR_FOR_INVITE_CONTACTS = 1111
        private const val KEY_PHONE_CONTACTS = "KEY_PHONE_CONTACTS"
        private const val KEY_ADDED_CONTACTS = "KEY_ADDED_CONTACTS"
        private const val KEY_IS_PERMISSION_GRANTED = "KEY_IS_PERMISSION_GRANTED"
        private const val KEY_IS_GET_CONTACT_COMPLETED = "KEY_IS_GET_CONTACT_COMPLETED"
        private const val CURRENT_SELECTED_CONTACT = "CURRENT_SELECTED_CONTACT"
        private const val CHECKED_INDEX = "CHECKED_INDEX"
        private const val SELECTED_CONTACT_INFO = "SELECTED_CONTACT_INFO"
        private const val UNSELECTED = "UNSELECTED"
        private const val USER_INDEX_NONE_EXIST = -1
        private const val MIN_LIST_SIZE_FOR_FAST_SCROLLER = 20
        private const val ADDED_CONTACT_VIEW_MARGIN_LEFT = 10
        private const val KEY_SENT_EMAIL: String = "sentEmail"
    }
}
