package mega.privacy.android.app;

import android.content.Context;
import android.os.AsyncTask;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.util.ArrayList;

import mega.privacy.android.app.lollipop.CloudDriveExplorerFragmentLollipop;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.IncomingSharesExplorerFragmentLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.SearchFragmentLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaCancelToken;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

import static mega.privacy.android.app.fragments.managerFragments.LinksFragment.getLinksOrderCloud;
import static mega.privacy.android.app.lollipop.ManagerActivityLollipop.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.SortUtil.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class SearchNodesTask extends AsyncTask<Void, Void, Void> {
    private Context context;
    private ManagerActivityLollipop managerA;
    private FileExplorerActivityLollipop fileExplorerA;
    private IncomingSharesExplorerFragmentLollipop iSharesExplorerF;
    private CloudDriveExplorerFragmentLollipop cDExplorerF;
    private SearchFragmentLollipop searchF;

    private MegaApiAndroid megaApi;
    private MegaCancelToken megaCancelToken;

    private String query;
    private long parentHandleSearch;
    private ArrayList<MegaNode> nodes;

    private int orderCloud;
    private int orderOthers;

    public SearchNodesTask(Context mContext, Fragment mFragment, String mQuery, long mParentHandleSearch, ArrayList<MegaNode> mNodes){

        context = mContext;

        if (context instanceof ManagerActivityLollipop) {
            managerA = (ManagerActivityLollipop) context;
        } else if (context instanceof FileExplorerActivityLollipop) {
            fileExplorerA = (FileExplorerActivityLollipop) context;
        }

        if (mFragment instanceof SearchFragmentLollipop) {
            searchF = (SearchFragmentLollipop) mFragment;
        } else if (mFragment instanceof IncomingSharesExplorerFragmentLollipop) {
            iSharesExplorerF = (IncomingSharesExplorerFragmentLollipop) mFragment;
        } else if (mFragment instanceof CloudDriveExplorerFragmentLollipop) {
            cDExplorerF = (CloudDriveExplorerFragmentLollipop) mFragment;
        }

        query = mQuery;
        parentHandleSearch = mParentHandleSearch;
        nodes = mNodes;

        getOrder();

        megaApi = MegaApplication.getInstance().getMegaApi();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        getSearchNodes();
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if (isSearchF()) {
            searchF.setProgressView(false);
            searchF.setNodes(nodes);
        } else if (isCDExplorerF()) {
            cDExplorerF.setProgressView(false);
            cDExplorerF.setSearchNodes(nodes);
        } else if (isISharesExplorerF()) {
            iSharesExplorerF.setProgressView(false);
            iSharesExplorerF.setSearchNodes(nodes);
        }
    }

    public void cancelSearch() {
        if (megaCancelToken != null && !megaCancelToken.isCancelled()) {
            megaCancelToken.cancel();
        }
    }

    private void getOrder() {
        MegaPreferences prefs = DatabaseHandler.getDbHandler(context).getPreferences();

        if (prefs != null && prefs.getPreferredSortCloud() !=  null) {
            orderCloud = Integer.parseInt(prefs.getPreferredSortCloud());
        } else {
            orderCloud = MegaApiJava.ORDER_DEFAULT_ASC;
        }

        if (prefs != null && prefs.getPreferredSortOthers() != null) {
            orderOthers = Integer.parseInt(prefs.getPreferredSortOthers());
        } else {
            orderOthers = MegaApiAndroid.ORDER_DEFAULT_ASC;
        }
    }

    private void getSearchNodes() {
        if (query == null) {
            nodes.clear();
            return;
        }

        MegaNode parent = null;
        long parentHandle;

        if (parentHandleSearch == INVALID_HANDLE) {
            if (isSearchF()) {
                final ManagerActivityLollipop.DrawerItem drawerItem = managerA.getSearchDrawerItem();
                if (drawerItem == null) return;

                switch (drawerItem) {
                    case CLOUD_DRIVE: {
                        parent = megaApi.getNodeByHandle(managerA.getParentHandleBrowser());
                        break;
                    }
                    case SHARED_ITEMS: {
                        if (managerA.getSearchSharedTab() == INCOMING_TAB) {
                            if (managerA.getParentHandleIncoming() == INVALID_HANDLE) {
                                nodes = filterInShares(query);
                                return;
                            }

                            parent = megaApi.getNodeByHandle(managerA.getParentHandleIncoming());
                        } else if (managerA.getSearchSharedTab() == OUTGOING_TAB) {
                            if (managerA.getParentHandleOutgoing() == INVALID_HANDLE) {
                                nodes = filterOutShares(query);
                                return;
                            }

                            parent = megaApi.getNodeByHandle(managerA.getParentHandleOutgoing());
                        } else if (managerA.getSearchSharedTab() == LINKS_TAB) {
                            if (managerA.getParentHandleLinks() == INVALID_HANDLE) {
                                nodes = filterLinks(query);
                                return;
                            }

                            parent = megaApi.getNodeByHandle(managerA.getParentHandleLinks());
                        }
                        break;
                    }
                    case RUBBISH_BIN: {
                        parentHandle = managerA.getParentHandleRubbish();
                        if (parentHandle == INVALID_HANDLE) {
                            parent = megaApi.getRubbishNode();
                        } else {
                            parent = megaApi.getNodeByHandle(managerA.getParentHandleRubbish());
                        }
                        break;
                    }
                    case INBOX: {
                        parentHandle = managerA.getParentHandleInbox();
                        if (parentHandle == INVALID_HANDLE) {
                            parent = megaApi.getInboxNode();
                        } else {
                            parent = megaApi.getNodeByHandle(parentHandle);
                        }
                        break;
                    }
                }
            } else if (isCDExplorerF()) {
                parent = megaApi.getNodeByHandle(fileExplorerA.getParentHandleCloud());
            } else if (isISharesExplorerF()) {
                if (fileExplorerA.getParentHandleIncoming() == INVALID_HANDLE) {
                    nodes = filterInShares(query);
                    return;
                }

                parent = megaApi.getNodeByHandle(fileExplorerA.getParentHandleIncoming());
            }
        } else {
            parent = megaApi.getNodeByHandle(parentHandleSearch);
        }

        if (parent != null) {
            if (query.isEmpty() || parentHandleSearch != INVALID_HANDLE) {
                nodes = megaApi.getChildren(parent);
            } else {
                megaCancelToken = MegaCancelToken.createInstance();
                nodes = megaApi.search(parent, query, megaCancelToken, true, orderCloud);
            }
        }
    }

    private ArrayList<MegaNode> filterInShares(String query) {
        ArrayList<MegaNode> inShares = megaApi.getInShares();
        ArrayList<MegaNode> filteredInShares = new ArrayList<>();

        for (MegaNode inShare : inShares) {
            if (shouldNodeBeFilter(inShare, query)) {
                filteredInShares.add(inShare);
            }
        }

        if(orderOthers == MegaApiJava.ORDER_DEFAULT_DESC){
            sortByMailDescending(filteredInShares);
        }

        return filteredInShares;
    }

    private ArrayList<MegaNode> filterOutShares(String query) {
        ArrayList<MegaShare> outShares = megaApi.getOutShares();
        ArrayList<MegaNode> filteredOutShares = new ArrayList<>();

        long lastHandle = INVALID_HANDLE;

        for (MegaShare outShare : outShares) {
            MegaNode node = megaApi.getNodeByHandle(outShare.getNodeHandle());
            if (node != null && lastHandle != node.getHandle()) {
                lastHandle = node.getHandle();
                if (shouldNodeBeFilter(node, query)) {
                    filteredOutShares.add(node);
                }
            }
        }

        if(orderOthers == MegaApiJava.ORDER_DEFAULT_DESC){
            sortByNameDescending(filteredOutShares);
        }
        else{
            sortByNameAscending(filteredOutShares);
        }

        return filteredOutShares;
    }

    private ArrayList<MegaNode> filterLinks(String query) {
        ArrayList<MegaNode> links = megaApi.getPublicLinks(getLinksOrderCloud(managerA.orderCloud, managerA.isFirstNavigationLevel()));
        ArrayList<MegaNode> filteredLinks = new ArrayList<>();

        for (MegaNode node : links) {
            if (node == null) continue;

            if (shouldNodeBeFilter(node, query)) {
                filteredLinks.add(node);
            }
        }

        return filteredLinks;
    }

    private boolean shouldNodeBeFilter(MegaNode node, String query) {
        return node != null && node.getName().toLowerCase().contains(query.toLowerCase());
    }

    private boolean isSearchF() {
        return managerA != null && searchF != null;
    }

    private boolean isFileExplorerA() {
        return fileExplorerA != null;
    }

    private boolean isISharesExplorerF() {
        return isFileExplorerA() && iSharesExplorerF != null;
    }

    private boolean isCDExplorerF() {
        return isFileExplorerA() && cDExplorerF != null;
    }

    public static void setSearchProgressView(RelativeLayout contentLayout, ProgressBar searchProgressBar, RecyclerView recyclerView, boolean inProgress) {
        if (contentLayout == null || searchProgressBar == null || recyclerView == null) {
            logWarning("Cannot set search progress view, one or more parameters are NULL.");
            logDebug("contentLayout: " + contentLayout + ", searchProgressBar: " + searchProgressBar + ", recyclerView: " + recyclerView);
            return;
        }

        contentLayout.setEnabled(!inProgress);
        if (inProgress) {
            contentLayout.setAlpha(0.4f);
            searchProgressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            contentLayout.setAlpha(1);
            searchProgressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    public static ArrayList<MegaNode> getSearchedNodes(ArrayList<String> handles) {
        MegaApiAndroid megaApi = MegaApplication.getInstance().getMegaApi();
        ArrayList<MegaNode> nodes = new ArrayList<>();

        for (String handle : handles) {
            MegaNode node = megaApi.getNodeByHandle(Long.parseLong(handle));
            if (node != null) {
                nodes.add(node);
            }
        }

        return nodes;
    }
}