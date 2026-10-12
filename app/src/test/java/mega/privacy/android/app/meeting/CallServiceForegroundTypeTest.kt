package mega.privacy.android.app.meeting

import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CallServiceForegroundTypeTest {

    @ParameterizedTest(name = "phoneCall {0}, microphone {1}, camera {2} -> {3}")
    @MethodSource("upsideDownCakePermissionCombinations")
    fun `test that callServiceForegroundType only includes the types whose permissions are granted on API 34 and above`(
        hasPhoneCallPermissions: Boolean,
        hasMicrophonePermissions: Boolean,
        hasCameraPermissions: Boolean,
        expected: Int,
    ) {
        listOf(
            Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            Build.VERSION_CODES.VANILLA_ICE_CREAM,
        ).forEach { sdkInt ->
            assertThat(
                callServiceForegroundType(
                    sdkInt = sdkInt,
                    hasPhoneCallPermissions = hasPhoneCallPermissions,
                    hasMicrophonePermissions = hasMicrophonePermissions,
                    hasCameraPermissions = hasCameraPermissions,
                )
            ).isEqualTo(expected)
        }
    }

    private fun upsideDownCakePermissionCombinations(): Stream<Arguments> = Stream.of(
        Arguments.of(
            true, true, true,
            FOREGROUND_SERVICE_TYPE_PHONE_CALL or FOREGROUND_SERVICE_TYPE_MICROPHONE or FOREGROUND_SERVICE_TYPE_CAMERA
        ),
        Arguments.of(
            true, true, false,
            FOREGROUND_SERVICE_TYPE_PHONE_CALL or FOREGROUND_SERVICE_TYPE_MICROPHONE
        ),
        Arguments.of(
            true, false, true,
            FOREGROUND_SERVICE_TYPE_PHONE_CALL or FOREGROUND_SERVICE_TYPE_CAMERA
        ),
        Arguments.of(true, false, false, FOREGROUND_SERVICE_TYPE_PHONE_CALL),
        Arguments.of(
            false, true, true,
            FOREGROUND_SERVICE_TYPE_MICROPHONE or FOREGROUND_SERVICE_TYPE_CAMERA
        ),
        Arguments.of(false, true, false, FOREGROUND_SERVICE_TYPE_MICROPHONE),
        Arguments.of(false, false, true, FOREGROUND_SERVICE_TYPE_CAMERA),
        Arguments.of(false, false, false, 0),
    )

    @Test
    fun `test that callServiceForegroundType includes phone call when only phone call and microphone permissions are granted on API 34`() {
        val actual = callServiceForegroundType(
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            hasPhoneCallPermissions = true,
            hasMicrophonePermissions = true,
            hasCameraPermissions = false,
        )

        assertThat(actual!! and FOREGROUND_SERVICE_TYPE_PHONE_CALL)
            .isEqualTo(FOREGROUND_SERVICE_TYPE_PHONE_CALL)
        assertThat(actual and FOREGROUND_SERVICE_TYPE_CAMERA).isEqualTo(0)
    }

    @ParameterizedTest(name = "API {0}")
    @ValueSource(ints = [Build.VERSION_CODES.R, Build.VERSION_CODES.S, Build.VERSION_CODES.TIRAMISU])
    fun `test that callServiceForegroundType returns phone call and microphone on API 30 to 33 regardless of permissions`(
        sdkInt: Int,
    ) {
        assertThat(
            callServiceForegroundType(
                sdkInt = sdkInt,
                hasPhoneCallPermissions = false,
                hasMicrophonePermissions = false,
                hasCameraPermissions = false,
            )
        ).isEqualTo(FOREGROUND_SERVICE_TYPE_PHONE_CALL or FOREGROUND_SERVICE_TYPE_MICROPHONE)
    }

    @Test
    fun `test that callServiceForegroundType returns phone call on API 29`() {
        assertThat(
            callServiceForegroundType(
                sdkInt = Build.VERSION_CODES.Q,
                hasPhoneCallPermissions = false,
                hasMicrophonePermissions = true,
                hasCameraPermissions = true,
            )
        ).isEqualTo(FOREGROUND_SERVICE_TYPE_PHONE_CALL)
    }

    @Test
    fun `test that callServiceForegroundType returns null below API 29`() {
        assertThat(
            callServiceForegroundType(
                sdkInt = Build.VERSION_CODES.P,
                hasPhoneCallPermissions = true,
                hasMicrophonePermissions = true,
                hasCameraPermissions = true,
            )
        ).isNull()
    }
}
