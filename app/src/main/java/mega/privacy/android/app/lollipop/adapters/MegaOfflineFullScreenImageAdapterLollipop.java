package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeThumbnail;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.TouchImageView;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;

public class MegaOfflineFullScreenImageAdapterLollipop extends PagerAdapter implements OnClickListener {
	
	private Activity activity;
	private ArrayList<MegaOffline> mOffList;
	private ArrayList<String> paths;
	private SparseArray<ViewHolderOfflineFullImage> visibleImgs = new SparseArray<ViewHolderOfflineFullImage>();
	DatabaseHandler dbH = null;
	private boolean zipImage = false;
    Context context;
		
	/*view holder class*/
    public class ViewHolderOfflineFullImage {
        TouchImageView imgDisplay;
        ImageView gifImgDisplay;
        ProgressBar progressBar;
        ProgressBar downloadProgressBar;
        String currentPath;
        int position;
        boolean isGIF;
    }
	
	// constructor
	public MegaOfflineFullScreenImageAdapterLollipop(Context context, Activity activity, ArrayList<MegaOffline> mOffList) {
		this.activity = activity;
		this.mOffList = mOffList;
		dbH = DatabaseHandler.getDbHandler(activity);
		this.zipImage = false;
        this.context = context;
    }
	
	// constructor
	public MegaOfflineFullScreenImageAdapterLollipop(Context context, Activity activity, ArrayList<String> paths, boolean zipImage) {

		this.activity = activity;
		this.paths = paths;
		dbH = DatabaseHandler.getDbHandler(activity);
		this.zipImage = zipImage;
        this.context = context;

    }

	@Override
	public int getCount() {
		if (zipImage){
			return paths.size();
		}
		else{
			return mOffList.size();
		}
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
        return view == ((RelativeLayout) object);
	}
	
	@Override
    public Object instantiateItem(ViewGroup container, int position) {

        File imageFile;
        String path;
        if (zipImage) {
            path = paths.get(position);
            imageFile = new File(path);
        } else {
            path = mOffList.get(position).getPath();
            imageFile = getOfflineFile(context, mOffList.get(position));
        }
        
		ViewHolderOfflineFullImage holder = new ViewHolderOfflineFullImage();
		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View viewLayout = inflater.inflate(R.layout.item_full_screen_image_viewer, container,false);

		holder.progressBar = (ProgressBar) viewLayout.findViewById(R.id.full_screen_image_viewer_progress_bar);
		holder.progressBar.setVisibility(View.VISIBLE);
		final ProgressBar pb = holder.progressBar;
		holder.downloadProgressBar = (ProgressBar) viewLayout.findViewById(R.id.full_screen_image_viewer_download_progress_bar);
		holder.downloadProgressBar.setVisibility(View.GONE);
		
		holder.imgDisplay = (TouchImageView) viewLayout.findViewById(R.id.full_screen_image_viewer_image);
		holder.imgDisplay.setOnClickListener(this);
		holder.gifImgDisplay = (ImageView) viewLayout.findViewById(R.id.full_screen_image_viewer_gif);
		holder.gifImgDisplay.setOnClickListener(this);

		holder.currentPath = path;

        int icon = MimeTypeThumbnail.typeForName(imageFile.getName()).getIconResourceId();

        if (MimeTypeThumbnail.typeForName(imageFile.getName()).isGIF()) {
            holder.imgDisplay.setVisibility(View.GONE);
            holder.gifImgDisplay.setVisibility(View.VISIBLE);
            setGlideParams(imageFile, icon, holder.gifImgDisplay, pb);
        } else {
            holder.imgDisplay.setVisibility(View.VISIBLE);
            holder.gifImgDisplay.setVisibility(View.GONE);
            setGlideParams(imageFile, icon, holder.imgDisplay, pb);
        }

        visibleImgs.put(position, holder);

        ((ViewPager) container).addView(viewLayout);
		
		return viewLayout;
	}
	
	@Override
    public void destroyItem(ViewGroup container, int position, Object object) {
		visibleImgs.remove(position);
        ((ViewPager) container).removeView((RelativeLayout) object);
        System.gc();
		logDebug ("DESTROY POSITION " + position + " SIZE SPARSE: " + visibleImgs.size());
 
    }

	public ImageView getVisibleImage(int position){
    	if (visibleImgs.get(position).isGIF) {
			return visibleImgs.get(position).gifImgDisplay;
		}
		else {
			return visibleImgs.get(position).imgDisplay;
		}
	}

	@Override
	public void onClick(View v) {
		logDebug("onClick");

		switch(v.getId()){
			case R.id.full_screen_image_viewer_gif:
			case R.id.full_screen_image_viewer_image:{
                ((FullScreenImageViewerLollipop) context).touchImage();

                RelativeLayout activityLayout = (RelativeLayout) activity.findViewById(R.id.full_image_viewer_parent_layout);
				activityLayout.invalidate();
				
				break;
			}
		}
	}

	private void setGlideParams(File imgFile, int icon, ImageView imageView, final ProgressBar pb) {
    	if (MimeTypeThumbnail.typeForName(imgFile.getName()).isGIF()) {
    		RequestListener<GifDrawable> gifListener = new RequestListener<GifDrawable>() {
				@Override
				public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<GifDrawable> target, boolean isFirstResource) {
					return false;
				}

				@Override
				public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
					pb.setVisibility(View.GONE);
					return false;
				}
			};

    		Glide.with(context)
					.asGif()
					.load(imgFile)
					.listener(gifListener)
					.placeholder(icon)
					.diskCacheStrategy(DiskCacheStrategy.ALL)
					.transition(withCrossFade())
					.into(imageView);
		} else {
    		RequestListener<Drawable> imgListener = new RequestListener<Drawable>() {
				@Override
				public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
					return false;
				}

				@Override
				public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
					pb.setVisibility(View.GONE);
					return false;
				}
    		};

    		Glide.with(context)
					.load(imgFile)
					.listener(imgListener)
					.placeholder(icon)
					.thumbnail(Glide.with(context).load(imgFile).listener(imgListener))
					.diskCacheStrategy(DiskCacheStrategy.ALL)
					.transition(withCrossFade())
					.into(imageView);
		}
    }
}
