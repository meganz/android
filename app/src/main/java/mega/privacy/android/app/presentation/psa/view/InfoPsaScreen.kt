package mega.privacy.android.app.presentation.psa.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.psa.model.PsaState

@Composable
fun InfoPsaScreen(
    state: PsaState.InfoPsa,
    markAsSeen: (Int) -> Unit,
){
    InfoPsaView(
        title = state.title,
        text = state.text,
        imageUrl = state.imageUrl,
        onDismiss = { markAsSeen(state.id) },
        modifier = Modifier,
    )
}