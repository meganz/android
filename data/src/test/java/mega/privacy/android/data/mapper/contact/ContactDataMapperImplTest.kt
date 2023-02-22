package mega.privacy.android.data.mapper.contact

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.contacts.ContactData
import org.junit.Before
import org.junit.Test

class ContactDataMapperImplTest {
    private lateinit var underTest: ContactDataMapper

    @Before
    fun setUp() {
        underTest = ContactDataMapperImpl()
    }

    @Test
    fun `test correct ContactData is returned`() {
        val expectedFullName = "Clark Kent"
        val expectedAlias = "Superman"
        val expectedAvatar = "<S>"
        val expected = ContactData(expectedFullName, expectedAlias, expectedAvatar)

        val actual = underTest.invoke(expectedFullName, expectedAlias, expectedAvatar)

        Truth.assertThat(actual).isEqualTo(expected)
    }
}