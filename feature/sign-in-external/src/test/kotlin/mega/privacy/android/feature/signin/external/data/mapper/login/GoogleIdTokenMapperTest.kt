package mega.privacy.android.feature.signin.external.data.mapper.login

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.util.Base64

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GoogleIdTokenMapperTest {

    private val underTest = GoogleIdTokenMapper(json = Json { ignoreUnknownKeys = true })

    private fun buildToken(payloadJson: String): String {
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payloadJson.toByteArray())
        return "header.$payload.sig"
    }

    @Test
    fun `test that invoke returns correct GoogleSignInResult when given valid token`() {
        val result = underTest(buildToken("""{"sub":"12345","email":"user@gmail.com","given_name":"John","family_name":"Doe"}"""))
        assertThat(result.sub).isEqualTo("12345")
        assertThat(result.email).isEqualTo("user@gmail.com")
        assertThat(result.firstName).isEqualTo("John")
        assertThat(result.lastName).isEqualTo("Doe")
    }

    @Test
    fun `test that invoke returns null firstName and lastName when name fields are absent`() {
        val result = underTest(buildToken("""{"sub":"12345","email":"user@gmail.com"}"""))
        assertThat(result.firstName).isNull()
        assertThat(result.lastName).isNull()
    }

    @Test
    fun `test that invoke throws IllegalArgumentException when email is missing`() {
        assertThrows<IllegalArgumentException> { underTest(buildToken("""{"sub":"12345"}""")) }
    }

    @Test
    fun `test that invoke throws IllegalArgumentException when sub is missing`() {
        assertThrows<IllegalArgumentException> { underTest(buildToken("""{"email":"user@gmail.com"}""")) }
    }

    @Test
    fun `test that invoke throws IllegalArgumentException when JWT is malformed`() {
        assertThrows<IllegalArgumentException> { underTest("not.a.valid.jwt.with.extra.parts") }
        assertThrows<IllegalArgumentException> { underTest("only-one-part") }
    }
}
