package mega.privacy.android.feature.sync.data.mock;

import javax.inject.Inject;

import nz.mega.sdk.MegaRequestListenerInterface;

public class MegaApiMock {

    @Inject
    MegaApiMock() {
        // mock
    }

    public boolean isSyncStalled() {
        return false;
    }

    public void requestMegaSyncStallList(MegaRequestListenerInterface listener) {
        // mock
    }
}
