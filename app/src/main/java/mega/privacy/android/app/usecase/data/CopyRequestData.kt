package mega.privacy.android.app.usecase.data

/**
 * Copy Request Data holder
 * @param count the total count of copy request
 * @param errorCount the total count of error copy request
 */
data class CopyRequestData(
    val count: Int,
    val errorCount: Int
)