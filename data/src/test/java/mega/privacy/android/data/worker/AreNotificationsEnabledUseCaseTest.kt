package mega.privacy.android.data.worker

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.common.truth.Truth.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.EnvironmentRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AreNotificationsEnabledUseCaseTest {

    private lateinit var underTest: AreNotificationsEnabledUseCase

    private val notificationManager: NotificationManagerCompat = mock()
    private val environmentRepository: EnvironmentRepository = mock()
    private val context: Context = mock()
    private val staticMock = Mockito.mockStatic(ContextCompat::class.java)

    @BeforeAll
    fun init() {
        underTest = AreNotificationsEnabledUseCase(
            notificationManager = notificationManager,
            environmentRepository = environmentRepository,
            context = context
        )

    }

    @BeforeEach
    fun resetMocks() {
        reset(notificationManager, context, environmentRepository)
        staticMock.reset()
    }

    @AfterAll
    fun cleanup() {
        staticMock.close()
    }

    @Test
    fun `test that areNotificationsEnabled is called when the use case is invoked`() = runTest {
        underTest()
        verify(notificationManager).areNotificationsEnabled()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that areNotificationsEnabled result is returned when sdk is below tiramisu`(expected: Boolean) =
        runTest {
            whenever(environmentRepository.getDeviceSdkVersionInt()).thenReturn(Build.VERSION_CODES.TIRAMISU - 1)
            whenever(notificationManager.areNotificationsEnabled()).thenReturn(expected)
            val actual = underTest()
            assertThat(expected).isEqualTo(actual)
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that ActivityCompat checkSelfPermission result is returned when sdk is tiramisu and areNotificationsEnabled is true`(
        expected: Boolean,
    ) =
        runTest {
            whenever(environmentRepository.getDeviceSdkVersionInt()).thenReturn(Build.VERSION_CODES.TIRAMISU)
            whenever(notificationManager.areNotificationsEnabled()).thenReturn(true)

            whenever(ContextCompat.checkSelfPermission(any(), any())).thenReturn(
                if (expected) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
            )

            val actual = underTest()
            assertThat(expected).isEqualTo(actual)
        }
}