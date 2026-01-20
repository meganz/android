package mega.privacy.android.data.mapper


import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.mapper.banner.MegaBannerMapper
import nz.mega.sdk.MegaBanner
import nz.mega.sdk.MegaBannerList
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MegaBannerMapperTest {

    private val underTest: MegaBannerMapper = MegaBannerMapper()

    @Test
    fun `invoke should map MegaBannerList to List of Banner domain models`() {
        val expectedId = 123
        val expectedTitle = "Test Title"
        val expectedDescription = "Test Description"
        val expectedImage = "image_url"
        val expectedBackgroundImage = "bg_image_url"
        val expectedUrl = "action_url"
        val expectedImageLocation = "top"
        val expectedButtonText = "Click Me"
        val expectedVariant = 1

        val megaBanner = mock<MegaBanner> {
            on { id } doReturn expectedId
            on { title } doReturn expectedTitle
            on { description } doReturn expectedDescription
            on { image } doReturn expectedImage
            on { backgroundImage } doReturn expectedBackgroundImage
            on { url } doReturn expectedUrl
            on { imageLocation } doReturn expectedImageLocation
            on { button } doReturn expectedButtonText
            on { variant } doReturn expectedVariant
        }

        val megaBannerList = mock<MegaBannerList> {
            on { size() } doReturn 1
            on { get(0) } doReturn megaBanner
        }

        val result = underTest(megaBannerList)

        assertThat(result).hasSize(1)
        val banner = result[0]
        assertThat(banner.id).isEqualTo(expectedId)
        assertThat(banner.title).isEqualTo(expectedTitle)
        assertThat(banner.description).isEqualTo(expectedDescription)
        assertThat(banner.image).isEqualTo(expectedImage)
        assertThat(banner.backgroundImage).isEqualTo(expectedBackgroundImage)
        assertThat(banner.url).isEqualTo(expectedUrl)
        assertThat(banner.imageLocation).isEqualTo(expectedImageLocation)
        assertThat(banner.buttonText).isEqualTo(expectedButtonText)
        assertThat(banner.variant).isEqualTo(expectedVariant)
    }

    @Test
    fun `invoke should handle null values in MegaBanner by using orEmpty`() {
        val megaBanner = mock<MegaBanner> {
            on { id } doReturn 0
            on { title } doReturn null
            on { description } doReturn null
            on { image } doReturn null
            on { backgroundImage } doReturn null
            on { url } doReturn null
            on { imageLocation } doReturn null
            on { button } doReturn null
            on { variant } doReturn 0
        }

        val megaBannerList = mock<MegaBannerList> {
            on { size() } doReturn 1
            on { get(0) } doReturn megaBanner
        }

        val result = underTest(megaBannerList)

        val banner = result[0]
        assertThat(banner.title).isEmpty()
        assertThat(banner.description).isEmpty()
        assertThat(banner.image).isEmpty()
        assertThat(banner.backgroundImage).isEmpty()
        assertThat(banner.url).isEmpty()
        assertThat(banner.imageLocation).isEmpty()
        assertThat(banner.buttonText).isEmpty()
    }

    @Test
    fun `invoke with empty list should return empty list`() {
        val megaBannerList = mock<MegaBannerList> {
            on { size() } doReturn 0
        }

        val result = underTest(megaBannerList)

        assertThat(result).isEmpty()
    }
}
