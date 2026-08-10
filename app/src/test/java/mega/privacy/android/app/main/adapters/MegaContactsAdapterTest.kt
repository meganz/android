package mega.privacy.android.app.main.adapters

import android.app.Activity
import android.widget.FrameLayout
import androidx.compose.ui.graphics.Color
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.app.MegaContactAdapter
import mega.privacy.android.app.main.adapters.mapper.MegaContactAdapterToContactItemUiStateMapper
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaUser
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
class MegaContactsAdapterTest {

    private lateinit var underTest: MegaContactsAdapter

    private val activity: AddContactActivity = mock()
    private val megaApi: MegaApiAndroid = mock {
        on { myEmail } doReturn "me@example.com"
        on { areCredentialsVerified(any()) } doReturn false
    }
    private val megaChatApi: MegaChatApiAndroid = mock()
    private val mapper: MegaContactAdapterToContactItemUiStateMapper = mock()
    private val listFragment: RecyclerView = mock()

    private var hostController: ActivityController<Activity>? = null
    private var cacheFolderManager: MockedStatic<CacheFolderManager>? = null
    private var avatarUtil: MockedStatic<AvatarUtil>? = null

    @Before
    fun setUp() {
        // The adapter reaches into MegaApplication via Hilt for avatar lookups.
        // Bypass that here so onBindViewHolder runs in unit tests without a
        // real Android Application.
        cacheFolderManager = mockStatic(CacheFolderManager::class.java).also { ms ->
            ms.`when`<Any?> { CacheFolderManager.buildAvatarFile(any()) }.thenReturn(null)
        }
        avatarUtil = mockStatic(AvatarUtil::class.java).also { ms ->
            ms.`when`<Int> { AvatarUtil.getColorAvatar(any<MegaUser>()) }
                .thenReturn(0xFF2E7D32.toInt())
        }

        whenever(
            mapper.invoke(
                megaContact = anyOrNull(),
                mail = any(),
                avatarFile = anyOrNull(),
                avatarColorArgb = any(),
                chatStatusValue = any(),
                isVerified = any(),
            )
        ).thenReturn(uiState())
    }

    @After
    fun tearDown() {
        cacheFolderManager?.close()
        cacheFolderManager = null
        avatarUtil?.close()
        avatarUtil = null
    }

    // region getSectionTitle

    @Test
    fun `test that getSectionTitle returns uppercase first letter of full name`() {
        underTest = newAdapter(
            contacts = arrayListOf(megaContact(fullName = "alice")),
        )

        assertThat(underTest.getSectionTitle(0, context = null)).isEqualTo("A")
    }

    @Test
    fun `test that getSectionTitle returns empty string when full name is null`() {
        underTest = newAdapter(
            contacts = arrayListOf(megaContact(fullName = null)),
        )

        assertThat(underTest.getSectionTitle(0, context = null)).isEmpty()
    }

    // endregion

    // region item-count / view-type / id

    @Test
    fun `test that getItemCount returns the size of the contacts list`() {
        underTest = newAdapter(
            contacts = arrayListOf(megaContact(), megaContact(), megaContact()),
        )

        assertThat(underTest.itemCount).isEqualTo(3)
    }

    @Test
    fun `test that getItemViewType returns the configured adapter type`() {
        underTest = newAdapter(
            adapterType = MegaContactsAdapter.ITEM_VIEW_TYPE_LIST_GROUP_CHAT,
        )

        assertThat(underTest.getItemViewType(0))
            .isEqualTo(MegaContactsAdapter.ITEM_VIEW_TYPE_LIST_GROUP_CHAT)
    }

    @Test
    fun `test that getItemId returns the position as long`() {
        underTest = newAdapter()

        assertThat(underTest.getItemId(7)).isEqualTo(7L)
    }

    // endregion

    // region multi-select state

    @Test
    fun `test that setMultipleSelect true initializes selected items`() {
        underTest = newAdapter()

        underTest.setMultipleSelect(true)

        assertThat(underTest.isMultipleSelect()).isTrue()
        assertThat(underTest.getSelectedItemCount()).isEqualTo(0)
    }

    @Test
    fun `test that toggleSelection adds and removes items from the selection`() {
        underTest = newAdapter(
            contacts = arrayListOf(megaContact("Alice"), megaContact("Bob")),
        )
        underTest.setMultipleSelect(true)

        underTest.toggleSelection(0)
        assertThat(underTest.getSelectedItems()).containsExactly(0)

        underTest.toggleSelection(1)
        assertThat(underTest.getSelectedItems()).containsExactly(0, 1).inOrder()

        underTest.toggleSelection(0)
        assertThat(underTest.getSelectedItems()).containsExactly(1)
    }

    @Test
    fun `test that selectAll selects every position`() {
        underTest = newAdapter(
            contacts = arrayListOf(megaContact("Alice"), megaContact("Bob"), megaContact("Carol")),
        )
        underTest.setMultipleSelect(true)

        underTest.selectAll()

        assertThat(underTest.getSelectedItemCount()).isEqualTo(3)
        assertThat(underTest.getSelectedItems()).containsExactly(0, 1, 2).inOrder()
    }

    @Test
    fun `test that clearSelections empties the selection`() {
        underTest = newAdapter(
            contacts = arrayListOf(megaContact("Alice"), megaContact("Bob")),
        )
        underTest.setMultipleSelect(true)
        underTest.selectAll()

        underTest.clearSelections()

        assertThat(underTest.getSelectedItemCount()).isEqualTo(0)
    }

    @Test
    fun `test that clearSelectionsNoAnimations empties the selection`() {
        underTest = newAdapter(
            contacts = arrayListOf(megaContact("Alice"), megaContact("Bob")),
        )
        underTest.setMultipleSelect(true)
        underTest.selectAll()

        underTest.clearSelectionsNoAnimations()

        assertThat(underTest.getSelectedItemCount()).isEqualTo(0)
    }

    @Test
    fun `test that getSelectedUsers returns the mega users for the selected positions`() {
        val alice = megaContact("Alice", handle = 1L)
        val bob = megaContact("Bob", handle = 2L)
        underTest = newAdapter(contacts = arrayListOf(alice, bob))
        underTest.setMultipleSelect(true)
        underTest.toggleSelection(1)

        val selected = underTest.getSelectedUsers()

        assertThat(selected).hasSize(1)
        assertThat(selected.first()).isEqualTo(bob.megaUser)
    }

    // endregion

    // region contact accessors

    @Test
    fun `test that getContactAt returns the mega user at the given position`() {
        val alice = megaContact("Alice")
        val bob = megaContact("Bob")
        underTest = newAdapter(contacts = arrayListOf(alice, bob))

        assertThat(underTest.getContactAt(0)).isEqualTo(alice.megaUser)
        assertThat(underTest.getContactAt(1)).isEqualTo(bob.megaUser)
    }

    @Test
    fun `test that getContactAt returns null for an out of bounds position`() {
        underTest = newAdapter(contacts = arrayListOf(megaContact("Alice")))

        assertThat(underTest.getContactAt(99)).isNull()
    }

    // endregion

    // region compose onClick wiring

    @Test
    fun `test that creating a view holder wires the row's compose onClick`() {
        underTest = newAdapter(
            contacts = arrayListOf(megaContact("Alice")),
            adapterType = MegaContactsAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT,
        )

        val holder = createHolder(MegaContactsAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT)

        assertThat(holder.rowState.onClick).isNotNull()
    }

    // endregion

    // region selection propagation to the Compose row state

    @Test
    fun `test that binding a selected contact in add contact mode marks the row state as selected`() {
        val contact = megaContact("Alice").apply { isSelected = true }
        underTest = newAdapter(
            contacts = arrayListOf(contact),
            adapterType = MegaContactsAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT,
        )

        val holder = createAndBindHolder(
            position = 0,
            adapterType = MegaContactsAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT,
        )

        assertThat(holder.rowState.isSelected).isTrue()
    }

    @Test
    fun `test that binding an unselected contact leaves the row state unselected`() {
        val contact = megaContact("Alice").apply { isSelected = false }
        underTest = newAdapter(
            contacts = arrayListOf(contact),
            adapterType = MegaContactsAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT,
        )

        val holder = createAndBindHolder(
            position = 0,
            adapterType = MegaContactsAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT,
        )

        assertThat(holder.rowState.isSelected).isFalse()
    }

    @Test
    fun `test that group chat mode never marks a row as selected even when the contact is selected`() {
        val contact = megaContact("Alice").apply { isSelected = true }
        underTest = newAdapter(
            contacts = arrayListOf(contact),
            adapterType = MegaContactsAdapter.ITEM_VIEW_TYPE_LIST_GROUP_CHAT,
        )

        val holder = createAndBindHolder(
            position = 0,
            adapterType = MegaContactsAdapter.ITEM_VIEW_TYPE_LIST_GROUP_CHAT,
        )

        assertThat(holder.rowState.isSelected).isFalse()
    }

    // endregion

    // region helpers

    private fun newAdapter(
        contacts: ArrayList<MegaContactAdapter> = arrayListOf(),
        adapterType: Int = MegaContactsAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT,
    ): MegaContactsAdapter = MegaContactsAdapter(
        context = activity,
        contacts = contacts,
        listFragment = listFragment,
        adapterType = adapterType,
        megaApi = megaApi,
        megaChatApi = megaChatApi,
        contactItemUiStateMapper = mapper,
    )

    /**
     * Inflates a row via Robolectric, binds it at [position], and returns the
     * holder. Does not run RecyclerView layout, so the embedded ComposeView is
     * not composed — only the adapter's row-state writes are exercised.
     */
    private fun createAndBindHolder(
        position: Int,
        adapterType: Int,
    ): MegaContactsAdapter.ViewHolderContacts {
        val holder = createHolder(adapterType)
        underTest.onBindViewHolder(holder, position)
        return holder
    }

    private fun createHolder(adapterType: Int): MegaContactsAdapter.ViewHolderContacts {
        val controller = hostController ?: Robolectric.buildActivity(Activity::class.java).setup()
        hostController = controller
        val parent = FrameLayout(controller.get())
        return underTest.onCreateViewHolder(parent, adapterType)
    }

    private fun megaContact(
        fullName: String? = "Alice",
        handle: Long = 1L,
    ): MegaContactAdapter {
        val email = "${(fullName ?: "user").lowercase().substringBefore(' ')}@example.com"
        val mockMegaUser: MegaUser = mock {
            on { this.handle } doReturn handle
            on { this.email } doReturn email
        }
        return MegaContactAdapter(
            contact = null,
            megaUser = mockMegaUser,
            fullName = fullName,
        )
    }

    private fun uiState(): ContactItemUiState = ContactItemUiState(
        handle = 1L,
        displayName = "Alice",
        status = ContactItemStatus.Online,
        lastSeen = null,
        avatar = AvatarData.Initials(initials = "A", avatarColor = Color.Red),
        isVerified = false,
    )

    // endregion
}
