package mega.privacy.android.app.presentation.fingerprintauth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.presentation.theme.body2
import mega.privacy.android.presentation.theme.jade_300
import mega.privacy.android.presentation.theme.subtitle1

/**
 * Security upgrade dialog body
 *
 * @param folderName    : Name of the folder for which security settings will upgrade
 * @param onOkClick     : Ok button click listener
 * @param onCancelClick : Cancel button click listener
 */
@Composable
fun SecurityUpgradeDialogView(
    folderName: String,
    onOkClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    Surface(modifier = Modifier
        .padding(10.dp)
        .fillMaxSize(),
        shape = RoundedCornerShape(8.dp),
        color = if (MaterialTheme.colors.isLight) {
            Color.White
        } else {
            Color.Transparent
        },
        content = {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = {
                    Image(modifier = Modifier
                        .height(140.dp)
                        .width(114.dp)
                        .testTag("HeaderImage"),
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_security_upgrade),
                        contentDescription = "Empty")

                    Text(text = stringResource(id = R.string.shared_items_security_upgrade_dialog_title),
                        style = subtitle1,
                        color = if (MaterialTheme.colors.isLight) {
                            Color.Black
                        } else {
                            Color.White
                        })

                    Spacer(Modifier.height(16.dp))

                    Text(text = stringResource(id = R.string.shared_items_security_upgrade_dialog_content),
                        style = body2.copy(textAlign = TextAlign.Center),
                        color = if (MaterialTheme.colors.isLight) {
                            Color.Black
                        } else {
                            Color.White
                        })

                    Spacer(Modifier.height(20.dp))

                    Text(modifier = Modifier.testTag("SharedNodeInfo"),
                        text = stringResource(id = R.string.shared_items_security_upgrade_dialog_node_sharing_info,
                            folderName),
                        style = body2.copy(textAlign = TextAlign.Center),
                        color = if (MaterialTheme.colors.isLight) {
                            Color.Black
                        } else {
                            Color.White
                        })

                    Spacer(Modifier.height(20.dp))

                    Button(modifier = Modifier
                        .height(45.dp)
                        .fillMaxWidth()
                        .padding(start = 25.dp, end = 25.dp),
                        shape = RoundedCornerShape(8.dp),
                        content = {
                            Text(text = stringResource(id = R.string.general_ok),
                                color = Color.White)
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = jade_300),
                        onClick = onOkClick)

                    Spacer(Modifier.height(10.dp))

                    Button(modifier = Modifier
                        .height(45.dp)
                        .fillMaxWidth()
                        .padding(start = 25.dp, end = 25.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = if (MaterialTheme.colors.isLight) Color.White else Color.DarkGray),
                        onClick = onCancelClick) {
                        Text(text = stringResource(id = R.string.button_cancel),
                            color = if (MaterialTheme.colors.isLight) {
                                Color.Black
                            } else {
                                jade_300
                            })
                    }
                })
        })
}