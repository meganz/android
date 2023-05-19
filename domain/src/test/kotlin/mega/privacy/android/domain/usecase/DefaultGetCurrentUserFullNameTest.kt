package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetCurrentUserFullNameTest {

    private lateinit var underTest: DefaultGetCurrentUserFullName

    private val getUserFullNameUseCase: GetUserFullNameUseCase = mock()

    private val defaultFirstName = "defaultFirstName"
    private val defaultLastName = "defaultLastName"

    @Before
    fun setUp() {
        underTest = DefaultGetCurrentUserFullName(
            getUserFullNameUseCase = getUserFullNameUseCase
        )
    }

    @Test
    fun `test that default names are used when both first name, last name and email are null or blank`() =
        runTest {
            assertThat(
                underTest(true, defaultFirstName, defaultLastName)
            ).isEqualTo("$defaultFirstName $defaultLastName")
        }

    @Test
    fun `test that default names are not used when getUserFullName returns valid name`() = runTest {
        val expectedFullName = "Full Name"
        whenever(getUserFullNameUseCase(any())).thenReturn(expectedFullName)
        assertThat(
            underTest(true, defaultFirstName, defaultLastName)
        ).isEqualTo(expectedFullName)
    }
}