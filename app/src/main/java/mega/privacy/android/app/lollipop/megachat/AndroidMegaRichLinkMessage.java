package mega.privacy.android.app.lollipop.megachat;


import android.net.Uri;
import android.util.Patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaNode;

public class AndroidMegaRichLinkMessage {

    private String url;
    private String server;
    private String folderContent;
    private MegaNode node = null;
    private boolean isFile;

    public AndroidMegaRichLinkMessage (String url, MegaNode node){
        this.node = node;
        this.url = url;

        Uri uri = Uri.parse(url);
        String server = uri.getAuthority();

        this.server = server;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public MegaNode getNode() {
        return node;
    }

    public void setNode(MegaNode node) {
        this.node = node;
    }

    public String getFolderContent() {
        return folderContent;
    }

    public void setFolderContent(String folderContent) {
        this.folderContent = folderContent;
    }

    public boolean isFile() {
        return isFile;
    }

    public void setFile(boolean file) {
        isFile = file;
    }

    public static String[] extractMegaLinks(String text) {
        List<String> links = new ArrayList<String>();
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            String url = m.group();
            log("URL extracted: " + url);
            if (url != null && (url.matches("^https://mega\\.co\\.nz/#!.+$") || url.matches("^https://mega\\.nz/#!.+$"))) {
                log("file link found");
                return links.toArray(new String[links.size()]);
            }
            if (url != null && (url.matches("^https://mega\\.co\\.nz/#F!.+$") || url.matches("^https://mega\\.nz/#F!.+$"))) {
                log("folder link found");
                links.add(url);
                return links.toArray(new String[links.size()]);
            }
        }

        return links.toArray(new String[links.size()]);
    }

    public static String extractMegaLink(String text) {
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            String url = m.group();
            log("URL extracted: " + url);
            if (url != null && (url.matches("^https://mega\\.co\\.nz/#!.+$") || url.matches("^https://mega\\.nz/#!.+$"))) {
                log("file link found");
                return url;
            }
            if (url != null && (url.matches("^https://mega\\.co\\.nz/#F!.+$") || url.matches("^https://mega\\.nz/#F!.+$"))) {
                log("folder link found");
                return url;
            }
        }

        return null;
    }

    public static boolean isFileLink(String url) {
        if (url != null && (url.matches("^https://mega\\.co\\.nz/#!.+$") || url.matches("^https://mega\\.nz/#!.+$"))) {
            log("IS file link found");
            return true;
        }
        return false;
    }

    public static boolean isChatLink(String url) {
        if (url != null && (url.matches("^https://mega\\.co\\.nz/c/.+$") || url.matches("^https://mega\\.nz/c/.+$"))) {
            log("IS chat link found");
            return true;
        }
        return false;
    }

    private static void log(String log) {
        Util.log("AndroidMegaRichLinkMessage", log);
    }

}
