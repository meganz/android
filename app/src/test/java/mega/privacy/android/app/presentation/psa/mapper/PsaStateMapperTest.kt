package mega.privacy.android.app.presentation.psa.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.psa.model.PsaState
import mega.privacy.android.domain.entity.psa.Psa
import org.junit.jupiter.api.Test

class PsaStateMapperTest {
    private val underTest = PsaStateMapper()

    @Test
    fun `test that null returns no psa`() {
        assertThat(underTest(null)).isEqualTo(PsaState.NoPsa)
    }

    @Test
    fun `test that a psa with a url returns web psa`() {
        val url = "url"
        val psa = createPsa(url)
        assertThat(underTest(psa)).isEqualTo(PsaState.WebPsa(1, url))
    }

    @Test
    fun `test that a psa with a null url returns a standard psa`() {
        val url = null
        val psa = createPsa(url)
        assertThat(underTest(psa)).isEqualTo(
            PsaState.StandardPsa(
                1,
                "title",
                "text",
                "imageUrl",
                "positiveText",
                "positiveLink"
            )
        )
    }

    @Test
    fun `test that a psa with an empty url returns standard psa`() {
        val url = ""
        val psa = createPsa(url)
        assertThat(underTest(psa)).isEqualTo(
            PsaState.StandardPsa(
                1,
                "title",
                "text",
                "imageUrl",
                "positiveText",
                "positiveLink"
            )
        )
    }

    @Test
    fun `test that psa with a blank url returns standard psa`() {
        val url = " "
        val psa = createPsa(url)
        assertThat(underTest(psa)).isEqualTo(
            PsaState.StandardPsa(
                1,
                "title",
                "text",
                "imageUrl",
                "positiveText",
                "positiveLink"
            )
        )
    }

    @Test
    fun `test that psa without a positive button text returns an info psa`() {
        val psa = createPsa(url = null, positiveText = null)
        assertThat(underTest(psa)).isEqualTo(
            PsaState.InfoPsa(
                1,
                "title",
                "text",
                "imageUrl",
            )
        )
    }

    @Test
    fun `test that psa without a positive link text returns an info psa`() {
        val psa = createPsa(url = null, positiveLink = null)
        assertThat(underTest(psa)).isEqualTo(
            PsaState.InfoPsa(
                1,
                "title",
                "text",
                "imageUrl",
            )
        )
    }

    private fun createPsa(
        url: String?,
        positiveText: String? = "positiveText",
        positiveLink: String? = "positiveLink",
    ) = Psa(
        id = 1,
        title = "title",
        text = "text",
        imageUrl = "imageUrl",
        positiveText = positiveText,
        positiveLink = positiveLink,
        url = url
    )
}