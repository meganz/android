package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.data.mapper.FileTypeInfoMapper
import mega.privacy.android.app.domain.usecase.DefaultGetRecentActionNodes
import mega.privacy.android.app.domain.usecase.DoNotFailOnSingleGetRecentActionNodes
import mega.privacy.android.app.domain.usecase.FailFastGetRecentActionNodes
import mega.privacy.android.app.domain.usecase.GetRecentActionNodes
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.usecase.GetThumbnail
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.IOException

class GetRecentActionNodesTest {

    private lateinit var defaultUnderTest: GetRecentActionNodes
    private lateinit var doNotFailOnIndividualFailuresUnderTest: GetRecentActionNodes
    private lateinit var failFastUnderTest: GetRecentActionNodes

    private val getThumbnail = mock<GetThumbnail> {
        onBlocking { invoke(any()) }.thenReturn(null)
    }

    private val mockNodes = (0L..5L).map { id ->
        mock<MegaNode> { on { handle }.thenReturn(id) }
    }


    private val nodesList = mock<MegaNodeList> {
        on { size() }.thenReturn(mockNodes.size)
        on { get(any()) }.thenAnswer { mockNodes[it.arguments[0] as Int] }
    }

    private val fileTypeInfoMapper = mock<FileTypeInfoMapper>{ on { invoke(any()) }.thenReturn(VideoFileTypeInfo("",""))}

    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
//        defaultUnderTest = DefaultGetRecentActionNodes(
//            getThumbnail = getThumbnail,
//            ioDispatcher = StandardTestDispatcher()
//        )
        doNotFailOnIndividualFailuresUnderTest = DoNotFailOnSingleGetRecentActionNodes(
            getThumbnail = getThumbnail,
            ioDispatcher = testDispatcher,
            fileTypeInfoMapper = fileTypeInfoMapper
        )
        failFastUnderTest = FailFastGetRecentActionNodes(
            getThumbnail = getThumbnail,
            ioDispatcher = testDispatcher,
            fileTypeInfoMapper = fileTypeInfoMapper
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that a single failing map still carries on`() = runTest {
        whenever(getThumbnail(3L)).thenAnswer {
            throw IOException("Error!")
        }

        val actualNotIndividual = doNotFailOnIndividualFailuresUnderTest(nodesList)

        assertThat(actualNotIndividual.size).isEqualTo(mockNodes.size - 1)
    }

    @Test(expected = IOException::class)
    fun `test that a single failing map fails the whole use case`() = runTest{
        whenever(getThumbnail(3L)).thenAnswer {
            throw IOException("Error!")
        }

        val result = failFastUnderTest(nodesList)
    }

}