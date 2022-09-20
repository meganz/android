package mega.privacy.android.app.main.adapters;

import static mega.privacy.android.app.utils.Constants.THUMB_CORNER_RADIUS_DP;
import static mega.privacy.android.app.utils.ThumbnailUtils.getRoundedBitmap;
import static mega.privacy.android.app.utils.ThumbnailUtils.getThumbnailFromCache;
import static mega.privacy.android.app.utils.ThumbnailUtils.getThumbnailFromFolder;
import static mega.privacy.android.app.utils.Util.dp2px;
import static nz.mega.sdk.MegaTransfer.STATE_CANCELLED;
import static nz.mega.sdk.MegaTransfer.STATE_COMPLETED;
import static nz.mega.sdk.MegaTransfer.STATE_FAILED;
import static nz.mega.sdk.MegaTransfer.TYPE_DOWNLOAD;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import mega.privacy.android.app.AndroidCompletedTransfer;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.utils.ColorUtils;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import timber.log.Timber;

public class MegaCompletedTransfersAdapter extends RecyclerView.Adapter<MegaCompletedTransfersAdapter.ViewHolderTransfer> {

    private final Context context;
    private List<AndroidCompletedTransfer> tL;
    private final MegaApiAndroid megaApi;

    public MegaCompletedTransfersAdapter(Context _context, List<AndroidCompletedTransfer> _transfers) {
        this.context = _context;
        this.tL = _transfers;

        megaApi = MegaApplication.getInstance().getMegaApi();
    }

    /*private view holder class*/
    public class ViewHolderTransfer extends RecyclerView.ViewHolder {
        public ViewHolderTransfer(View v) {
            super(v);
        }

        public ImageView imageView;
        public ImageView iconDownloadUploadView;
        public TextView textViewFileName;
        public ImageView imageViewCompleted;
        public TextView textViewCompleted;
        public RelativeLayout itemLayout;
        public ImageView optionReorder;
        public ImageView optionPause;
        public long document;
        public TextView progressText;
        public TextView speedText;
    }

    public void setTransfers(List<AndroidCompletedTransfer> transfers) {
        this.tL = transfers;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolderTransfer onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transfers_list, parent, false);

        ViewHolderTransfer holder = new ViewHolderTransfer(v);
        holder.itemLayout = v.findViewById(R.id.transfers_list_item_layout);
        holder.imageView = v.findViewById(R.id.transfers_list_thumbnail);
        holder.iconDownloadUploadView = v.findViewById(R.id.transfers_list_small_icon);
        holder.textViewFileName = v.findViewById(R.id.transfers_list_filename);
        holder.progressText = v.findViewById(R.id.transfers_progress_text);
        holder.speedText = v.findViewById(R.id.transfers_speed_text);
        holder.imageViewCompleted = v.findViewById(R.id.transfers_list_completed_image);
        holder.textViewCompleted = v.findViewById(R.id.transfers_list_completed_text);
        holder.optionReorder = v.findViewById(R.id.transfers_list_option_reorder);
        holder.optionPause = v.findViewById(R.id.transfers_list_option_pause);
        v.setTag(holder);

        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolderTransfer holder, int position) {
        holder.itemLayout.setOnClickListener(view -> {
            Timber.d("onClick");
            ((ManagerActivity) context).showManageTransferOptionsPanel(getItem(holder.getAbsoluteAdapterPosition()));
        });

        AndroidCompletedTransfer transfer = getItem(position);

        String fileName = transfer.getFileName();
        holder.textViewFileName.setText(fileName);
        holder.optionPause.setVisibility(View.GONE);
        holder.optionReorder.setVisibility(View.GONE);
        holder.progressText.setVisibility(View.GONE);
        holder.speedText.setVisibility(View.GONE);

        holder.imageView.setImageResource(MimeTypeList.typeForName(transfer.getFileName()).getIconResourceId());
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
        params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
        params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
        params.setMargins(36, 0, 0, 0);
        holder.imageView.setLayoutParams(params);

        RelativeLayout.LayoutParams params3 = (RelativeLayout.LayoutParams) holder.iconDownloadUploadView.getLayoutParams();
        params3.setMargins(0, 0, 0, 0);
        holder.iconDownloadUploadView.setLayoutParams(params3);

        if (MimeTypeList.typeForName(transfer.getFileName()).isImage() || MimeTypeList.typeForName(transfer.getFileName()).isVideo()) {
            long handle = Long.parseLong(transfer.getNodeHandle());
            Bitmap thumb = getThumbnailFromCache(handle);
            if (thumb == null) {
                MegaNode node = megaApi.getNodeByHandle(handle);
                thumb = getThumbnailFromFolder(node, context);
            }

            if (thumb != null) {
                RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
                params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                params1.setMargins(54, 0, 18, 0);
                holder.imageView.setLayoutParams(params1);

                RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams) holder.iconDownloadUploadView.getLayoutParams();
                params2.setMargins(0, -12, -12, 0);
                holder.iconDownloadUploadView.setLayoutParams(params2);
                holder.imageView.setImageBitmap(getRoundedBitmap(context, thumb, dp2px(THUMB_CORNER_RADIUS_DP)));
            }
        }

        holder.iconDownloadUploadView.setImageResource(transfer.getType() == TYPE_DOWNLOAD ? R.drawable.ic_download_transfers : R.drawable.ic_upload_transfers);

        holder.textViewCompleted.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary));
        RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageViewCompleted.getLayoutParams();
        params1.rightMargin = dp2px(5, context.getResources().getDisplayMetrics());

        switch (transfer.getState()) {
            case STATE_COMPLETED:
                holder.textViewCompleted.setText(transfer.getPath());
                holder.imageViewCompleted.setColorFilter(ContextCompat.getColor(context, R.color.green_500_300), PorterDuff.Mode.SRC_IN);
                holder.imageViewCompleted.setImageResource(R.drawable.ic_transfers_completed);
                break;

            case STATE_FAILED:
                holder.textViewCompleted.setTextColor(ColorUtils.getThemeColor(context, R.attr.colorError));
                holder.textViewCompleted.setText(String.format("%s: %s", context.getString(R.string.failed_label), transfer.getError()));
                params1.rightMargin = 0;
                holder.imageViewCompleted.setImageBitmap(null);
                break;

            case STATE_CANCELLED:
                holder.textViewCompleted.setText(R.string.transfer_cancelled);
                params1.rightMargin = 0;
                holder.imageViewCompleted.setImageBitmap(null);
                break;

            default:
                holder.textViewCompleted.setText(context.getResources().getString(R.string.transfer_unknown));
                holder.imageViewCompleted.clearColorFilter();
                holder.imageViewCompleted.setImageResource(R.drawable.ic_queue);
                break;
        }

        holder.imageViewCompleted.setLayoutParams(params1);
    }

    @Override
    public int getItemCount() {
        return tL.size();
    }

    public AndroidCompletedTransfer getItem(int position) {
        return tL.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void removeItemData(int position, List<AndroidCompletedTransfer> transfers) {
        tL = transfers;
        notifyItemRemoved(position);
    }
}
