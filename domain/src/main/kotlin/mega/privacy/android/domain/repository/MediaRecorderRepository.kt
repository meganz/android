package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Media recorder repository
 */
interface MediaRecorderRepository {

    /**
     * Creates a flow that starts recording when collecting its values begins and stops recording when cancelled. Meanwhile, it will emit the sampled current maximum amplitude.
     * @param destination where the recording will be stored
     * @return a flow with sampled current maximum amplitude.
     */
    fun recordAudio(destination: File): Flow<Int>
}