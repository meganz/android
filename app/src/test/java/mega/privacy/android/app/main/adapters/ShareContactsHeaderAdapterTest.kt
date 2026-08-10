package mega.privacy.android.app.main.adapters

import android.app.Activity
import android.content.Context
import android.widget.FrameLayout
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.app.MegaContactAdapter
import mega.privacy.android.app.main.PhoneContactInfo
import mega.privacy.android.app.main.ShareContactInfo
import mega.privacy.android.app.main.adapters.mapper.ShareContactInfoToContactItemUiStateMapper
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.Constants.HEADER_VIEW_TYPE
import mega.privacy.android.app.utils.Constants.ITEM_PROGRESS
import mega.privacy.android.app.utils.Constants.ITEM_VIEW_TYPE
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
class ShareContactsHeaderAdapterTest {

    private lateinit var underTest: ShareContactsHeaderAdapter

    private val context: Context = mock<AddContactActivity>()
    private val megaApi: MegaApiAndroid = mock()
    private val megaChatApi: MegaChatApiAndroid = mock()
    private val mapper: ShareContactInfoToContactItemUiStateMapper = mock()

    private var hostController: ActivityController<Activity>? = null
    private var cacheFolderManager: MockedStatic<CacheFolderManager>? = null
    private var avatarUtil: MockedStatic<AvatarUtil>? = null

    @Before
    fun setUp() {
        underTest = newAdapter(emptyList())
    }

    @After
    fun tearDown() {
        cacheFolderManager?.close()
        cacheFolderManager = null
        avatarUtil?.close()
        avatarUtil = null
    }

    @Test
    fun `test that getItemCount returns 0 when contacts list is null`() {
        underTest = newAdapter(null)

        assertThat(underTest.itemCount).isEqualTo(0)
    }

    @Test
    fun `test that getItemCount returns size of contacts list`() {
        underTest = newAdapter(
            listOf(megaContact("Alice"), megaContact("Bob"), phoneContact("Carol"))
        )

        assertThat(underTest.itemCount).isEqualTo(3)
    }

    @Test
    fun `test that getItem returns null when position is out of bounds`() {
        underTest = newAdapter(listOf(megaContact("Alice")))

        assertThat(underTest.getItem(5)).isNull()
    }

    @Test
    fun `test that getItem returns null when position is negative`() {
        underTest = newAdapter(listOf(megaContact("Alice")))

        assertThat(underTest.getItem(-1)).isNull()
    }

    @Test
    fun `test that getItem returns the contact at the given position`() {
        val alice = megaContact("Alice")
        val bob = megaContact("Bob")
        underTest = newAdapter(listOf(alice, bob))

        assertThat(underTest.getItem(1)).isSameInstanceAs(bob)
    }

    @Test
    fun `test that getItemId returns the position as Long`() {
        assertThat(underTest.getItemId(7)).isEqualTo(7L)
    }

    @Test
    fun `test that getItemViewType returns HEADER_VIEW_TYPE for a header row`() {
        underTest = newAdapter(listOf(megaHeader()))

        assertThat(underTest.getItemViewType(0)).isEqualTo(HEADER_VIEW_TYPE)
    }

    @Test
    fun `test that getItemViewType returns ITEM_PROGRESS for a progress row`() {
        underTest = newAdapter(listOf(progressRow()))

        assertThat(underTest.getItemViewType(0)).isEqualTo(ITEM_PROGRESS)
    }

    @Test
    fun `test that getItemViewType returns ITEM_VIEW_TYPE for a regular mega contact`() {
        underTest = newAdapter(listOf(megaContact("Alice")))

        assertThat(underTest.getItemViewType(0)).isEqualTo(ITEM_VIEW_TYPE)
    }

    @Test
    fun `test that getItemViewType returns ITEM_VIEW_TYPE for a regular phone contact`() {
        underTest = newAdapter(listOf(phoneContact("Alice")))

        assertThat(underTest.getItemViewType(0)).isEqualTo(ITEM_VIEW_TYPE)
    }

    @Test
    fun `test that getSectionTitle returns null for a header row`() {
        underTest = newAdapter(listOf(megaHeader()))

        assertThat(underTest.getSectionTitle(0, context = null)).isNull()
    }

    @Test
    fun `test that getSectionTitle returns the uppercase first letter of a mega contact full name`() {
        underTest = newAdapter(listOf(megaContact("alice anderson")))

        assertThat(underTest.getSectionTitle(0, context = null)).isEqualTo("A")
    }

    @Test
    fun `test that getSectionTitle returns the uppercase first letter of a phone contact name`() {
        underTest = newAdapter(listOf(phoneContact("bob baker")))

        assertThat(underTest.getSectionTitle(0, context = null)).isEqualTo("B")
    }

    @Test
    fun `test that setContacts replaces the underlying list`() {
        underTest.setContacts(listOf(megaContact("Alice")))
        underTest.setContacts(listOf(megaContact("Bob"), megaContact("Carol")))

        assertThat(underTest.itemCount).isEqualTo(2)
        assertThat(underTest.getItem(0)?.megaContactAdapter?.fullName).isEqualTo("Bob")
    }

    // region selection propagation to the Compose row state

    @Test
    fun `test that binding a selected mega contact marks the row state as selected`() {
        installStaticMocks()
        stubMapper()
        val activity = context as AddContactActivity
        whenever(activity.getShareContactMail(any())).thenReturn("alice@example.com")

        val contact = megaContact("Alice", selected = true)
        underTest = newAdapter(listOf(contact))

        val holder = createAndBindHolder(0)

        assertThat(holder.rowState.isSelected).isTrue()
    }

    @Test
    fun `test that binding an unselected mega contact leaves the row state unselected`() {
        installStaticMocks()
        stubMapper()
        val activity = context as AddContactActivity
        whenever(activity.getShareContactMail(any())).thenReturn("alice@example.com")

        val contact = megaContact("Alice", selected = false)
        underTest = newAdapter(listOf(contact))

        val holder = createAndBindHolder(0)

        assertThat(holder.rowState.isSelected).isFalse()
    }

    @Test
    fun `test that binding a phone contact never marks the row state as selected`() {
        installStaticMocks()
        stubMapper()

        val contact = phoneContact("Bob")
        underTest = newAdapter(listOf(contact))

        val holder = createAndBindHolder(0)

        assertThat(holder.rowState.isSelected).isFalse()
    }

    @Test
    fun `test that creating a view holder wires the row's compose onClick to the item click listener`() {
        val listener: ShareContactsHeaderAdapter.OnItemClickListener = mock()
        underTest = newAdapter(emptyList())
        underTest.SetOnItemClickListener(listener)

        val holder = createHolder()

        assertThat(holder.rowState.onClick).isNotNull()
        holder.rowState.onClick?.invoke()
        verify(listener).onItemClick(any(), any())
    }

    @Test
    fun `test that creating a view holder wires the row's compose onLongClick to the long item click listener`() {
        val listener: ShareContactsHeaderAdapter.OnLongItemClickListener = mock()
        underTest = newAdapter(emptyList())
        underTest.SetOnLongItemClickListener(listener)

        val holder = createHolder()

        assertThat(holder.rowState.onLongClick).isNotNull()
        holder.rowState.onLongClick?.invoke()
        verify(listener).onLongItemClick(any(), any())
    }

    private fun createHolder(): ShareContactsHeaderAdapter.ViewHolderShareContacts {
        val controller = hostController ?: Robolectric.buildActivity(Activity::class.java).setup()
        hostController = controller
        val parent = FrameLayout(controller.get())
        return underTest.onCreateViewHolder(parent, ITEM_VIEW_TYPE)
    }

    private fun installStaticMocks() {
        cacheFolderManager = mockStatic(CacheFolderManager::class.java).also { ms ->
            ms.`when`<Any?> { CacheFolderManager.buildAvatarFile(any()) }.thenReturn(null)
        }
        avatarUtil = mockStatic(AvatarUtil::class.java).also { ms ->
            ms.`when`<Int> { AvatarUtil.getColorAvatar(anyOrNull<MegaUser>()) }
                .thenReturn(0xFF2E7D32.toInt())
        }
    }

    private fun stubMapper() {
        whenever(
            mapper.invoke(
                info = any(),
                mail = any(),
                avatarFile = anyOrNull(),
                avatarColorArgb = any(),
                chatStatusValue = any(),
                isVerified = any(),
            )
        ).thenReturn(
            ContactItemUiState(
                handle = 1L,
                displayName = "Alice",
                status = ContactItemStatus.Online,
                lastSeen = null,
                avatar = AvatarData.Initials(initials = "A", avatarColor = Color.Red),
                isVerified = false,
            )
        )
    }

    private fun createAndBindHolder(
        position: Int,
    ): ShareContactsHeaderAdapter.ViewHolderShareContacts {
        val controller = hostController ?: Robolectric.buildActivity(Activity::class.java).setup()
        hostController = controller
        val parent = FrameLayout(controller.get())
        val holder = underTest.onCreateViewHolder(parent, ITEM_VIEW_TYPE)
        underTest.onBindViewHolder(holder, position)
        return holder
    }

    // endregion

    private fun newAdapter(contacts: List<ShareContactInfo>?) = ShareContactsHeaderAdapter(
        context = context,
        shareContacts = contacts,
        megaApi = megaApi,
        megaChatApi = megaChatApi,
        contactItemUiStateMapper = mapper,
    )

    private fun megaContact(
        fullName: String,
        selected: Boolean = false,
    ): ShareContactInfo = ShareContactInfo(
        null,
        MegaContactAdapter(
            contact = null,
            megaUser = null,
            fullName = fullName,
            isSelected = selected,
        ),
        "$fullName@example.com",
    )

    private fun phoneContact(name: String): ShareContactInfo = ShareContactInfo(
        PhoneContactInfo(0L, name, "$name@example.com", "+10000000"),
        null,
        "$name@example.com",
    )

    private fun megaHeader(): ShareContactInfo =
        ShareContactInfo(true, true, false)

    private fun progressRow(): ShareContactInfo = ShareContactInfo()
}
