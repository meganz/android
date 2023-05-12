package mega.privacy.android.data.mapper.contact

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.entity.ContactEntity
import mega.privacy.android.domain.entity.Contact
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class ContactEntityMapperTest {
    private val encryptData: EncryptData = mock()
    private lateinit var underTest: ContactEntityMapper

    @Before
    fun setUp() {
        underTest = ContactEntityMapper(encryptData)
    }

    @Test
    fun `test that mapper returns model correctly when invoke function`() = runTest {
        val model = Contact(
            userId = 1L,
            email = "lh@mega.co.nz",
            nickname = "Jayce",
            firstName = "Hai",
            lastName = "Luong"
        )
        val expected = ContactEntity(
            handle = "1",
            mail = "lh@mega.co.nz",
            nickName = "Jayce",
            firstName = "Hai",
            lastName = "Luong"
        )
        whenever(encryptData(model.userId.toString())).thenReturn(expected.handle)
        whenever(encryptData(model.email)).thenReturn(expected.mail)
        whenever(encryptData(model.nickname)).thenReturn(expected.nickName)
        whenever(encryptData(model.firstName)).thenReturn(expected.firstName)
        whenever(encryptData(model.lastName)).thenReturn(expected.lastName)
        Truth.assertThat(underTest(model)).isEqualTo(expected)
    }
}