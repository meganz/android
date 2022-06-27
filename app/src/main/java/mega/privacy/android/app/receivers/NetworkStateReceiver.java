package mega.privacy.android.app.receivers;

import static mega.privacy.android.app.utils.JobUtil.scheduleCameraUploadJob;
import static mega.privacy.android.app.utils.Util.getLocalIpAddress;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import timber.log.Timber;

public class NetworkStateReceiver extends BroadcastReceiver {

    protected List<NetworkStateReceiverListener> listeners;
    protected Boolean connected;

    Handler handler = new Handler();

    private MegaChatApiAndroid megaChatApi;
    private MegaApiAndroid megaApi;

    public NetworkStateReceiver() {
        listeners = new ArrayList<NetworkStateReceiverListener>();
        connected = null;
    }

    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getExtras() == null)
            return;

        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = manager.getActiveNetworkInfo();

        MegaApplication mApplication = ((MegaApplication) context.getApplicationContext());

        if (ni != null && ni.getState() == NetworkInfo.State.CONNECTED) {
            Timber.d("Network state: CONNECTED");

            megaApi = mApplication.getMegaApi();
            megaChatApi = mApplication.getMegaChatApi();

            String previousIP = mApplication.getLocalIpAddress();
            String currentIP = getLocalIpAddress(context);

            Timber.d("Previous IP: %s", previousIP);
            Timber.d("Current IP: %s", currentIP);

            mApplication.setLocalIpAddress(currentIP);

            if ((currentIP != null) && (currentIP.length() != 0) && (currentIP.compareTo("127.0.0.1") != 0)) {
                if ((previousIP == null) || (currentIP.compareTo(previousIP) != 0)) {
                    Timber.d("Reconnecting...");
                    megaApi.reconnect();

                    if (megaChatApi != null) {
                        megaChatApi.retryPendingConnections(true, null);
                    }
                } else {
                    Timber.d("Retrying pending connections...");
                    megaApi.retryPendingConnections();

                    if (megaChatApi != null) {
                        megaChatApi.retryPendingConnections(false, null);
                    }
                }
            }

            connected = true;
            scheduleCameraUploadJob(context);
        } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
            Timber.d("Network state: DISCONNECTED");
            mApplication.setLocalIpAddress(null);
            connected = false;
        }

        notifyStateToAll();
    }

    private void notifyStateToAll() {
        for (NetworkStateReceiverListener listener : listeners)
            notifyState(listener);
    }

    private void notifyState(NetworkStateReceiverListener listener) {
        if (connected == null || listener == null)
            return;

        if (connected == true)
            listener.networkAvailable();
        else
            listener.networkUnavailable();
    }

    public void addListener(NetworkStateReceiverListener l) {
        listeners.add(l);
        notifyState(l);
    }

    public void removeListener(NetworkStateReceiverListener l) {
        listeners.remove(l);
    }

    public interface NetworkStateReceiverListener {
        public void networkAvailable();

        public void networkUnavailable();
    }
}