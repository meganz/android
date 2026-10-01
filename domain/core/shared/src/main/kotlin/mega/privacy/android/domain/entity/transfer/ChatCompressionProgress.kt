package mega.privacy.android.domain.entity.transfer

import mega.privacy.android.domain.entity.Progress

/**
 * Chat compression state
 */
sealed interface ChatCompressionState

/**
 * Chat compression progress
 * @param alreadyCompressed the amount of files already fully compressed
 * @param totalToCompress the total amount of files that would need to be compressed, including the already compressed
 * @param progress the overall progress of the compression
 */
data class ChatCompressionProgress(
    val alreadyCompressed: Int,
    val totalToCompress: Int,
    val progress: Progress,
) : ChatCompressionState

/**
 * Chat compression has finished
 */
data object ChatCompressionFinished : ChatCompressionState