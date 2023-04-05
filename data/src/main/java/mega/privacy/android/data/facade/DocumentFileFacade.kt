package mega.privacy.android.data.facade

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.data.wrapper.DocumentFileWrapper
import javax.inject.Inject

/**
 * Default implementation of [DocumentFileWrapper]
 *
 * @property context [ApplicationContext]
 */
class DocumentFileFacade @Inject constructor(
    @ApplicationContext private val context: Context,
) : DocumentFileWrapper {

    override fun fromTreeUri(uri: Uri): DocumentFile? =
        DocumentFile.fromTreeUri(context, uri)
}