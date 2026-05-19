package mega.privacy.android.app.main.adapters

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.MegaContactAdapter
import mega.privacy.android.app.main.PhoneContactInfo
import mega.privacy.android.app.main.ShareContactInfo
import mega.privacy.android.app.main.adapters.mapper.ShareContactInfoToContactItemUiStateMapper
import mega.privacy.android.app.utils.Constants.HEADER_VIEW_TYPE
import mega.privacy.android.app.utils.Constants.ITEM_PROGRESS
import mega.privacy.android.app.utils.Constants.ITEM_VIEW_TYPE
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class ShareContactsHeaderAdapterTest {

    private lateinit var underTest: ShareContactsHeaderAdapter

    private val context: Context = mock()
    private val megaApi: MegaApiAndroid = mock()
    private val megaChatApi: MegaChatApiAndroid = mock()
    private val mapper: ShareContactInfoToContactItemUiStateMapper = mock()

    @Before
    fun setUp() {
        underTest = newAdapter(emptyList())
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

    private fun newAdapter(contacts: List<ShareContactInfo>?) = ShareContactsHeaderAdapter(
        context = context,
        shareContacts = contacts,
        megaApi = megaApi,
        megaChatApi = megaChatApi,
        contactItemUiStateMapper = mapper,
    )

    private fun megaContact(fullName: String): ShareContactInfo = ShareContactInfo(
        null,
        MegaContactAdapter(contact = null, megaUser = null, fullName = fullName),
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
