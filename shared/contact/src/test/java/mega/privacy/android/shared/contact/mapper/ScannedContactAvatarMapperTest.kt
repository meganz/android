package mega.privacy.android.shared.contact.mapper

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.qrcode.QRCodeQueryResults
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import mega.privacy.android.shared.contact.model.AvatarData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class ScannedContactAvatarMapperTest {

    private lateinit var underTest: ScannedContactAvatarMapper

    @BeforeEach
    fun setUp() {
        underTest = ScannedContactAvatarMapper()
    }

    @Test
    fun `test that avatar is Image when result has an avatar file`() {
        val avatarFile = File("/data/user/0/avatar.jpg")

        val result = underTest(createResult(avatarFile = avatarFile))

        assertThat(result).isEqualTo(AvatarData.Image(file = avatarFile))
    }

    @Test
    fun `test that avatar is Initials with first letter of name when result has no avatar file`() {
        val result = underTest(createResult(contactName = "alice anderson"))

        assertThat(result).isInstanceOf(AvatarData.Initials::class.java)
        assertThat((result as AvatarData.Initials).initials).isEqualTo("A")
    }

    @Test
    fun `test that initials fall back to email when contact name is blank`() {
        val result = underTest(createResult(contactName = " ", email = "bob@mega.co.nz"))

        assertThat((result as AvatarData.Initials).initials).isEqualTo("B")
    }

    @Test
    fun `test that initials avatar uses the result avatar color when present`() {
        val avatarColor = 0xFF2E7D32.toInt()

        val result = underTest(createResult(avatarColor = avatarColor))

        assertThat((result as AvatarData.Initials).avatarColor).isEqualTo(Color(avatarColor))
    }

    @Test
    fun `test that initials avatar color is unspecified when result has no avatar color`() {
        val result = underTest(createResult(avatarColor = null))

        assertThat((result as AvatarData.Initials).avatarColor).isEqualTo(Color.Unspecified)
    }

    private fun createResult(
        contactName: String = "Alice",
        email: String = "alice@mega.co.nz",
        avatarFile: File? = null,
        avatarColor: Int? = null,
    ) = ScannedContactLinkResult(
        contactName = contactName,
        email = email,
        handle = 12345L,
        isContact = false,
        qrCodeQueryResult = QRCodeQueryResults.CONTACT_QUERY_OK,
        avatarFile = avatarFile,
        avatarColor = avatarColor,
    )
}
