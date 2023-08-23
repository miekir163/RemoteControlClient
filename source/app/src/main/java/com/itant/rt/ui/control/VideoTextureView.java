package com.itant.rt.ui.control;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;

import com.miekir.mvp.common.log.L;

/**
 * 视频通话时显示视频，解决了从后台返回视频变黑的问题
 */
public class VideoTextureView extends TextureView implements TextureView.SurfaceTextureListener {

    private static final String TAG = VideoTextureView.class.getSimpleName();

    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private ITextureViewLifeCycleListener itextureViewLifeCycleListener;

    public VideoTextureView(Context context) {
        this(context, null);
    }

    public VideoTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mSurfaceTexture != null) {
            setSurfaceTexture(mSurfaceTexture);
        } else {
            mSurfaceTexture = surface;
        }
        if (itextureViewLifeCycleListener != null) {
            itextureViewLifeCycleListener.available();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mSurfaceTexture = surface;
        if (itextureViewLifeCycleListener != null) {
            itextureViewLifeCycleListener.destroy();
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        L.d(TAG, "onDetachedFromWindow");
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
    }

    @Override
    public SurfaceTexture getSurfaceTexture() {
        if (mSurfaceTexture != null) {
            return mSurfaceTexture;
        }
        return super.getSurfaceTexture();
    }

    public Surface getSurface() {
        if (mSurface == null) {
            if (getSurfaceTexture() != null) {
                mSurface = new Surface(getSurfaceTexture());
            }
        }
        return mSurface;
    }


    public void setLifeCycleListener(ITextureViewLifeCycleListener itextureViewLifeCycleListener) {
        this.itextureViewLifeCycleListener = itextureViewLifeCycleListener;
    }

    public interface ITextureViewLifeCycleListener {
        void available();

        void destroy();
    }

    public void drawBitmap(Bitmap bitmap, Paint bitmapPaint) {
        //锁定画布
        Canvas canvas = lockCanvas();
        if (canvas != null) {
            // 先清空画布再画
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            // 将bitmap画到画布上
            canvas.drawBitmap(bitmap, 0f, 0f, bitmapPaint);
            // 解锁画布同时提交
            unlockCanvasAndPost(canvas);
        }
    }
}
