package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.data.gateway.MediaRecorderGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.MediaRecorderRepository
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

internal class MediaRecorderRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val mediaRecorderGateway: MediaRecorderGateway,
) : MediaRecorderRepository {
    override fun recordAudio(destination: File): Flow<Int> =
        flow {
            while (true) {
                emit(mediaRecorderGateway.getCurrentMaxAmplitude())
                delay(SAMPLE_RATE)
            }
        }.onStart {
            mediaRecorderGateway.startRecording(destination)
        }.onCompletion {
            mediaRecorderGateway.stopRecording()
        }.flowOn(ioDispatcher)

    companion object {
        private val SAMPLE_RATE = 40.milliseconds
    }
}
