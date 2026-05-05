package mega.privacy.android.feature.clouddrive.presentation.publiclink.model

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.ExtraConstant.TYPE_TEXT_PLAIN
import mega.privacy.android.shared.resources.R as sharedR
import java.util.UUID

/**
 * App bar menu share link action used by both folder link and file link screens.
 */
internal data object PublicLinkShareAction : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() =
        rememberVectorPainter(IconPack.Medium.Thin.Outline.ShareNetwork)

    override val testTag = "public_link_action:share"

    @Composable
    override fun getDescription() = stringResource(sharedR.string.general_share)
}

/**
 * Start an intent to share a public link as plain text.
 *
 * @param link  link of the node to share.
 * @param title title of the share intent.
 */
internal fun Context.startPublicLinkShareIntent(link: String?, title: String?) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = TYPE_TEXT_PLAIN
        putExtra(Intent.EXTRA_TEXT, link)
        putExtra(Intent.EXTRA_SUBJECT, title ?: "${UUID.randomUUID()}.url")
    }
    startActivity(
        Intent.createChooser(shareIntent, getString(sharedR.string.general_share))
    )
}
