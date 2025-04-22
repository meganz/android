package mega.privacy.android.app.presentation.permissions.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.banner.InlineInfoBanner
import mega.android.core.ui.components.button.AnchoredButtonGroup
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.model.Button
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.TextColor

private val permissionMainImageSize = 200.dp
typealias ButtonAttributes = Pair<String, () -> Unit>

@Immutable
internal data class PermissionAttributes(
    val title: String,
    val description: String,
    val bannerText: SpannableText?,
    val image: Painter,
    val primaryButton: ButtonAttributes,
    val secondaryButton: ButtonAttributes,
)

@Composable
internal fun NewPermissionsScreen(
    attributes: PermissionAttributes,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val isScrollable by remember {
        derivedStateOf {
            scrollState.canScrollForward || scrollState.canScrollBackward
        }
    }
    val spacing = LocalSpacing.current

    MegaScaffold(
        modifier = modifier
            .safeDrawingPadding()
            .fillMaxSize(),
        bottomBar = {
            val (primaryButtonText, primaryButtonAction) = attributes.primaryButton
            val (secondaryButtonText, secondaryButtonAction) = attributes.secondaryButton

            AnchoredButtonGroup(
                modifier = Modifier.fillMaxWidth(),
                buttonGroup = listOf(
                    {
                        Button.PrimaryButton(
                            modifier = Modifier
                                .fillMaxWidth(),
                            text = primaryButtonText,
                            onClick = primaryButtonAction,
                        )
                    },
                    {
                        Button.TextOnlyButton(
                            modifier = Modifier
                                .fillMaxWidth(),
                            text = secondaryButtonText,
                            onClick = secondaryButtonAction
                        )
                    }
                ),
                withDivider = isScrollable
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(spacing.x16)
                .verticalScroll(scrollState)
        ) {
            Image(
                modifier = Modifier
                    .size(permissionMainImageSize)
                    .align(Alignment.CenterHorizontally),
                painter = attributes.image,
                contentDescription = attributes.title
            )

            MegaText(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = spacing.x24),
                text = attributes.title,
                style = AppTheme.typography.headlineSmall,
                textColor = TextColor.Primary,
            )

            MegaText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.x16),
                text = attributes.description,
                style = AppTheme.typography.bodyLarge,
                textColor = TextColor.Secondary,
            )

            attributes.bannerText?.let {
                InlineInfoBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.x24),
                    body = it,
                    showCancelButton = false,
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
internal fun PermissionScreenPreview() {
    AndroidThemeForPreviews {
        NewPermissionsScreen(
            attributes = PermissionAttributes(
                title = "Never miss an important update",
                description = "Stay informed with real-time updates that matter to you. Get alerts for shared folder activity, security updates, and exclusive offers so you never miss anything important.",
                image = painterResource(id = mega.privacy.android.icon.pack.R.drawable.ic_illustrator_thumbnail_outline),
                primaryButton = "Allow" to {},
                secondaryButton = "Not Now" to {},
                bannerText = SpannableText("This is a sample inline info banner"),
            ),
            modifier = Modifier.fillMaxSize()
        )
    }
}

@CombinedThemePreviews
@Composable
internal fun PermissionScreenNoBannerPreview() {
    AndroidThemeForPreviews {
        NewPermissionsScreen(
            attributes = PermissionAttributes(
                title = "Never miss an important update",
                description = "Stay informed with real-time updates that matter to you. Get alerts for shared folder activity, security updates, and exclusive offers so you never miss anything important.",
                image = painterResource(id = mega.privacy.android.icon.pack.R.drawable.ic_illustrator_medium_solid),
                primaryButton = "Allow" to {},
                secondaryButton = "Not Now" to {},
                bannerText = null,
            ),
            modifier = Modifier.fillMaxSize()
        )
    }
}