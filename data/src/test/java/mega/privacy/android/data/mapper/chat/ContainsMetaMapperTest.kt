package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.chat.ContainsMeta
import mega.privacy.android.domain.entity.chat.ContainsMetaType
import nz.mega.sdk.MegaChatContainsMeta
import nz.mega.sdk.MegaChatGeolocation
import nz.mega.sdk.MegaChatGiphy
import nz.mega.sdk.MegaChatRichPreview
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContainsMetaMapperTest {

    private lateinit var underTest: ContainsMetaMapper
    private lateinit var richPreviewMapper: RichPreviewMapper
    private lateinit var chatGeolocationMapper: ChatGeolocationMapper
    private lateinit var giphyMapper: GiphyMapper

    @BeforeAll
    fun setup() {
        richPreviewMapper = RichPreviewMapper()
        chatGeolocationMapper = ChatGeolocationMapper()
        giphyMapper = GiphyMapper()
        underTest = ContainsMetaMapper(richPreviewMapper, chatGeolocationMapper, giphyMapper)
    }

    @ParameterizedTest(name = "when type {0}")
    @MethodSource("provideTestParameters")
    fun `test that contains meta mapper returns correctly`(
        containsMetaType: Int,
        containsMetaTextMessage: String?,
        containsMetaRichPreview: MegaChatRichPreview?,
        containsMetaGeolocation: MegaChatGeolocation?,
        containsMetaGiphy: MegaChatGiphy?,
    ) {
        val megaChatContainsMeta = mock<MegaChatContainsMeta> {
            on { type }.thenReturn(containsMetaType)
            on { textMessage }.thenReturn(containsMetaTextMessage)
            on { richPreview }.thenReturn(containsMetaRichPreview)
            on { geolocation }.thenReturn(containsMetaGeolocation)
            on { giphy }.thenReturn(containsMetaGiphy)
        }
        val containsMeta = ContainsMeta(
            type = megaChatContainsMeta.type.toContainsMetaType(),
            textMessage = megaChatContainsMeta.textMessage.orEmpty(),
            richPreview = megaChatContainsMeta.richPreview?.let { richPreviewMapper(it) },
            geolocation = megaChatContainsMeta.geolocation?.let { chatGeolocationMapper(it) },
            giphy = megaChatContainsMeta.giphy?.let { giphyMapper(it) }
        )
        Truth.assertThat(underTest.invoke(megaChatContainsMeta)).isEqualTo(containsMeta)
    }

    private val textMessage = "textMessage"
    private val richPreview = mock<MegaChatRichPreview> {
        on { url }.thenReturn("url")
        on { domainName }.thenReturn("domainName")
    }
    private val geolocation = mock<MegaChatGeolocation>()
    private val giphy = mock<MegaChatGiphy>()

    private fun provideTestParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(MegaChatContainsMeta.CONTAINS_META_INVALID, null, null, null, null),
        Arguments.of(
            MegaChatContainsMeta.CONTAINS_META_INVALID,
            textMessage,
            null,
            geolocation,
            null
        ),
        Arguments.of(
            MegaChatContainsMeta.CONTAINS_META_INVALID,
            textMessage,
            richPreview,
            null,
            null
        ),
        Arguments.of(
            MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW,
            null,
            richPreview,
            null,
            null
        ),
        Arguments.of(
            MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW,
            textMessage,
            richPreview,
            null,
            null
        ),
        Arguments.of(MegaChatContainsMeta.CONTAINS_META_GEOLOCATION, null, null, geolocation, null),
        Arguments.of(
            MegaChatContainsMeta.CONTAINS_META_GEOLOCATION,
            textMessage,
            null,
            geolocation,
            null
        ),
        Arguments.of(MegaChatContainsMeta.CONTAINS_META_GIPHY, null, null, null, giphy),
        Arguments.of(MegaChatContainsMeta.CONTAINS_META_GIPHY, textMessage, null, null, giphy),
    )

    private fun Int.toContainsMetaType(): ContainsMetaType = when (this) {
        MegaChatContainsMeta.CONTAINS_META_INVALID -> ContainsMetaType.INVALID
        MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW -> ContainsMetaType.RICH_PREVIEW
        MegaChatContainsMeta.CONTAINS_META_GEOLOCATION -> ContainsMetaType.GEOLOCATION
        MegaChatContainsMeta.CONTAINS_META_GIPHY -> ContainsMetaType.GIPHY
        else -> ContainsMetaType.INVALID
    }
}