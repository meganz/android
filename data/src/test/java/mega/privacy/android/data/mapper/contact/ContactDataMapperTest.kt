package mega.privacy.android.data.mapper.contact

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.user.UserVisibility
import org.junit.Before
import org.junit.Test

class ContactDataMapperTest {
    private lateinit var underTest: ContactDataMapper

    @Before
    fun setUp() {
        underTest = ContactDataMapper()
    }

    @Test
    fun `test correct ContactData is returned`() {
        val expectedFullName = "Clark Kent"
        val expectedAlias = "Superman"
        val expectedAvatar = "<S>"
        val expectedVisibility = UserVisibility.Inactive

        val expected =
            ContactData(expectedFullName, expectedAlias, expectedAvatar, expectedVisibility)

        val actual =
            underTest.invoke(expectedFullName, expectedAlias, expectedAvatar, expectedVisibility)

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that unknown is returned if visibility is null`() {
        val expectedFullName = "Clark Kent"
        val expectedAlias = "Superman"
        val expectedAvatar = "<S>"
        val expectedVisibility = UserVisibility.Unknown

        val expected =
            ContactData(expectedFullName, expectedAlias, expectedAvatar, expectedVisibility)

        val actual =
            underTest.invoke(expectedFullName, expectedAlias, expectedAvatar, null)

        Truth.assertThat(actual).isEqualTo(expected)
    }
}