package mega.privacy.mobile.home.presentation.configuration.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.home.HomeWidgetConfiguration
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.android.navigation.contract.home.HomeWidgetOrder
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class WidgetConfigurationItemMapperTest {
    private val underTest = WidgetConfigurationItemMapper()

    @Test
    fun `test that widget without configuration returns default order`() = runTest {
        val expected = HomeWidgetOrder.ContinueWhereLeftOff
        val homeWidget = createHomeWidget(defaultOrder = expected)
        val actual = underTest(
            homeWidget = homeWidget,
            widgetConfiguration = null,
        )

        assertThat(actual.index).isEqualTo(expected.ordinal)
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
            val homeWidget = createHomeWidget(defaultOrder = HomeWidgetOrder.Banner)
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

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that isConfigurable is mapped from home widget`(isConfigurable: Boolean) = runTest {
        val homeWidget = createHomeWidget(isConfigurable = isConfigurable)

        val actual = underTest(
            homeWidget = homeWidget,
            widgetConfiguration = null,
        )

        assertThat(actual.isConfigurable).isEqualTo(isConfigurable)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that isDraggable is mapped from home widget`(isDraggable: Boolean) = runTest {
        val homeWidget = createHomeWidget(isDraggable = isDraggable)

        val actual = underTest(
            homeWidget = homeWidget,
            widgetConfiguration = null,
        )

        assertThat(actual.isDraggable).isEqualTo(isDraggable)
    }

    private fun createHomeWidget(
        identifier: String = "identifier",
        defaultOrder: HomeWidgetOrder = HomeWidgetOrder.ContinueWhereLeftOff,
        name: LocalizedText = LocalizedText.Literal("Test Widget"),
        isConfigurable: Boolean = true,
        isDraggable: Boolean = true,
    ): HomeWidget = mock {
        on { this.identifier } doReturn identifier
        on { this.defaultOrder } doReturn defaultOrder
        on { this.isConfigurable } doReturn isConfigurable
        on { this.isDraggable } doReturn isDraggable
        onBlocking { getWidgetName() } doReturn name
    }
}
