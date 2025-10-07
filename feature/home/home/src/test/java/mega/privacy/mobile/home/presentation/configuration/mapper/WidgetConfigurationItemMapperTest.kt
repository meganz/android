package mega.privacy.mobile.home.presentation.configuration.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.home.HomeWidgetConfiguration
import mega.privacy.android.navigation.contract.home.HomeWidget
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class WidgetConfigurationItemMapperTest {
    private val underTest = WidgetConfigurationItemMapper()

    @Test
    fun `test that widget without configuration returns default order`() = runTest {
        val expected = 5
        val homeWidget = mock<HomeWidget> {
            on { identifier } doReturn "identifier"
            on { defaultOrder } doReturn expected
            onBlocking { getWidgetName() } doReturn LocalizedText.Literal("Test Widget")
        }
        val actual = underTest(
            homeWidget = homeWidget,
            widgetConfiguration = null,
        )

        assertThat(actual.index).isEqualTo(expected)
    }

    @Test
    fun `test that widget without configuration returns enabled is true`() = runTest {
        val homeWidget = mock<HomeWidget> {
            on { identifier } doReturn "identifier"
            on { defaultOrder } doReturn 5
            onBlocking { getWidgetName() } doReturn LocalizedText.Literal("Test Widget")
        }
        val actual = underTest(
            homeWidget = homeWidget,
            widgetConfiguration = null,
        )

        assertThat(actual.enabled).isTrue()
    }

    @Test
    fun `test that widget with configuration returns order and enabled status from configuration`() =
        runTest {
            val expectedOrder = 5
            val homeWidget = mock<HomeWidget> {
                on { identifier } doReturn "identifier"
                on { defaultOrder } doReturn expectedOrder + 1
                onBlocking { getWidgetName() } doReturn LocalizedText.Literal("Test Widget")
            }
            val actual = underTest(
                homeWidget = homeWidget,
                widgetConfiguration = HomeWidgetConfiguration(
                    widgetIdentifier = "identifier",
                    widgetOrder = expectedOrder,
                    enabled = false,
                ),
            )


            assertThat(actual.index).isEqualTo(expectedOrder)
            assertThat(actual.enabled).isFalse()
        }
}