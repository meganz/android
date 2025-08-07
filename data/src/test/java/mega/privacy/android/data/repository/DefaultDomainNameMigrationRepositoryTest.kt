package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import mega.privacy.android.data.repository.DomainNameMigrationRepositoryImpl.Companion.DOMAIN_NAME_MEGA_APP_KEY
import mega.privacy.android.domain.repository.DomainNameMigrationRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultDomainNameMigrationRepositoryTest {
    private lateinit var underTest: DomainNameMigrationRepository

    private val appPreferencesGateway = mock<AppPreferencesGateway>()

    @BeforeAll
    fun setup() {
        underTest = DomainNameMigrationRepositoryImpl(appPreferencesGateway)
    }

    @BeforeEach
    fun cleanUp() {
        reset(appPreferencesGateway)
    }

    @Test
    fun `test that default value is false`() = runTest {
        stubPreferences(emptyFlow())

        assertThat(underTest.isDomainNameMegaDotApp()).isEqualTo(false)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that value is returned from preferences gateway`(expected: Boolean) = runTest {
        stubPreferences(flowOf(expected))

        assertThat(underTest.isDomainNameMegaDotApp()).isEqualTo(expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that value is set to preferences gateway`(newValue: Boolean) = runTest {

        underTest.setDomainNameMegaDotApp(newValue)

        verify(appPreferencesGateway).putBoolean(DOMAIN_NAME_MEGA_APP_KEY, newValue)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that cached value is updated when value is fetched`(expected: Boolean) = runTest {
        stubPreferences(flowOf(expected))
        underTest.isDomainNameMegaDotApp()

        assertThat(underTest.isDomainNameMegaDotAppFromCache()).isEqualTo(expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that cached value is updated when value is updated`(newValue: Boolean) = runTest {

        underTest.setDomainNameMegaDotApp(newValue)

        assertThat(underTest.isDomainNameMegaDotAppFromCache()).isEqualTo(newValue)
    }

    private fun stubPreferences(flow: Flow<Boolean>) {
        whenever(
            appPreferencesGateway.monitorBoolean(eq(DOMAIN_NAME_MEGA_APP_KEY), any())
        ) doReturn flow
    }
}