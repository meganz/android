package mega.privacy.android.app.presentation.permissions.view

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.banner.InlineInfoBanner
import mega.android.core.ui.components.button.AnchoredButtonGroup
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.TextOnlyButton
import mega.android.core.ui.components.divider.StrongDivider
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.model.Button
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.preview.CombinedThemePreviewsTablet
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.devicetype.DeviceType
import mega.android.core.ui.theme.devicetype.LocalDeviceType
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.TextColor

internal const val NEW_PERMISSION_SCREEN_PORTRAIT_TAG = "new_permission_screen_portrait"
internal const val NEW_PERMISSION_SCREEN_LANDSCAPE_TAG = "new_permission_screen_landscape"
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
    val isTablet = LocalDeviceType.current == DeviceType.Tablet
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    if (isTablet || isPortrait) {
        PortraitPermissionScreen(
            attributes = attributes,
            modifier = modifier
                .testTag(NEW_PERMISSION_SCREEN_PORTRAIT_TAG)
        )
    } else {
        LandscapePermissionScreen(
            attributes = attributes,
            modifier = modifier
                .testTag(NEW_PERMISSION_SCREEN_LANDSCAPE_TAG)
        )
    }
}

@Composable
private fun PortraitPermissionScreen(
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
    val (primaryButtonText, primaryButtonAction) = attributes.primaryButton
    val (secondaryButtonText, secondaryButtonAction) = attributes.secondaryButton

    MegaScaffold(
        modifier = modifier
            .safeDrawingPadding()
            .fillMaxSize(),
        bottomBar = {
            Box(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
            ) {
                AnchoredButtonGroup(
                    modifier = Modifier
                        .widthIn(max = 500.dp)
                        .align(Alignment.Center),
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
            }
        }
    ) {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxHeight()
                    .padding(spacing.x16)
                    .align(Alignment.Center)
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
}

@Composable
private fun LandscapePermissionScreen(
    attributes: PermissionAttributes,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val (primaryButtonText, primaryButtonAction) = attributes.primaryButton
    val (secondaryButtonText, secondaryButtonAction) = attributes.secondaryButton

    MegaScaffold(
        modifier = modifier
            .safeDrawingPadding()
            .fillMaxSize(),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                StrongDivider(modifier = Modifier.fillMaxWidth())

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(89.dp)
                        .padding(spacing.x16)
                ) {
                    TextOnlyButton(
                        modifier = Modifier.weight(1f),
                        text = secondaryButtonText,
                        onClick = secondaryButtonAction,
                    )

                    PrimaryFilledButton(
                        modifier = Modifier.weight(1f),
                        text = primaryButtonText,
                        onClick = primaryButtonAction,
                    )
                }
            }
        },
    ) { padding ->
        Row(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(
                    start = spacing.x16,
                    end = spacing.x16,
                    top = spacing.x16,
                )
        ) {
            Image(
                modifier = Modifier
                    .weight(1f)
                    .size(200.dp)
                    .align(Alignment.CenterVertically),
                painter = attributes.image,
                contentDescription = attributes.title
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
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

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(spacing.x20)
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PortraitPermissionScreenPreview() {
    AndroidThemeForPreviews {
        PortraitPermissionScreen(
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

@CombinedThemePreviewsTablet
@Composable
private fun LandscapePermissionScreenPreview() {
    AndroidThemeForPreviews {
        LandscapePermissionScreen(
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