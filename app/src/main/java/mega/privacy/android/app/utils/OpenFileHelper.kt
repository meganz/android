package mega.privacy.android.app.utils

import android.content.Context
import android.content.Intent
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.presentation.favourites.facade.OpenFileWrapper
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.DefaultImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * The helper class for open file
 */
class OpenFileHelper @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
) : OpenFileWrapper {

    override fun getIntentForOpenFile(
        context: Context,
        node: MegaNode,
        isText: Boolean,
        snackbarShower: SnackbarShower,
    ): Intent? {
        var intent: Intent? = null
        val localPath = FileUtil.getLocalFile(node)
        MimeTypeList.typeForName(node.name).apply {
            when {
                isImage -> {
                    intent = getIntentForOpenImage(context, node)
                }

                isPdf -> {
                    intent = getIntentForOpenPdf(
                        context = context,
                        node = node,
                        localPath = localPath,
                        isText = isText,
                        snackbarShower = snackbarShower
                    )
                }
            }
        }
        return intent
    }

    /**
     * Get intent to open image file
     * @param context Context
     * @param node MegaNode
     */
    private fun getIntentForOpenImage(context: Context, node: MegaNode): Intent {
        return ImagePreviewActivity.createIntent(
            context = context,
            imageSource = ImagePreviewFetcherSource.DEFAULT,
            menuOptionsSource = ImagePreviewMenuSource.DEFAULT,
            anchorImageNodeId = NodeId(node.handle),
            params = mapOf(DefaultImageNodeFetcher.NODE_IDS to longArrayOf(node.handle)),
        )
    }

    /**
     * Get intent to open pdf file
     * @param context Context
     * @param node mega node
     * @param localPath local path of current node
     * @param isText isText
     * @param snackbarShower SnackbarShower
     */
    private fun getIntentForOpenPdf(
        context: Context,
        node: MegaNode,
        localPath: String?,
        isText: Boolean,
        snackbarShower: SnackbarShower,
    ): Intent? {
        val intent = Intent(context, PdfViewerActivity::class.java).apply {
            putExtra(Constants.INTENT_EXTRA_KEY_INSIDE, true)
        }
        val paramsSetSuccessfully = getParamsSetSuccessfully(
            context = context,
            intent = intent,
            node = node,
            localPath = localPath,
            isText = isText,
            snackbarShower = snackbarShower
        )
        if (paramsSetSuccessfully && MegaApiUtils.isIntentAvailable(context, intent)) {
            intent.putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, node.handle)
            return intent
        }
        return null
    }

    /**
     * Get if the parameters is set successful
     * @param context Context
     * @param node MegaNode
     * @param intent intent
     * @param localPath local path of current node
     * @param isText isText
     * @param snackbarShower SnackbarShower
     * @return true is success
     */
    private fun getParamsSetSuccessfully(
        context: Context,
        node: MegaNode,
        intent: Intent,
        localPath: String?,
        isText: Boolean,
        snackbarShower: SnackbarShower,
    ): Boolean {
        return if (FileUtil.isLocalFile(node, megaApi, localPath)) {
            FileUtil.setLocalIntentParams(context, node, intent, localPath, isText, snackbarShower)
        } else {
            FileUtil.setStreamingIntentParams(context, node, megaApi, intent, snackbarShower)
        }
    }
}