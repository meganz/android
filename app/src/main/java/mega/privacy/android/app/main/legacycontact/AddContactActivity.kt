package mega.privacy.android.app.main.legacycontact

import mega.privacy.android.shared.resources.R as sharedR
import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract
import android.text.Editable
import android.text.Html
import android.text.InputFilter
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaContactAdapter
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.HeaderItemDecoration
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.components.scrollBar.FastScroller
import mega.privacy.android.app.components.scrollBar.FastScrollerScrollListener
import mega.privacy.android.app.components.twemoji.EmojiEditText
import mega.privacy.android.app.extensions.consumeInsetsWithToolbar
import mega.privacy.android.app.main.PhoneContactInfo
import mega.privacy.android.app.main.ShareContactInfo
import mega.privacy.android.app.main.adapters.AddContactsAdapter
import mega.privacy.android.app.main.adapters.MegaAddContactsAdapter
import mega.privacy.android.app.main.adapters.MegaContactsAdapter
import mega.privacy.android.app.main.adapters.PhoneContactsAdapter
import mega.privacy.android.app.main.adapters.ShareContactsAdapter
import mega.privacy.android.app.main.adapters.ShareContactsHeaderAdapter
import mega.privacy.android.app.main.model.AddContactState
import mega.privacy.android.app.presentation.meeting.model.MeetingState
import mega.privacy.android.app.presentation.meeting.view.ParticipantsLimitWarningView
import mega.privacy.android.app.presentation.qrcode.QRCodeComposeActivity
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.ColorUtils.setErrorAwareInputAppearance
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.android.domain.usecase.contact.MonitorChatPresenceLastGreenUpdatesUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaEvent
import nz.mega.sdk.MegaGlobalListenerInterface
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElement
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import timber.log.Timber
import javax.inject.Inject

/**
 * Add contact activity
 *
 * @constructor Create empty Add contact activity
 */
@AndroidEntryPoint
class AddContactActivity : PasscodeActivity(), View.OnClickListener,
    RecyclerView.OnItemTouchListener, TextWatcher, TextView.OnEditorActionListener,
    MegaRequestListenerInterface, MegaGlobalListenerInterface {

    /**
     * Monitor user last green updates use case
     */
    @Inject
    lateinit var monitorChatPresenceLastGreenUpdatesUseCase: MonitorChatPresenceLastGreenUpdatesUseCase

    /**
     * Navigator
     */
    @Inject
    lateinit var navigator: MegaNavigator

    private val viewModel by viewModels<AddContactViewModel>()

    /**
     * Contact type
     */
    var contactType: Int = 0

    private var emailsContactsSelected: ArrayList<String> = ArrayList()

    // Determine if open this page from meeting
    private var isFromMeeting = false

    private var multipleSelectIntent = 0
    private var nodeHandle: Long = -1
    private var nodeHandles: LongArray = LongArray(0)
    private var chatId: Long = -1

    private var maxParticipants = -1

    private var addContactActivity: AddContactActivity? = null

    private var containerContacts: RelativeLayout? = null
    private var recyclerViewList: RecyclerView? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var emptyImageView: ImageView? = null
    private var emptyTextView: TextView? = null
    private var emptySubTextView: TextView? = null
    private var emptyInviteButton: Button? = null

    /**
     * Progress bar
     */
    var progressBar: ProgressBar? = null
    private var addedContactsRecyclerView: RecyclerView? = null
    private var containerAddedContactsRecyclerView: RelativeLayout? = null
    private var mLayoutManager: LinearLayoutManager? = null

    private var mWarningMessage: TextView? = null

    /**
     * Input string
     */
    var inputString: String = ""
    private var savedInputString: String = ""

    /**
     * Adapter list MEGA contacts
     */
    private var adapterMEGA: MegaContactsAdapter? = null

    /**
     * Adapter list chips MEGA contacts
     */
    var adapterMEGAContacts: MegaAddContactsAdapter? = null

    private var contactsMEGA: ArrayList<MegaUser> = ArrayList()

    /**
     * Visible contacts m e g a
     */
    var visibleContactsMEGA: ArrayList<MegaContactAdapter> = ArrayList()

    /**
     * Filtered contact m e g a
     */
    var filteredContactMEGA: ArrayList<MegaContactAdapter> = ArrayList()

    /**
     * Added contacts m e g a
     */
    var addedContactsMEGA: ArrayList<MegaContactAdapter> = ArrayList()

    /**
     * Query contact m e g a
     */
    var queryContactMEGA: ArrayList<MegaContactAdapter> = ArrayList()


    //    Adapter list Phone contacts
    private var adapterPhone: PhoneContactsAdapter? = null

    //    Adapter list chips Phone contacts
    private var adapterContacts: AddContactsAdapter? = null

    private var phoneContacts: ArrayList<PhoneContactInfo> = ArrayList()

    /**
     * Added contacts phone
     */
    var addedContactsPhone: ArrayList<PhoneContactInfo> = ArrayList()

    /**
     * Filtered contacts phone
     */
    var filteredContactsPhone: ArrayList<PhoneContactInfo> = ArrayList()

    /**
     * Query contacts phone
     */
    var queryContactsPhone: ArrayList<PhoneContactInfo> = ArrayList()

    //    Adapter list Share contacts
    private var adapterShareHeader: ShareContactsHeaderAdapter? = null

    //    Adapter list chips MEGA/Phone contacts
    private var adapterShare: ShareContactsAdapter? = null

    /**
     * Added contacts share
     */
    var addedContactsShare: ArrayList<ShareContactInfo> = ArrayList()

    /**
     * Share contacts
     */
    var shareContacts: ArrayList<ShareContactInfo> = ArrayList()

    /**
     * Filtered contacts share
     */
    var filteredContactsShare: ArrayList<ShareContactInfo> = ArrayList()


    private var relativeLayout: RelativeLayout? = null

    private var participantsLimitWarningView: ParticipantsLimitWarningView? = null

    /**
     * Saved added contacts
     */
    var savedAddedContacts: ArrayList<String> = ArrayList()

    private var sendInvitationMenuItem: MenuItem? = null
    private var scanQrMenuItem: MenuItem? = null

    private var comesFromChat = false
    private var isStartConversation = false
    private var comesFromRecent = false
    private var headerContacts: RelativeLayout? = null
    private var textHeader: TextView? = null

    private var fromAchievements = false
    private var mailsFromAchievements: ArrayList<String> = ArrayList()

    private var searchMenuItem: MenuItem? = null

    /**
     * Search expand
     */
    var searchExpand: Boolean = false

    /**
     * Filter contacts task
     */
    var filterContactsTask: FilterContactsTask? = null
    private var getContactsTask: GetContactsTask? = null

    /**
     * Get phone contacts task
     */
    var getPhoneContactsTask: GetPhoneContactsTask? = null
    private var recoverContactsTask: RecoverContactsTask? = null

    /**
     * Query if contact should be added task
     */
    var queryIfContactShouldBeAddedTask: QueryIfContactShouldBeAddedTask? = null

    private var fastScroller: FastScroller? = null

    private var fabImageGroup: FloatingActionButton? = null
    private var fabButton: FloatingActionButton? = null
    private var nameGroup: EmojiEditText? = null

    /**
     * On new group
     */
    var onNewGroup: Boolean = false
    private var isConfirmDeleteShown = false
    private var confirmDeleteMail: String? = null

    private var mailError: RelativeLayout? = null
    private var typeContactLayout: RelativeLayout? = null

    /**
     * Type contact edit text
     */
    var typeContactEditText: EditText? = null
    private var scanQRButton: RelativeLayout? = null

    /**
     * Is confirm add shown
     */
    var isConfirmAddShown: Boolean = false

    /**
     * Confirm add mail
     */
    var confirmAddMail: String? = null
    private var createNewGroup = false
    private var title: String? = ""

    /**
     * Query permissions
     */
    var queryPermissions: Boolean = true

    private var addContactsLayout: LinearLayout? = null
    private var newGroupLayout: NestedScrollView? = null
    private var ekrSwitch: MegaSwitch? = null
    private var isEKREnabled = false
    private var getChatLinkLayout: RelativeLayout? = null
    private var getChatLinkBox: CheckBox? = null
    private var allowAddParticipantsSwitch: MegaSwitch? = null
    private var isAllowAddParticipantsEnabled = true
    private var newGroupLinearLayoutManager: LinearLayoutManager? = null
    private var newGroupRecyclerView: RecyclerView? = null
    private var newGroupHeaderList: TextView? = null

    /**
     * New group
     */
    var newGroup: Boolean = false

    /**
     * Contacts new group
     */
    var contactsNewGroup: ArrayList<String> = ArrayList()

    private var myContact: MegaContactAdapter? = null

    private var onlyCreateGroup = false

    /**
     * Waiting for phone contacts
     */
    var waitingForPhoneContacts: Boolean = false

    private var isContactVerificationOn = false

    private var isWarningMessageShown = false


    /**
     * Shows the fabButton
     */
    private fun showFabButton() {
        setSendInvitationVisibility()
    }

    override fun onGlobalSyncStateChanged(api: MegaApiJava) {
    }

    /**
     * Get share contact mail
     *
     * @param contact
     * @return
     */
    fun getShareContactMail(contact: ShareContactInfo): String? {
        var mail: String? = null

        if (contact.isMegaContact && !contact.isHeader) {
            if (contact.getMegaContactAdapter().megaUser != null && contact.getMegaContactAdapter().megaUser?.email != null) {
                mail = contact.getMegaContactAdapter().megaUser?.email
            } else if (contact.getMegaContactAdapter().contact != null && contact.getMegaContactAdapter().contact?.email != null) {
                mail = contact.getMegaContactAdapter().contact?.email
            }
        } else if (contact.isPhoneContact && !contact.isHeader) {
            mail = contact.phoneContactInfo.email
        } else {
            mail = contact.mail
        }

        return mail
    }

    /**
     * getDeviceContacts
     */
    fun getDeviceContacts() {
        if (queryPermissions) {
            phoneContacts.clear()
            filteredContactsPhone.clear()
            phoneContacts = getPhoneContacts()
            for (i in phoneContacts.indices) {
                filteredContactsPhone.add(phoneContacts[i])
            }
        }
    }

    /**
     * Get both contacts
     */
    fun getBothContacts() {
        getDeviceContacts()
        getVisibleMEGAContacts()
    }

    /**
     * Set added adapter contacts
     */
    fun setAddedAdapterContacts() {
        when (contactType) {
            Constants.CONTACT_TYPE_MEGA -> {
                if (adapterMEGAContacts == null) {
                    adapterMEGAContacts =
                        MegaAddContactsAdapter(addContactActivity, addedContactsMEGA)
                } else {
                    adapterMEGAContacts?.setContacts(addedContactsMEGA)
                }

                if (addedContactsMEGA.isEmpty()) {
                    containerAddedContactsRecyclerView?.visibility = View.GONE
                } else {
                    containerAddedContactsRecyclerView?.visibility = View.VISIBLE
                }

                addedContactsRecyclerView?.adapter = adapterMEGAContacts
            }

            Constants.CONTACT_TYPE_DEVICE -> {
                if (adapterContacts == null) {
                    adapterContacts = AddContactsAdapter(this, addedContactsPhone)
                } else {
                    adapterContacts?.setContacts(addedContactsPhone)
                }

                if (addedContactsPhone.isEmpty()) {
                    containerAddedContactsRecyclerView?.visibility = View.GONE
                } else {
                    containerAddedContactsRecyclerView?.visibility = View.VISIBLE
                }

                addedContactsRecyclerView?.adapter = adapterContacts
            }

            else -> {
                if (adapterShare == null) {
                    adapterShare = ShareContactsAdapter(addContactActivity, addedContactsShare)
                } else {
                    adapterShare?.setContacts(addedContactsShare)
                }

                if (addedContactsShare.size == 0) {
                    containerAddedContactsRecyclerView?.visibility = View.GONE
                } else {
                    containerAddedContactsRecyclerView?.visibility = View.VISIBLE
                }

                addedContactsRecyclerView?.adapter = adapterShare
            }
        }

        setSendInvitationVisibility()
    }

    /**
     * Set phone adapter contacts
     *
     * @param contacts
     */
    fun setPhoneAdapterContacts(contacts: ArrayList<PhoneContactInfo>?) {
        if (queryPermissions) {
            if (filteredContactsPhone.size == 0) {
                showHeader(false)
                var textToShow = String.format(getString(R.string.context_empty_contacts))
                try {
                    textToShow = textToShow.replace(
                        "[A]", "<font color=\'"
                                + getColorHexString(this, R.color.grey_900_grey_100)
                                + "\'>"
                    )
                    textToShow = textToShow.replace("[/A]", "</font>")
                    textToShow = textToShow.replace(
                        "[B]", "<font color=\'"
                                + getColorHexString(this, R.color.grey_300_grey_600)
                                + "\'>"
                    )
                    textToShow = textToShow.replace("[/B]", "</font>")
                } catch (e: Exception) {
                    Timber.e(e)
                }
                val result = HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY)
                emptyTextView?.text = result
            } else {
                emptyTextView?.setText(R.string.contacts_list_empty_text_loading)
            }
        } else {
            emptyTextView?.setText(R.string.no_contacts_permissions)
            val hasReadContactsPermission =
                hasPermissions(applicationContext, Manifest.permission.READ_CONTACTS)
            if (!hasReadContactsPermission) {
                Timber.w("PhoneContactsTask: No read contacts permission")
            }
        }

        if (adapterPhone == null) {
            adapterPhone = PhoneContactsAdapter(addContactActivity, contacts)

            recyclerViewList?.adapter = adapterPhone

            adapterPhone?.SetOnItemClickListener { view, position -> itemClick(view, position) }
        } else {
            adapterPhone?.setContacts(contacts)
        }

        if (adapterPhone != null) {
            if (adapterPhone?.itemCount == 0) {
                showHeader(false)
                if (contactType == Constants.CONTACT_TYPE_BOTH) {
                    if (adapterMEGA != null) {
                        if (adapterMEGA?.itemCount == 0) {
                            setEmptyStateVisibility(true)
                        } else {
                            setEmptyStateVisibility(false)
                        }
                    }
                } else {
                    setEmptyStateVisibility(true)
                }
            } else {
                showHeader(true)
                setEmptyStateVisibility(false)
            }
        }
    }

    /**
     * Set mega adapter contacts
     *
     * @param contacts
     * @param adapter
     */
    fun setMegaAdapterContacts(contacts: ArrayList<MegaContactAdapter>, adapter: Int) {
        if (onNewGroup) {
            adapterMEGA =
                MegaContactsAdapter(addContactActivity, contacts, newGroupRecyclerView, adapter)

            adapterMEGA?.positionClicked = -1
            newGroupRecyclerView?.adapter = adapterMEGA
        } else {
            if (adapterMEGA == null) {
                adapterMEGA =
                    MegaContactsAdapter(addContactActivity, contacts, recyclerViewList, adapter)
            } else {
                adapterMEGA?.setAdapterType(adapter)
                adapterMEGA?.contacts = contacts
            }

            adapterMEGA?.positionClicked = -1
            recyclerViewList?.adapter = adapterMEGA

            if (adapterMEGA?.itemCount == 0) {
                var textToShow = getString(R.string.context_empty_contacts)
                try {
                    textToShow = textToShow.replace(
                        "[A]", "<font color=\'"
                                + getColorHexString(this, R.color.grey_900_grey_100)
                                + "\'>"
                    )
                    textToShow = textToShow.replace("[/A]", "</font>")
                    textToShow = textToShow.replace(
                        "[B]", "<font color=\'"
                                + getColorHexString(this, R.color.grey_300_grey_600)
                                + "\'>"
                    )
                    textToShow = textToShow.replace("[/B]", "</font>")
                } catch (e: Exception) {
                    Timber.e(e)
                }
                val result = HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY)
                emptyTextView?.text = result
                showHeader(false)
                recyclerViewList?.visibility = View.GONE
                setEmptyStateVisibility(true)
            } else {
                showHeader(true)
                recyclerViewList?.visibility = View.VISIBLE
                setEmptyStateVisibility(false)
            }

            addSelectedContactMEGA()
        }
    }

    /**
     * Set share adapter contacts
     *
     * @param contacts
     */
    fun setShareAdapterContacts(contacts: ArrayList<ShareContactInfo>) {
        if (adapterShareHeader == null) {
            adapterShareHeader = ShareContactsHeaderAdapter(addContactActivity, contacts)
            recyclerViewList?.adapter = adapterShareHeader
            adapterShareHeader?.SetOnItemClickListener { view, position ->
                itemClick(
                    view,
                    position
                )
            }
        } else {
            adapterShareHeader?.setContacts(contacts)
        }

        if (adapterShareHeader?.itemCount == 0) {
            var textToShow = String.format(getString(R.string.context_empty_contacts))
            try {
                textToShow = textToShow.replace(
                    "[A]", "<font color=\'"
                            + getColorHexString(this, R.color.grey_900_grey_100)
                            + "\'>"
                )
                textToShow = textToShow.replace("[/A]", "</font>")
                textToShow = textToShow.replace(
                    "[B]", "<font color=\'"
                            + getColorHexString(this, R.color.grey_300_grey_600)
                            + "\'>"
                )
                textToShow = textToShow.replace("[/B]", "</font>")
            } catch (e: Exception) {
                Timber.e(e)
            }
            val result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY)
            emptyTextView?.text = result
        } else {
            setEmptyStateVisibility(false)
        }
    }

    /**
     * Set send invitation visibility
     *
     */
    fun setSendInvitationVisibility() {
        if (fabButton != null) {
            if (contactType == Constants.CONTACT_TYPE_MEGA && !onNewGroup && (createNewGroup || (comesFromChat && ((adapterMEGAContacts != null && adapterMEGAContacts.countOrZero() > 0) || emailsContactsSelected.isNotEmpty())))
            ) {
                fabButton?.show()
            } else if (contactType == Constants.CONTACT_TYPE_DEVICE && adapterContacts != null && adapterContacts.countOrZero() > 0) {
                fabButton?.show()
            } else if (contactType == Constants.CONTACT_TYPE_BOTH && adapterShare != null && adapterShare.countOrZero() > 0) {
                fabButton?.show()
            } else {
                fabButton?.hide()
            }
        }
        if (sendInvitationMenuItem != null) {
            if (contactType == Constants.CONTACT_TYPE_MEGA && onNewGroup) {
                sendInvitationMenuItem?.setVisible(true)
            } else {
                sendInvitationMenuItem?.setVisible(false)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Timber.d("onCreateOptionsMenu")

        val inflater = menuInflater
        inflater.inflate(R.menu.activity_add_contact, menu)

        searchMenuItem = menu.findItem(R.id.action_search)

        val searchView = searchMenuItem?.actionView as SearchView?
        searchView?.queryHint = getString(R.string.hint_action_search)
        searchView?.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
        searchView?.setIconifiedByDefault(true)


        searchMenuItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                Timber.d("onMenuItemActionExpand")
                searchExpand = true
                typeContactEditText?.text?.clear()
                if (isAsyncTaskRunning(filterContactsTask)) {
                    filterContactsTask?.cancel(true)
                }
                filterContactsTask = FilterContactsTask(this@AddContactActivity)
                filterContactsTask?.execute()
                setSendInvitationVisibility()
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                Timber.d("onMenuItemActionCollapse")
                searchExpand = false
                setSendInvitationVisibility()
                supportActionBar?.let { setTitleAB() }
                if (isAsyncTaskRunning(filterContactsTask)) {
                    filterContactsTask?.cancel(true)
                }
                return true
            }
        })
        searchView?.maxWidth = Int.MAX_VALUE
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                hideSoftKeyboard()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                Timber.d("onQueryTextChange searchView")
                if (isAsyncTaskRunning(filterContactsTask)) {
                    filterContactsTask?.cancel(true)
                }
                filterContactsTask = FilterContactsTask(this@AddContactActivity)
                filterContactsTask?.execute()
                return true
            }
        })

        scanQrMenuItem = menu.findItem(R.id.action_scan_qr)
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            && contactType != Constants.CONTACT_TYPE_MEGA
        ) {
            scanQrMenuItem?.setVisible(true)
        } else {
            scanQrMenuItem?.setVisible(false)
        }

        sendInvitationMenuItem = menu.findItem(R.id.action_send_invitation)
        setSendInvitationVisibility()

        if (searchExpand && searchMenuItem != null) {
            searchMenuItem?.expandActionView()
            Timber.d("searchView != null inputString: %s", savedInputString)
            searchView?.setQuery(savedInputString, false)
            if (recoverContactsTask != null && recoverContactsTask?.status == AsyncTask.Status.FINISHED) {
                filterContactsTask = FilterContactsTask(this)
                filterContactsTask?.execute()
            }
        }
        setSearchVisibility()

        if (!queryPermissions && contactType == Constants.CONTACT_TYPE_DEVICE) {
            searchMenuItem?.setVisible(false)
        }

        return super.onCreateOptionsMenu(menu)
    }

    private fun hideSoftKeyboard() {
        currentFocus?.let {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    fun setSearchVisibility() {
        if (searchMenuItem == null) {
            return
        }

        val visible =
            !((contactType == Constants.CONTACT_TYPE_MEGA && filteredContactMEGA.isEmpty())
                    || (contactType == Constants.CONTACT_TYPE_DEVICE && filteredContactsPhone.isEmpty())
                    || (contactType == Constants.CONTACT_TYPE_BOTH && filteredContactsShare.isEmpty()))

        if (searchMenuItem?.isVisible != visible) {
            searchMenuItem?.setVisible(visible)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected")

        val id = item.itemId
        when (id) {
            android.R.id.home -> {
                onBackPressed()
            }

            R.id.action_scan_qr -> {
                initScanQR()
            }

            R.id.action_send_invitation -> {
                if (contactType == Constants.CONTACT_TYPE_MEGA) {
                    setResultContacts(addedContactsMEGA, true)
                } else {
                    shareWith(addedContactsShare)
                }
                hideSoftKeyboard()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun refreshKeyboard() {
        val s = typeContactEditText?.text.toString()
        val imeOptions = typeContactEditText?.imeOptions

        if (s.isEmpty() && (addedContactsMEGA.isNotEmpty() || addedContactsPhone.isNotEmpty() || addedContactsShare.isNotEmpty())) {
            typeContactEditText?.imeOptions = EditorInfo.IME_ACTION_SEND
        } else {
            typeContactEditText?.imeOptions = EditorInfo.IME_ACTION_DONE
        }

        val imeOptionsNew = typeContactEditText?.imeOptions
        if (imeOptions != imeOptionsNew) {
            val view = currentFocus
            if (view != null) {
                val inputMethodManager =
                    getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.restartInput(view)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean("fromAchievements", fromAchievements)
        outState.putStringArrayList("mailsFromAchievements", mailsFromAchievements)
        outState.putBoolean("searchExpand", searchExpand)
        if (searchExpand) {
            (searchMenuItem?.actionView as SearchView?)?.query?.let {
                outState.putString("inputString", it.toString())
            }
        } else {
            if (typeContactEditText != null) {
                outState.putString("inputString", typeContactEditText?.text.toString())
            }
        }
        outState.putBoolean("onNewGroup", onNewGroup)
        outState.putBoolean("isConfirmDeleteShown", isConfirmDeleteShown)
        outState.putString("confirmDeleteMail", confirmDeleteMail)
        outState.putBoolean(FROM_RECENT, comesFromRecent)
        if (isAsyncTaskRunning(queryIfContactShouldBeAddedTask)) {
            isConfirmAddShown = true
            queryIfContactShouldBeAddedTask?.cancel(true)
        }
        outState.putBoolean("isConfirmAddShown", isConfirmAddShown)
        outState.putString("confirmAddMail", confirmAddMail)
        outState.putBoolean("createNewGroup", createNewGroup)
        outState.putBoolean("queryPermissions", queryPermissions)
        outState.putBoolean("isEKREnabled", isEKREnabled)
        outState.putBoolean(IS_ALLOWED_ADD_PARTICIPANTS, isAllowAddParticipantsEnabled)
        outState.putBoolean("newGroup", newGroup)
        outState.putBoolean("onlyCreateGroup", onlyCreateGroup)
        outState.putBoolean("warningBannerShown", isWarningMessageShown)
        saveContactsAdded(outState)
    }

    /**
     * Is async task running
     *
     * @param asyncTask
     */
    fun isAsyncTaskRunning(asyncTask: AsyncTask<*, *, *>?) =
        asyncTask != null && asyncTask.status == AsyncTask.Status.RUNNING

    private fun saveContactsAdded(outState: Bundle) {
        var finished = true

        if (isAsyncTaskRunning(getContactsTask)) {
            getContactsTask?.cancel(true)
            finished = false
            if (contactType == Constants.CONTACT_TYPE_DEVICE) {
                outState.putParcelableArrayList("addedContactsPhone", null)
                outState.putParcelableArrayList("filteredContactsPhone", null)
                outState.putParcelableArrayList("phoneContacts", null)
            } else {
                outState.putStringArrayList("savedaddedContacts", null)
            }
        } else if (isAsyncTaskRunning(getPhoneContactsTask)) {
            getPhoneContactsTask?.cancel(true)
            finished = false
            outState.putStringArrayList("savedaddedContacts", null)
        } else if (isAsyncTaskRunning(filterContactsTask)) {
            filterContactsTask?.cancel(true)
            finished = true
        } else if (isAsyncTaskRunning(recoverContactsTask)) {
            recoverContactsTask?.cancel(true)
            finished = false
            if (contactType == Constants.CONTACT_TYPE_DEVICE) {
                outState.putParcelableArrayList("addedContactsPhone", addedContactsPhone)
                outState.putParcelableArrayList("filteredContactsPhone", filteredContactsPhone)
                outState.putParcelableArrayList("phoneContacts", phoneContacts)
            } else {
                outState.putStringArrayList("savedaddedContacts", savedAddedContacts)
            }
        }

        if (finished) {
            savedAddedContacts.clear()
            when (contactType) {
                Constants.CONTACT_TYPE_MEGA -> {
                    if (onNewGroup) {
                        createMyContact()
                        if (addedContactsMEGA.contains(myContact)) {
                            addedContactsMEGA.remove(myContact)
                        }
                    }
                    for (i in addedContactsMEGA.indices) {
                        if (getMegaContactMail(addedContactsMEGA[i]) != null) {
                            getMegaContactMail(addedContactsMEGA[i])?.let {
                                savedAddedContacts.add(
                                    it
                                )
                            }
                        } else {
                            addedContactsMEGA[i].fullName?.let { savedAddedContacts.add(it) }
                        }
                    }
                    outState.putStringArrayList("savedaddedContacts", savedAddedContacts)
                }

                Constants.CONTACT_TYPE_DEVICE -> {
                    outState.putParcelableArrayList("addedContactsPhone", addedContactsPhone)
                    outState.putParcelableArrayList("filteredContactsPhone", filteredContactsPhone)
                    outState.putParcelableArrayList("phoneContacts", phoneContacts)
                }

                else -> {
                    for (i in addedContactsShare.indices) {
                        if (addedContactsShare[i].isMegaContact) {
                            getMegaContactMail(addedContactsShare[i].getMegaContactAdapter())?.let {
                                savedAddedContacts.add(
                                    it
                                )
                            }
                        } else if (addedContactsShare[i].isPhoneContact) {
                            savedAddedContacts.add(addedContactsShare[i].phoneContactInfo.email)
                        } else {
                            savedAddedContacts.add(addedContactsShare[i].mail)
                        }
                    }
                    outState.putStringArrayList("savedaddedContacts", savedAddedContacts)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        megaApi.removeGlobalListener(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            return
        }

        if (intent != null) {
            contactType = intent.getIntExtra(
                Constants.INTENT_EXTRA_KEY_CONTACT_TYPE,
                Constants.CONTACT_TYPE_MEGA
            )
            intent.getStringArrayListExtra(Constants.INTENT_EXTRA_KEY_CONTACTS_SELECTED)
                ?.let { emailsContactsSelected = it }
            isFromMeeting = intent.getBooleanExtra(Constants.INTENT_EXTRA_IS_FROM_MEETING, false)
            chatId = intent.getLongExtra(Constants.INTENT_EXTRA_KEY_CHAT_ID, -1)
            maxParticipants = intent.getIntExtra(
                Constants.INTENT_EXTRA_KEY_MAX_USER,
                MeetingState.FREE_PLAN_PARTICIPANTS_LIMIT
            )
            newGroup = intent.getBooleanExtra("newGroup", false)
            comesFromRecent = intent.getBooleanExtra(FROM_RECENT, false)
            if (newGroup) {
                createNewGroup = true
                intent.getStringArrayListExtra("contactsNewGroup")?.let { contactsNewGroup = it }
            }
            fromAchievements = intent.getBooleanExtra("fromAchievements", false)
            if (fromAchievements) {
                intent.getStringArrayListExtra(EXTRA_CONTACTS)?.let { mailsFromAchievements = it }
            }
            comesFromChat = intent.getBooleanExtra(Constants.INTENT_EXTRA_KEY_CHAT, false)
            if (comesFromChat) {
                title = intent.getStringExtra(Constants.INTENT_EXTRA_KEY_TOOL_BAR_TITLE)
            }
            onlyCreateGroup = intent.getBooleanExtra(EXTRA_ONLY_CREATE_GROUP, false)
            isStartConversation = intent.getBooleanExtra(EXTRA_IS_START_CONVERSATION, false)
            if (contactType == Constants.CONTACT_TYPE_MEGA || contactType == Constants.CONTACT_TYPE_BOTH) {
                multipleSelectIntent = intent.getIntExtra(EXTRA_MULTISELECT, -1)
                if (multipleSelectIntent == 0) {
                    nodeHandle = intent.getLongExtra(EXTRA_NODE_HANDLE, -1)
                } else if (multipleSelectIntent == 1) {
                    Timber.d("Multiselect YES!")
                    intent.getLongArrayExtra(EXTRA_NODE_HANDLE)?.let { nodeHandles = it }
                }
            }
        }

        this.collectFlow(
            viewModel.sensitiveItemsCountFlow,
            Lifecycle.State.STARTED
        ) { type: Int? ->
            val isPaid = viewModel.state.value.accountType?.isPaid ?: false
            val isBusinessAccountExpired = viewModel.state.value.isBusinessAccountExpired
            if (type != null) {
                if (type == 0 || !isPaid || isBusinessAccountExpired) {
                    initialize(savedInstanceState)
                } else {
                    showSharingSensitiveItemsWarningDialog(savedInstanceState, type)
                }
                viewModel.clearSensitiveItemsCheck()
            }
            Unit
        }

        val handles: MutableList<Long> = ArrayList()
        if (nodeHandle != -1L) {
            handles.add(nodeHandle)
        } else {
            for (handle in nodeHandles) {
                handles.add(handle)
            }
        }

        viewModel.checkSensitiveItems(handles)
    }

    private fun initialize(savedInstanceState: Bundle?) {
        viewModel.setChatId(chatId)

        val display = windowManager.defaultDisplay
        display.getMetrics(outMetrics)

        megaApi.addGlobalListener(this)

        addContactActivity = this

        checkChatChanges()
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_contact)

        val tB = findViewById<Toolbar>(R.id.add_contact_toolbar)
        if (tB == null) {
            Timber.w("Tb is Null")
            return
        }

        tB.visibility = View.VISIBLE
        setSupportActionBar(tB)
        consumeInsetsWithToolbar(customToolbar = tB)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
            it.title = ""
            it.subtitle = ""
        }

        participantsLimitWarningView = findViewById(R.id.participants_limit_warning_view)

        relativeLayout = findViewById<View>(R.id.relative_container_add_contact) as RelativeLayout

        fabButton = findViewById<View>(R.id.fab_button_next) as FloatingActionButton
        fabButton?.setOnClickListener(this)

        mailError = findViewById<View>(R.id.add_contact_email_error) as RelativeLayout
        mailError?.visibility = View.GONE
        typeContactLayout = findViewById<View>(R.id.layout_type_mail) as RelativeLayout
        typeContactLayout?.visibility = View.GONE
        typeContactEditText = findViewById<View>(R.id.type_mail_edit_text) as EditText
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val params1 = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Util.dp2px(40f, outMetrics)
            )
            typeContactLayout?.layoutParams = params1
        }
        typeContactEditText?.addTextChangedListener(this)
        typeContactEditText?.setOnEditorActionListener(this)
        typeContactEditText?.imeOptions = EditorInfo.IME_ACTION_DONE
        typeContactEditText?.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (searchExpand) {
                    (searchMenuItem?.actionView as SearchView?)?.setQuery("", false)
                    if (searchMenuItem != null) {
                        searchMenuItem?.collapseActionView()
                    }
                    if (isAsyncTaskRunning(filterContactsTask)) {
                        filterContactsTask?.cancel(true)
                    }
                    filterContactsTask = FilterContactsTask(this@AddContactActivity)
                    filterContactsTask?.execute()
                }
            }
        }
        scanQRButton = findViewById<View>(R.id.layout_scan_qr) as RelativeLayout
        scanQRButton?.setOnClickListener(this)
        scanQRButton?.visibility = View.GONE
        addContactsLayout = findViewById<View>(R.id.add_contacts_container) as LinearLayout
        mWarningMessage = findViewById<View>(R.id.text_warning_message) as TextView
        addedContactsRecyclerView =
            findViewById<View>(R.id.contact_adds_recycler_view) as RecyclerView
        containerAddedContactsRecyclerView =
            findViewById<View>(R.id.contacts_adds_container) as RelativeLayout
        containerAddedContactsRecyclerView?.visibility = View.GONE
        fabImageGroup = findViewById<View>(R.id.image_group_floating_button) as FloatingActionButton
        nameGroup = findViewById(R.id.name_group_edittext)
        nameGroup?.setSingleLine()
        nameGroup?.setImeOptions(EditorInfo.IME_ACTION_DONE)
        nameGroup?.setFilters(arrayOf<InputFilter>(InputFilter.LengthFilter(Constants.MAX_ALLOWED_CHARACTERS_AND_EMOJIS)))

        mLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        addedContactsRecyclerView?.layoutManager = mLayoutManager
        addedContactsRecyclerView?.itemAnimator = DefaultItemAnimator()

        headerContacts = findViewById<View>(R.id.header_list) as RelativeLayout
        textHeader = findViewById<View>(R.id.text_header_list) as TextView

        fastScroller = findViewById<View>(R.id.fastscroll) as FastScroller

        linearLayoutManager = LinearLayoutManager(this)
        recyclerViewList = findViewById<View>(R.id.add_contact_list) as RecyclerView
        recyclerViewList?.clipToPadding = false
        recyclerViewList?.addOnItemTouchListener(this)
        recyclerViewList?.itemAnimator = DefaultItemAnimator()
        fastScroller?.setRecyclerView(recyclerViewList)

        when (contactType) {
            Constants.CONTACT_TYPE_MEGA -> {
                recyclerViewList?.layoutManager = linearLayoutManager
                showHeader(true)
                textHeader?.text = getString(R.string.section_contacts)
                recyclerViewList?.addItemDecoration(SimpleDividerItemDecoration(this))
            }

            Constants.CONTACT_TYPE_DEVICE -> {
                typeContactLayout?.visibility = View.VISIBLE
                if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    scanQRButton?.visibility = View.VISIBLE
                }
                recyclerViewList?.layoutManager = linearLayoutManager
                showHeader(true)
                textHeader?.text = getString(R.string.contacts_phone)
                recyclerViewList?.addItemDecoration(SimpleDividerItemDecoration(this))
            }

            else -> {
                typeContactLayout?.visibility = View.VISIBLE
                if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    scanQRButton?.visibility = View.VISIBLE
                }
                recyclerViewList?.layoutManager = linearLayoutManager
                recyclerViewList?.addItemDecoration(HeaderItemDecoration(this))
                showHeader(false)
            }
        }

        containerContacts = findViewById<View>(R.id.container_list_contacts) as RelativeLayout

        emptyImageView = findViewById<View>(R.id.add_contact_list_empty_image) as ImageView
        emptyTextView = findViewById<View>(R.id.add_contact_list_empty_text) as TextView
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            emptyImageView?.setImageResource(R.drawable.empty_contacts_portrait)
        } else {
            // auto scroll to the bottom to show the invite button
            val scrollView = findViewById<ScrollView>(R.id.scroller)
            Handler().postDelayed({ scrollView.fullScroll(View.FOCUS_DOWN) }, 100)
            emptyImageView?.setImageResource(R.drawable.empty_contacts_landscape)
        }
        emptyTextView?.setText(R.string.contacts_list_empty_text_loading_share)
        emptySubTextView = findViewById<View>(R.id.add_contact_list_empty_subtext) as TextView
        emptyInviteButton = findViewById<View>(R.id.add_contact_list_empty_invite_button) as Button
        emptyInviteButton?.setText(R.string.contact_invite)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val params1 = emptySubTextView?.layoutParams as LinearLayout.LayoutParams
            params1.setMargins(Util.dp2px(34f, outMetrics), 0, Util.dp2px(34f, outMetrics), 0)
            emptyTextView?.layoutParams = params1
            val params2 = emptyInviteButton?.layoutParams as LinearLayout.LayoutParams
            params2.setMargins(0, Util.dp2px(5f, outMetrics), 0, Util.dp2px(32f, outMetrics))
            emptyInviteButton?.layoutParams = params2
        }

        emptyInviteButton?.setOnClickListener(this)

        progressBar = findViewById<View>(R.id.add_contact_progress_bar) as ProgressBar

        newGroupLayout = findViewById<View>(R.id.new_group_layout) as NestedScrollView
        newGroupLayout?.visibility = View.GONE
        ekrSwitch = findViewById<View>(R.id.ekr_switch) as MegaSwitch
        ekrSwitch?.setOnClickListener(this)
        getChatLinkBox = findViewById<View>(R.id.get_chat_link_checkbox) as CheckBox
        getChatLinkLayout = findViewById<View>(R.id.get_chat_link_layout) as RelativeLayout
        newGroupHeaderList = findViewById<View>(R.id.new_group_text_header_list) as TextView
        allowAddParticipantsSwitch = findViewById(R.id.allow_add_participants_switch)
        allowAddParticipantsSwitch?.setOnClickListener(this)
        newGroupRecyclerView = findViewById<View>(R.id.new_group_add_contact_list) as RecyclerView
        newGroupRecyclerView?.clipToPadding = false
        newGroupRecyclerView?.addOnItemTouchListener(this)
        newGroupRecyclerView?.itemAnimator = DefaultItemAnimator()
        newGroupLinearLayoutManager = LinearLayoutManager(this)
        newGroupRecyclerView?.layoutManager = newGroupLinearLayoutManager
        newGroupRecyclerView?.addItemDecoration(SimpleDividerItemDecoration(this))

        //Get MEGA contacts and phone contacts: first name, last name and email
        if (savedInstanceState != null) {
            onNewGroup = savedInstanceState.getBoolean("onNewGroup", onNewGroup)
            isConfirmDeleteShown = savedInstanceState.getBoolean("isConfirmDeleteShown", false)
            confirmDeleteMail = savedInstanceState.getString("confirmDeleteMail")
            comesFromRecent = savedInstanceState.getBoolean(FROM_RECENT, false)
            searchExpand = savedInstanceState.getBoolean("searchExpand", false)
            savedInstanceState.getString("inputString")?.let { savedInputString = it }
            isConfirmAddShown = savedInstanceState.getBoolean("isConfirmAddShown", false)
            confirmAddMail = savedInstanceState.getString("confirmAddMail")
            createNewGroup = savedInstanceState.getBoolean("createNewGroup", false)
            queryPermissions = savedInstanceState.getBoolean("queryPermissions", true)
            isEKREnabled = savedInstanceState.getBoolean("isEKREnabled", false)
            ekrSwitch?.isChecked = isEKREnabled
            isAllowAddParticipantsEnabled = savedInstanceState.getBoolean(
                IS_ALLOWED_ADD_PARTICIPANTS, true
            )
            allowAddParticipantsSwitch?.setChecked(isAllowAddParticipantsEnabled)
            onlyCreateGroup = savedInstanceState.getBoolean("onlyCreateGroup", false)
            isWarningMessageShown = savedInstanceState.getBoolean("warningBannerShown", false)
            mWarningMessage?.visibility =
                if (isWarningMessageShown && isContactVerificationOn) View.VISIBLE else View.GONE
            if (contactType == Constants.CONTACT_TYPE_MEGA || contactType == Constants.CONTACT_TYPE_BOTH) {
                savedInstanceState.getStringArrayList("savedaddedContacts")
                    ?.let { savedAddedContacts = it }

                if (createNewGroup) {
                    supportActionBar?.let { setTitleAB() }
                }

                if (savedAddedContacts.isEmpty() && (contactType == Constants.CONTACT_TYPE_MEGA || contactType == Constants.CONTACT_TYPE_BOTH)) {
                    setAddedAdapterContacts()
                    getContactsTask = GetContactsTask(this)
                    getContactsTask?.execute()
                } else {
                    recoverContactsTask = RecoverContactsTask(this)
                    recoverContactsTask?.execute()
                }
            } else if (contactType == Constants.CONTACT_TYPE_DEVICE) {
                savedInstanceState.getParcelableArrayList<PhoneContactInfo>("addedContactsPhone")
                    ?.let { addedContactsPhone = it }
                savedInstanceState.getParcelableArrayList<PhoneContactInfo>("filteredContactsPhone")
                    ?.let {
                        filteredContactsPhone = it
                    }
                savedInstanceState.getParcelableArrayList<PhoneContactInfo>("phoneContacts")
                    ?.let { phoneContacts = it }

                setAddedAdapterContacts()

                if (queryPermissions && filteredContactsPhone.isEmpty() && phoneContacts.isEmpty()) {
                    queryIfHasReadContactsPermissions()
                } else {
                    if (addedContactsPhone.size == 0) {
                        containerAddedContactsRecyclerView?.visibility = View.GONE
                    } else {
                        containerAddedContactsRecyclerView?.visibility = View.VISIBLE
                    }
                    addedContactsRecyclerView?.adapter = adapterContacts

                    if (phoneContacts.isNotEmpty()) {
                        if (filteredContactsPhone.isEmpty() && addedContactsPhone.isEmpty()) {
                            for (i in phoneContacts.indices) {
                                filteredContactsPhone.add(phoneContacts[i])
                            }
                        }
                        setPhoneAdapterContacts(filteredContactsPhone)
                    } else if (addedContactsPhone.isNotEmpty()) {
                        setEmptyStateVisibility(true)
                    } else {
                        setEmptyStateVisibility(true)

                        progressBar?.visibility = View.VISIBLE
                        getContactsTask = GetContactsTask(this)
                        getContactsTask?.execute()
                    }
                }
                supportActionBar?.let { setTitleAB() }
                setRecyclersVisibility()
            }
        } else {
            isEKREnabled = false
            ekrSwitch?.isChecked = isEKREnabled
            allowAddParticipantsSwitch?.setChecked(isAllowAddParticipantsEnabled)
            setAddedAdapterContacts()

            if (contactType == Constants.CONTACT_TYPE_MEGA) {
                progressBar?.visibility = View.VISIBLE
                getContactsTask = GetContactsTask(this)
                getContactsTask?.execute()
            } else {
                queryIfHasReadContactsPermissions()
            }
        }

        if (onlyCreateGroup) {
            createNewGroup = true
            supportActionBar?.let { setTitleAB() }
        }

        setGetChatLinkVisibility()

        if (comesFromRecent && !onNewGroup) {
            if (isAsyncTaskRunning(getContactsTask)) {
                getContactsTask?.cancel(true)
            }

            newGroup()
        }
        observeFlow()
    }

    private fun showSharingSensitiveItemsWarningDialog(bundle: Bundle?, type: Int) {
        val builder = MaterialAlertDialogBuilder(this@AddContactActivity)

        val title =
            if (type % 2 == 1) getString(mega.privacy.android.shared.resources.R.string.hidden_item) else getString(
                mega.privacy.android.shared.resources.R.string.hidden_items
            )
        builder.setTitle(title)

        var message = ""
        when (type) {
            1 -> {
                message =
                    getString(mega.privacy.android.shared.resources.R.string.share_hidden_item_link_description)
            }

            2 -> {
                message =
                    getString(mega.privacy.android.shared.resources.R.string.share_hidden_item_links_description)
            }

            3 -> {
                message =
                    getString(mega.privacy.android.shared.resources.R.string.share_hidden_folder_description)
            }

            4 -> {
                message =
                    getString(mega.privacy.android.shared.resources.R.string.share_hidden_folders_description)
            }
        }
        builder.setMessage(message)

        builder.setCancelable(false)
        builder.setPositiveButton(getString(R.string.button_continue)) { _: DialogInterface?, _: Int ->
            initialize(
                bundle
            )
        }
        builder.setNegativeButton(getString(sharedR.string.general_dialog_cancel_button)) { _: DialogInterface?, _: Int -> finish() }
        builder.show()
    }

    private fun setEmptyStateVisibility(visible: Boolean) {
        if (visible) {
            emptyImageView?.visibility = View.VISIBLE
            emptyTextView?.visibility = View.VISIBLE
            if (contactType == Constants.CONTACT_TYPE_MEGA && addedContactsMEGA.isEmpty()) {
                if (!isFromMeeting) {
                    emptyInviteButton?.visibility = View.VISIBLE
                } else {
                    emptyInviteButton?.visibility = View.GONE
                }
            } else {
                emptySubTextView?.visibility = View.GONE
                emptyInviteButton?.visibility = View.GONE
            }
        } else {
            emptyImageView?.visibility = View.GONE
            emptyTextView?.visibility = View.GONE
            emptySubTextView?.visibility = View.GONE
            emptyInviteButton?.visibility = View.GONE
        }
    }

    private fun setGetChatLinkVisibility() {
        getChatLinkLayout?.visibility = if (isEKREnabled) View.GONE else View.VISIBLE
    }

    private fun queryIfHasReadContactsPermissions() {
        val hasReadContactsPermission = hasPermissions(this, Manifest.permission.READ_CONTACTS)
        if (!hasReadContactsPermission) {
            Timber.w("No read contacts permission")
            requestPermission(
                this,
                Constants.REQUEST_READ_CONTACTS,
                Manifest.permission.READ_CONTACTS
            )
            if (contactType == Constants.CONTACT_TYPE_DEVICE) {
                return
            }
        }

        if (waitingForPhoneContacts) {
            filteredContactsShare.add(ShareContactInfo())
            getPhoneContactsTask = GetPhoneContactsTask(this)
            getPhoneContactsTask?.execute()
            return
        }

        setEmptyStateVisibility(true)
        progressBar?.visibility = View.VISIBLE
        getContactsTask = GetContactsTask(this)
        getContactsTask?.execute()
    }

    /**
     * Set title a b
     *
     * @param actionBar
     */
    fun setTitleAB() {
        val actionBar = supportActionBar ?: return
        Timber.d("setTitleAB")
        if (contactType == Constants.CONTACT_TYPE_MEGA) {
            if (comesFromChat) {
                actionBar.title = title
                if (addedContactsMEGA.size > 0) {
                    actionBar.subtitle = resources.getString(
                        R.string.selected_items,
                        addedContactsMEGA.size
                    )
                } else {
                    actionBar.subtitle = null
                }
            } else if (createNewGroup && !onNewGroup) {
                actionBar.title = getString(R.string.title_new_group)
                if (addedContactsMEGA.size > 0) {
                    actionBar.subtitle = resources.getString(
                        R.string.selected_items,
                        addedContactsMEGA.size
                    )
                } else {
                    actionBar.subtitle = getString(R.string.add_participants_menu_item)
                }
            }
        } else if (contactType == Constants.CONTACT_TYPE_DEVICE) {
            actionBar.title = getString(R.string.invite_contacts)
            if (addedContactsPhone.size > 0) {
                actionBar.subtitle = resources.getQuantityString(
                    R.plurals.general_selection_num_contacts,
                    addedContactsPhone.size, addedContactsPhone.size
                )
            } else {
                actionBar.subtitle = null
            }
        } else {
            actionBar.title = getString(R.string.share_with)
            if (addedContactsShare.size > 0) {
                actionBar.subtitle = resources.getQuantityString(
                    R.plurals.general_selection_num_contacts,
                    addedContactsShare.size, addedContactsShare.size
                )
            } else {
                actionBar.subtitle = null
            }
        }
    }

    private fun setError() {
        Timber.d("setError")
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val params = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(
                Util.dp2px(18f, outMetrics),
                Util.dp2px(-10f, outMetrics),
                Util.dp2px(18f, outMetrics),
                0
            )
            typeContactEditText?.layoutParams = params
        }
        mailError?.visibility = View.VISIBLE

        typeContactEditText?.let { setErrorAwareInputAppearance(it, true) }
    }

    private fun quitError() {
        Timber.d("quitError")
        if (mailError?.visibility != View.GONE) {
            mailError?.visibility = View.GONE
        }
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val params = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(
                Util.dp2px(18f, outMetrics),
                Util.dp2px(0f, outMetrics),
                Util.dp2px(18f, outMetrics),
                0
            )
            typeContactEditText?.layoutParams = params
        }

        typeContactEditText?.let { setErrorAwareInputAppearance(it, false) }
    }

    /**
     * Add share contact
     *
     * @param contact
     */
    fun addShareContact(contact: ShareContactInfo) {
        Timber.d("addShareContact")

        if (searchExpand && searchMenuItem != null) {
            searchMenuItem?.collapseActionView()
        }
        if (typeContactEditText?.text?.isNotEmpty() == true) {
            typeContactEditText?.text?.clear()
        }
        hideSoftKeyboard()
        typeContactEditText?.clearFocus()

        var foundIndex = -1
        for (i in addedContactsShare.indices) {
            if (getShareContactMail(addedContactsShare[i]) == getShareContactMail(contact)) {
                foundIndex = i
                break
            }
        }
        if (foundIndex != -1) {
            deleteContact(foundIndex)
            return
        } else {
            addedContactsShare.add(contact)
        }
        adapterShare?.setContacts(addedContactsShare)
        isWarningMessageShown = checkForUnVerifiedContacts()
        mWarningMessage?.visibility =
            if (isWarningMessageShown && isContactVerificationOn) View.VISIBLE else View.GONE
        if (adapterShare.countOrZero() - 1 >= 0) {
            mLayoutManager?.scrollToPosition((adapterShare.countOrZero() - 1).coerceAtLeast(0))
        }
        setSendInvitationVisibility()
        containerAddedContactsRecyclerView?.visibility = View.VISIBLE
        supportActionBar?.let { setTitleAB() }

        if (adapterShareHeader != null) {
            if (adapterShareHeader?.itemCount == 0) {
                setEmptyStateVisibility(true)

                var textToShow = String.format(getString(R.string.context_empty_contacts))
                try {
                    textToShow = textToShow.replace(
                        "[A]", "<font color=\'"
                                + getColorHexString(this, R.color.grey_900_grey_100)
                                + "\'>"
                    )
                    textToShow = textToShow.replace("[/A]", "</font>")
                    textToShow = textToShow.replace(
                        "[B]", "<font color=\'"
                                + getColorHexString(this, R.color.grey_300_grey_600)
                                + "\'>"
                    )
                    textToShow = textToShow.replace("[/B]", "</font>")
                } catch (e: Exception) {
                    Timber.e(e)
                }
                val result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY)
                emptyTextView?.text = result
            } else {
                setEmptyStateVisibility(false)
            }
        }
        setRecyclersVisibility()
        refreshKeyboard()
    }

    /**
     * Method that selects the initially selected participants
     */
    private fun addSelectedContactMEGA() {
        if (emailsContactsSelected.isNotEmpty()) {
            for (email in emailsContactsSelected) {
                if (filteredContactMEGA.isNotEmpty()) {
                    for (i in filteredContactMEGA.indices) {
                        val contact = filteredContactMEGA[i]
                        if (getMegaContactMail(contact) == email) {
                            contact.isSelected = true
                            addContactMEGA(contact)
                            adapterMEGA?.contacts = filteredContactMEGA
                            break
                        }
                    }
                }
            }
        }
    }

    private fun addContactMEGA(contact: MegaContactAdapter) {
        Timber.d("Contact: %s", contact.fullName)

        if (searchExpand && searchMenuItem != null) {
            searchMenuItem?.collapseActionView()
        }
        hideSoftKeyboard()

        if (addedContactsMEGA.contains(contact)) {
            deleteContact(addedContactsMEGA.indexOf(contact))
            return
        } else {
            addedContactsMEGA.add(contact)
        }
        adapterMEGAContacts?.setContacts(addedContactsMEGA)
        if (adapterMEGAContacts.countOrZero() - 1 >= 0) {
            mLayoutManager?.scrollToPosition((adapterMEGAContacts.countOrZero() - 1).coerceAtLeast(0))
        }
        setSendInvitationVisibility()
        containerAddedContactsRecyclerView?.visibility = View.VISIBLE
        supportActionBar?.let { setTitleAB() }
        if (adapterMEGA != null) {
            if (adapterMEGA?.itemCount == 0) {
                showHeader(false)
                setEmptyStateVisibility(true)

                var textToShow = String.format(getString(R.string.context_empty_contacts))
                try {
                    textToShow = textToShow.replace(
                        "[A]", "<font color=\'"
                                + getColorHexString(this, R.color.grey_900_grey_100)
                                + "\'>"
                    )
                    textToShow = textToShow.replace("[/A]", "</font>")
                    textToShow = textToShow.replace(
                        "[B]", "<font color=\'"
                                + getColorHexString(this, R.color.grey_300_grey_600)
                                + "\'>"
                    )
                    textToShow = textToShow.replace("[/B]", "</font>")
                } catch (e: Exception) {
                    Timber.e(e)
                }
                val result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY)
                emptyTextView?.text = result
            }
        }
        setRecyclersVisibility()
        refreshKeyboard()
    }

    /**
     * Add contact
     *
     * @param contact
     */
    fun addContact(contact: PhoneContactInfo) {
        Timber.d("Contact: %s, Mail: %s", contact.name, contact.email)

        if (searchExpand && searchMenuItem != null) {
            searchMenuItem?.collapseActionView()
        }
        if (typeContactEditText?.text.toString() != "") {
            typeContactEditText?.text?.clear()
        }
        hideSoftKeyboard()
        typeContactEditText?.clearFocus()

        var found = false
        for (i in addedContactsPhone.indices) {
            found = false
            if (addedContactsPhone[i].email == contact.email) {
                found = true
                break
            }
        }
        if (found) {
            showSnackbar(getString(R.string.contact_not_added))
        } else {
            addedContactsPhone.add(contact)
        }

        adapterContacts?.setContacts(addedContactsPhone)
        if (adapterContacts.countOrZero() - 1 >= 0) {
            mLayoutManager?.scrollToPosition((adapterContacts.countOrZero() - 1).coerceAtLeast(0))
        }
        setSendInvitationVisibility()
        containerAddedContactsRecyclerView?.visibility = View.VISIBLE
        supportActionBar?.let { setTitleAB() }

        if (adapterPhone != null) {
            if (adapterPhone?.itemCount == 0) {
                showHeader(false)
                setEmptyStateVisibility(true)

                var textToShow = String.format(getString(R.string.context_empty_contacts))
                try {
                    textToShow = textToShow.replace(
                        "[A]", "<font color=\'"
                                + getColorHexString(this, R.color.grey_900_grey_100)
                                + "\'>"
                    )
                    textToShow = textToShow.replace("[/A]", "</font>")
                    textToShow = textToShow.replace(
                        "[B]", "<font color=\'"
                                + getColorHexString(this, R.color.grey_300_grey_600)
                                + "\'>"
                    )
                    textToShow = textToShow.replace("[/B]", "</font>")
                } catch (e: Exception) {
                    Timber.e(e)
                }
                val result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY)
                emptyTextView?.text = result
            }
        }
        setRecyclersVisibility()
        refreshKeyboard()
    }

    /**
     * Delete contact
     *
     * @param position
     */
    fun deleteContact(position: Int) {
        Timber.d("Position: %s", position)
        if (position < 0) {
            return
        }
        if (contactType == Constants.CONTACT_TYPE_MEGA) {
            if (position >= addedContactsMEGA.size) {
                return
            }
            val deleteContact = addedContactsMEGA[position]
            addedContactsMEGA.remove(deleteContact)

            val filteredPosition = filteredContactMEGA.indexOf(deleteContact)
            if (filteredPosition != Constants.INVALID_POSITION) {
                filteredContactMEGA[filteredPosition].isSelected = false
            }
            adapterMEGA?.contacts = filteredContactMEGA

            setSendInvitationVisibility()
            adapterMEGAContacts?.setContacts(addedContactsMEGA)
            if (addedContactsMEGA.size == 0) {
                containerAddedContactsRecyclerView?.visibility = View.GONE
            }
        } else if (contactType == Constants.CONTACT_TYPE_DEVICE) {
            if (position >= addedContactsPhone.size) {
                return
            }
            val deleteContact = addedContactsPhone[position]
            if (deleteContact.name != null) {
                addFilteredContact(deleteContact)
            }
            addedContactsPhone.remove(deleteContact)
            setSendInvitationVisibility()
            adapterContacts?.setContacts(addedContactsPhone)
            if (addedContactsPhone.size == 0) {
                containerAddedContactsRecyclerView?.visibility = View.GONE
            }
        } else {
            if (position >= addedContactsShare.size) {
                return
            }
            val deleteContact = addedContactsShare[position]

            if (deleteContact.isPhoneContact) {
                addFilteredContact(deleteContact.phoneContactInfo)
            } else if (deleteContact.isMegaContact) {
                val filteredPosition = filteredContactsShare.indexOf(deleteContact)
                if (filteredPosition != Constants.INVALID_POSITION) {
                    filteredContactsShare[filteredPosition].getMegaContactAdapter().isSelected =
                        false
                    adapterShareHeader?.setContacts(filteredContactsShare)
                }
            }

            addedContactsShare.remove(deleteContact)
            setSendInvitationVisibility()
            adapterShare?.setContacts(addedContactsShare)
            if (addedContactsShare.size == 0) {
                containerAddedContactsRecyclerView?.visibility = View.GONE
            }
        }
        supportActionBar?.let { setTitleAB() }
        setRecyclersVisibility()
        refreshKeyboard()
        setSearchVisibility()
        isWarningMessageShown = checkForUnVerifiedContacts()
        mWarningMessage?.visibility =
            if (isWarningMessageShown && isContactVerificationOn) View.VISIBLE else View.GONE
    }

    /**
     * Show snackbar
     *
     * @param message
     */
    fun showSnackbar(message: String) {
        hideSoftKeyboard()
        relativeLayout?.let { showSnackbar(it, message) }
    }

    private fun addMEGAFilteredContact(contact: MegaContactAdapter) {
        filteredContactMEGA.add(contact)
        filteredContactMEGA.sortWith { c1, c2 ->
            val name1 = c1.fullName.orEmpty()
            val name2 = c2.fullName.orEmpty()
            var res = java.lang.String.CASE_INSENSITIVE_ORDER.compare(name1, name2)
            if (res == 0) {
                res = name1.compareTo(name2)
            }
            res
        }

        val index = filteredContactMEGA.indexOf(contact)
        if (searchExpand) {
            setInputStringToSearchQueryText()
        } else {
            inputString = typeContactEditText?.text.toString()
        }

        if (contactType == Constants.CONTACT_TYPE_BOTH) {
            val i = filteredContactMEGA.indexOf(contact)
            val contactToAdd = ShareContactInfo(null, contact, null)
            if (filteredContactMEGA.size == 1) {
                filteredContactsShare.add(0, ShareContactInfo(true, true, false))
                filteredContactsShare.add(1, contactToAdd)
            } else {
                filteredContactsShare.add(i + 1, contactToAdd)
            }
            if (inputString.isNotEmpty()) {
                filterContactsTask = FilterContactsTask(this)
                filterContactsTask?.execute()
            } else {
                adapterShareHeader?.setContacts(filteredContactsShare)
                if (index >= 0 && index < adapterShareHeader.countOrZero()) {
                    linearLayoutManager?.scrollToPosition(index)
                }
            }
            if (adapterShareHeader?.itemCount != 0) {
                setEmptyStateVisibility(false)
            }
        } else {
            if (!onNewGroup) {
                if (inputString.isNotEmpty()) {
                    filterContactsTask = FilterContactsTask(this)
                    filterContactsTask?.execute()
                } else {
                    adapterMEGA?.contacts = filteredContactMEGA
                    if (index >= 0 && index < adapterMEGA.countOrZero()) {
                        linearLayoutManager?.scrollToPosition(index)
                    }
                    if (adapterMEGA?.itemCount != 0) {
                        showHeader(true)
                        setEmptyStateVisibility(false)
                    }
                    recyclerViewList?.visibility =
                        if (adapterMEGA.countOrZero() > 0) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun addFilteredContact(contact: PhoneContactInfo) {
        Timber.d("addFilteredContact")
        filteredContactsPhone.add(contact)
        filteredContactsPhone.sort()
        val index = filteredContactsPhone.indexOf(contact)
        val position: Int

        Timber.d("Size filteredContactsPhone: %s", filteredContactsPhone.size)

        if (searchExpand) {
            setInputStringToSearchQueryText()
        } else {
            inputString = typeContactEditText?.text.toString()
        }

        if (contactType == Constants.CONTACT_TYPE_BOTH) {
            if (filteredContactsPhone.size == 1) {
                filteredContactsShare.add(
                    filteredContactsShare.size,
                    ShareContactInfo(true, false, true)
                )
                filteredContactsShare.add(
                    filteredContactsShare.size,
                    ShareContactInfo(contact, null, null)
                )
                position = filteredContactsShare.size
            } else {
                position = (adapterShareHeader.countOrZero() - filteredContactsPhone.size) + index
                if (position > 0 && (position + 1 <= filteredContactsShare.size)) {
                    filteredContactsShare.add(position + 1, ShareContactInfo(contact, null, null))
                }
            }
            if (inputString.isNotEmpty()) {
                filterContactsTask = FilterContactsTask(this)
                filterContactsTask?.execute()
            } else {
                adapterShareHeader?.setContacts(filteredContactsShare)
                if (position >= 0 && position < adapterShareHeader.countOrZero()) {
                    linearLayoutManager?.scrollToPosition(position)
                }
            }
            if (adapterShareHeader?.itemCount != 0) {
                setEmptyStateVisibility(false)
            }
        } else {
            if (inputString.isNotEmpty()) {
                filterContactsTask = FilterContactsTask(this)
                filterContactsTask?.execute()
            } else {
                adapterPhone?.setContacts(filteredContactsPhone)
                if (index >= 0 && index < adapterPhone.countOrZero()) {
                    linearLayoutManager?.scrollToPosition(index)
                }
                if (adapterPhone != null) {
                    if (adapterPhone?.itemCount != 0) {
                        showHeader(true)
                        setEmptyStateVisibility(false)
                    }
                }
            }
        }
    }

    private fun createMegaContact(contact: MegaUser): MegaContactAdapter {
        val contactDB = ContactUtil.getContactDB(contact.handle)
        var fullName = ContactUtil.getContactNameDB(contactDB)
        if (fullName == null) {
            fullName = contact.email
        }

        return MegaContactAdapter(contactDB, contact, fullName)
    }

    /**
     * Get visible m e g a contacts
     *
     */
    fun getVisibleMEGAContacts() {
        contactsMEGA = megaApi.contacts
        visibleContactsMEGA.clear()
        filteredContactMEGA.clear()
        addedContactsMEGA.clear()

        if (chatId != -1L) {
            Timber.d("Add participant to chat")
            val chat = megaChatApi.getChatRoom(chatId)
            if (chat != null) {
                val participantsCount = chat.peerCount
                val isModerator = chat.ownPrivilege == MegaChatRoom.PRIV_MODERATOR
                participantsLimitWarningView?.isModerator = isModerator
                for (i in contactsMEGA.indices) {
                    if (contactsMEGA[i].visibility == MegaUser.VISIBILITY_VISIBLE) {
                        var found = false

                        for (j in 0 until participantsCount) {
                            val peerHandle = chat.getPeerHandle(j)

                            if (peerHandle == contactsMEGA[i].handle) {
                                found = true
                                break
                            }
                        }

                        if (!found) {
                            visibleContactsMEGA.add(createMegaContact(contactsMEGA[i]))
                        } else {
                            Timber.d("Removed from list - already included on chat: ")
                        }
                    }
                }
            } else {
                for (i in contactsMEGA.indices) {
                    Timber.d(
                        "Contact: %s_%d",
                        contactsMEGA[i].email,
                        contactsMEGA[i].visibility
                    )
                    if (contactsMEGA[i].visibility == MegaUser.VISIBILITY_VISIBLE) {
                        val megaContactAdapter = createMegaContact(contactsMEGA[i])
                        visibleContactsMEGA.add(megaContactAdapter)
                    }
                }
            }
        } else if ((multipleSelectIntent == 0 && nodeHandle != -1L) || (multipleSelectIntent == 1 && nodeHandles.size == 1)) {
            val shared = ArrayList<MegaShare>()
            if (multipleSelectIntent == 0) {
                shared.addAll(megaApi.getOutShares(megaApi.getNodeByHandle(nodeHandle)))
            } else {
                shared.addAll(megaApi.getOutShares(megaApi.getNodeByHandle(nodeHandles[0])))
            }
            var found: Boolean
            for (i in contactsMEGA.indices) {
                found = false
                Timber.d(
                    "Contact: %s_%d",
                    contactsMEGA[i].email,
                    contactsMEGA[i].visibility
                )
                if (contactsMEGA[i].visibility == MegaUser.VISIBILITY_VISIBLE) {
                    val megaContactAdapter = createMegaContact(contactsMEGA[i])
                    for (j in shared.indices) {
                        if (getMegaContactMail(megaContactAdapter) == shared[j].user) {
                            found = true
                            break
                        }
                    }
                    if (!found) {
                        visibleContactsMEGA.add(megaContactAdapter)
                    }
                }
            }
        } else {
            for (i in contactsMEGA.indices) {
                Timber.d(
                    "Contact: %s_%d",
                    contactsMEGA[i].email,
                    contactsMEGA[i].visibility
                )
                if (contactsMEGA[i].visibility == MegaUser.VISIBILITY_VISIBLE) {
                    val megaContactAdapter = createMegaContact(contactsMEGA[i])
                    visibleContactsMEGA.add(megaContactAdapter)
                }
            }
        }

        visibleContactsMEGA.sortWith { c1, c2 ->
            val name1 = c1.fullName.orEmpty()
            val name2 = c2.fullName.orEmpty()
            var res = java.lang.String.CASE_INSENSITIVE_ORDER.compare(name1, name2)
            if (res == 0) {
                res = name1.compareTo(name2)
            }
            res
        }

        var handle: Long
        for (i in visibleContactsMEGA.indices) {
            filteredContactMEGA.add(visibleContactsMEGA[i])
            if (contactType == Constants.CONTACT_TYPE_MEGA) {
                //Ask for presence info and last green
                handle = getMegaContactHandle(visibleContactsMEGA[i])
                if (handle != -1L) {
                    val userStatus = megaChatApi.getUserOnlineStatus(handle)
                    if (userStatus != MegaChatApi.STATUS_ONLINE && userStatus != MegaChatApi.STATUS_BUSY && userStatus != MegaChatApi.STATUS_INVALID) {
                        Timber.d("Request last green for user")
                        megaChatApi.requestLastGreen(handle)
                    }
                }
            }
        }
    }

    @SuppressLint("InlinedApi") //Get the contacts explicitly added
    private fun getPhoneContacts(): ArrayList<PhoneContactInfo> {
        Timber.d("getPhoneContacts")
        val contactList = ArrayList<PhoneContactInfo>()

        try {
            val cr = contentResolver
            val cursor = cr.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
                null,
                null,
                null
            )

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val name = cursor.getString(1)

                    var emailAddress: String? = null
                    val cursor2 = cr.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                        arrayOf(id.toString()),
                        ContactsContract.Contacts.SORT_KEY_PRIMARY
                    )

                    if (cursor2 != null && cursor2.moveToFirst()) {
                        try {
                            emailAddress = cursor2.getString(
                                cursor2.getColumnIndexOrThrow(
                                    ContactsContract.CommonDataKinds.Email.DATA
                                )
                            )
                        } catch (exception: IllegalArgumentException) {
                            Timber.w(exception, "Exception getting contact email")
                        }
                        cursor2.close()
                    }

                    if (emailAddress != null && emailAddress.contains("@") && !emailAddress.contains(
                            "s.whatsapp.net"
                        )
                    ) {
                        contactList.add(PhoneContactInfo(id, name, emailAddress, null))
                    }
                }

                cursor.close()
            }
        } catch (e: Exception) {
            Timber.w(e, "Exception getting phone contacts")
        }

        return contactList
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        quitError()
        refreshKeyboard()
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        Timber.d("onTextChanged: %s_ %d__%d__%d", s.toString(), start, before, count)
        if (contactType == Constants.CONTACT_TYPE_DEVICE) {
            if (s.isNotEmpty()) {
                val temp = s.toString()
                val last = s[s.length - 1]
                if (last == ' ') {
                    val isValid = isValidEmail(temp.trim { it <= ' ' })
                    if (isValid) {
                        confirmAddMail = temp.trim { it <= ' ' }
                        queryIfContactShouldBeAddedTask = QueryIfContactShouldBeAddedTask(this)
                        queryIfContactShouldBeAddedTask?.execute(false)
                        typeContactEditText?.text?.clear()
                    } else {
                        setError()
                    }
                    if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        hideSoftKeyboard()
                    }
                } else {
                    Timber.d("Last character is: %s", last)
                }
            }
        } else if (contactType == Constants.CONTACT_TYPE_BOTH) {
            if (s.isNotEmpty()) {
                val temp = s.toString()
                val last = s[s.length - 1]
                if (last == ' ') {
                    val isValid = isValidEmail(temp.trim { it <= ' ' })
                    if (isValid) {
                        confirmAddMail = temp.trim { it <= ' ' }
                        queryIfContactShouldBeAddedTask = QueryIfContactShouldBeAddedTask(this)
                        queryIfContactShouldBeAddedTask?.execute(false)
                        typeContactEditText?.text?.clear()
                    } else {
                        setError()
                    }
                    if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        hideSoftKeyboard()
                    }
                } else {
                    Timber.d("Last character is: %s", last)
                }
            }
        }

        if (isAsyncTaskRunning(filterContactsTask)) {
            filterContactsTask?.cancel(true)
        }
        filterContactsTask = FilterContactsTask(this)
        filterContactsTask?.execute()
        refreshKeyboard()
    }

    override fun afterTextChanged(editable: Editable) {
        refreshKeyboard()
    }

    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
        refreshKeyboard()
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            val s = v.text.toString()
            Timber.d("s: %s", s)
            if (s.isEmpty() || s == "null" || s == "") {
                hideSoftKeyboard()
            } else {
                if (contactType == Constants.CONTACT_TYPE_DEVICE) {
                    val isValid = isValidEmail(s.trim { it <= ' ' })
                    if (isValid) {
                        confirmAddMail = s.trim { it <= ' ' }
                        queryIfContactShouldBeAddedTask = QueryIfContactShouldBeAddedTask(this)
                        queryIfContactShouldBeAddedTask?.execute(false)
                        typeContactEditText?.text?.clear()
                        hideSoftKeyboard()
                    } else {
                        setError()
                    }
                    if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        hideSoftKeyboard()
                    }
                } else if (contactType == Constants.CONTACT_TYPE_BOTH) {
                    val isValid = isValidEmail(s.trim { it <= ' ' })
                    if (isValid) {
                        confirmAddMail = s.trim { it <= ' ' }
                        queryIfContactShouldBeAddedTask = QueryIfContactShouldBeAddedTask(this)
                        queryIfContactShouldBeAddedTask?.execute(false)
                        typeContactEditText?.text?.clear()
                        hideSoftKeyboard()
                    } else {
                        setError()
                    }
                    if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        hideSoftKeyboard()
                    }
                }
                if (isAsyncTaskRunning(filterContactsTask)) {
                    filterContactsTask?.cancel(true)
                }
                filterContactsTask = FilterContactsTask(this)
                filterContactsTask?.execute()
            }
            return true
        }
        if ((event?.keyCode == KeyEvent.KEYCODE_ENTER) || (actionId == EditorInfo.IME_ACTION_SEND)) {
            if (contactType == Constants.CONTACT_TYPE_DEVICE) {
                if (addedContactsPhone.isEmpty()) {
                    hideSoftKeyboard()
                } else {
                    inviteContacts(addedContactsPhone)
                }
            } else if (contactType == Constants.CONTACT_TYPE_MEGA) {
                if (addedContactsMEGA.isEmpty()) {
                    hideSoftKeyboard()
                } else {
                    setResultContacts(addedContactsMEGA, true)
                }
            } else {
                if (addedContactsShare.isEmpty()) {
                    hideSoftKeyboard()
                } else {
                    shareWith(addedContactsShare)
                }
            }
            return true
        }
        return false
    }

    /**
     * Item click
     *
     * @param email
     * @param adapter
     */
    fun itemClick(email: String, adapter: Int) {
        Timber.d("itemClick")

        if (contactType == Constants.CONTACT_TYPE_MEGA) {
            if (createNewGroup || comesFromChat) {
                if (adapter == MegaContactsAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT) {
                    if (searchExpand) {
                        setInputStringToSearchQueryText()
                    } else {
                        inputString = typeContactEditText?.text.toString()
                    }
                    if (inputString != "") {
                        for (i in queryContactMEGA.indices) {
                            val contact = queryContactMEGA[i]
                            if (getMegaContactMail(contact) == email) {
                                val filteredPosition = filteredContactMEGA.indexOf(contact)
                                if (filteredPosition != Constants.INVALID_POSITION) {
                                    filteredContactMEGA[filteredPosition].isSelected = true
                                }
                                addContactMEGA(contact)
                                break
                            }
                        }
                    } else {
                        for (i in filteredContactMEGA.indices) {
                            val contact = filteredContactMEGA[i]
                            if (getMegaContactMail(contact) == email) {
                                contact.isSelected = true
                                addContactMEGA(contact)
                                adapterMEGA?.contacts = filteredContactMEGA
                                break
                            }
                        }
                    }

                    if (addedContactsMEGA.size == 0) {
                        setSendInvitationVisibility()
                    }
                } else {
                    for (i in addedContactsMEGA.indices) {
                        if (getMegaContactMail(addedContactsMEGA[i]) == email) {
                            showConfirmationDeleteFromChat(addedContactsMEGA[i])
                            break
                        }
                    }
                }
            } else {
                val contacts = ArrayList<String>()
                contacts.add(email)
                startConversation(contacts, true, null)
            }
        }
        setSearchVisibility()
    }

    private fun showConfirmationDeleteFromChat(contact: MegaContactAdapter) {
        val builder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)

        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    addMEGAFilteredContact(contact)
                    addedContactsMEGA.remove(contact)
                    newGroupHeaderList?.text = resources.getQuantityString(
                        R.plurals.subtitle_of_group_chat,
                        addedContactsMEGA.size,
                        addedContactsMEGA.size
                    )
                    adapterMEGA?.contacts = addedContactsMEGA
                    adapterMEGAContacts?.setContacts(addedContactsMEGA)
                }

                DialogInterface.BUTTON_NEGATIVE -> {
                    //No button clicked
                    isConfirmDeleteShown = false
                }
            }
        }

        confirmDeleteMail = getMegaContactMail(contact)
        builder.setMessage(getString(R.string.confirmation_delete_contact, contact.fullName))
        builder.setOnDismissListener { isConfirmDeleteShown = false }
        builder.setPositiveButton(R.string.context_remove, dialogClickListener)
            .setNegativeButton(sharedR.string.general_dialog_cancel_button, dialogClickListener).show()
        isConfirmDeleteShown = true
    }

    private fun itemClick(view: View, position: Int) {
        if (searchExpand) {
            setInputStringToSearchQueryText()
        } else {
            inputString = typeContactEditText?.text.toString()
        }
        if (contactType == Constants.CONTACT_TYPE_DEVICE) {
            if (adapterPhone == null) {
                return
            }

            val contact = adapterPhone?.getItem(position) ?: return

            if (inputString != "") {
                for (i in queryContactsPhone.indices) {
                    if (queryContactsPhone[i].email == contact.email) {
                        filteredContactsPhone.remove(queryContactsPhone[i])
                        queryContactsPhone.removeAt(i)
                        adapterPhone?.setContacts(queryContactsPhone)
                    }
                }
            } else {
                for (i in filteredContactsPhone.indices) {
                    if (filteredContactsPhone[i].email == contact.email) {
                        filteredContactsPhone.removeAt(i)
                        adapterPhone?.setContacts(filteredContactsPhone)
                        break
                    }
                }
            }
            addContact(contact)
        } else if (contactType == Constants.CONTACT_TYPE_BOTH) {
            if (adapterShareHeader == null) {
                return
            }

            val contact = adapterShareHeader?.getItem(position)
            if (contact == null || contact.isHeader || contact.isProgress) {
                return
            }

            if (contact.isPhoneContact) {
                filteredContactsPhone.remove(contact.phoneContactInfo)
                if (filteredContactsPhone.size == 0) {
                    filteredContactsShare.removeAt(filteredContactsShare.size - 2)
                }
                filteredContactsShare.remove(contact)
            } else if (contact.isMegaContact) {
                val contactPosition = filteredContactsShare.indexOf(contact)
                if (contactPosition != Constants.INVALID_POSITION) {
                    filteredContactsShare[contactPosition].getMegaContactAdapter().isSelected = true
                }
            }

            if (inputString != "") {
                filterContactsTask = FilterContactsTask(this)
                filterContactsTask?.execute()
            } else {
                adapterShareHeader?.setContacts(filteredContactsShare)
            }

            addShareContact(contact)
        }
        setSearchVisibility()
    }

    /**
     * Set input string to search query text
     *
     */
    fun setInputStringToSearchQueryText() {
        (searchMenuItem?.actionView as SearchView?)?.let {
            inputString = it.query.toString()
        }
    }

    /**
     * Get mega contact mail
     *
     * @param contact
     * @return
     */
    fun getMegaContactMail(contact: MegaContactAdapter): String? {
        var mail: String? = null
        if (contact.megaUser != null && contact.megaUser.email != null) {
            mail = contact.megaUser.email
        } else if (contact.contact?.email != null) {
            mail = contact.contact.email
        }
        return mail
    }

    private fun getMegaContactHandle(contact: MegaContactAdapter): Long {
        var handle: Long = -1
        if (contact.megaUser != null && contact.megaUser.handle != -1L) {
            handle = contact.megaUser.handle
        } else if (contact.contact?.email != null) {
            handle = contact.contact.userId
        }
        return handle
    }

    private fun setResultContacts(
        addedContacts: ArrayList<MegaContactAdapter>,
        megaContacts: Boolean,
    ) {
        Timber.d("setResultContacts")
        val contactsSelected = ArrayList<String>()
        var contactEmail: String?

        for (i in addedContacts.indices) {
            contactEmail =
                if (addedContacts[i].megaUser != null && addedContacts[i].contact != null) {
                    addedContacts[i].megaUser?.email
                } else {
                    addedContacts[i].fullName
                }
            if (contactEmail != null) {
                contactsSelected.add(contactEmail)
            }
        }
        Timber.d("Contacts selected: %s", contactsSelected.size)

        if (comesFromChat) {
            addParticipants(contactsSelected)
        } else if (onNewGroup) {
            val chatTitle: String
            if (nameGroup != null && nameGroup?.text?.isNotEmpty() == true) {
                chatTitle = nameGroup?.text.toString()
                startConversation(contactsSelected, megaContacts, chatTitle)
            } else {
                startConversation(contactsSelected, megaContacts, null)
            }
        } else {
            newGroup()
        }
        hideSoftKeyboard()
    }

    private fun toInviteContact() {
        navigator.openInviteContactActivity(
            this,
            false
        )
    }

    override fun onClick(v: View) {
        val id = v.id
        when (id) {
            R.id.layout_scan_qr -> {
                Timber.d("Scan QR code pressed")
                if (CallUtil.isNecessaryDisableLocalCamera() != MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
                    CallUtil.showConfirmationOpenCamera(this, Constants.ACTION_OPEN_QR, true)
                    return
                }
                initScanQR()
            }

            R.id.add_contact_list_empty_invite_button -> {
                toInviteContact()
            }

            R.id.ekr_switch -> {
                isEKREnabled = ekrSwitch?.isChecked == true
                setGetChatLinkVisibility()
            }

            R.id.allow_add_participants_switch -> {
                isAllowAddParticipantsEnabled = allowAddParticipantsSwitch?.isChecked == true
            }

            R.id.fab_button_next -> {
                when (contactType) {
                    Constants.CONTACT_TYPE_DEVICE -> {
                        inviteContacts(addedContactsPhone)
                    }

                    Constants.CONTACT_TYPE_MEGA -> {
                        if (onlyCreateGroup && !isStartConversation && addedContactsMEGA.isEmpty()) {
                            showSnackbar(getString(R.string.error_creating_group_and_attaching_file))
                            return
                        }
                        setResultContacts(addedContactsMEGA, true)
                    }

                    else -> {
                        shareWith(addedContactsShare)
                    }
                }
                hideSoftKeyboard()
            }
        }
    }

    /**
     * Init scan q r
     *
     */
    fun initScanQR() {
        val intent = Intent(this, QRCodeComposeActivity::class.java)
        intent.putExtra(Constants.INVITE_CONTACT, true)
        startActivityForResult(intent, SCAN_QR_FOR_ADD_CONTACTS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        Timber.d("onActivityResult")
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == SCAN_QR_FOR_ADD_CONTACTS && resultCode == RESULT_OK && intent != null) {
            val mail = intent.getStringExtra(Constants.INTENT_EXTRA_KEY_MAIL)

            if (mail != null && mail != "") {
                confirmAddMail = mail
                queryIfContactShouldBeAddedTask = QueryIfContactShouldBeAddedTask(this)
                queryIfContactShouldBeAddedTask?.execute(true)
            }
        }
    }

    override fun onBackPressed() {
        val psaWebBrowser = psaWebBrowser
        if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return
        retryConnectionsAndSignalPresence()

        if (onNewGroup) {
            if (addedContactsMEGA.contains(myContact)) {
                addedContactsMEGA.remove(myContact)
            }
            if (comesFromRecent) {
                finish()
            } else {
                returnToAddContacts()
                createMyContact()
            }
        } else if (createNewGroup && (newGroup || onlyCreateGroup)) {
            finish()
        } else {
            super.onBackPressed()
        }
    }

    private fun addParticipants(contacts: ArrayList<String>) {
        val intent = Intent()
        intent.putStringArrayListExtra(EXTRA_CONTACTS, contacts)

        setResult(RESULT_OK, intent)
        hideSoftKeyboard()
        finish()
    }

    private fun returnToAddContacts() {
        onNewGroup = false
        supportActionBar?.let { setTitleAB() }
        setRecyclersVisibility()
        addContactsLayout?.visibility = View.VISIBLE
        if (addedContactsMEGA.size == 0) {
            containerAddedContactsRecyclerView?.visibility = View.GONE
        }
        setMegaAdapterContacts(
            filteredContactMEGA,
            MegaContactsAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT
        )
        newGroupLayout?.visibility = View.GONE
        visibilityFastScroller()
        setSendInvitationVisibility()
        setSearchVisibility()
        if (visibleContactsMEGA.isEmpty()) {
            onBackPressed()
        }
    }

    private fun createMyContact() {
        if (myContact == null) {
            val contactDB =
                dbH.findContactByHandle(MegaApiJava.base64ToUserHandle(megaApi.myUserHandle))
            var myFullName = megaChatApi.myFullname

            if (myFullName != null) {
                if (myFullName.trim { it <= ' ' }.isEmpty()) {
                    myFullName = megaChatApi.myEmail
                }
            } else {
                myFullName = megaChatApi.myEmail
            }

            myContact = MegaContactAdapter(
                contactDB,
                megaApi.myUser,
                getString(R.string.chat_me_text_bracket, myFullName)
            )
        }
    }

    /**
     * New group
     *
     */
    fun newGroup() {
        Timber.d("newGroup")

        if (isAsyncTaskRunning(filterContactsTask)) {
            filterContactsTask?.cancel(true)
        }
        onNewGroup = true
        searchExpand = false
        supportActionBar?.let {
            it.title = getString(R.string.title_new_group)
            it.subtitle = getString(R.string.subtitle_new_group)
        }

        createMyContact()

        myContact?.let { addedContactsMEGA.add(it) }
        newGroupHeaderList?.text = resources.getQuantityString(
            R.plurals.subtitle_of_group_chat,
            addedContactsMEGA.size,
            addedContactsMEGA.size
        )

        searchMenuItem?.setVisible(false)
        addContactsLayout?.visibility = View.GONE
        newGroupLayout?.visibility = View.VISIBLE

        setSendInvitationVisibility()
        setMegaAdapterContacts(
            addedContactsMEGA,
            MegaContactsAdapter.ITEM_VIEW_TYPE_LIST_GROUP_CHAT
        )
        visibilityFastScroller()
        setRecyclersVisibility()
        if (isConfirmDeleteShown) {
            for (i in addedContactsMEGA.indices) {
                if (getMegaContactMail(addedContactsMEGA[i]) == confirmDeleteMail) {
                    showConfirmationDeleteFromChat(addedContactsMEGA[i])
                    break
                }
            }
        }
    }

    private fun startConversation(
        contacts: ArrayList<String>,
        megaContacts: Boolean,
        chatTitle: String?,
    ) {
        Timber.d("startConversation")
        val intent = Intent()
        intent.putStringArrayListExtra(EXTRA_CONTACTS, contacts)

        intent.putExtra(EXTRA_MEGA_CONTACTS, megaContacts)

        if (getChatLinkBox?.isChecked == true && (chatTitle == null || chatTitle.trim { it <= ' ' }
                .isEmpty())) {
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
                .setTitle(getString(R.string.enter_group_name))
                .setMessage(getString(R.string.alert_enter_group_name))
                .setPositiveButton(getString(R.string.general_ok), null)
                .show()
            return
        }

        if (chatTitle != null) {
            intent.putExtra(EXTRA_CHAT_TITLE, chatTitle)
        }

        if (onNewGroup) {
            intent.putExtra(EXTRA_EKR, isEKREnabled)
            intent.putExtra(ALLOW_ADD_PARTICIPANTS, isAllowAddParticipantsEnabled)
            intent.putExtra(EXTRA_GROUP_CHAT, onNewGroup)
            intent.putExtra(EXTRA_CHAT_LINK, getChatLinkBox?.isChecked)
        }

        setResult(RESULT_OK, intent)
        hideSoftKeyboard()
        finish()
    }

    private fun shareWith(addedContacts: ArrayList<ShareContactInfo>?) {
        Timber.d("shareWith")

        val contactsSelected = ArrayList<String?>()
        if (addedContacts != null) {
            for (i in addedContacts.indices) {
                contactsSelected.add(getShareContactMail(addedContacts[i]))
            }
        }

        val intent = Intent()
        intent.putStringArrayListExtra(EXTRA_CONTACTS, contactsSelected)
        if (multipleSelectIntent == 0) {
            intent.putExtra(EXTRA_NODE_HANDLE, nodeHandle)
            intent.putExtra(EXTRA_MULTISELECT, 0)
        } else if (multipleSelectIntent == 1) {
            intent.putExtra(EXTRA_NODE_HANDLE, nodeHandles)
            intent.putExtra(EXTRA_MULTISELECT, 1)
        }

        intent.putExtra(EXTRA_MEGA_CONTACTS, false)
        setResult(RESULT_OK, intent)
        hideSoftKeyboard()
        finish()
    }

    private fun inviteContacts(addedContacts: ArrayList<PhoneContactInfo>?) {
        Timber.d("inviteContacts")

        var contactEmail: String?
        val contactsSelected = ArrayList<String>()
        if (addedContacts != null) {
            for (i in addedContacts.indices) {
                contactEmail = addedContacts[i].email
                if (fromAchievements) {
                    if (contactEmail != null && mailsFromAchievements?.contains(contactEmail) == false) {
                        contactsSelected.add(contactEmail)
                    }
                } else {
                    if (contactEmail != null) {
                        contactsSelected.add(contactEmail)
                    }
                }
            }
        }

        val intent = Intent()
        intent.putStringArrayListExtra(EXTRA_CONTACTS, contactsSelected)
        for (i in contactsSelected.indices) {
            Timber.d("setResultContacts: %s", contactsSelected[i])
        }

        intent.putExtra(EXTRA_MEGA_CONTACTS, false)
        setResult(RESULT_OK, intent)
        hideSoftKeyboard()
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        Timber.d("onRequestPermissionsResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.REQUEST_READ_CONTACTS -> {
                Timber.d("REQUEST_READ_CONTACTS")
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val hasReadContactsPermissions =
                        hasPermissions(this, Manifest.permission.READ_CONTACTS)
                    if (hasReadContactsPermissions && contactType == Constants.CONTACT_TYPE_DEVICE) {
                        filteredContactsPhone.clear()
                        setEmptyStateVisibility(true)

                        progressBar?.visibility = View.VISIBLE
                        GetContactsTask(this).execute()
                    } else if (hasReadContactsPermissions && contactType == Constants.CONTACT_TYPE_BOTH) {
                        progressBar?.visibility = View.VISIBLE
                        emptyTextView?.setText(R.string.contacts_list_empty_text_loading_share)

                        getContactsTask = GetContactsTask(this)
                        getContactsTask?.execute()
                    }
                } else if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    val hasReadContactsPermissions =
                        hasPermissions(this, Manifest.permission.READ_CONTACTS)
                    queryPermissions = false
                    supportInvalidateOptionsMenu()
                    if (!hasReadContactsPermissions && contactType == Constants.CONTACT_TYPE_DEVICE) {
                        Timber.w("Permission denied")
                        supportActionBar?.let { setTitleAB() }
                        filteredContactsPhone.clear()
                        setEmptyStateVisibility(true)
                        emptyTextView?.setText(R.string.no_contacts_permissions)

                        progressBar?.visibility = View.GONE
                    }
                }
            }
        }
    }

    /**
     * Set recyclers visibility
     *
     */
    fun setRecyclersVisibility() {
        if (contactType == Constants.CONTACT_TYPE_MEGA) {
            if (filteredContactMEGA.size > 0) {
                containerContacts?.visibility = View.VISIBLE
            } else {
                if (onNewGroup) {
                    containerContacts?.visibility = View.VISIBLE
                } else {
                    containerContacts?.visibility = View.GONE
                }
            }
        } else if (contactType == Constants.CONTACT_TYPE_DEVICE) {
            if (filteredContactsPhone.size > 0) {
                containerContacts?.visibility = View.VISIBLE
            } else {
                containerContacts?.visibility = View.GONE
            }
        } else {
            if (filteredContactsShare.size >= 2) {
                containerContacts?.visibility = View.VISIBLE
            } else {
                containerContacts?.visibility = View.GONE
            }
        }
    }

    /**
     * Visibility fast scroller
     *
     */
    fun visibilityFastScroller() {
        fastScroller?.setRecyclerView(recyclerViewList)
        if (contactType == Constants.CONTACT_TYPE_MEGA) {
            if (adapterMEGA == null) {
                fastScroller?.visibility = View.GONE
            } else {
                if (adapterMEGA.countOrZero() < Constants.MIN_ITEMS_SCROLLBAR_CONTACT) {
                    fastScroller?.visibility = View.GONE
                } else {
                    fastScroller?.visibility = View.VISIBLE
                }
            }
        } else if (contactType == Constants.CONTACT_TYPE_DEVICE) {
            if (adapterPhone == null) {
                fastScroller?.visibility = View.GONE
            } else {
                if (adapterPhone.countOrZero() < Constants.MIN_ITEMS_SCROLLBAR_CONTACT) {
                    fastScroller?.visibility = View.GONE
                } else {
                    fastScroller?.visibility = View.VISIBLE
                }
            }
        } else {
            if (adapterShareHeader == null) {
                fastScroller?.visibility = View.GONE
            } else {
                if (adapterShareHeader.countOrZero() < Constants.MIN_ITEMS_SCROLLBAR_CONTACT) {
                    fastScroller?.visibility = View.GONE
                } else {
                    fastScroller?.visibility = View.VISIBLE
                }
            }
        }
        fastScroller?.setUpScrollListener(object : FastScrollerScrollListener {
            override fun onScrolled() {
                fabButton?.hide()
            }

            override fun onScrolledToTop() {
                showFabButton()
            }
        })
    }

    private fun showHeader(isVisible: Boolean) {
        headerContacts?.visibility =
            if (!comesFromChat && isVisible) View.VISIBLE else View.GONE
    }

    override fun onUsersUpdate(api: MegaApiJava, users: ArrayList<MegaUser>?) {
    }

    override fun onUserAlertsUpdate(api: MegaApiJava, userAlerts: ArrayList<MegaUserAlert>?) {
    }

    override fun onNodesUpdate(api: MegaApiJava, nodeList: ArrayList<MegaNode>?) {
    }

    override fun onReloadNeeded(api: MegaApiJava) {
    }

    override fun onAccountUpdate(api: MegaApiJava) {
    }

    override fun onContactRequestsUpdate(
        api: MegaApiJava,
        requests: ArrayList<MegaContactRequest>?,
    ) {
        if (requests != null) {
            for (i in requests.indices) {
                val cr = requests[i]
                if ((cr.status == MegaContactRequest.STATUS_ACCEPTED) && (cr.isOutgoing)) {
                    Timber.d(
                        "ACCEPT OPR: %s cr.isOutgoing: %s cr.getStatus: %d",
                        cr.sourceEmail,
                        cr.isOutgoing,
                        cr.status
                    )
                    getContactsTask = GetContactsTask(this)
                    getContactsTask?.execute()
                }
            }
        }
    }

    override fun onEvent(api: MegaApiJava, event: MegaEvent?) {
    }

    override fun onSetsUpdate(api: MegaApiJava, sets: ArrayList<MegaSet>?) {
    }

    override fun onSetElementsUpdate(api: MegaApiJava, elements: ArrayList<MegaSetElement>?) {
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
    }

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestStart: %s", request.requestString)
    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type == MegaRequest.TYPE_INVITE_CONTACT) {
            Timber.d("MegaRequest.TYPE_INVITE_CONTACT finished: %s", request.number)

            if (request.number == MegaContactRequest.INVITE_ACTION_REMIND.toLong()) {
                showSnackbar(getString(R.string.context_contact_invitation_resent))
            } else {
                if (e.errorCode == MegaError.API_OK) {
                    Timber.d("OK INVITE CONTACT: %s", request.email)
                    if (request.number == MegaContactRequest.INVITE_ACTION_ADD.toLong()) {
                        showSnackbar(
                            getString(
                                R.string.context_contact_request_sent,
                                request.email
                            )
                        )
                    } else if (request.number == MegaContactRequest.INVITE_ACTION_DELETE.toLong()) {
                        showSnackbar(getString(R.string.context_contact_invitation_deleted))
                    }
                } else {
                    Timber.w("ERROR: %d___%s", e.errorCode, e.errorString)
                    if (e.errorCode == MegaError.API_EEXIST) {
                        showSnackbar(
                            getString(
                                R.string.context_contact_already_exists,
                                request.email
                            )
                        )
                    } else if (request.number == MegaContactRequest.INVITE_ACTION_ADD.toLong() && e.errorCode == MegaError.API_EARGS) {
                        showSnackbar(getString(R.string.error_own_email_as_contact))
                    } else {
                        showSnackbar(getString(R.string.general_error))
                    }
                }
            }
        }
    }

    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {
    }

    private fun onChatPresenceLastGreen(userHandle: Long, lastGreen: Int) {
        Timber.d("onChatPresenceLastGreen")
        val state = megaChatApi.getUserOnlineStatus(userHandle)
        if (state != MegaChatApi.STATUS_ONLINE && state != MegaChatApi.STATUS_BUSY && state != MegaChatApi.STATUS_INVALID) {
            val formattedDate = TimeUtils.lastGreenDate(this, lastGreen)
            if (userHandle != megaChatApi.myUserHandle) {
                Timber.d("Status last green for the user: %s", userHandle)
                //                Replace on visible MEGA contacts (all my visible contacts)
                val itrReplace: ListIterator<MegaContactAdapter> =
                    visibleContactsMEGA.listIterator()
                while (itrReplace.hasNext()) {
                    val contactToUpdate = itrReplace.next()
                    if (getMegaContactHandle(contactToUpdate) == userHandle) {
                        contactToUpdate.lastGreen = formattedDate
                        break
                    }
                }
                //                Replace on list adapter (filtered or search filtered MEGA contacts)
                var indexToReplace = -1
                adapterMEGA?.contacts?.firstOrNull { getMegaContactHandle(it) == userHandle }?.let {
                    it.lastGreen = formattedDate
                    indexToReplace = adapterMEGA?.contacts?.indexOf(it) ?: -1
                }
                if (indexToReplace != -1) {
                    adapterMEGA?.updateContactStatus(indexToReplace)
                }
                //                Replace on filtered MEGA contacts (without search)
                if (filteredContactMEGA.size > 0) {
                    val itrReplace3: ListIterator<MegaContactAdapter?> =
                        filteredContactMEGA.listIterator()
                    while (itrReplace3.hasNext()) {
                        val contactToUpdate = itrReplace3.next()
                        if (contactToUpdate != null) {
                            if (getMegaContactHandle(contactToUpdate) == userHandle) {
                                contactToUpdate.lastGreen = formattedDate
                                break
                            }
                        } else {
                            break
                        }
                    }
                }
                //                Replace, if exist, on search filtered MEGA contacts
                if (queryContactMEGA.size > 0) {
                    val itrReplace4: ListIterator<MegaContactAdapter> =
                        queryContactMEGA.listIterator()
                    while (itrReplace4.hasNext()) {
                        val contactToUpdate = itrReplace4.next()
                        if (getMegaContactHandle(contactToUpdate) == userHandle) {
                            contactToUpdate.lastGreen = formattedDate
                            break
                        }
                    }
                }
                //                Replace, if exist, on added adapter and added MEGA contacts
                if (addedContactsMEGA.size > 0) {
                    val itrReplace5: ListIterator<MegaContactAdapter?> =
                        addedContactsMEGA.listIterator()
                    while (itrReplace5.hasNext()) {
                        val contactToUpdate = itrReplace5.next()
                        if (contactToUpdate != null) {
                            if (getMegaContactHandle(contactToUpdate) == userHandle) {
                                contactToUpdate.lastGreen = formattedDate
                                break
                            }
                        } else {
                            break
                        }
                    }
                    if (adapterMEGAContacts != null) {
                        adapterMEGAContacts?.setContacts(addedContactsMEGA)
                    }
                }
            }
            Timber.d("Date last green: %s", formattedDate)
        }
    }

    /**
     * Receive changes to OnChatPresenceLastGreen and make the necessary changes
     */
    private fun checkChatChanges() {
        lifecycleScope.launch {
            monitorChatPresenceLastGreenUpdatesUseCase()
                .collect {
                    onChatPresenceLastGreen(it.handle, it.lastGreen)
                }
        }
    }

    private fun checkForUnVerifiedContacts(): Boolean {
        for (info in addedContactsShare) {
            if (!info.isMegaContact) {
                return true
            } else {
                val isVerified =
                    megaApi.areCredentialsVerified(info.getMegaContactAdapter().megaUser)
                if (!isVerified) return true
            }
        }
        return false
    }

    private fun observeFlow() {
        this.collectFlow(
            viewModel.state,
            Lifecycle.State.STARTED
        ) { addContactState: AddContactState ->
            if (adapterShare == null) {
                adapterShare = ShareContactsAdapter(addContactActivity, addedContactsShare)
            }
            adapterShare?.updateContactVerification(addContactState.isContactVerificationWarningEnabled)
            isContactVerificationOn = addContactState.isContactVerificationWarningEnabled
            participantsLimitWarningView?.visibility =
                if (addContactState.showUserLimitWarningDialog) View.VISIBLE else View.GONE
        }
    }

    companion object {
        private const val SCAN_QR_FOR_ADD_CONTACTS = 1111

        /**
         * Extra Mega Contacts
         */
        const val EXTRA_MEGA_CONTACTS: String = "mega_contacts"

        /**
         * Extra Contacts
         */
        const val EXTRA_CONTACTS: String = "extra_contacts"

        /**
         * Extra Meeting
         */
        const val EXTRA_MEETING: String = "extra_meeting"

        /**
         * Extra Node Handle
         */
        const val EXTRA_NODE_HANDLE: String = "node_handle"

        /**
         * Extra Chat Title
         */
        const val EXTRA_CHAT_TITLE: String = "chatTitle"

        /**
         * Extra Group Chat
         */
        const val EXTRA_GROUP_CHAT: String = "groupChat"

        /**
         * Extra Ekr
         */
        const val EXTRA_EKR: String = "EKR"

        /**
         * Allow Add Participants
         */
        const val ALLOW_ADD_PARTICIPANTS: String = "ALLOW_ADD_PARTICIPANTS"

        /**
         * Extra Chat Link
         */
        const val EXTRA_CHAT_LINK: String = "chatLink"

        /**
         * Extra Contact Type
         */
        const val EXTRA_CONTACT_TYPE: String = "contactType"

        /**
         * Extra Only Create Group
         */
        const val EXTRA_ONLY_CREATE_GROUP: String = "onlyCreateGroup"

        /**
         * Extra Is Start Conversation
         */
        const val EXTRA_IS_START_CONVERSATION: String = "isStartConversation"

        /**
         * Extra Multiselect
         */
        const val EXTRA_MULTISELECT: String = "MULTISELECT"

        /**
         * From Recent
         */
        const val FROM_RECENT: String = "comesFromRecent"

        /**
         * Is Allowed Add Participants
         */
        const val IS_ALLOWED_ADD_PARTICIPANTS: String = "isAllowAddParticipants"

        private fun isValidEmail(target: CharSequence?): Boolean {
            if (target == null) {
                return false
            } else {
                Timber.d("isValid")
                return Constants.EMAIL_ADDRESS.matcher(target).matches()
            }
        }
    }

    private fun RecyclerView.Adapter<*>?.countOrZero() = this?.itemCount ?: 0
}
