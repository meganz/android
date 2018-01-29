/**
 * Copyright 2016 Bartosz Schiller
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.barteksc.pdfviewer;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.PointF;
import android.view.animation.DecelerateInterpolator;
import android.widget.OverScroller;


/**
 * This manager is used by the PDFView to launch animations.
 * It uses the ValueAnimator appeared in API 11 to start
 * an animation, and call moveTo() on the PDFView as a result
 * of each animation update.
 */
class AnimationManager {

    private PDFView pdfView;

    private ValueAnimator animation;

    private OverScroller scroller;

    private boolean flinging = false;

    public AnimationManager(PDFView pdfView) {
        log("AnimationManager");
        this.pdfView = pdfView;
        scroller = new OverScroller(pdfView.getContext());
    }

    public void startXAnimation(float xFrom, float xTo) {
        log("startXAnimation");
        stopAll();
        animation = ValueAnimator.ofFloat(xFrom, xTo);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.addUpdateListener(new XAnimation());
        animation.setDuration(400);
        animation.start();
    }

    public void startYAnimation(float yFrom, float yTo) {
        log("startYAnimation");
        stopAll();
        animation = ValueAnimator.ofFloat(yFrom, yTo);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.addUpdateListener(new YAnimation());
        animation.setDuration(400);
        animation.start();
    }

    public void startZoomAnimation(float centerX, float centerY, float zoomFrom, float zoomTo) {
        log("startZoomAnimation");
        stopAll();
        animation = ValueAnimator.ofFloat(zoomFrom, zoomTo);
        animation.setInterpolator(new DecelerateInterpolator());
        ZoomAnimation zoomAnim = new ZoomAnimation(centerX, centerY);
        animation.addUpdateListener(zoomAnim);
        animation.addListener(zoomAnim);
        animation.setDuration(400);
        animation.start();
    }

    public void startFlingAnimation(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY) {
        log("startFlingAnimation");
        stopAll();
        flinging = true;
        scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY);
    }

    void computeFling() {
        log("computeFling");
        if (scroller.computeScrollOffset()) {
            pdfView.moveTo(scroller.getCurrX(), scroller.getCurrY());
            pdfView.loadPageByOffset();
        } else if (flinging) { // fling finished
            flinging = false;
            pdfView.loadPages();
            hideHandle();
        }
    }

    public void stopAll() {
        log("stopAll");
        if (animation != null) {
            animation.cancel();
            animation = null;
        }
        stopFling();
    }

    public void stopFling() {
        log("stopFling");
        flinging = false;
        scroller.forceFinished(true);
    }

    class XAnimation implements AnimatorUpdateListener {

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            log("onAnimationUpdate");
            float offset = (Float) animation.getAnimatedValue();
            pdfView.moveTo(offset, pdfView.getCurrentYOffset());
        }

    }

    class YAnimation implements AnimatorUpdateListener {

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            log("onAnimationUpdate");
            float offset = (Float) animation.getAnimatedValue();
            pdfView.moveTo(pdfView.getCurrentXOffset(), offset);
        }

    }

    class ZoomAnimation implements AnimatorUpdateListener, AnimatorListener {

        private final float centerX;
        private final float centerY;

        public ZoomAnimation(float centerX, float centerY) {
            log("ZoomAnimation");
            this.centerX = centerX;
            this.centerY = centerY;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            log("onAnimationUpdate");
            float zoom = (Float) animation.getAnimatedValue();
            pdfView.zoomCenteredTo(zoom, new PointF(centerX, centerY));
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            log("onAnimationCancel");
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            log("onAnimationEnd");
            pdfView.loadPages();
            hideHandle();
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
            log("onAnimationRepeat");
        }

        @Override
        public void onAnimationStart(Animator animation) {
            log("onAnimationStart");
        }

    }

    private void hideHandle() {
        log("hideHandle");
        if (pdfView.getScrollHandle() != null) {
            pdfView.getScrollHandle().hideDelayed();
        }
    }
    public static void log(String log) {
        mega.privacy.android.app.utils.Util.log("AnimationManager: ", log);
    }

}
