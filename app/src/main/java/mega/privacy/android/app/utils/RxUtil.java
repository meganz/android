package mega.privacy.android.app.utils;

import io.reactivex.rxjava3.functions.Consumer;

import static mega.privacy.android.app.utils.LogUtil.logError;

public class RxUtil {
    private RxUtil() {
    }

    public static Consumer<? super Throwable> logErr(String context) {
        return throwable -> logError(context + " onError", throwable);
    }
}
