package mega.privacy.android.app.nav.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.MegaApplication
import timber.log.Timber

/**
 * ActivityResultContract for opening multiple documents with persistable URI permissions.
 * This contract allows users to select multiple files and grants the application
 * persistable URI permissions for those files.
 */
class OpenMultipleDocumentsPersistable :
    ActivityResultContract<Array<String>, List<@JvmSuppressWildcards Uri>>() {

    override fun createIntent(context: Context, input: Array<String>): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT)
            .putExtra(Intent.EXTRA_MIME_TYPES, input)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            .setType("*/*")
    }

    override fun getSynchronousResult(
        context: Context,
        input: Array<String>,
    ): SynchronousResult<List<Uri>>? = null

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        return intent.takeIf { resultCode == Activity.RESULT_OK }?.getClipDataUris().orEmpty()
            .map {
                runCatching {
                    MegaApplication.getInstance().contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }.onFailure { e ->
                    Timber.e(e, "Failed to take persistable URI permission for $it")
                }
                it
            }
    }

    // a copy of the method from [androidx.activity.result.contract.GetMultipleContents]
    internal companion object {
        internal fun Intent.getClipDataUris(): List<Uri> {
            // Use a LinkedHashSet to maintain any ordering that may be
            // present in the ClipData
            val resultSet = LinkedHashSet<Uri>()
            data?.let { data -> resultSet.add(data) }
            val clipData = clipData
            if (clipData == null && resultSet.isEmpty()) {
                return emptyList()
            } else if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    if (uri != null) {
                        resultSet.add(uri)
                    }
                }
            }
            return ArrayList(resultSet)
        }
    }
}