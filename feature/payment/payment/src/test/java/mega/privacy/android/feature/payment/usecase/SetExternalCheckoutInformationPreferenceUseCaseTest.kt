package mega.privacy.android.feature.payment.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetExternalCheckoutInformationPreferenceUseCaseTest {

    private lateinit var underTest: SetExternalCheckoutInformationPreferenceUseCase
    private val appPreferencesGateway = mock<AppPreferencesGateway>()

    @BeforeEach
    fun setUp() {
        reset(appPreferencesGateway)
        underTest = SetExternalCheckoutInformationPreferenceUseCase(appPreferencesGateway)
    }

    @ParameterizedTest(name = "test that preference is set to {0}")
    @ValueSource(booleans = [true, false])
    fun `test that preference is set to expected value`(value: Boolean) = runTest {
        underTest(value)

        verify(appPreferencesGateway).putBoolean("external_checkout_show_information", value)
    }
}

