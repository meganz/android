package mega.privacy.android.data.repository.files

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.gateway.api.MegaApiGateway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FingerprintRepositoryTest {
    private lateinit var underTest: FingerprintRepositoryImpl

    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val megaApiGateway = mock<MegaApiGateway>()

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())

        underTest = FingerprintRepositoryImpl(
            ioDispatcher,
            megaApiGateway,
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            megaApiGateway,
        )
    }

    @Test
    fun `test that gateway result is returned when getFingerprint is invoked`() = runTest {
        val fingerprint = "fingerprint"
        val path = "path"
        whenever(underTest.getFingerprint(path)) doReturn fingerprint

        val actual = underTest.getFingerprint(path)

        assertThat(actual).isEqualTo(fingerprint)

    }
}