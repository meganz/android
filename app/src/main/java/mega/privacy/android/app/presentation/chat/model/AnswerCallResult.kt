package mega.privacy.android.app.presentation.chat.model

/**
 * Call Result.
 *
 * @property chatHandle       Chat ID
 * @property enableVideo      Video ON
 * @property enableAudio      Audio ON
 */
data class AnswerCallResult(
    val chatHandle: Long,
    val enableVideo: Boolean,
    val enableAudio: Boolean,
)