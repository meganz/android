package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.RelativeLayout;

import java.nio.ByteBuffer;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.GroupCallAdapter;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatVideoListenerInterface;


public class GroupCallListener implements MegaChatVideoListenerInterface {

    Context context;
    GroupCallAdapter.ViewHolderGroupCall holder;
    int width;
    int height;
    Bitmap bitmap;

    public GroupCallListener(Context context, GroupCallAdapter.ViewHolderGroupCall holder) {
        this.context = context;
        this.holder = holder;
        this.width = 0;
        this.height = 0;
    }

    @Override
    public void onChatVideoData(MegaChatApiJava api, long chatid, int width, int height, byte[] byteBuffer) {
        if((width == 0) || (height == 0)){
            return;
        }

        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;

            SurfaceHolder Sholder = holder.surfaceView.getHolder();
            if (Sholder != null) {
                int viewWidth = holder.surfaceView.getWidth();
                int viewHeight = holder.surfaceView.getHeight();

                if ((viewWidth != 0) && (viewHeight != 0)) {
                    int holderWidth = viewWidth < width ? viewWidth : width;
                    int holderHeight = holderWidth * viewHeight / viewWidth;
                    if (holderHeight > viewHeight) {
                        holderHeight = viewHeight;
                        holderWidth = holderHeight * viewWidth / viewHeight;
                    }
                    this.bitmap = holder.localRenderer.CreateBitmap(width, height);

                    int marginTop = 50 + ((int)holder.localRenderer.getTopR());
                    int marginRight = 50 + (viewWidth - (int)holder.localRenderer.getRightR());
                    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(holder.microSurface.getLayoutParams());
                    lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                    lp.setMargins(0, marginTop, marginRight, 0);
                    holder.microSurface.setLayoutParams(lp);

                    Sholder.setFixedSize(holderWidth, holderHeight);
                }
                else{
                    this.width = -1;
                    this.height = -1;
                }
            }
        }

        if (bitmap != null) {
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(byteBuffer));

            // Instead of using this WebRTC renderer, we should probably draw the image by ourselves.
            // The renderer has been modified a bit and an update of WebRTC could break our app
            holder.localRenderer.DrawBitmap(false);
        }
    }

    private static void log(String log) {
        Util.log("GroupCallListener", log);
    }

}

