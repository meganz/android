package mega.privacy.android.app.presentation.hidenode

import mega.privacy.android.icon.pack.R as RPack
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.theme.black
import mega.privacy.android.shared.original.core.ui.theme.dark_grey
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle2medium
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_054
import mega.privacy.android.shared.original.core.ui.theme.teal_200
import mega.privacy.android.shared.original.core.ui.theme.teal_200_alpha_038
import mega.privacy.android.shared.original.core.ui.theme.teal_300
import mega.privacy.android.shared.original.core.ui.theme.teal_300_alpha_038
import mega.privacy.android.shared.original.core.ui.theme.white
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_054
import mega.privacy.mobile.analytics.event.HideNodeOnboardingScreenEvent
import mega.privacy.mobile.analytics.event.HideNodeUpgradeScreenEvent

@Composable
internal fun HiddenNodesOnboardingScreen(
    isOnboarding: Boolean,
    onClickBack: () -> Unit,
    onClickContinue: () -> Unit,
) {
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
            HiddenNodesOnboardingBottomBar(
                isOnboarding = isOnboarding,
                onClickBack = onClickBack,
                onClickContinue = onClickContinue,
            )
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
                        painter = painterResource(id = R.drawable.ic_close),
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
        Text(
            text = stringResource(id = R.string.button_not_now_rich_links),
            modifier = Modifier.clickable { onClickBack() },
            color = teal_300.takeIf { isLight } ?: teal_200,
            fontWeight = FontWeight.W500,
            style = MaterialTheme.typography.button,
        )

        Spacer(modifier = Modifier.size(28.dp))

        Button(
            onClick = onClickContinue,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = teal_300.takeIf { isLight } ?: teal_200,
                disabledBackgroundColor = teal_300_alpha_038.takeIf { isLight }
                    ?: teal_200_alpha_038,
            ),
        ) {
            Text(
                text = stringResource(
                    id = R.string.button_continue.takeIf { isOnboarding }
                        ?: R.string.plans_depleted_transfer_overquota),
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
                text = stringResource(id = R.string.hidden_nodes),
                color = dark_grey.takeIf { isLight } ?: white,
                fontSize = 16.sp,
                fontWeight = FontWeight.W500,
                style = MaterialTheme.typography.subtitle1,
            )

            Spacer(modifier = Modifier.height(32.dp))

            HiddenNodesBenefit(
                icon = painterResource(id = RPack.drawable.ic_eye_off_medium_regular_outline),
                title = stringResource(id = R.string.hidden_nodes_benefit_title_hide_files_folders),
                description = stringResource(id = R.string.hidden_nodes_benefit_description_hide_files_folders),
            )

            Spacer(modifier = Modifier.height(24.dp))

            HiddenNodesBenefit(
                icon = painterResource(id = RPack.drawable.ic_images),
                title = stringResource(id = R.string.hidden_nodes_benefit_title_exclude_timeline),
                description = stringResource(id = R.string.hidden_nodes_benefit_description_exclude_timeline),
            )

            Spacer(modifier = Modifier.height(24.dp))

            HiddenNodesBenefit(
                icon = painterResource(id = RPack.drawable.ic_eye_medium_regular_outline),
                title = stringResource(id = R.string.hidden_nodes_benefit_title_out_of_sight),
                description = stringResource(id = R.string.hidden_nodes_benefit_description_out_of_sight),
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
                tint = if (isLight) teal_300 else teal_200,
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
