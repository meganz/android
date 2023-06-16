package mega.privacy.android.data.mapper.notification

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.NotificationBehaviour
import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.domain.entity.settings.ChatSettings.Companion.VIBRATION_OFF
import mega.privacy.android.domain.entity.settings.ChatSettings.Companion.VIBRATION_ON
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatMessageNotificationBehaviourMapperTest {

    private lateinit var underTest: ChatMessageNotificationBehaviourMapper

    private val defaultSound = "defaultSound"
    private val emptyNotificationSound = ""
    private val notificationSound = "notificationSound"

    @BeforeAll
    fun setup() {
        underTest = ChatMessageNotificationBehaviourMapper()
    }

    @ParameterizedTest(name = "when chat settings is {0}, vibration == {1} and should beep == {2}")
    @MethodSource("provideParameters")
    fun `test that notification behaviour returns correctly`(
        notificationSound: String?,
        vibrate: String?,
        beep: Boolean,
    ) {
        val chatSettings =
            if (notificationSound == null && vibrate == null) {
                null
            } else {
                mock<ChatSettings> {
                    on { notificationsSound }.thenReturn(notificationSound)
                    on { vibrationEnabled }.thenReturn(vibrate)
                }

            }
        val expectedSound = when {
            beep && chatSettings != null -> chatSettings.notificationsSound.ifEmpty { defaultSound }
            beep -> defaultSound
            else -> null
        }
        val expectedVibration = when {
            beep && chatSettings != null -> chatSettings.vibrationEnabled
            beep -> VIBRATION_ON
            else -> VIBRATION_OFF
        }
        val expectedNotificationBehaviour =
            NotificationBehaviour(sound = expectedSound, vibration = expectedVibration)

        Truth.assertThat(underTest.invoke(chatSettings, beep, defaultSound))
            .isEqualTo(expectedNotificationBehaviour)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(null, null, true),
        Arguments.of(null, null, false),
        Arguments.of(emptyNotificationSound, VIBRATION_ON, true),
        Arguments.of(emptyNotificationSound, VIBRATION_ON, false),
        Arguments.of(emptyNotificationSound, VIBRATION_OFF, true),
        Arguments.of(emptyNotificationSound, VIBRATION_OFF, false),
        Arguments.of(notificationSound, VIBRATION_ON, true),
        Arguments.of(notificationSound, VIBRATION_ON, false),
        Arguments.of(notificationSound, VIBRATION_OFF, true),
        Arguments.of(notificationSound, VIBRATION_OFF, false),
    )
}