package mega.privacy.android.app.lollipop.megachat.calls;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import mega.privacy.android.app.MegaApplication;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.utils.Constants.*;

public class VolumeEventsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(VOLUME_CHANGED_ACTION)) {
            int volume = (Integer) intent.getExtras().get(EXTRA_VOLUME_STREAM_VALUE);
            Intent intentVolume = new Intent(BROADCAST_ACTION_INCOMING_CALL_VOLUME);
            intentVolume.putExtra(VOLUME_CALL, volume);
            MegaApplication.getInstance().sendBroadcast(intentVolume);
        }
    }
}