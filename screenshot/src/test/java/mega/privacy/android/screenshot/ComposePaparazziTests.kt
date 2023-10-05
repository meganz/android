package mega.privacy.android.screenshot

import app.cash.paparazzi.DeviceConfig.Companion.PIXEL_5
import app.cash.paparazzi.Paparazzi
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class ComposePaparazziTests {

    @get:Rule
    val paparazzi = Paparazzi(
        maxPercentDifference = 0.0,
        deviceConfig = PIXEL_5.copy(softButtons = false),
    )

    @Test
    fun preview_tests(
        @TestParameter(value = ["1.0", "1.5"]) fontScale: Float,
        @TestParameter(value = ["light", "dark"]) theme: String,
    ) {
        paparazzi.snapshot {
            TextMegaButton(
                text = "test",
                onClick = {}
            )
        }
    }
}