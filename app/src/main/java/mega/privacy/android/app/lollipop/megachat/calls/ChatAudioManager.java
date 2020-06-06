package mega.privacy.android.app.lollipop.megachat.calls;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;

import java.io.IOException;

import mega.privacy.android.app.R;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaHandleList;

import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class ChatAudioManager {

    Context myContext;
    private AudioManager audioManager = null;
    private MediaPlayer mediaPlayer = null;
    private Vibrator vibrator = null;

    private ChatAudioManager(Context context) {
        myContext = context;
        initializeAudioManager();
    }

    public static ChatAudioManager create(Context context) {
        return new ChatAudioManager(context);
    }

    public void initializeAudioManager() {
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            return;
        }

        logDebug("Initializing audio manager...");
        audioManager = (AudioManager) myContext.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_NORMAL);
    }






}
