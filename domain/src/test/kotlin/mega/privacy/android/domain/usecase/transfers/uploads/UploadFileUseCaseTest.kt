package mega.privacy.android.domain.usecase.transfers.uploads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferAppData.TransferGroup
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.file.GetGPSCoordinatesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.anyValueClass
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadFileUseCaseTest {

    private lateinit var underTest: UploadFileUseCase

    private val transferRepository = mock<TransferRepository>()
    private val cacheRepository = mock<CacheRepository>()
    private val getGPSCoordinatesUseCase = mock<GetGPSCoordinatesUseCase>()

    @BeforeAll
    fun setup() {
        underTest =
            UploadFileUseCase(
                transferRepository,
                cacheRepository,
                getGPSCoordinatesUseCase,
            )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            transferRepository,
            cacheRepository,
            getGPSCoordinatesUseCase,
        )
    }

    @Nested
    inner class OrdinaryUploads {

        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `test that upload is started with the correct parameters`(
            isSourceTemporary: Boolean,
        ) = runTest {
            whenever(cacheRepository.isFileInCacheDirectory(File(uriPath.value))) doReturn isSourceTemporary
            whenever(getGPSCoordinatesUseCase(uriPath)) doReturn null
            stubStartUpload()

            underTest(
                uriPath = uriPath,
                fileName = fileName,
                appData = ordinaryAppData,
                parentFolderId = parentFolderId,
                isHighPriority = isHighPriority
            ).test {
                awaitComplete()
            }

            verify(transferRepository).startUpload(
                localPath = uriPath.value,
                parentNodeId = parentFolderId,
                fileName = fileName,
                modificationTime = null,
                appData = ordinaryAppData,
                isSourceTemporary = isSourceTemporary,
                shouldStartFirst = isHighPriority,
            )
        }

        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `test that transfer events are emitted in the returned flow`(
            isSourceTemporary: Boolean,
        ) = runTest {
            whenever(cacheRepository.isFileInCacheDirectory(File(uriPath.value))) doReturn isSourceTemporary
            whenever(getGPSCoordinatesUseCase(uriPath)) doReturn null
            val events = listOf(
                mock<TransferEvent.TransferStartEvent>(),
                mock<TransferEvent.TransferUpdateEvent>(),
                mock<TransferEvent.TransferFinishEvent>(),
            )
            val transferEventsFlow = flowOf(*events.toTypedArray())
            stubStartUpload(transferEventsFlow)

            underTest(
                uriPath = uriPath,
                fileName = fileName,
                appData = ordinaryAppData,
                parentFolderId = parentFolderId,
                isHighPriority = isHighPriority
            ).test {
                events.forEach {
                    assertThat(awaitItem()).isEqualTo(it)
                }
                awaitComplete()
            }
        }

        @Test
        fun `test that gps coordinates are added to app data when it is a temporary upload`() =
            runTest {
                whenever(cacheRepository.isFileInCacheDirectory(File(uriPath.value))) doReturn true
                whenever(getGPSCoordinatesUseCase(uriPath)) doReturn Pair(latitude, longitude)
                stubStartUpload()
                val expectedAppData = ordinaryAppData + TransferAppData.Geolocation(latitude, longitude)

                underTest(
                    uriPath = uriPath,
                    fileName = fileName,
                    appData = ordinaryAppData,
                    parentFolderId = parentFolderId,
                    isHighPriority = isHighPriority
                ).test {
                    awaitComplete()
                }

                verify(transferRepository).startUpload(
                    localPath = uriPath.value,
                    parentNodeId = parentFolderId,
                    fileName = fileName,
                    modificationTime = null,
                    appData = expectedAppData ,
                    isSourceTemporary = true,
                    shouldStartFirst = isHighPriority,
                )
            }

        private fun stubStartUpload(transferEventsFlow: Flow<TransferEvent> = emptyFlow()) {
            whenever(
                transferRepository.startUpload(
                    any(),
                    anyValueClass(),
                    any(),
                    anyOrNull(),
                    any(),
                    any(),
                    any(),
                )
            ) doReturn transferEventsFlow
        }
    }

    @Nested
    inner class ChatUploads {

        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `test that upload is started with the correct parameters`(
            isSourceTemporary: Boolean,
        ) = runTest {
            whenever(cacheRepository.isFileInCacheDirectory(File(uriPath.value))) doReturn isSourceTemporary
            whenever(getGPSCoordinatesUseCase(uriPath)) doReturn null
            stubStartUpload()

            underTest(
                uriPath = uriPath,
                fileName = fileName,
                appData = chatAppData,
                parentFolderId = parentFolderId,
                isHighPriority = isHighPriority
            ).test {
                awaitComplete()
            }

            verify(transferRepository).startUploadForChat(
                localPath = uriPath.value,
                parentNodeId = parentFolderId,
                fileName = fileName,
                appData = chatAppData,
                isSourceTemporary = isSourceTemporary,
            )
        }

        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `test that transfer events are emitted in the returned flow`(
            isSourceTemporary: Boolean,
        ) = runTest {
            whenever(cacheRepository.isFileInCacheDirectory(File(uriPath.value))) doReturn isSourceTemporary
            whenever(getGPSCoordinatesUseCase(uriPath)) doReturn null
            val events = listOf(
                mock<TransferEvent.TransferStartEvent>(),
                mock<TransferEvent.TransferUpdateEvent>(),
                mock<TransferEvent.TransferFinishEvent>(),
            )
            val transferEventsFlow = flowOf(*events.toTypedArray())
            stubStartUpload(transferEventsFlow)

            underTest(
                uriPath = uriPath,
                fileName = fileName,
                appData = chatAppData,
                parentFolderId = parentFolderId,
                isHighPriority = isHighPriority
            ).test {
                events.forEach {
                    assertThat(awaitItem()).isEqualTo(it)
                }
                awaitComplete()
            }
        }

        @Test
        fun `test that gps coordinates are added to app data when it is a temporary upload`() =
            runTest {
                whenever(cacheRepository.isFileInCacheDirectory(File(uriPath.value))) doReturn true
                whenever(getGPSCoordinatesUseCase(uriPath)) doReturn Pair(latitude, longitude)
                stubStartUpload()
                val expectedAppData = chatAppData + TransferAppData.Geolocation(latitude, longitude)

                underTest(
                    uriPath = uriPath,
                    fileName = fileName,
                    appData = chatAppData,
                    parentFolderId = parentFolderId,
                    isHighPriority = isHighPriority
                ).test {
                    awaitComplete()
                }

                verify(transferRepository).startUploadForChat(
                    localPath = uriPath.value,
                    parentNodeId = parentFolderId,
                    fileName = fileName,
                    appData = expectedAppData ,
                    isSourceTemporary = true,
                )
            }

        private fun stubStartUpload(transferEventsFlow: Flow<TransferEvent> = emptyFlow()) {
            whenever(
                transferRepository.startUploadForChat(
                    any(),
                    anyValueClass(),
                    any(),
                    anyOrNull(),
                    any(),
                )
            ) doReturn transferEventsFlow
        }
    }

    private companion object {
        val uriPath = UriPath("foo")
        const val fileName = "newFileName.txt"
        val ordinaryAppData = listOf(mock<TransferGroup>())
        val chatAppData = listOf(mock<TransferAppData.ChatUpload>())
        val parentFolderId = NodeId(353L)
        const val isHighPriority = false
        const val latitude = 44.565
        const val longitude = 2.5454
    }
}