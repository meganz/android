package test.mega.privacy.android.app.namecollision

import com.jraska.livedata.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.namecollision.NameCollisionViewModel
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.file.GetFileVersionsOption
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import test.mega.privacy.android.app.InstantExecutorExtension
import test.mega.privacy.android.app.TestSchedulerExtension
import test.mega.privacy.android.app.extensions.withCoroutineExceptions

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(value = [InstantExecutorExtension::class, TestSchedulerExtension::class])
internal class NameCollisionViewModelTest {
    private lateinit var underTest: NameCollisionViewModel


    @BeforeEach
    internal fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    private val getFileVersionsOption = mock<GetFileVersionsOption>()
    private val monitorUserUpdates = mock<MonitorUserUpdates>()

    private fun initUnderTest() {
        underTest = NameCollisionViewModel(
            getFileVersionsOption = getFileVersionsOption,
            getNameCollisionResultUseCase = mock(),
            uploadUseCase = mock(),
            legacyMoveNodeUseCase = mock(),
            legacyCopyNodeUseCase = mock(),
            monitorUserUpdates = monitorUserUpdates,
            getNodeUseCase = mock(),
            setCopyLatestTargetPathUseCase = mock(),
            setMoveLatestTargetPathUseCase = mock(),
            copyRequestMessageMapper = mock(),
            moveRequestMessageMapper = mock(),
            getNodeByFingerprintAndParentNodeUseCase = mock()
        )
    }

    @AfterEach
    internal fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    internal fun `test that an exception in getFileVersionsOption is caught`() =
        withCoroutineExceptions {
            runTest {
                monitorUserUpdates.stub {
                    on { invoke() }.thenReturn(flowOf(UserChanges.DisableVersions))
                }

                getFileVersionsOption.stub {
                    onBlocking { invoke(true) }.thenAnswer { throw Exception("This test failed") }
                }

                initUnderTest()
                underTest.getFileVersioningInfo().test().assertNoValue()
            }
        }
}