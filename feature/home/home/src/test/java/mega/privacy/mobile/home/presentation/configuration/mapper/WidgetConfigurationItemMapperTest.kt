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
        val homeWidget = createHomeWidget(defaultOrder = expected)
        val actual = underTest(
            homeWidget = homeWidget,
            widgetConfiguration = null,
        )

        assertThat(actual.index).isEqualTo(expected)
    }

    @Test
    fun `test that widget without configuration returns enabled is true`() = runTest {
        val homeWidget = createHomeWidget()
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
            val homeWidget = createHomeWidget(defaultOrder = expectedOrder + 1)
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

    private fun createHomeWidget(
        identifier: String = "identifier",
        defaultOrder: Int = 5,
        name: LocalizedText = LocalizedText.Literal("Test Widget"),
    ): HomeWidget = mock {
        on { this.identifier } doReturn identifier
        on { this.defaultOrder } doReturn defaultOrder
        onBlocking { getWidgetName() } doReturn name
    }
}
