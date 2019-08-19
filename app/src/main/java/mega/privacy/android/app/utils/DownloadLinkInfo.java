package mega.privacy.android.app.utils;

import nz.mega.sdk.MegaNode;

public class DownloadLinkInfo {

    private MegaNode node;

    private String url;

    public DownloadLinkInfo(MegaNode node, String url) {
        this.node = node;
        this.url = url;
    }

    public MegaNode getNode() {
        return node;
    }

    public void setNode(MegaNode node) {
        this.node = node;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
