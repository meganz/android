package nz.mega.sdk;

import android.os.Handler;
import android.os.Looper;

public class MegaChatApiAndroid extends MegaChatApiJava {
    static Handler handler = new Handler(Looper.getMainLooper());

    public MegaChatApiAndroid(nz.mega.sdk.MegaApiAndroid megaApi) {
        super(megaApi);
    }

    @Override
    void runCallback(Runnable runnable) {
        handler.post(runnable);
    }
}
