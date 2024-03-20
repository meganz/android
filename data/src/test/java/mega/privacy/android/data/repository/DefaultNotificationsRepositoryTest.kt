package mega.privacy.android.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.NotificationsGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.data.mapper.EventMapper
import mega.privacy.android.data.mapper.NodeProvider
import mega.privacy.android.data.mapper.UserAlertContactProvider
import mega.privacy.android.data.mapper.UserAlertMapper
import mega.privacy.android.data.mapper.UserAlertScheduledMeetingOccurrProvider
import mega.privacy.android.data.mapper.UserAlertScheduledMeetingProvider
import mega.privacy.android.data.mapper.meeting.IntegerListMapper
import mega.privacy.android.data.mapper.notification.PromoNotificationListMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.model.chat.NonContactInfo
import mega.privacy.android.domain.entity.CallsMeetingInvitations
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.ContactAlert
import mega.privacy.android.domain.entity.ContactChangeContactEstablishedAlert
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.NormalEvent
import mega.privacy.android.domain.entity.notifications.PromoNotification
import mega.privacy.android.domain.repository.NotificationsRepository
import mega.privacy.android.domain.usecase.meeting.FetchNumberOfScheduledMeetingOccurrencesByChat
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeeting
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaEvent
import nz.mega.sdk.MegaIntegerList
import nz.mega.sdk.MegaNotification
import nz.mega.sdk.MegaNotificationList
import nz.mega.sdk.MegaPushNotificationSettings
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaStringMap
import nz.mega.sdk.MegaUserAlert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultNotificationsRepositoryTest {
    private lateinit var underTest: NotificationsRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val eventMapper = mock<EventMapper>()
    private val userHandle = 12L
    private val email = "email"

    private val userAlertsMapper: UserAlertMapper =
        { alert: MegaUserAlert, contactProvider: UserAlertContactProvider, _: UserAlertScheduledMeetingProvider, _: UserAlertScheduledMeetingOccurrProvider, _: NodeProvider ->
            val contact = contactProvider(userHandle, alert.email)
            ContactChangeContactEstablishedAlert(
                id = 12L,
                seen = false,
                createdTime = 1L,
                isOwnChange = false,
                contact = contact,
            )
        }
    private val megaLocalStorageGateway = mock<MegaLocalStorageGateway>()
    private val fetchSchedOccurrencesByChatUseCase =
        mock<FetchNumberOfScheduledMeetingOccurrencesByChat>()
    private val getScheduledMeetingUseCase = mock<GetScheduledMeeting>()
    private val callsPreferencesGateway = mock<CallsPreferencesGateway>()
    private val appEventGateway = mock<AppEventGateway>()
    private val notificationsGateway = mock<NotificationsGateway>()
    private val promoNotificationListMapper = mock<PromoNotificationListMapper>()
    private val integerListMapper = mock<IntegerListMapper>()

    @BeforeAll
    fun setUp() {
        underTest = DefaultNotificationsRepository(
            megaApiGateway = megaApiGateway,
            userAlertsMapper = userAlertsMapper,
            eventMapper = eventMapper,
            localStorageGateway = megaLocalStorageGateway,
            fetchSchedOccurrencesByChatUseCase = fetchSchedOccurrencesByChatUseCase,
            getScheduledMeetingUseCase = getScheduledMeetingUseCase,
            callsPreferencesGateway = callsPreferencesGateway,
            dispatcher = UnconfinedTestDispatcher(),
            appEventGateway = appEventGateway,
            notificationsGateway = notificationsGateway,
            promoNotificationListMapper = promoNotificationListMapper,
            integerListMapper = integerListMapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaApiGateway,
            eventMapper,
            megaLocalStorageGateway,
            fetchSchedOccurrencesByChatUseCase,
            getScheduledMeetingUseCase,
            callsPreferencesGateway,
            appEventGateway,
            notificationsGateway,
            promoNotificationListMapper,
            integerListMapper
        )

        whenever(callsPreferencesGateway.getCallsMeetingInvitationsPreference())
            .thenReturn(flowOf(CallsMeetingInvitations.Enabled))
        whenever(megaApiGateway.createInstanceMegaPushNotificationSettings())
            .thenReturn(mock())
    }

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

    @Test
    fun `test that current user alerts are fetched`() = runTest {
        whenever(megaApiGateway.getUserAlerts()).thenReturn(listOf())

        underTest.getUserAlerts()

        verify(megaApiGateway).getUserAlerts()
    }

    @Test
    fun `test that alert email is returned if found on alert`() = runTest {
        val expectedEmail = "expected@email"
        val userAlert = mock<MegaUserAlert> { on { email }.thenReturn(expectedEmail) }
        val globalUpdate = GlobalUpdate.OnUserAlertsUpdate(arrayListOf(userAlert))
        whenever(megaApiGateway.globalUpdates).thenReturn(flowOf(globalUpdate))

        whenever(megaApiGateway.getContact(any())).thenReturn(mock())

        underTest.monitorUserAlerts().test {
            cancelAndConsumeRemainingEvents()
            verify(megaLocalStorageGateway, never()).getNonContactByHandle(any())
            verify(megaApiGateway).getContact(expectedEmail)
        }
    }

    @Test
    fun `test that nickname is returned with user alert`() = runTest {
        val globalUpdate = GlobalUpdate.OnUserAlertsUpdate(arrayListOf(mock()))
        whenever(megaApiGateway.globalUpdates).thenReturn(flowOf(globalUpdate))
        val contactInfo = mock<NonContactInfo> { on { email }.thenReturn(email) }
        whenever(megaLocalStorageGateway.getNonContactByHandle(any())).thenReturn(contactInfo)
        whenever(megaApiGateway.getContact(any())).thenReturn(mock())
        val expectedNickname = "A nickname"
        val contactDB = mock<Contact> { on { nickname }.thenReturn(expectedNickname) }
        whenever(megaLocalStorageGateway.getContactByEmail(any())).thenReturn(contactDB)

        underTest.monitorUserAlerts().test {
            val alert = awaitItem().first() as ContactAlert
            assertThat(alert.contact.nickname).isEqualTo(expectedNickname)
            awaitComplete()
        }
    }

    @Test
    fun `test that event is returned if found`() = runTest {
        val expectedEvent = NormalEvent(
            handle = 1L,
            text = "expected text",
            number = 2L,
            type = EventType.NodesCurrent,
            eventString = "expected event string"
        )
        val megaEvent = mock<MegaEvent> {
            on { type }.thenReturn(MegaEvent.EVENT_NODES_CURRENT)
            on { handle }.thenReturn(expectedEvent.handle)
            on { text }.thenReturn(expectedEvent.text)
            on { number }.thenReturn(expectedEvent.number)
            on { eventString }.thenReturn(expectedEvent.eventString)
        }
        val globalUpdate = GlobalUpdate.OnEvent(megaEvent)
        whenever(megaApiGateway.globalUpdates).thenReturn(flowOf(globalUpdate))
        whenever(eventMapper(megaEvent)).thenReturn(expectedEvent)

        underTest.monitorEvent().test {
            assertThat(awaitItem()).isEqualTo(expectedEvent)
            awaitComplete()
        }
    }

    @Test
    fun `test that multiple events are returned if found`() = runTest {
        val expectedEvent = NormalEvent(
            handle = 1L,
            text = "expected text",
            number = 2L,
            type = EventType.NodesCurrent,
            eventString = "expected event string"
        )
        val megaEvent = mock<MegaEvent> {
            on { type }.thenReturn(MegaEvent.EVENT_NODES_CURRENT)
            on { handle }.thenReturn(expectedEvent.handle)
            on { text }.thenReturn(expectedEvent.text)
            on { number }.thenReturn(expectedEvent.number)
            on { eventString }.thenReturn(expectedEvent.eventString)
        }
        val globalUpdate = GlobalUpdate.OnEvent(megaEvent)
        whenever(megaApiGateway.globalUpdates).thenReturn(
            flowOf(
                globalUpdate,
                globalUpdate,
                globalUpdate
            )
        )
        whenever(eventMapper(megaEvent)).thenReturn(expectedEvent)

        underTest.monitorEvent().test {
            assertThat(awaitItem()).isEqualTo(expectedEvent)
            assertThat(awaitItem()).isEqualTo(expectedEvent)
            assertThat(awaitItem()).isEqualTo(expectedEvent)
            awaitComplete()
        }
    }

    @Test
    fun `test that list of enabled notifications will be returned when requested`() = runTest {
        val expectedList = listOf(1, 2, 3)

        val megaIntegerList = mock<MegaIntegerList> {
            on { size() } doReturn 3
            on { get(0) }.thenReturn(1)
            on { get(1) }.thenReturn(2)
            on { get(2) }.thenReturn(3)

        }
        whenever(notificationsGateway.getEnabledNotifications()).thenReturn(megaIntegerList)
        whenever(integerListMapper.invoke(megaIntegerList))
            .thenReturn(expectedList)
        val result = underTest.getEnabledNotifications()

        assertThat(result).isEqualTo(expectedList)
    }

    @Test
    fun `test that list of enabled notifications will be empty when there are no enabled notifications`() =
        runTest {
            val expectedList = emptyList<Int>()

            val megaIntegerList = mock<MegaIntegerList> {
                on { size() } doReturn 0
            }
            whenever(notificationsGateway.getEnabledNotifications()).thenReturn(megaIntegerList)
            whenever(integerListMapper.invoke(megaIntegerList))
                .thenReturn(expectedList)
            val result = underTest.getEnabledNotifications()

            assertThat(result).isEqualTo(expectedList)
        }

    @Test
    fun `test that list of promo notifications is fetched`() = runTest {
        val staticURL = "https://eu.static.mega.co.nz/psa/"
        val testImageName = "vpn"
        val promoNotification = PromoNotification(
            promoID = 1L,
            title = "title",
            description = "description",
            iconURL = "$staticURL$testImageName@2x.png",
            imageURL = "$staticURL$testImageName@2x.png",
            startTimeStamp = 1L,
            endTimeStamp = 2L,
            actionName = "actionName",
            actionURL = "actionURL"
        )
        val callToAction1Mock = mock<MegaStringMap> {
            on { get("text") } doReturn promoNotification.actionName
            on { get("link") } doReturn promoNotification.actionURL
        }
        val callToAction2Mock = mock<MegaStringMap> {
            on { get("text") } doReturn "actionName1"
            on { get("link") } doReturn "actionURL1"
        }
        val megaNotification = mock<MegaNotification> {
            on { id } doReturn 1L
            on { title } doReturn promoNotification.title
            on { description } doReturn promoNotification.description
            on { iconName } doReturn testImageName
            on { imageName } doReturn testImageName
            on { imagePath } doReturn staticURL
            on { start } doReturn 1L
            on { end } doReturn 2L
            on { callToAction1 }.thenReturn(callToAction1Mock)
            on { callToAction2 }.thenReturn(callToAction2Mock)
        }
        val megaNotificationList = mock<MegaNotificationList> {
            on { size() } doReturn 1
            on { get(0) }.thenReturn(megaNotification)
        }
        val megaApiJava = mock<MegaApiJava>()
        val request =
            mock<MegaRequest> { on { megaNotifications }.thenReturn(megaNotificationList) }
        val mock = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }

        whenever(notificationsGateway.getNotifications(any())).thenAnswer {
            (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                megaApiJava,
                request,
                mock
            )
        }
        whenever(promoNotificationListMapper.invoke(megaNotificationList))
            .thenReturn(listOf(promoNotification))
        val result = underTest.getPromoNotifications()
        assertThat(result).isEqualTo(listOf(promoNotification))
    }

    @Test
    fun `test that the empty list is returned if getNotifications throws API_ENOENT error`() =
        runTest {
            val megaNotificationList = mock<MegaNotificationList> {
                on { size() } doReturn 0
            }
            val megaApiJava = mock<MegaApiJava>()
            val request =
                mock<MegaRequest> { on { megaNotifications }.thenReturn(megaNotificationList) }
            val mock = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_ENOENT) }

            whenever(notificationsGateway.getNotifications(any())).thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    megaApiJava,
                    request,
                    mock
                )
            }
            val result = underTest.getPromoNotifications()
            assertThat(result).isEmpty()
        }

    @Test
    fun `test that the last notification id is returned`() = runTest {
        val expectedId = 123L
        val megaApiJava = mock<MegaApiJava>()
        val request = mock<MegaRequest> { on { number }.thenReturn(expectedId) }
        val mock = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }

        whenever(notificationsGateway.getLastReadNotificationId(any())).thenAnswer {
            (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                megaApiJava,
                request,
                mock
            )
        }
        val result = underTest.getLastReadNotificationId()
        assertThat(result).isEqualTo(expectedId)
    }

    @Test
    fun `test that the last notification id is 0 when MegaError is API_ENOENT`() = runTest {
        val expectedId = 0L
        val megaApiJava = mock<MegaApiJava>()
        val request = mock<MegaRequest> { on { number }.thenReturn(expectedId) }
        val mock = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_ENOENT) }

        whenever(notificationsGateway.getLastReadNotificationId(any())).thenAnswer {
            (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                megaApiJava,
                request,
                mock
            )
        }
        val result = underTest.getLastReadNotificationId()
        assertThat(result).isEqualTo(expectedId)
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    inner class PushNotificationSettings {

        private val settings = mock<MegaPushNotificationSettings>()

        @BeforeEach
        fun setupMocks(): Unit = runBlocking {
            whenever(megaApiGateway.getPushNotificationSettings(any())).thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock { on { megaPushNotificationSettings }.thenReturn(settings) },
                    mock { on { errorCode }.thenReturn(MegaError.API_OK) }
                )
            }
            whenever(megaApiGateway.copyMegaPushNotificationsSettings(settings)).thenReturn(settings)
        }

        @Test
        fun `test that isChatEnabled returns correct value`() = runTest {
            val chatId = 123L
            val enabled = true
            val settings = mock<MegaPushNotificationSettings> {
                on { isChatEnabled(chatId) }.thenReturn(enabled)
            }

            whenever(megaApiGateway.getPushNotificationSettings(any())).thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock { on { megaPushNotificationSettings }.thenReturn(settings) },
                    mock { on { errorCode }.thenReturn(MegaError.API_OK) }
                )
            }
            whenever(megaApiGateway.copyMegaPushNotificationsSettings(settings)).thenReturn(settings)

            underTest.updatePushNotificationSettings()

            val result = underTest.isChatEnabled(chatId)

            assertThat(result).isEqualTo(enabled)
        }

        @Test
        fun `test that isChatDoNotDisturbEnabled returns correct value`() = runTest {
            val chatId = 123L
            val enabled = true
            val settings = mock<MegaPushNotificationSettings> {
                on { isChatDndEnabled(chatId) }.thenReturn(enabled)
            }

            whenever(megaApiGateway.getPushNotificationSettings(any())).thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock { on { megaPushNotificationSettings }.thenReturn(settings) },
                    mock { on { errorCode }.thenReturn(MegaError.API_OK) }
                )
            }
            whenever(megaApiGateway.copyMegaPushNotificationsSettings(settings)).thenReturn(settings)

            underTest.updatePushNotificationSettings()

            val result = underTest.isChatDoNotDisturbEnabled(chatId)

            assertThat(result).isEqualTo(enabled)
        }

        @Test
        fun `test that setChatEnabled updates settings correctly`() = runTest {
            val chatId = 123L
            val enabled = true
            val expectedSettings = settings.apply {
                enableChat(chatId, enabled)
            }

            whenever(megaApiGateway.setPushNotificationSettings(eq(expectedSettings), any()))
                .thenAnswer {
                    (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                        mock(),
                        mock { on { megaPushNotificationSettings }.thenReturn(expectedSettings) },
                        mock { on { errorCode }.thenReturn(MegaError.API_OK) }
                    )
                }
            whenever(megaApiGateway.copyMegaPushNotificationsSettings(expectedSettings))
                .thenReturn(expectedSettings)

            underTest.updatePushNotificationSettings()

            underTest.setChatEnabled(chatId, enabled)

            verify(megaApiGateway).setPushNotificationSettings(eq(expectedSettings), any())
        }

        @Test
        fun `test that setChatEnabled broadcasts a PushNotificationSettings event`() = runTest {
            val chatId = 123L
            val enabled = true
            val expectedSettings = settings.apply {
                enableChat(chatId, enabled)
            }

            whenever(megaApiGateway.setPushNotificationSettings(eq(expectedSettings), any()))
                .thenAnswer {
                    (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                        mock(),
                        mock { on { megaPushNotificationSettings }.thenReturn(expectedSettings) },
                        mock { on { errorCode }.thenReturn(MegaError.API_OK) }
                    )
                }

            underTest.updatePushNotificationSettings()

            underTest.setChatEnabled(chatId, enabled)

            verify(appEventGateway, times(2)).broadcastPushNotificationSettings()
        }

        @Test
        fun `test that setChatDoNotDisturb updates settings correctly`() = runTest {
            val chatId = 123L
            val timestamp = 111111L
            val expectedSettings = settings.apply {
                setChatDnd(chatId, timestamp)
            }

            whenever(megaApiGateway.setPushNotificationSettings(eq(expectedSettings), any()))
                .thenAnswer {
                    (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                        mock(),
                        mock { on { megaPushNotificationSettings }.thenReturn(expectedSettings) },
                        mock { on { errorCode }.thenReturn(MegaError.API_OK) }
                    )
                }
            whenever(megaApiGateway.copyMegaPushNotificationsSettings(expectedSettings))
                .thenReturn(expectedSettings)

            underTest.updatePushNotificationSettings()

            underTest.setChatDoNotDisturb(chatId, timestamp)

            verify(megaApiGateway).setPushNotificationSettings(eq(expectedSettings), any())
        }

        @Test
        fun `test that setChatDoNotDisturb broadcasts a PushNotificationSettings event`() =
            runTest {
                val chatId = 123L
                val timestamp = 111111L
                val expectedSettings = settings.apply {
                    setChatDnd(chatId, timestamp)
                }

                whenever(megaApiGateway.setPushNotificationSettings(eq(expectedSettings), any()))
                    .thenAnswer {
                        (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                            mock(),
                            mock { on { megaPushNotificationSettings }.thenReturn(expectedSettings) },
                            mock { on { errorCode }.thenReturn(MegaError.API_OK) }
                        )
                    }
                whenever(megaApiGateway.copyMegaPushNotificationsSettings(expectedSettings))
                    .thenReturn(expectedSettings)

                underTest.updatePushNotificationSettings()

                underTest.setChatDoNotDisturb(chatId, timestamp)

                verify(appEventGateway, times(2)).broadcastPushNotificationSettings()
            }

        @Test
        fun `test that setChatsEnabled updates settings correctly`() = runTest {
            val enabled = true
            val expectedSettings = settings.apply {
                enableChats(enabled)
            }

            whenever(megaApiGateway.setPushNotificationSettings(eq(expectedSettings), any()))
                .thenAnswer {
                    (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                        mock(),
                        mock { on { megaPushNotificationSettings }.thenReturn(expectedSettings) },
                        mock { on { errorCode }.thenReturn(MegaError.API_OK) }
                    )
                }
            whenever(megaApiGateway.copyMegaPushNotificationsSettings(expectedSettings))
                .thenReturn(expectedSettings)

            underTest.updatePushNotificationSettings()

            underTest.setChatsEnabled(enabled)

            verify(megaApiGateway).setPushNotificationSettings(eq(expectedSettings), any())
        }

        @Test
        fun `test that setChatsEnabled broadcasts a PushNotificationSettings event`() = runTest {
            val enabled = true
            val expectedSettings = settings.apply {
                enableChats(enabled)
            }

            whenever(megaApiGateway.setPushNotificationSettings(eq(expectedSettings), any()))
                .thenAnswer {
                    (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                        mock(),
                        mock { on { megaPushNotificationSettings }.thenReturn(expectedSettings) },
                        mock { on { errorCode }.thenReturn(MegaError.API_OK) }
                    )
                }
            whenever(megaApiGateway.copyMegaPushNotificationsSettings(expectedSettings))
                .thenReturn(expectedSettings)

            underTest.updatePushNotificationSettings()

            underTest.setChatsEnabled(enabled)

            verify(appEventGateway, times(2)).broadcastPushNotificationSettings()
        }

        @Test
        fun `test that setChatsDoNotDisturb updates settings correctly`() = runTest {
            val timestamp = 111111L
            val expectedSettings = settings.apply {
                globalChatsDnd = timestamp
            }

            whenever(megaApiGateway.setPushNotificationSettings(eq(expectedSettings), any()))
                .thenAnswer {
                    (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                        mock(),
                        mock { on { megaPushNotificationSettings }.thenReturn(expectedSettings) },
                        mock { on { errorCode }.thenReturn(MegaError.API_OK) }
                    )
                }
            whenever(megaApiGateway.copyMegaPushNotificationsSettings(expectedSettings))
                .thenReturn(expectedSettings)

            underTest.updatePushNotificationSettings()

            underTest.setChatsDoNotDisturb(timestamp)

            verify(megaApiGateway).setPushNotificationSettings(eq(expectedSettings), any())
        }

        @Test
        fun `test that setChatsDoNotDisturb broadcasts a PushNotificationSettings event`() =
            runTest {
                val timestamp = 111111L
                val expectedSettings = settings.apply {
                    globalChatsDnd = timestamp
                }

                whenever(megaApiGateway.setPushNotificationSettings(eq(expectedSettings), any()))
                    .thenAnswer {
                        (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                            mock(),
                            mock { on { megaPushNotificationSettings }.thenReturn(expectedSettings) },
                            mock { on { errorCode }.thenReturn(MegaError.API_OK) }
                        )
                    }
                whenever(megaApiGateway.copyMegaPushNotificationsSettings(expectedSettings))
                    .thenReturn(expectedSettings)

                underTest.updatePushNotificationSettings()

                underTest.setChatsDoNotDisturb(timestamp)

                verify(appEventGateway, times(2)).broadcastPushNotificationSettings()
            }

        @Test
        fun `test that updatePushNotificationsSettings will fetch the MegaPushNotificationSettings from the server`() =
            runTest {
                underTest.updatePushNotificationSettings()
                verify(megaApiGateway).getPushNotificationSettings(any())
            }

        @Test
        fun `test that updatePushNotificationSettings broadcasts a PushNotificationSettings event`() =
            runTest {
                underTest.updatePushNotificationSettings()
                verify(appEventGateway).broadcastPushNotificationSettings()
            }
    }

}
