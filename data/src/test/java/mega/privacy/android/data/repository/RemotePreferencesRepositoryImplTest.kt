package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.MegaStringMapMapper
import mega.privacy.android.data.wrapper.StringWrapper
import mega.privacy.android.domain.entity.chat.MeetingTooltipItem
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaStringMap
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class RemotePreferencesRepositoryImplTest {
    private lateinit var underTest: RemotePreferencesRepositoryImpl
    private val megaApiGateway = mock<MegaApiGateway>()
    private val megaStringMapMapper = mock<MegaStringMapMapper>()
    private val stringWrapper = mock<StringWrapper>()
    private val ioDispatcher = UnconfinedTestDispatcher()

    @Before
    internal fun setUp() {
        underTest = RemotePreferencesRepositoryImpl(
            ioDispatcher = ioDispatcher,
            megaApiGateway = megaApiGateway,
            stringWrapper = stringWrapper,
            megaStringMapMapper = megaStringMapMapper,
        )

        whenever(megaStringMapMapper.invoke(any<MegaStringMap>())).thenReturn(emptyMap())
        whenever(megaStringMapMapper.invoke(any<Map<String, String>>())).thenReturn(mock())
        whenever(stringWrapper.encodeBase64(any())).thenAnswer { ((it.arguments[0]) as String) }
        whenever(stringWrapper.decodeBase64(any())).thenAnswer { ((it.arguments[0]) as String) }

        whenever(megaApiGateway.getUserAttribute(eq(MegaApiJava.USER_ATTR_APPS_PREFS), any()))
            .thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock { on { megaStringMap }.thenReturn(mock()) },
                    mock { on { errorCode }.thenReturn(MegaError.API_OK) }
                )
            }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setMeetingTooltipPreference should update app preferences`() = runTest(ioDispatcher) {
        val item = MeetingTooltipItem.CREATE
        val encodedItemName = "Q1JFQVRF"
        val preference = mapOf("aObSm" to encodedItemName)
        whenever(stringWrapper.encodeBase64(item.name)).thenReturn(encodedItemName)
        whenever(megaStringMapMapper.invoke(any<MegaStringMap>())).thenReturn(preference)
        whenever(
            megaApiGateway.setUserAttribute(
                eq(MegaApiJava.USER_ATTR_APPS_PREFS),
                any<MegaStringMap>(),
                any()
            )
        ).thenAnswer {
            ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                mock { on { megaStringMap }.thenReturn(mock()) },
                mock { on { errorCode }.thenReturn(MegaError.API_OK) }
            )
        }

        underTest.setMeetingTooltipPreference(item)

        verify(megaApiGateway).setUserAttribute(
            eq(MegaApiJava.USER_ATTR_APPS_PREFS),
            any<MegaStringMap>(),
            any()
        )
    }

    @Test
    fun `getMeetingTooltipPreference should return meeting tooltip preference`() =
        runTest(ioDispatcher) {
            val item = MeetingTooltipItem.CREATE
            val encodedItemName = "Q1JFQVRF"
            val preference = mapOf("aObSm" to encodedItemName)
            whenever(stringWrapper.decodeBase64(encodedItemName)).thenReturn(item.name)
            whenever(megaStringMapMapper.invoke(any<MegaStringMap>())).thenReturn(preference)
            whenever(megaApiGateway.getUserAttribute(eq(MegaApiJava.USER_ATTR_APPS_PREFS), any()))
                .thenAnswer {
                    ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                        mock(),
                        mock { on { megaStringMap }.thenReturn(mock()) },
                        mock { on { errorCode }.thenReturn(MegaError.API_OK) }
                    )
                }

            val result = underTest.getMeetingTooltipPreference()

            assertThat(result).isEqualTo(item)
        }
}
