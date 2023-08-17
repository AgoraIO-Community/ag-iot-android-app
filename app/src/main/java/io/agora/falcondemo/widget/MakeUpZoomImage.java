package io.agora.falcondemo.widget;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class MakeUpZoomImage {
    private Activity activity;
    private volatile static MakeUpZoomImage makeUpZoomImage;
    private ImageView imageView;

    private MakeUpZoomImage(Activity activity) {
        this.activity = activity;
        imageView = new ImageView(activity);
        activity.addContentView(imageView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setVisibility(View.GONE);
    }

    public static MakeUpZoomImage attach(Activity activity) {
        if (makeUpZoomImage == null) {
            synchronized (MakeUpZoomImage.class) {
                if (makeUpZoomImage == null) {
                    makeUpZoomImage = new MakeUpZoomImage(activity);
                }
            }
        }
        return makeUpZoomImage;
    }

    public static MakeUpZoomImage get() {
        if (makeUpZoomImage == null) {
            synchronized (MakeUpZoomImage.class) {
                if (makeUpZoomImage == null) {
                    throw new IllegalStateException("Must be initialized");
                }
            }
        }
        return makeUpZoomImage;
    }

    public void release() {
        remove();
        makeUpZoomImage = null;
    }

    public void update(View view, Drawable drawable, double zoom, float tx, float ty) {
        if (view == null) {
            return;
        }
        int[] location = new int[2];
        view.getLocationInWindow(location);

        ViewGroup.LayoutParams lp = imageView.getLayoutParams();
        lp.width = view.getWidth();
        lp.height = view.getHeight();
        imageView.setLayoutParams(lp);
        imageView.setX(location[0] + tx);
        imageView.setY(location[1] - getNotificationBarHeight() + ty);

        imageView.setVisibility(View.VISIBLE);
        imageView.setImageDrawable(drawable);
        imageView.setScaleX((float) zoom);
        imageView.setScaleY((float) zoom);
    }

    private int getNotificationBarHeight() {
        Resources resources = activity.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        int height = resources.getDimensionPixelSize(resourceId);
        return height;
    }

    public void remove() {
        imageView.setVisibility(View.GONE);
        imageView.setImageDrawable(null);
    }
}