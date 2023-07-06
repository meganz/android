package mega.privacy.android.app.utils;

import static mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.TextUtil.getFolderInfo;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import mega.privacy.android.app.MegaApplication;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;
import timber.log.Timber;


public class MegaApiUtils {

    public static long getFolderSize(MegaNode parent, Context context) {
        Timber.d("getFolderSize: %s", parent.getName());
        MegaApiAndroid megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        long size = 0;

        ArrayList<MegaNode> nodeList = megaApi.getChildren(parent);
        for (MegaNode node : nodeList) {
            if (node.isFile()) {
                size += node.getSize();
            } else {
                size += getFolderSize(node, context);
            }
        }
        return size;
    }

    /**
     * Gets the string to show as content of a folder.
     *
     * @param node The folder to get its string content.
     * @return The string to show as content of the folder.
     */
    public static String getMegaNodeFolderInfo(MegaNode node, Context context) {
        MegaApiJava megaApi = MegaApplication.getInstance().getMegaApi();

        return getFolderInfo(megaApi.getNumChildFolders(node), megaApi.getNumChildFiles(node), context);
    }

    /**
     * Gets the string to show as content of a folder link.
     *
     * @param node The folder to get its string content.
     * @return The string to show as content of the folder.
     */
    public static String getMegaNodeFolderLinkInfo(MegaNode node, Context context) {
        MegaApiJava megaApiFolder = MegaApplication.getInstance().getMegaApiFolder();

        return getFolderInfo(megaApiFolder.getNumChildFolders(node), megaApiFolder.getNumChildFiles(node), context);
    }


    /*
     * If there is an application that can manage the Intent, returns true. Otherwise, false.
     */
    public static boolean isIntentAvailable(Context ctx, Intent intent) {
        Timber.d("isIntentAvailable");
        final PackageManager mgr = ctx.getPackageManager();
        List<ResolveInfo> list = mgr.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    public static int calculateDeepBrowserTreeIncoming(MegaNode node, Context context) {
        Timber.d("calculateDeepBrowserTreeIncoming");
        MegaApiAndroid megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        String path = megaApi.getNodePath(node);
        Timber.d("The path is: %s", path);

        Pattern pattern = Pattern.compile("/");
        int count = Util.countMatches(pattern, path);

        return count + 1;
    }

    public static String createStringTree(MegaNode node, Context context) {
        Timber.d("createStringTree");
        MegaApiAndroid megaApi = null;
        if (context instanceof Service) {
            megaApi = ((MegaApplication) ((Service) context).getApplication()).getMegaApi();
        } else if (context instanceof Activity) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }
        if (megaApi == null) {
            Timber.e("ERROR megaApi is null");
            return null;
        }

        ArrayList<MegaNode> dTreeList = new ArrayList<MegaNode>();
        MegaNode parentNode = null;
        MegaNode nodeTemp = node;
        StringBuilder dTree = new StringBuilder();
        String s;

        dTreeList.add(node);

        if (node.getType() != MegaNode.TYPE_ROOT) {
            parentNode = megaApi.getParentNode(nodeTemp);

            if (parentNode != null) {

                if ((parentNode.getType() != MegaNode.TYPE_ROOT) & (parentNode.getHandle() != megaApi.getInboxNode().getHandle())) {
                    do {

                        dTreeList.add(parentNode);
                        dTree.insert(0, parentNode.getName() + "/");
                        nodeTemp = parentNode;

                        parentNode = megaApi.getParentNode(nodeTemp);
                        if (parentNode == null) {
                            break;
                        }
                    } while ((parentNode.getType() != MegaNode.TYPE_ROOT) & (parentNode.getHandle() != megaApi.getInboxNode().getHandle()));
                }
            }
        }

        if (dTree.length() > 0) {
            s = dTree.toString();
        } else {
            s = "";
        }

        Timber.d("createStringTree: %s", s);
        return s;
    }

    public static String getNodePath(Context context, MegaNode node) {
        String path = MegaApiUtils.createStringTree(node, context);

        if (path == null) {
            return File.separator;
        }

        return File.separator + path;
    }

    public static ArrayList<MegaUser> getLastContactedUsers(Context context) {

        MegaApiAndroid megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();

        ArrayList<MegaUser> lastContacted = new ArrayList<MegaUser>();

        MegaChatApiAndroid megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();

        ArrayList<MegaChatListItem> chats = megaChatApi.getActiveChatListItems();

        //Order by last interaction
        Collections.sort(chats, new Comparator<MegaChatListItem>() {

            public int compare(MegaChatListItem c1, MegaChatListItem c2) {
                long timestamp1 = c1.getLastTimestamp();
                long timestamp2 = c2.getLastTimestamp();

                long result = timestamp2 - timestamp1;
                return (int) result;
            }
        });

        for (int i = 0; i < chats.size(); i++) {
            MegaChatListItem chatItem = chats.get(i);
            if (!chatItem.isGroup()) {
                long peer = chatItem.getPeerHandle();
                MegaUser user = megaApi.getContact(MegaApiJava.userHandleToBase64(peer));
                if (user != null && user.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                    lastContacted.add(user);
                }
            }

            if (lastContacted.size() >= 6) {
                return lastContacted;
            }
        }

        //Still not 6 to fill the array
        ArrayList<MegaUser> users = megaApi.getContacts();

        ArrayList<MegaUser> usersNoAvatar = new ArrayList<>();

        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                boolean included = false;
                for (int j = 0; j < lastContacted.size(); j++) {
                    if (lastContacted.get(j).getHandle() == users.get(i).getHandle()) {
                        //Already included on the list
                        included = true;
                        break;
                    }
                }

                if (!included) {
                    File avatar = buildAvatarFile(users.get(i).getEmail() + ".jpg");
                    if (isFileAvailable(avatar)) {
                        if (avatar.length() > 0) {
                            lastContacted.add(users.get(i));
                        } else {
                            usersNoAvatar.add(users.get(i));
                        }
                    } else {
                        usersNoAvatar.add(users.get(i));
                    }

                    if (lastContacted.size() >= 6) {
                        return lastContacted;
                    }
                }
            }
        }

        //Add contacts without avatar
        for (int i = 0; i < usersNoAvatar.size(); i++) {
            lastContacted.add(usersNoAvatar.get(i));

            if (lastContacted.size() >= 6) {
                return lastContacted;
            }
        }

        return lastContacted;
    }
}
