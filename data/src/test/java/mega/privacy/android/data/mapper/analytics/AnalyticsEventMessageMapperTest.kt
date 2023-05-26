package mega.privacy.android.data.mapper.analytics

import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import mega.privacy.android.domain.entity.analytics.ScreenViewEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

internal class AnalyticsEventMessageMapperTest {
    private lateinit var underTest: AnalyticsEventMessageMapper

    @BeforeEach
    internal fun setUp() {
        underTest = AnalyticsEventMessageMapper(GsonBuilder())
    }

    @Test
    internal fun `test that gson results contains all map values from the analytics event`() {
        //We need to mock a specific event type because Mockito cannot mock sealed interfaces
        val event = mock<ScreenViewEvent> {
            on { data() }.thenReturn(
                mapOf(
                    "string" to "string",
                    "int" to 1,
                    "long" to 1L,
                    "null" to null,
                    "array" to listOf(1, 2, 3)
                )
            )
        }

        val expected = """{"string":"string","int":1,"long":1,"null":null,"array":[1,2,3]}"""

        assertThat(underTest(event)).isEqualTo(expected)
    }
}