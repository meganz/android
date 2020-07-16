package mega.privacy.android.app.lollipop.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.RecentsItem;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.RecentsFragment;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaRecentActionBucket;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.ThumbnailUtilsLollipop.*;
import static mega.privacy.android.app.utils.Util.*;

public class RecentsAdapter extends RecyclerView.Adapter<RecentsAdapter.ViewHolderBucket> implements View.OnClickListener, SectionTitleProvider {

    private Object fragment;
    private Context context;
    private MegaApiAndroid megaApi;

    private DisplayMetrics outMetrics;

    private ArrayList<RecentsItem> recentsItems;

    public RecentsAdapter(Context context, Object fragment, ArrayList<RecentsItem> items) {
        logDebug("new RecentsAdapter");
        this.context = context;
        this.fragment = fragment;
        setItems(items);

        megaApi = MegaApplication.getInstance().getMegaApi();

        outMetrics = context.getResources().getDisplayMetrics();
    }

    public class ViewHolderBucket extends RecyclerView.ViewHolder {

        private RelativeLayout headerLayout;
        private TextView headerText;
        private RelativeLayout itemBucketLayout;
        private ImageView imageThumbnail;
        private TextView title;
        private TextView actionBy;
        private TextView subtitle;
        private ImageView sharedIcon;
        private ImageView actionIcon;
        private TextView time;
        private ImageButton threeDots;
        private LinearLayout mediaLayout;
        private RecyclerView mediaRecycler;

        private long document = -1;

        public long getDocument() {
            return document;
        }

        public void setImageThumbnail(Bitmap image) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) this.imageThumbnail.getLayoutParams();
            params.width = params.height = px2dp(36, outMetrics);
            int margin = px2dp(18, outMetrics);
            params.setMargins(margin, margin, margin, 0);

            this.imageThumbnail.setLayoutParams(params);
            this.imageThumbnail.setImageBitmap(getRoundedBitmap(context, image, THUMB_ROUND_PIXEL));
        }

        public ImageView getImageThumbnail() {
            return imageThumbnail;
        }

        public ViewHolderBucket(View itemView) {
            super(itemView);
        }
    }

    @Override
    public RecentsAdapter.ViewHolderBucket onCreateViewHolder(ViewGroup parent, int viewType) {
        logDebug("onCreateViewHolder");

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bucket, parent, false);
        ViewHolderBucket holder = new ViewHolderBucket(v);

        holder.headerLayout = v.findViewById(R.id.header_layout);
        holder.headerText = v.findViewById(R.id.header_text);
        holder.itemBucketLayout = v.findViewById(R.id.item_bucket_layout);
        holder.itemBucketLayout.setTag(holder);
        holder.imageThumbnail = v.findViewById(R.id.thumbnail_view);
        holder.threeDots = v.findViewById(R.id.three_dots);
        holder.threeDots.setTag(holder);
        holder.title = v.findViewById(R.id.first_line_text);
        holder.actionBy = v.findViewById(R.id.second_line_text);
        holder.subtitle = v.findViewById(R.id.name_text);
        holder.sharedIcon = v.findViewById(R.id.shared_image);
        holder.actionIcon = v.findViewById(R.id.action_image);
        holder.time = v.findViewById(R.id.time_text);
        holder.mediaLayout = v.findViewById(R.id.media_bucket_layout);
        holder.mediaRecycler = v.findViewById(R.id.media_recyler);

        holder.mediaRecycler.setNestedScrollingEnabled(false);
//        holder.mediaRecycler.getParent().requestDisallowInterceptTouchEvent(true);

        v.setTag(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(RecentsAdapter.ViewHolderBucket holder, int position) {
        logDebug("Position: " + position);
        RecentsItem item = getItemtAtPosition(position);
        if (item == null) return;

        if (item.getViewType() == RecentsItem.TYPE_HEADER) {
            logDebug("onBindViewHolder: TYPE_HEADER");
            holder.itemBucketLayout.setVisibility(View.GONE);
            holder.headerLayout.setVisibility(View.VISIBLE);
            holder.headerText.setText(item.getDate());
        } else if (item.getViewType() == RecentsItem.TYPE_BUCKET) {
            logDebug("onBindViewHolder: TYPE_BUCKET");
            holder.itemBucketLayout.setVisibility(View.VISIBLE);
            holder.itemBucketLayout.setOnClickListener(this);
            holder.headerLayout.setVisibility(View.GONE);

            MegaRecentActionBucket bucket = item.getBucket();
            if (bucket == null || bucket.getNodes() == null || bucket.getNodes().size() == 0)
                return;

            MegaNodeList nodeList = bucket.getNodes();
            MegaNode node = nodeList.get(0);
            if (node == null) return;

            MegaNode parentNode = megaApi.getNodeByHandle(bucket.getParentHandle());
            if (parentNode == null) return;

            holder.subtitle.setText(parentNode.getName());

            String mail = bucket.getUserEmail();
            String user;
            String userAction;
            if (mail.equals(megaApi.getMyEmail())) {
                holder.actionBy.setVisibility(View.GONE);
            } else {
                user = ((RecentsFragment) fragment).findUserName(mail);
                if (bucket.isUpdate()) {
                    userAction = context.getString(R.string.update_action_bucket, user);
                } else {
                    userAction = context.getString(R.string.create_action_bucket, user);
                }
                holder.actionBy.setVisibility(View.VISIBLE);
                holder.actionBy.setText(formatUserAction(userAction));
            }

            parentNode = getOutgoingOrIncomingParent(megaApi, parentNode);

            if (parentNode == null) {
//              No outShare, no inShare
                holder.sharedIcon.setVisibility(View.GONE);
            } else {
                holder.sharedIcon.setVisibility(View.VISIBLE);
                if (parentNode.isInShare()) {
                    holder.sharedIcon.setImageResource(R.drawable.ic_folder_incoming_list);
                } else if (isOutShare(parentNode)) {
                    holder.sharedIcon.setImageResource(R.drawable.ic_folder_outgoing_list);
                }
            }

            holder.time.setText(item.getTime());

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageThumbnail.getLayoutParams();
            params.width = params.height = px2dp(48, outMetrics);
            int margin = px2dp(12, outMetrics);
            params.setMargins(margin, margin, margin, 0);
            holder.imageThumbnail.setLayoutParams(params);
            holder.imageThumbnail.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());

            holder.mediaLayout.setVisibility(View.GONE);

            if (nodeList.size() == 1) {
                holder.document = node.getHandle();
                holder.threeDots.setVisibility(View.VISIBLE);
                holder.threeDots.setOnClickListener(this);
                holder.title.setText(node.getName());

                Bitmap thumbnail = getThumbnailFromCache(node);
                if (thumbnail == null) {
                    thumbnail = getThumbnailFromFolder(node, context);
                    if (thumbnail == null) {
                        try {
                            if (node.hasThumbnail()) {
                                thumbnail = getThumbnailFromMegaList(node, context, holder, megaApi, this);
                            } else {
                                createThumbnailList(context, node, holder, megaApi, this);
                            }
                        } catch (Exception e) {
                            logError("Error getting or creating node thumbnail", e);
                            e.printStackTrace();
                        }
                    }
                }
                if (thumbnail != null) {
                    holder.setImageThumbnail(thumbnail);
                }
            } else {
                holder.threeDots.setVisibility(View.INVISIBLE);
                holder.threeDots.setOnClickListener(null);

                if (bucket.isMedia()) {
                    holder.mediaLayout.setVisibility(View.VISIBLE);
                    holder.title.setText(getMediaTitle(nodeList));
                    holder.imageThumbnail.setImageResource(R.drawable.media);

                    MediaRecentsAdapter adapter = new MediaRecentsAdapter(context, fragment, bucket.getNodes(), bucket);

                    holder.mediaRecycler.setHasFixedSize(true);
                    holder.mediaRecycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
                    holder.mediaRecycler.setAdapter(adapter);
//                    holder.mediaRecycler.setNestedScrollingEnabled(false);
//                    holder.mediaRecycler.requestDisallowInterceptTouchEvent(true);
                } else {
                    holder.title.setText(context.getString(R.string.title_bucket, node.getName(), (nodeList.size() - 1)));
                }
            }

            if (bucket.isUpdate()) {
                holder.actionIcon.setImageResource(R.drawable.ic_versions_small);
            } else {
                holder.actionIcon.setImageResource(R.drawable.ic_recents_up);
            }
        }
    }

    private Spanned formatUserAction(String userAction) {
        try {
            userAction = userAction.replace("[A]", "<font color=\'#7a7a7a\'>");
            userAction = userAction.replace("[/A]", "</font>");
        } catch (Exception e) {
            logError("Exception formatting string", e);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(userAction, Html.FROM_HTML_MODE_LEGACY);
        }

        return Html.fromHtml(userAction);
    }

    private String getMediaTitle(MegaNodeList nodeList) {
        int numImages = 0;
        int numVideos = 0;
        String mediaTitle = null;

        for (int i = 0; i < nodeList.size(); i++) {
            if (MimeTypeList.typeForName(nodeList.get(i).getName()).isImage()) {
                numImages++;
            } else {
                numVideos++;
            }
        }

        if (numImages > 0 && numVideos == 0) {
            mediaTitle = context.getString(R.string.title_media_bucket_only_images, numImages);
        } else if (numImages == 0 && numVideos > 0) {
            mediaTitle = context.getString(R.string.title_media_bucket_only_videos, numVideos);
        } else if (numImages == 1 && numVideos == 1) {
            mediaTitle = context.getString(R.string.title_media_bucket_image_and_video);
        } else if (numImages == 1 && numVideos > 1) {
            mediaTitle = context.getString(R.string.title_media_bucket_image_and_videos, numVideos);
        } else if (numImages > 1 && numVideos == 1) {
            mediaTitle = context.getString(R.string.title_media_bucket_images_and_video, numImages);
        } else {
            mediaTitle = context.getString(R.string.title_media_bucket_images_and_videos, numImages, numVideos);
        }

        return mediaTitle;
    }

    public void setItems(ArrayList<RecentsItem> recentsItems) {
        this.recentsItems = recentsItems;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (recentsItems == null || recentsItems.isEmpty()) return 0;

        return recentsItems.size();
    }

    @Override
    public void onClick(View v) {

        ViewHolderBucket holder = (ViewHolderBucket) v.getTag();
        if (holder == null) return;

        RecentsItem item = getItemtAtPosition(holder.getAdapterPosition());
        if (item == null) return;

        MegaNode node = getNodeOfItem(item);

        switch (v.getId()) {
            case R.id.three_dots: {
                if (!isOnline(context)) {
                    ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
                    break;
                }
                if (node != null) {
                    ((ManagerActivityLollipop) context).showNodeOptionsPanel(node);
                }
                break;
            }
            case R.id.item_bucket_layout: {
                MegaNodeList nodeList = getMegaNodeListOfItem(item);
                if (nodeList == null) break;
                if (nodeList.size() == 1) {
                    ((RecentsFragment) fragment).openFile(node, false);
                    break;
                }
                if (item.getBucket() == null) break;
                ((RecentsFragment) fragment).openMultipleBucket(item.getBucket());
                break;
            }
        }
    }

    @Override
    public int getItemViewType(int pos) {
        if (recentsItems == null || recentsItems.isEmpty() || pos >= recentsItems.size())
            return super.getItemViewType(pos);

        return recentsItems.get(pos).getViewType();
    }

    private RecentsItem getItemtAtPosition(int pos) {
        if (recentsItems == null || recentsItems.isEmpty() || pos >= recentsItems.size())
            return null;

        return recentsItems.get(pos);
    }

    private MegaRecentActionBucket getBucketOfItem(RecentsItem item) {
        if (item == null) return null;

        return item.getBucket();
    }

    private MegaNodeList getMegaNodeListOfItem(RecentsItem item) {
        MegaRecentActionBucket bucket = getBucketOfItem(item);
        if (bucket == null) return null;

        return bucket.getNodes();
    }

    private MegaNode getNodeOfItem(RecentsItem item) {
        MegaNodeList nodeList = getMegaNodeListOfItem(item);
        if (nodeList == null || nodeList.size() > 1) return null;

        return nodeList.get(0);
    }

    @Override
    public String getSectionTitle(int position) {
        if (recentsItems == null || recentsItems.isEmpty()
                || position < 0 || position >= recentsItems.size()) return "";

        return recentsItems.get(position).getDate();
    }
}
