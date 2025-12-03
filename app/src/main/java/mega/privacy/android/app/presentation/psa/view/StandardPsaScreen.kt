package mega.privacy.android.app.presentation.psa.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.psa.model.PsaState

@Composable
fun StandardPsaScreen(
    state: PsaState.StandardPsa,
    markAsSeen: (Int) -> Unit,
    navigateToPsaPage: (String) -> Unit,
){
    PsaView(
        title = state.title,
        text = state.text,
        imageUrl = state.imageUrl,
        positiveText = state.positiveText,
        onPositiveTapped = {
            navigateToPsaPage(state.positiveLink)
            markAsSeen(state.id)
        },
        onDismiss = { markAsSeen(state.id) },
        modifier = Modifier
    )
}