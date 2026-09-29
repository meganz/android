package mega.privacy.android.data.mapper.chat.giphy

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.model.giphy.GiphyData
import mega.privacy.android.data.model.giphy.GiphyImageAttributes
import mega.privacy.android.data.model.giphy.GiphyImages
import mega.privacy.android.domain.entity.chat.Giphy
import org.junit.jupiter.api.Test

class GiphyDataMapperTest {

    private val underTest = GiphyDataMapper()

    @Test
    fun `test that invoke maps fixed height attributes to giphy`() {
        val giphyData = GiphyData(
            id = "id",
            title = "title",
            images = GiphyImages(
                fixedHeight = GiphyImageAttributes(
                    height = 200,
                    width = 300,
                    mp4Size = 1000L,
                    mp4 = "mp4",
                    webpSize = 2000L,
                    webp = "webp",
                )
            ),
        )

        assertThat(underTest(giphyData)).isEqualTo(
            Giphy(
                mp4Src = "mp4",
                webpSrc = "webp",
                title = "title",
                mp4Size = 1000,
                webpSize = 2000,
                width = 300,
                height = 200,
            )
        )
    }

    @Test
    fun `test that invoke returns empty values when fixed height is missing`() {
        val giphyData = GiphyData(id = "id", title = "title", images = null)

        assertThat(underTest(giphyData)).isEqualTo(
            Giphy(
                mp4Src = null,
                webpSrc = null,
                title = "title",
                mp4Size = 0,
                webpSize = 0,
                width = 0,
                height = 0,
            )
        )
    }

    @Test
    fun `test that invoke maps every item of a list`() {
        val giphyData = listOf(
            GiphyData(id = "1", title = "first", images = null),
            GiphyData(id = "2", title = "second", images = null),
        )

        assertThat(underTest(giphyData).map { it.title }).containsExactly("first", "second")
            .inOrder()
    }
}
