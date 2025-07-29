package mega.privacy.android.domain.usecase.domainmigration

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.DomainNameMigrationRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetDomainNameUseCaseTest {

    private lateinit var underTest: GetDomainNameUseCase

    private val domainNameRepository = mock<DomainNameMigrationRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetDomainNameUseCase(
            domainNameRepository
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(domainNameRepository)
    }

    @Test
    fun `test that use case returns mega app domain when repository flag is true and is not for email`() =
        runTest {
            whenever(domainNameRepository.isDomainNameMegaDotAppFromCache()) doReturn true

            val actual = underTest(false)

            assertThat(actual).isEqualTo("mega.app")
        }

    @Test
    fun `test that use case returns mega io domain when repository flag is true and is for email`() =
        runTest {
            whenever(domainNameRepository.isDomainNameMegaDotAppFromCache()) doReturn true

            val actual = underTest(true)

            assertThat(actual).isEqualTo("mega.io")
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that use case returns mega nz when repository flag is false`(isForEmail: Boolean) =
        runTest {
            whenever(domainNameRepository.isDomainNameMegaDotAppFromCache()) doReturn false

            val actual = underTest(isForEmail)

            assertThat(actual).isEqualTo("mega.nz")
        }

}