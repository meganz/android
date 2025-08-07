package mega.privacy.android.app.presentation.mediaplayer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.TimberJUnit5Extension
import mega.privacy.android.app.mediaplayer.SelectSubtitleFileViewModel
import mega.privacy.android.app.mediaplayer.mapper.SubtitleFileInfoItemMapper
import mega.privacy.android.app.mediaplayer.model.SubtitleFileInfoItem
import mega.privacy.android.app.mediaplayer.model.SubtitleLoadState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetSRTSubtitleFileListUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(value = [CoroutineMainDispatcherExtension::class, TimberJUnit5Extension::class])
internal class SelectSubtitleFileViewModelTest {
    private lateinit var underTest: SelectSubtitleFileViewModel

    private val getSRTSubtitleFileListUseCase = mock<GetSRTSubtitleFileListUseCase>()
    private val subtitleFileInfoItemMapper = mock<SubtitleFileInfoItemMapper>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val accountDetailFakeFlow = MutableSharedFlow<AccountDetail>()
    private val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()

    @BeforeEach
    fun setUp() {
        reset(
            getSRTSubtitleFileListUseCase,
            subtitleFileInfoItemMapper,
            monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase
        )
        wheneverBlocking { getSRTSubtitleFileListUseCase() }.thenReturn(emptyList())
        wheneverBlocking { monitorShowHiddenItemsUseCase() }.thenReturn(flowOf(false))
        wheneverBlocking { monitorAccountDetailUseCase() }.thenReturn(accountDetailFakeFlow)
        underTest = SelectSubtitleFileViewModel(
            getSRTSubtitleFileListUseCase = getSRTSubtitleFileListUseCase,
            subtitleFileInfoItemMapper = subtitleFileInfoItemMapper,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
        )
    }

    @Test
    fun `test getSubtitleFileInfoList return empty list`() = runTest {
        val accountDetail = AccountDetail(
            levelDetail = AccountLevelDetail(
                accountType = AccountType.BUSINESS,
                subscriptionStatus = null,
                subscriptionRenewTime = 0L,
                accountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
                proExpirationTime = 0L,
                accountPlanDetail = null,
                accountSubscriptionDetailList = listOf(),
            )
        )
        accountDetailFakeFlow.emit(accountDetail)
        underTest.getSubtitleFileInfoList()
        underTest.state.test {
            assertThat(awaitItem() is SubtitleLoadState.Empty).isTrue()
        }
    }

    @Test
    fun `test getSubtitleFileInfoList return correctly`() = runTest {
        val accountDetail = AccountDetail(
            levelDetail = AccountLevelDetail(
                accountType = AccountType.BUSINESS,
                subscriptionStatus = null,
                subscriptionRenewTime = 0L,
                accountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
                proExpirationTime = 0L,
                accountPlanDetail = null,
                accountSubscriptionDetailList = listOf(),
            )
        )
        accountDetailFakeFlow.emit(accountDetail)
        val expectedSubtitleFileInfoList: List<SubtitleFileInfo> = listOf(mock(), mock(), mock())

        getSRTSubtitleFileListUseCase.stub {
            onBlocking { invoke() }.thenReturn(expectedSubtitleFileInfoList)
        }

        whenever(subtitleFileInfoItemMapper(anyOrNull(), anyOrNull())).thenReturn(mock())

        underTest.state.test {
            underTest.getSubtitleFileInfoList()
            assertThat(awaitItem() is SubtitleLoadState.Empty).isTrue()
            val actual = awaitItem()
            assertThat(actual is SubtitleLoadState.Success).isTrue()
            assertThat((actual as SubtitleLoadState.Success).items.size).isEqualTo(3)
        }
    }

    @Test
    fun `test that state is updated correctly after itemClickedUpdate and clearSelectedItem are invoked`() =
        runTest {
            val accountDetail = AccountDetail(
                levelDetail = AccountLevelDetail(
                    accountType = AccountType.BUSINESS,
                    subscriptionStatus = null,
                    subscriptionRenewTime = 0L,
                    accountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
                    proExpirationTime = 0L,
                    accountPlanDetail = null,
                    accountSubscriptionDetailList = listOf(),
                )
            )
            accountDetailFakeFlow.emit(accountDetail)
            val testInfos = (1..3).map { id ->
                mock<SubtitleFileInfo> {
                    on { this.id }.thenReturn(id.toLong())
                }
            }
            val testInfoItems = testInfos.map { info ->
                mock<SubtitleFileInfoItem> {
                    on { selected }.thenReturn(false)
                    on { subtitleFileInfo }.thenReturn(info)
                }
            }
            val expectedSubtitleFileInfoList: List<SubtitleFileInfo> = testInfos

            whenever(getSRTSubtitleFileListUseCase()).thenReturn(expectedSubtitleFileInfoList)
            testInfos.mapIndexed { index, it ->
                whenever(subtitleFileInfoItemMapper(false, it)).thenReturn(testInfoItems[index])
            }
            val testSelectedItem = mock<SubtitleFileInfoItem> {
                on { selected }.thenReturn(true)
                on { subtitleFileInfo }.thenReturn(testInfos[0])
            }
            whenever(subtitleFileInfoItemMapper(true, testInfos[0])).thenReturn(testSelectedItem)
            underTest.getSubtitleFileInfoList()
            underTest.itemClickedUpdate(testInfos[0])
            advanceUntilIdle()
            underTest.state.test {
                val actual = awaitItem()
                assertThat(actual is SubtitleLoadState.Success).isTrue()
                assertThat((actual as SubtitleLoadState.Success).items.size).isEqualTo(3)
                assertThat(actual.items[0].selected).isTrue()
                underTest.clearSelectedItem()
                awaitItem().let {
                    assertThat(it is SubtitleLoadState.Success).isTrue()
                    assertThat((it as SubtitleLoadState.Success).items.size).isEqualTo(3)
                    assertThat(it.items[0].selected).isFalse()
                }
            }
        }
}