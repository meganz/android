package mega.privacy.android.app.presentation.psa.view

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.psa.model.PsaState

@Composable
fun WebPsaScreen(
    state: PsaState.WebPsa,
    markAsSeen: (Int) -> Unit,
){
    WebPsaView(
        psa = state,
        markAsSeen = { markAsSeen(state.id) }
    )
}