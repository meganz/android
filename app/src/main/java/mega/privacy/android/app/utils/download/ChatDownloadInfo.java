package mega.privacy.android.app.utils.download;

import java.util.ArrayList;

import nz.mega.sdk.MegaNode;

public class ChatDownloadInfo {

    private long size;

    private ArrayList<String> serializedNodes;

    private ArrayList<MegaNode> nodeList;

    public ChatDownloadInfo(long size, ArrayList<String> serializedNodes, ArrayList<MegaNode> nodeList) {
        this.size = size;
        this.serializedNodes = serializedNodes;
        this.nodeList = nodeList;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public ArrayList<String> getSerializedNodes() {
        return serializedNodes;
    }

    public void setSerializedNodes(ArrayList<String> serializedNodes) {
        this.serializedNodes = serializedNodes;
    }

    public ArrayList<MegaNode> getNodeList() {
        return nodeList;
    }

    public void setNodeList(ArrayList<MegaNode> nodeList) {
        this.nodeList = nodeList;
    }
}
