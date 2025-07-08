package mega.privacy.android.app.presentation.hidenode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as RPack
import mega.privacy.android.shared.original.core.ui.theme.accent_050
import mega.privacy.android.shared.original.core.ui.theme.accent_900
import mega.privacy.android.shared.original.core.ui.theme.black
import mega.privacy.android.shared.original.core.ui.theme.dark_grey
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle2medium
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_054
import mega.privacy.android.shared.original.core.ui.theme.teal_200_alpha_038
import mega.privacy.android.shared.original.core.ui.theme.teal_300_alpha_038
import mega.privacy.android.shared.original.core.ui.theme.white
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_054
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
    Scaffold(
        modifier = Modifier.systemBarsPadding().fillMaxSize(),
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
    val isLight = MaterialTheme.colors.isLight

    TopAppBar(
        title = {},
        modifier = modifier,
        navigationIcon = {
            IconButton(
                onClick = onClickBack,
                content = {
                    Icon(
                        painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.X),
                        contentDescription = null,
                        tint = black.takeIf { isLight } ?: white,
                    )
                },
            )
        },
        elevation = 0.dp,
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
    val isLight = MaterialTheme.colors.isLight

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(white.takeIf { isLight } ?: dark_grey)
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

        Button(
            onClick = action,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = accent_900.takeIf { isLight } ?: accent_050,
                disabledBackgroundColor = teal_300_alpha_038.takeIf { isLight }
                    ?: teal_200_alpha_038,
            ),
        ) {
            Text(
                text = stringResource(strRes),
                color = white.takeIf { isLight } ?: dark_grey,
                fontWeight = FontWeight.W500,
                style = MaterialTheme.typography.button,
            )
        }
    }
}

@Composable
private fun HiddenNodesOnboardingContent(
    modifier: Modifier = Modifier,
) {
    val isLight = MaterialTheme.colors.isLight
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            Image(
                painter = painterResource(id = R.drawable.hidden_node),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(id = sharedR.string.hidden_nodes_new_feature),
                color = dark_grey.takeIf { isLight } ?: white,
                fontSize = 16.sp,
                fontWeight = FontWeight.W500,
                style = MaterialTheme.typography.subtitle1,
            )

            Spacer(modifier = Modifier.height(32.dp))

            HiddenNodesBenefit(
                icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.EyeOff),
                title = stringResource(id = sharedR.string.hidden_nodes_title_hide_files_folders_feature),
                description = stringResource(id = sharedR.string.hidden_nodes_description_hide_files_folders_feature),
            )

            Spacer(modifier = Modifier.height(24.dp))

            HiddenNodesBenefit(
                icon = painterResource(id = RPack.drawable.ic_images),
                title = stringResource(id = sharedR.string.hidden_nodes_title_control_visibility),
                description = stringResource(id = sharedR.string.hidden_nodes_description_control_visibility),
            )

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
) {
    val isLight = MaterialTheme.colors.isLight

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isLight) accent_900 else accent_050,
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                content = {
                    Text(
                        text = title,
                        color = dark_grey.takeIf { isLight } ?: white,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        style = MaterialTheme.typography.subtitle2medium,
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = description,
                        color = grey_alpha_054.takeIf { isLight } ?: white_alpha_054,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W400,
                        lineHeight = 18.sp,
                    )
                },
            )
        },
    )
}
