package mega.privacy.android.app.presentation.chat.model

/**
 * Call Result.
 *
 * @property chatHandle       Chat ID
 * @property actionString     String action
 */
data class AnswerCallResult(
    val chatHandle: Long,
    val actionString: String,
)