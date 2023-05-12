package mega.privacy.android.data.mapper.contact

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.database.entity.ContactEntity
import mega.privacy.android.domain.entity.Contact
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class ContactModelMapperTest {
    private val decryptData: DecryptData = mock()
    private lateinit var underTest: ContactModelMapper

    @Before
    fun setUp() {
        underTest = ContactModelMapper(decryptData)
    }

    @Test
    fun `test that mapper returns model correctly when invoke function`() = runTest {
        val entity = ContactEntity(
            handle = "1",
            mail = "lh@mega.co.nz",
            nickName = "Jayce",
            firstName = "Hai",
            lastName = "Luong"
        )
        val expected = Contact(
            userId = 1L,
            email = "lh@mega.co.nz",
            nickname = "Jayce",
            firstName = "Hai",
            lastName = "Luong"
        )
        whenever(decryptData(entity.handle)).thenReturn(expected.userId.toString())
        whenever(decryptData(entity.mail)).thenReturn(expected.email)
        whenever(decryptData(entity.nickName)).thenReturn(expected.nickname)
        whenever(decryptData(entity.firstName)).thenReturn(expected.firstName)
        whenever(decryptData(entity.lastName)).thenReturn(expected.lastName)
        Truth.assertThat(underTest(entity)).isEqualTo(expected)
    }
}