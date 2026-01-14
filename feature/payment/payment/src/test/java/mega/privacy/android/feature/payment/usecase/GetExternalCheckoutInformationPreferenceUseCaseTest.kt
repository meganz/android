package mega.privacy.android.feature.payment.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetExternalCheckoutInformationPreferenceUseCaseTest {

    private lateinit var underTest: GetExternalCheckoutInformationPreferenceUseCase
    private val appPreferencesGateway = mock<AppPreferencesGateway>()

    @BeforeEach
    fun setUp() {
        reset(appPreferencesGateway)
        underTest = GetExternalCheckoutInformationPreferenceUseCase(appPreferencesGateway)
    }

    @ParameterizedTest(name = "test that preference returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that preference returns expected value`(expectedValue: Boolean) = runTest {
        whenever(
            appPreferencesGateway.monitorBoolean(
                "external_checkout_show_information", defaultValue = true
            )
        ).thenReturn(flowOf(expectedValue))

        val result = underTest()
        assertThat(result).isEqualTo(expectedValue)
    }
}

