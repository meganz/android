package mega.privacy.android.app.main.megachat.chat.explorer

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.extensions.consumeInsetsWithToolbar
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.main.listeners.CreateGroupChatWithPublicLink
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.user.UserLastGreen
import mega.privacy.android.domain.usecase.contact.MonitorChatPresenceLastGreenUpdatesUseCase
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatPeerList
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaUser
import timber.log.Timber
import javax.inject.Inject


private const val CHAT_EXPLORER_FRAGMENT = "chatExplorerFragment"
private const val QUERY_SEARCH = "querySearch"
private const val IS_SEARCH_EXPANDED = "isSearchExpanded"
private const val CONTACT_TYPE = "contactType"

@AndroidEntryPoint
internal class ChatExplorerActivity : PasscodeActivity(), View.OnClickListener,
    MegaChatRequestListenerInterface {

    @Inject
    lateinit var monitorChatPresenceLastGreenUpdatesUseCase: MonitorChatPresenceLastGreenUpdatesUseCase

    private var fragmentContainer: FrameLayout? = null
    var chatExplorerFragment: ChatExplorerFragment? = null
    var fab: FloatingActionButton? = null
    private var chatIdFrom: Long = -1

    private var nodeHandles: LongArray? = null
    private var messagesIds: LongArray? = null
    private var userHandles: LongArray? = null

    var searchMenuItem: MenuItem? = null

    var searchView: SearchView? = null

    var querySearch: String = ""
    var isSearchExpanded: Boolean = false
    private var pendingToOpenSearchView: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        Timber.d("onCreate first")
        super.onCreate(savedInstanceState)

        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            return
        }

        this.enableEdgeToEdge()
        setContentView(R.layout.activity_chat_explorer)
        this.consumeInsetsWithToolbar(customToolbar = findViewById(R.id.app_bar_layout_chat_explorer))

        fragmentContainer = findViewById<View>(R.id.fragment_container_chat_explorer) as FrameLayout
        fab = findViewById<View>(R.id.fab_chat_explorer) as FloatingActionButton
        fab?.setOnClickListener(this)

        //Set toolbar
        setSupportActionBar(findViewById(R.id.toolbar_chat_explorer))
        supportActionBar?.let {
            it.title = getString(R.string.title_chat_explorer)
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }

        showFabButton(false)

        if (intent != null) {
            Timber.d("Intent received")
            if (intent.action != null) {
                if (intent.action === Constants.ACTION_FORWARD_MESSAGES) {
                    messagesIds = intent.getLongArrayExtra(Constants.ID_MESSAGES)
                    if (messagesIds != null) {
                        messagesIds?.size?.let { Timber.d("Number of messages to forward: $it") }
                    }
                    chatIdFrom = intent.getLongExtra(Constants.ID_CHAT_FROM, -1)
                }
            } else {
                nodeHandles = intent.getLongArrayExtra(Constants.NODE_HANDLES)
                nodeHandles?.first()?.let {
                    Timber.d("Node handle is: $it")
                }
                userHandles = intent.getLongArrayExtra(Constants.USER_HANDLES)
                userHandles?.size?.let { Timber.d("User handles size: $it") }
            }
        }

        if (savedInstanceState != null) {
            chatExplorerFragment = supportFragmentManager.getFragment(
                savedInstanceState,
                CHAT_EXPLORER_FRAGMENT
            ) as ChatExplorerFragment?
            querySearch = savedInstanceState.getString(QUERY_SEARCH, "")
            isSearchExpanded = savedInstanceState.getBoolean(IS_SEARCH_EXPANDED, isSearchExpanded)

            if (isSearchExpanded) {
                pendingToOpenSearchView = true
            }
        } else if (chatExplorerFragment == null) {
            chatExplorerFragment = ChatExplorerFragment.newInstance()
        }

        chatExplorerFragment?.let {
            val ft = supportFragmentManager.beginTransaction()
            ft.replace(
                R.id.fragment_container_chat_explorer,
                it, CHAT_EXPLORER_FRAGMENT
            )
            ft.commitNow()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        supportFragmentManager.findFragmentByTag(CHAT_EXPLORER_FRAGMENT)?.let {
            supportFragmentManager.putFragment(
                outState, CHAT_EXPLORER_FRAGMENT,
                it
            )
        }

        outState.putString(QUERY_SEARCH, querySearch)
        outState.putBoolean(IS_SEARCH_EXPANDED, isSearchExpanded)
    }

    fun setToolbarSubtitle(subTitle: String?) {
        supportActionBar?.subtitle = subTitle
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Timber.d("onCreateOptionsMenu")

        // Inflate the menu items for use in the action bar
        val inflater = menuInflater
        inflater.inflate(R.menu.file_explorer_action, menu)
        searchMenuItem = menu.findItem(R.id.cab_menu_search)
        val createFolderMenuItem = menu.findItem(R.id.cab_menu_create_folder)
        val newChatMenuItem = menu.findItem(R.id.cab_menu_new_chat)

        createFolderMenuItem.setVisible(false)
        newChatMenuItem.setVisible(false)

        searchView = searchMenuItem?.actionView as SearchView?

        searchView?.queryHint = getString(R.string.hint_action_search)
        searchView?.findViewById<View>(androidx.appcompat.R.id.search_plate)
            ?.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))

        if (searchView != null) {
            searchView?.setIconifiedByDefault(true)
        }

        searchMenuItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                isSearchExpanded = true
                chatExplorerFragment?.enableSearch(true)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                isSearchExpanded = false
                chatExplorerFragment?.enableSearch(false)
                return true
            }
        })

        searchView?.maxWidth = Int.MAX_VALUE
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                Timber.d("Query: $query")
                Util.hideKeyboard(this@ChatExplorerActivity, 0)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                querySearch = newText
                chatExplorerFragment?.search(newText)
                return true
            }
        })

        return super.onCreateOptionsMenu(menu)
    }

    fun isPendingToOpenSearchView() {
        if (pendingToOpenSearchView && searchMenuItem != null && searchView != null) {
            val query = querySearch
            searchMenuItem?.expandActionView()
            searchView?.setQuery(query, false)
            pendingToOpenSearchView = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected")

        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }

            R.id.cab_menu_new_chat -> {
                if (megaApi.rootNode != null) {
                    val contacts = megaApi.contacts
                    if (contacts == null) {
                        showSnackbar(getString(R.string.no_contacts_invite))
                    } else {
                        if (contacts.isEmpty()) {
                            showSnackbar(getString(R.string.no_contacts_invite))
                        } else {
                            Intent(
                                this,
                                AddContactActivity::class.java
                            ).apply {
                                putExtra(CONTACT_TYPE, Constants.CONTACT_TYPE_MEGA)
                            }.let {
                                startActivityForResult(it, Constants.REQUEST_CREATE_CHAT)
                            }
                        }
                    }
                } else {
                    Timber.w("Online but not megaApi")
                    Util.showErrorAlertDialog(
                        getString(R.string.error_server_connection_problem), false,
                        this
                    )
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        Timber.d("onActivityResult ${requestCode}____$resultCode")

        if (requestCode == Constants.REQUEST_CREATE_CHAT && resultCode == RESULT_OK) {
            Timber.d("REQUEST_CREATE_CHAT OK")

            if (intent == null) {
                Timber.w("Intent is null")
                return
            }

            val contactsData = intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)

            if (contactsData != null) {
                if (contactsData.size == 1) {
                    val user = megaApi.getContact(contactsData[0])
                    if (user != null) {
                        Timber.d("Start one to one chat")
                        startOneToOneChat(user)
                    }
                } else {
                    Timber.d("Create GROUP chat")
                    val peers = MegaChatPeerList.createInstance()
                    for (i in contactsData.indices) {
                        val user = megaApi.getContact(contactsData[i])
                        if (user != null) {
                            peers.addPeer(user.handle, MegaChatPeerList.PRIV_STANDARD)
                        }
                    }
                    Timber.d("Create group chat with participants: ${peers.size()}")

                    val chatTitle = intent.getStringExtra(AddContactActivity.EXTRA_CHAT_TITLE)
                    val allowAddParticipants =
                        intent.getBooleanExtra(AddContactActivity.ALLOW_ADD_PARTICIPANTS, false)

                    val isEKR = intent.getBooleanExtra(AddContactActivity.EXTRA_EKR, false)
                    if (isEKR) {
                        megaChatApi.createGroupChat(
                            peers, chatTitle, false, false, allowAddParticipants,
                            this
                        )
                    } else {
                        val chatLink =
                            intent.getBooleanExtra(AddContactActivity.EXTRA_CHAT_LINK, false)

                        if (chatLink) {
                            if (chatTitle.isNullOrEmpty().not()) {
                                val listener = CreateGroupChatWithPublicLink(this, chatTitle)
                                megaChatApi.createPublicChat(
                                    peers,
                                    chatTitle,
                                    false,
                                    false,
                                    allowAddParticipants,
                                    listener
                                )
                            } else {
                                Util.showAlert(
                                    this,
                                    getString(R.string.message_error_set_title_get_link),
                                    null
                                )
                            }
                        } else {
                            megaChatApi.createPublicChat(
                                peers, chatTitle, false, false, allowAddParticipants,
                                this
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startOneToOneChat(user: MegaUser) {
        Timber.d("User Handle: ${user.handle}")
        val chat = megaChatApi.getChatRoomByUser(user.handle)
        val peers = MegaChatPeerList.createInstance()
        if (chat == null) {
            Timber.d("No chat, create it!")
            peers.addPeer(user.handle, MegaChatPeerList.PRIV_STANDARD)
            megaChatApi.createChat(false, peers, this)
        } else {
            Timber.d("There is already a chat, open it!")
            showSnackbar(getString(R.string.chat_already_exists))
        }
    }

    fun showSnackbar(snackbarMessage: String) {
        fragmentContainer?.let { showSnackbar(it, snackbarMessage) }
    }

    private fun chooseChats(listItems: ArrayList<ChatExplorerListItem>) {
        Timber.d("chooseChats")

        val intent = Intent()

        if (nodeHandles != null) {
            intent.putExtra(Constants.NODE_HANDLES, nodeHandles)
        }

        if (userHandles != null) {
            intent.putExtra(Constants.USER_HANDLES, userHandles)
        }

        if (messagesIds != null) {
            intent.putExtra(Constants.ID_MESSAGES, messagesIds)
        }
        val selectedChats = listItems.mapNotNull { it.chat?.chatId }.toLongArray()
        val selectedUsers =
            listItems.filter { it.chat?.chatId == null }.mapNotNull { it.contactItem?.user?.handle }
                .toLongArray()

        if (selectedChats.isNotEmpty()) {
            intent.putExtra(Constants.SELECTED_CHATS, selectedChats)
        }

        if (selectedUsers.isNotEmpty()) {
            intent.putExtra(Constants.SELECTED_USERS, selectedUsers)
        }

        setResult(RESULT_OK, intent)
        finish()
    }

    fun showFabButton(show: Boolean) {
        if (show) {
            fab?.visibility = View.VISIBLE
        } else {
            fab?.visibility = View.GONE
        }
    }

    fun collapseSearchView() {
        searchMenuItem?.collapseActionView()
    }

    override fun onClick(v: View) {
        Timber.d("onClick")

        val id = v.id
        if (id == R.id.fab_chat_explorer) {
            chatExplorerFragment?.addedChats?.let { chooseChats(it) }
        } else if (id == R.id.new_group_button) {
            if (megaApi.rootNode != null) {
                val contacts = megaApi.contacts
                if (contacts.isNullOrEmpty()) {
                    showSnackbar(getString(R.string.no_contacts_invite))
                    return
                }

                val intent = Intent(
                    this,
                    AddContactActivity::class.java
                )
                intent.putExtra(CONTACT_TYPE, Constants.CONTACT_TYPE_MEGA)
                intent.putExtra("onlyCreateGroup", true)
                startActivityForResult(intent, Constants.REQUEST_CREATE_CHAT)
            } else {
                Timber.w("Online but not megaApi")
                Util.showErrorAlertDialog(
                    getString(R.string.error_server_connection_problem), false,
                    this
                )
            }
        }
    }

    override fun onRequestStart(api: MegaChatApiJava, request: MegaChatRequest) {
    }

    override fun onRequestUpdate(api: MegaChatApiJava, request: MegaChatRequest) {
    }

    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        Timber.d("onRequestFinish(CHAT)")

        if (request.type == MegaChatRequest.TYPE_CREATE_CHATROOM) {
            Timber.d("Create chat request finish.")
            onRequestFinishCreateChat(e.errorCode, request.chatHandle, false)
        }
    }

    fun onRequestFinishCreateChat(errorCode: Int, chatHandle: Long, publicLink: Boolean) {
        Timber.d("onRequestFinishCreateChat")

        if (errorCode == MegaChatError.ERROR_OK) {
            Timber.d("Chat CREATED.")

            //Update chat view
            if (chatExplorerFragment?.isAdded == true) {
                chatExplorerFragment?.setChats()
            }
            showSnackbar(getString(R.string.new_group_chat_created))
        } else {
            Timber.e("ERROR WHEN CREATING CHAT %s", errorCode)
            showSnackbar(getString(R.string.create_chat_error))
        }
    }

    override fun onRequestTemporaryError(
        api: MegaChatApiJava,
        request: MegaChatRequest,
        e: MegaChatError,
    ) {
    }

    private fun onChatPresenceLastGreen(userHandle: Long, lastGreen: Int) {
        val state = megaChatApi.getUserOnlineStatus(userHandle)
        if (state != MegaChatApi.STATUS_ONLINE && state != MegaChatApi.STATUS_BUSY && state != MegaChatApi.STATUS_INVALID) {
            val formattedDate = TimeUtils.lastGreenDate(this, lastGreen)
            if (userHandle != megaChatApi.myUserHandle) {
                chatExplorerFragment =
                    supportFragmentManager.findFragmentByTag(CHAT_EXPLORER_FRAGMENT) as ChatExplorerFragment?
                chatExplorerFragment?.updateLastGreenContact(userHandle, formattedDate)
            }
        }
    }

    /**
     * Receive changes to OnChatPresenceLastGreen and make the necessary changes
     */
    fun checkChatChanges() {
        this.collectFlow(
            monitorChatPresenceLastGreenUpdatesUseCase(),
            Lifecycle.State.STARTED
        ) { userPresenceLastGreen: UserLastGreen ->
            Timber.d("onChatPresenceLastGreen %s", userPresenceLastGreen)
            onChatPresenceLastGreen(userPresenceLastGreen.handle, userPresenceLastGreen.lastGreen)
        }
    }
}
