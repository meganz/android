package mega.privacy.android.domain.usecase.domainmigration

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.DomainNameMigrationRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
    fun `test that use case returns mega app domain when repository flag is true`() =
        runTest {
            whenever(domainNameRepository.isDomainNameMegaDotAppFromCache()) doReturn true

            val actual = underTest()

            assertThat(actual).isEqualTo("mega.app")
        }

    @Test
    fun `test that use case returns mega nz when repository flag is false`() =
        runTest {
            whenever(domainNameRepository.isDomainNameMegaDotAppFromCache()) doReturn false

            val actual = underTest()

            assertThat(actual).isEqualTo("mega.nz")
        }

}