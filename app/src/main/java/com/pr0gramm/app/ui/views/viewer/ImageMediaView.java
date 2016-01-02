package com.pr0gramm.app.ui.views.viewer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.pr0gramm.app.ActivityComponent;
import com.pr0gramm.app.BuildConfig;
import com.pr0gramm.app.R;
import com.pr0gramm.app.Settings;
import com.pr0gramm.app.services.SingleShotService;
import com.pr0gramm.app.ui.ImageDecoders;
import com.pr0gramm.app.util.AndroidUtility;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.Picasso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import butterknife.Bind;

/**
 */
@SuppressLint("ViewConstructor")
public class ImageMediaView extends MediaView {
    private static final Logger logger = LoggerFactory.getLogger("ImageMediaView");

    // we cap the image if it is more than 15 times as high as it is wide
    private static final float CAP_IMAGE_RATIO = 1.f / 15.f;

    private final String tag = "ImageMediaView" + System.identityHashCode(this);
    private final boolean zoomView;
    private boolean viewInitialized;

    @Bind(R.id.image)
    SubsamplingScaleImageView imageView;

    @Bind(R.id.error)
    View errorIndicator;

    @Inject
    Settings settings;

    @Inject
    Picasso picasso;

    @Inject
    Downloader downloader;

    @Inject
    SingleShotService singleShotService;


    public ImageMediaView(Activity context, MediaUri url, Runnable onViewListener) {
        super(context, R.layout.player_image, url, onViewListener);

        zoomView = findViewById(R.id.tabletlayout) != null;

        if (zoomView) {
            logger.info("Media view has a zoomview now");
        }

        imageView.setVisibility(INVISIBLE);
        imageView.setDebug(BuildConfig.DEBUG);
        imageView.setZoomEnabled(zoomView);

        imageView.setBitmapDecoderFactory(() -> new ImageDecoders.PicassoDecoder(tag, picasso));
        imageView.setRegionDecoderFactory(() -> new ImageDecoders.PicassoRegionDecoder(downloader));
        imageView.setOnImageEventListener(new SubsamplingScaleImageView.DefaultOnImageEventListener() {
            @Override
            public void onImageLoaded() {
                hideBusyIndicator();
                onMediaShown();
            }

            @Override
            public void onImageLoadError(Exception e) {
                hideBusyIndicator();
                showErrorIndicator();
            }

            @Override
            public void onReady() {
                float ratio = imageView.getSWidth() / (float) imageView.getSHeight();
                float ratioCapped = Math.max(ratio, CAP_IMAGE_RATIO);

                setViewAspect(ratioCapped);

                float maxScale = imageView.getWidth() / (float) imageView.getSWidth();
                float minScale = zoomView
                        ? imageView.getHeight() / (float) imageView.getSHeight()
                        : maxScale * (ratio / ratioCapped);

                imageView.setMinScale(minScale);
                imageView.setMaxScale(maxScale);
                imageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM);
            }
        });
    }

    @Override
    protected void injectComponent(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    public void setViewAspect(float viewAspect) {
        if (!zoomView) {
            super.setViewAspect(viewAspect);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!viewInitialized) {
            imageView.setImage(ImageSource.uri(getEffectiveUri()));
            viewInitialized = true;
        }
    }

    @Override
    public void onTransitionEnds() {
        super.onTransitionEnds();
        imageView.setVisibility(VISIBLE);
    }

    @Override
    public void onDestroy() {
        picasso.cancelTag(tag);
        AndroidUtility.removeView(imageView);

        super.onDestroy();
    }

    private void showErrorIndicator() {
        errorIndicator.setVisibility(VISIBLE);
        errorIndicator.setAlpha(0);
        errorIndicator.animate().alpha(1).start();
    }
}
