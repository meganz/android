package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;
import android.media.AudioManager;

import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;

public class AudioFocusListener implements AudioManager.OnAudioFocusChangeListener{

    Context context;
    public AudioFocusListener(Context context) {
        this.context = context;
    }
    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (context instanceof ChatActivityLollipop) {
                    ((ChatActivityLollipop) context).cancelRecording();
                }
                if(context instanceof AudioVideoPlayerLollipop){
                    ((AudioVideoPlayerLollipop) context).stopPlayback();
                }
                break;
        }
    }
}
