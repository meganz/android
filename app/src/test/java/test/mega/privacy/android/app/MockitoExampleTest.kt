package test.mega.privacy.android.app

import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever

/**
 * Example unit test using Mockito
 */
@RunWith(MockitoJUnitRunner::class)
class MockitoExampleTest {

    @Test
    fun test_getGreetings_ReturnsValidIfUseridIsValid() {
        val mockService = Mockito.mock(DependentService::class.java)
        val myClass = MyClassUnderTest(mockService)

        whenever(mockService.getUserName(1)).thenReturn("Robin")
        Truth.assertThat(myClass.getGreetings(1)).isEqualTo("Hello Robin")

        whenever(mockService.getUserName(2)).thenReturn("Jack")
        Truth.assertThat(myClass.getGreetings(2)).isEqualTo("Hello Jack")
    }

    @Test
    fun test_getGreetings_ReturnsUserNotExistIfUseridIsInvalid() {
        val mockService = Mockito.mock(DependentService::class.java)
        val myClass = MyClassUnderTest(mockService)

        whenever(mockService.getUserName(-1)).thenReturn(null)
        Truth.assertThat(myClass.getGreetings(-1)).isEqualTo("User Not Exist")
    }
}

/**
 * a Service interface that MyClassUnderTest depends on.
 * We will use Mockito to mock this interface.
 */
interface DependentService {
    /**
     * get user name by id. return null if user id is not found.
     */
    fun getUserName(userId: Int): String?
}

/**
 * The class we are going to test
 */
class MyClassUnderTest(private val service: DependentService) {
    fun getGreetings(userId: Int): String {
        val userName = service.getUserName(userId)
        return if (userName != null) {
            "Hello $userName"
        } else {
            "User Not Exist"
        }
    }
}
