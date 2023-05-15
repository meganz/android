package mega.privacy.android.app.main.megaachievements.composables

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.theme.extensions.dark_blue_500_dark_blue_200
import mega.privacy.android.core.ui.theme.extensions.red_600_red_400
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

@Composable
internal fun AchievementListItem(
    @DrawableRes iconId: Int,
    @StringRes titleId: Int,
    alphaLevel: Float = 1.0f,
    zeroFiguresTitle: String? = null,
    hasFiguresTitle: String? = null,
    @StringRes buttonTitleId: Int? = null,
    onButtonClick: () -> Unit = {},
    daysLeft: Long? = null,
) {
    Row(
        modifier = Modifier
            .padding(start = 20.dp, top = 16.dp, bottom = 16.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            modifier = Modifier
                .requiredSize(32.dp)
                .alpha(alphaLevel),
            painter = painterResource(id = iconId),
            contentScale = ContentScale.Fit,
            contentDescription = "Icon",
        )

        Column(
            modifier = Modifier
                .padding(start = 20.dp, end = 16.dp)
                .fillMaxSize()
                .weight(1f),
        ) {
            Text(
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .wrapContentSize(),
                text = stringResource(id = titleId),
                style = MaterialTheme.typography.subtitle1,
            )

            zeroFiguresTitle?.let {
                Text(
                    modifier = Modifier
                        .wrapContentSize(),
                    text = it,
                    style = MaterialTheme.typography.subtitle2.copy(
                        color = MaterialTheme.colors.textColorSecondary
                    ),
                )
            }

            hasFiguresTitle?.let {
                Text(
                    modifier = Modifier
                        .wrapContentSize()
                        .alpha(alphaLevel),
                    text = it,
                    style = MaterialTheme.typography.subtitle2.copy(
                        color = MaterialTheme.colors.dark_blue_500_dark_blue_200
                    ),
                )

                Text(
                    modifier = Modifier
                        .wrapContentSize()
                        .alpha(alphaLevel),
                    text = stringResource(id = R.string.storage_space).lowercase(),
                    style = MaterialTheme.typography.caption.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colors.textColorSecondary
                    ),
                )
            }
        }

        buttonTitleId?.let {
            TextButton(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(end = 16.dp),
                onClick = onButtonClick,
            ) {
                Text(
                    text = stringResource(id = it),
                    style = MaterialTheme.typography.subtitle2.copy(
                        color = MaterialTheme.colors.secondary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        daysLeft?.let { days ->
            TextButton(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(end = 16.dp),
                onClick = onButtonClick,
                border = if (days <= 0) {
                    BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colors.red_600_red_400,
                    )
                } else {
                    null
                },
            ) {
                Text(
                    text = if (days > 0) {
                        stringResource(id = R.string.general_num_days_left, days.toInt())
                    } else {
                        stringResource(id = R.string.expired_label)
                    },
                    style = MaterialTheme.typography.subtitle2.copy(
                        color = if (days <= 15) {
                            MaterialTheme.colors.red_600_red_400
                        } else {
                            MaterialTheme.colors.textColorSecondary
                        },
                    ),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
        }
    }
}
