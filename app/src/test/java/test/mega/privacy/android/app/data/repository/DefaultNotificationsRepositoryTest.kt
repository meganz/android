package test.mega.privacy.android.app.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.gateway.api.MegaLocalStorageGateway
import mega.privacy.android.app.data.mapper.ContactRequestMapper
import mega.privacy.android.app.data.mapper.UserAlertContactProvider
import mega.privacy.android.app.data.mapper.UserAlertEmailProvider
import mega.privacy.android.app.data.mapper.UserAlertMapper
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.data.repository.DefaultNotificationsRepository
import mega.privacy.android.app.main.megachat.NonContactInfo
import mega.privacy.android.domain.repository.NotificationsRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaUserAlert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultNotificationsRepositoryTest {
    private lateinit var underTest: NotificationsRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val contactRequestMapper = mock<ContactRequestMapper>()
    private val userHandle = 12L
    private val email = "email"

    private val userAlertsMapper: UserAlertMapper =
        { _: MegaUserAlert, emailProvider: UserAlertEmailProvider, contactProvider: UserAlertContactProvider ->
            emailProvider(userHandle)
            contactProvider(email)
            mock()
        }
    private val megaLocalStorageGateway = mock<MegaLocalStorageGateway>()

    @Before
    fun setUp() {
        underTest = DefaultNotificationsRepository(
            megaApiGateway = megaApiGateway,
            contactRequestMapper = contactRequestMapper,
            userAlertsMapper = userAlertsMapper,
            localStorageGateway = megaLocalStorageGateway,
            dispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `test that user alert email is fetched locally`() = runTest {
        val globalUpdate = GlobalUpdate.OnUserAlertsUpdate(arrayListOf(mock()))
        whenever(megaApiGateway.globalUpdates).thenReturn(flowOf(globalUpdate))
        val contactInfo = mock<NonContactInfo> { on { email }.thenReturn("Email") }
        whenever(megaLocalStorageGateway.getNonContactByHandle(any())).thenReturn(contactInfo)

        underTest.monitorUserAlerts().test {
            cancelAndConsumeRemainingEvents()
            verify(megaLocalStorageGateway).getNonContactByHandle(userHandle)
            verify(megaApiGateway, never()).getUserEmail(any(), any())
        }
    }

    @Test
    fun `test that email is fetched if not found locally`() = runTest {
        val globalUpdate = GlobalUpdate.OnUserAlertsUpdate(arrayListOf(mock()))
        whenever(megaApiGateway.globalUpdates).thenReturn(flowOf(globalUpdate))
        whenever(megaLocalStorageGateway.getNonContactByHandle(any())).thenReturn(null)
        val megaApiJava = mock<MegaApiJava>()
        val request = mock<MegaRequest> { on { email }.thenReturn("Email") }
        val mock = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
        whenever(megaApiGateway.getUserEmail(any(), any())).thenAnswer {
            (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                megaApiJava,
                request,
                mock
            )
        }

        underTest.monitorUserAlerts().test {
            cancelAndConsumeRemainingEvents()
            verify(megaLocalStorageGateway).getNonContactByHandle(userHandle)
            verify(megaApiGateway).getUserEmail(any(), any())
        }
    }

    @Test
    fun `test that fetched email is cached`() = runTest {
        val globalUpdate = GlobalUpdate.OnUserAlertsUpdate(arrayListOf(mock()))
        whenever(megaApiGateway.globalUpdates).thenReturn(flowOf(globalUpdate))
        whenever(megaLocalStorageGateway.getNonContactByHandle(any())).thenReturn(null)
        val megaApiJava = mock<MegaApiJava>()
        val fetchedEmail = "Email"
        val request = mock<MegaRequest> { on { email }.thenReturn(fetchedEmail) }
        val mock = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
        whenever(megaApiGateway.getUserEmail(any(), any())).thenAnswer {
            (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                megaApiJava,
                request,
                mock
            )
        }

        underTest.monitorUserAlerts().test {
            cancelAndConsumeRemainingEvents()
            verify(megaLocalStorageGateway).setNonContactEmail(userHandle, fetchedEmail)
        }
    }

    @Test
    fun `test that contact is fetched from the api gateway`() = runTest {
        val globalUpdate = GlobalUpdate.OnUserAlertsUpdate(arrayListOf(mock()))
        whenever(megaApiGateway.globalUpdates).thenReturn(flowOf(globalUpdate))
        val contactInfo = mock<NonContactInfo> { on { email }.thenReturn(email) }
        whenever(megaLocalStorageGateway.getNonContactByHandle(any())).thenReturn(contactInfo)
        whenever(megaApiGateway.getContact(any())).thenReturn(mock())

        underTest.monitorUserAlerts().test {
            cancelAndConsumeRemainingEvents()
            verify(megaApiGateway).getContact(email)
        }
    }
}