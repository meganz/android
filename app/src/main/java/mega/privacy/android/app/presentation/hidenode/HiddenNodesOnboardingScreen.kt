package mega.privacy.android.app.presentation.hidenode

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.LinkSpannedText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.LinkColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.SettingsActivity
import mega.privacy.android.app.presentation.settings.SettingsFragment.Companion.NAVIGATE_TO_SHOW_HIDDEN_ITEMS_PREFERENCE
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as RPack
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.images.MegaIcon
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.HideNodeOnboardingScreenEvent
import mega.privacy.mobile.analytics.event.HideNodeUpgradeScreenEvent

@Composable
internal fun HiddenNodesOnboardingScreen(
    viewModel: HiddenNodesOnboardingViewModel,
    isOnboarding: Boolean,
    onClickBack: () -> Unit,
    onClickContinue: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        Analytics.tracker.trackEvent(
            if (isOnboarding) HideNodeOnboardingScreenEvent else HideNodeUpgradeScreenEvent
        )
    }
    MegaScaffold(
        modifier = Modifier
            .systemBarsPadding()
            .fillMaxSize(),
        topBar = {
            HiddenNodesOnboardingAppBar(
                onClickBack = onClickBack,
            )
        },
        bottomBar = {
            if (state.isInitialized) {
                HiddenNodesOnboardingBottomBar(
                    accountType = state.accountType,
                    isBusinessAccountExpired = state.isBusinessAccountExpired,
                    isOnboarding = isOnboarding,
                    onClickBack = onClickBack,
                    onClickContinue = onClickContinue,
                )
            }
        },
        content = { paddingValues ->
            HiddenNodesOnboardingContent(
                disableNavigateToSettings = state.accountType?.isPaid == false || state.isBusinessAccountExpired,
                modifier = Modifier.padding(paddingValues),
            )
        },
    )
}

@Composable
private fun HiddenNodesOnboardingAppBar(
    modifier: Modifier = Modifier,
    onClickBack: () -> Unit,
) {
    MegaTopAppBar(
        modifier = modifier,
        title = "",
        navigationType = AppBarNavigationType.Close(
            onNavigationIconClicked = onClickBack,
        )
    )
}

@Composable
private fun HiddenNodesOnboardingBottomBar(
    modifier: Modifier = Modifier,
    accountType: AccountType?,
    isBusinessAccountExpired: Boolean,
    isOnboarding: Boolean,
    onClickBack: () -> Unit,
    onClickContinue: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (strRes, action) = if (isOnboarding) {
            R.string.button_continue to onClickContinue
        } else if (accountType?.isPaid == false || isBusinessAccountExpired) {
            R.string.plans_depleted_transfer_overquota to onClickContinue
        } else {
            R.string.general_close to onClickBack
        }

        RaisedDefaultMegaButton(
            text = stringResource(strRes),
            onClick = action,
        )
    }
}

@Composable
private fun HiddenNodesOnboardingContent(
    disableNavigateToSettings: Boolean,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = modifier.verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            Spacer(modifier = Modifier.height(32.dp))

            Image(
                painter = painterResource(id = R.drawable.hidden_node),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            MegaText(
                text = stringResource(id = sharedR.string.hidden_nodes_new_feature),
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(32.dp))

            HiddenNodesBenefit(
                icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.EyeOff),
                title = stringResource(id = sharedR.string.hidden_nodes_title_hide_files_folders_feature),
                description = stringResource(id = sharedR.string.hidden_nodes_description_hide_files_folders_feature),
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (disableNavigateToSettings) {
                HiddenNodesBenefit(
                    icon = painterResource(id = RPack.drawable.ic_images),
                    title = stringResource(id = sharedR.string.hidden_nodes_title_control_visibility),
                    description = stringResource(id = sharedR.string.hidden_nodes_description_control_visibility),
                )
            } else {
                HiddenNodesBenefit(
                    icon = painterResource(id = RPack.drawable.ic_images),
                    title = stringResource(id = sharedR.string.hidden_nodes_title_control_visibility),
                    description = stringResource(id = sharedR.string.hidden_nodes_description_control_visibility_tag),
                    onActionClicked = {
                        context.startActivity(
                            Intent(
                                context,
                                SettingsActivity::class.java
                            ).putExtra(NAVIGATE_TO_SHOW_HIDDEN_ITEMS_PREFERENCE, true)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            HiddenNodesBenefit(
                icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Eye),
                title = stringResource(id = sharedR.string.hidden_nodes_title_visible_shared_files),
                description = stringResource(id = sharedR.string.hidden_nodes_description_visible_shared_files),
            )
        },
    )
}

@Composable
private fun HiddenNodesBenefit(
    modifier: Modifier = Modifier,
    icon: Painter,
    title: String,
    description: String,
    onActionClicked: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            MegaIcon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                content = {
                    MegaText(
                        text = title,
                        textColor = TextColor.Primary,
                        style = MaterialTheme.typography.labelLarge,
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    if (onActionClicked == null) {
                        MegaText(
                            text = description,
                            textColor = TextColor.Secondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        LinkSpannedText(
                            value = description,
                            spanStyles = hashMapOf(
                                SpanIndicator('A') to SpanStyleWithAnnotation(
                                    MegaSpanStyle.LinkColorStyle(
                                        SpanStyle(),
                                        LinkColor.Primary
                                    ),
                                    description
                                        .substringAfter("[A]")
                                        .substringBefore("[/A]")
                                )
                            ),
                            onAnnotationClick = {
                                onActionClicked()
                            },
                            baseStyle = AppTheme.typography.bodyMedium,
                            baseTextColor = TextColor.Secondary
                        )
                    }
                }
            )
        }
    )
}
