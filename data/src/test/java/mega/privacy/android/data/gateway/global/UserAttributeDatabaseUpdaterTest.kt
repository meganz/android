package mega.privacy.android.data.gateway.global

import android.content.Context
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class UserAttributeDatabaseUpdaterTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockLocalRoomGateway: MegaLocalRoomGateway

    @Mock
    private lateinit var mockApi: MegaApiJava

    @Mock
    private lateinit var mockRequest: MegaRequest

    @Mock
    private lateinit var mockError: MegaError

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun `test onRequestFinish with USER_ATTR_FIRSTNAME success`() = runTest {
        val userAttributeDatabaseUpdater = UserAttributeDatabaseUpdater(
            applicationScope = testScope,
            context = mockContext,
            localRoomGateway = mockLocalRoomGateway,
            databaseHandler = mock<Lazy<DatabaseHandler>>()
        )

        whenever(mockRequest.type).thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
        whenever(mockRequest.paramType).thenReturn(MegaApiJava.USER_ATTR_FIRSTNAME)
        whenever(mockRequest.email).thenReturn("test@example.com")
        whenever(mockRequest.text).thenReturn("John")
        whenever(mockError.errorCode).thenReturn(MegaError.API_OK)

        userAttributeDatabaseUpdater.onRequestFinish(mockApi, mockRequest, mockError)

        testDispatcher.scheduler.advanceUntilIdle()
        verify(mockLocalRoomGateway).updateContactNameByEmail(
            firstName = "John",
            email = "test@example.com"
        )
    }

    @Test
    fun `test onRequestFinish with USER_ATTR_FIRSTNAME failure`() = runTest {
        val userAttributeDatabaseUpdater = UserAttributeDatabaseUpdater(
            applicationScope = testScope,
            context = mockContext,
            localRoomGateway = mockLocalRoomGateway,
            databaseHandler = mock<Lazy<DatabaseHandler>>()
        )

        whenever(mockRequest.type).thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
        whenever(mockRequest.paramType).thenReturn(MegaApiJava.USER_ATTR_FIRSTNAME)
        whenever(mockError.errorCode).thenReturn(MegaError.API_EARGS)

        userAttributeDatabaseUpdater.onRequestFinish(mockApi, mockRequest, mockError)

        verify(mockLocalRoomGateway, never()).updateContactNameByEmail(any(), any())
    }

    @Test
    fun `test onRequestFinish with USER_ATTR_FIRSTNAME null email`() = runTest {
        val userAttributeDatabaseUpdater = UserAttributeDatabaseUpdater(
            applicationScope = testScope,
            context = mockContext,
            localRoomGateway = mockLocalRoomGateway,
            databaseHandler = mock<Lazy<DatabaseHandler>>()
        )

        whenever(mockRequest.type).thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
        whenever(mockRequest.paramType).thenReturn(MegaApiJava.USER_ATTR_FIRSTNAME)
        whenever(mockRequest.email).thenReturn(null)
        whenever(mockError.errorCode).thenReturn(MegaError.API_OK)

        userAttributeDatabaseUpdater.onRequestFinish(mockApi, mockRequest, mockError)

        verify(mockLocalRoomGateway, never()).updateContactNameByEmail(any(), any())
    }

    @Test
    fun `test onRequestFinish with USER_ATTR_FIRSTNAME blank email`() = runTest {
        val userAttributeDatabaseUpdater = UserAttributeDatabaseUpdater(
            applicationScope = testScope,
            context = mockContext,
            localRoomGateway = mockLocalRoomGateway,
            databaseHandler = mock<Lazy<DatabaseHandler>>()
        )

        whenever(mockRequest.type).thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
        whenever(mockRequest.paramType).thenReturn(MegaApiJava.USER_ATTR_FIRSTNAME)
        whenever(mockRequest.email).thenReturn("")
        whenever(mockError.errorCode).thenReturn(MegaError.API_OK)

        userAttributeDatabaseUpdater.onRequestFinish(mockApi, mockRequest, mockError)

        verify(mockLocalRoomGateway, never()).updateContactNameByEmail(any(), any())
    }

    @Test
    fun `test onRequestFinish with USER_ATTR_LASTNAME success`() = runTest {
        val userAttributeDatabaseUpdater = UserAttributeDatabaseUpdater(
            applicationScope = testScope,
            context = mockContext,
            localRoomGateway = mockLocalRoomGateway,
            databaseHandler = mock<Lazy<DatabaseHandler>>()
        )

        whenever(mockRequest.type).thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
        whenever(mockRequest.paramType).thenReturn(MegaApiJava.USER_ATTR_LASTNAME)
        whenever(mockRequest.email).thenReturn("test@example.com")
        whenever(mockRequest.text).thenReturn("Doe")
        whenever(mockError.errorCode).thenReturn(MegaError.API_OK)

        userAttributeDatabaseUpdater.onRequestFinish(mockApi, mockRequest, mockError)

        testDispatcher.scheduler.advanceUntilIdle()
        verify(mockLocalRoomGateway).updateContactLastNameByEmail(
            lastName = "Doe",
            email = "test@example.com"
        )
    }
} 
