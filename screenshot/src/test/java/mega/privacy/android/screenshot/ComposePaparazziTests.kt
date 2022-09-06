package mega.privacy.android.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Density
import app.cash.paparazzi.DeviceConfig.Companion.PIXEL_5
import app.cash.paparazzi.Paparazzi
import com.airbnb.android.showkase.models.Showkase
import com.airbnb.android.showkase.models.ShowkaseBrowserComponent
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import mega.privacy.android.presentation.theme.AndroidTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

class ComponentPreview(
    private val showkaseBrowserComponent: ShowkaseBrowserComponent
) {
    val content: @Composable () -> Unit = showkaseBrowserComponent.component
    override fun toString(): String =
        showkaseBrowserComponent.group + ":" + showkaseBrowserComponent.componentName
}

@RunWith(TestParameterInjector::class)
class ComposePaparazziTests {

    object PreviewProvider : TestParameter.TestParameterValuesProvider {
        override fun provideValues(): List<ComponentPreview> =
            Showkase.getMetadata().componentList.map(::ComponentPreview)
    }

    @get:Rule
    val paparazzi = Paparazzi(
        maxPercentDifference = 0.0,
        deviceConfig = PIXEL_5.copy(softButtons = false),
    )

    @Test
    fun preview_tests(
        @TestParameter(valuesProvider = PreviewProvider::class) componentPreview: ComponentPreview,
        @TestParameter(value = ["1.0", "1.5"]) fontScale: Float,
        @TestParameter(value = ["light", "dark"]) theme: String
    ) {
        paparazzi.snapshot() {
            CompositionLocalProvider(
                LocalInspectionMode provides true,
                LocalDensity provides Density(
                    density = LocalDensity.current.density,
                    fontScale = fontScale
                )
            ) {
                AndroidTheme(isDark = (theme == "dark")) {
                    componentPreview.content()
                }
            }
        }
    }
}