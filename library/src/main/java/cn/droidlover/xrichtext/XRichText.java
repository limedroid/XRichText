package cn.droidlover.xrichtext;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wanglei on 2016/11/02.
 */

public class XRichText extends TextView implements ViewTreeObserver.OnGlobalLayoutListener {
    private static Pattern PATTERN_IMG_TAG = Pattern.compile("\\<img(.*?)\\>");
    private static Pattern PATTERN_IMG_WIDTH = Pattern.compile("width=\"(.*?)\"");
    private static Pattern PATTERN_IMG_HEIGHT = Pattern.compile("height=\"(.*?)\"");
    private static Pattern PATTERN_IMG_SRC = Pattern.compile("src=\"(.*?)\"");

    private HashMap<String, ImageHolder> imageHolderMap = new HashMap<String, ImageHolder>();

    Callback callback;
    ImageLoader downLoader;

    private int richWidth;   //控件的宽高
    private boolean isInit = true;
    LocalImageGetter imgGetter;

    public XRichText(Context context) {
        super(context);
    }

    public XRichText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public XRichText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public void text(String text) {
        queryImgs(text);

        if (imgGetter == null) {
            imgGetter = new LocalImageGetter();
        }
        Spanned spanned = Html.fromHtml(text, imgGetter, null);

        SpannableStringBuilder builder;
        if (spanned instanceof SpannableStringBuilder) {
            builder = (SpannableStringBuilder) spanned;
        } else {
            builder = new SpannableStringBuilder(spanned);
        }

        ImageSpan[] imgSpans = builder.getSpans(0, builder.length(), ImageSpan.class);
        final List<String> imgUrls = new ArrayList<String>();

        for (int i = 0, size = imgSpans.length; i < size; i++) {

            ImageSpan span = imgSpans[i];
            String path = span.getSource();
            int start = builder.getSpanStart(span);
            int end = builder.getSpanEnd(span);
            imgUrls.add(path);

            final int position = i;
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    if (callback != null) {
                        callback.onImageClick((ArrayList<String>) imgUrls, position);
                    }
                }
            };

            ClickableSpan[] clickableSpans = builder.getSpans(start, end, ClickableSpan.class);
            if (clickableSpans != null && clickableSpans.length != 0) {
                for (ClickableSpan cs : clickableSpans) {
                    builder.removeSpan(cs);
                }
            }
            builder.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        }

        // 处理超链接点击事件
        URLSpan[] urlSpans = builder.getSpans(0, builder.length(), URLSpan.class);

        for (int i = 0, size = urlSpans == null ? 0 : urlSpans.length; i < size; i++) {
            URLSpan span = urlSpans[i];

            int start = builder.getSpanStart(span);
            int end = builder.getSpanEnd(span);

            builder.removeSpan(span);
            builder.setSpan(new LinkSpan(span.getURL(), callback), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        super.setText(spanned);
        setMovementMethod(LinkMovementMethod.getInstance());
    }


    /**
     * 查询图片
     *
     * @param text
     */
    private void queryImgs(String text) {
        ImageHolder holder = null;
        Matcher imgMatcher, srcMatcher, wMatcher, hMatcher;
        int position = 0;

        imgMatcher = PATTERN_IMG_TAG.matcher(text);

        while (imgMatcher.find()) {
            String img = imgMatcher.group().trim();
            srcMatcher = PATTERN_IMG_SRC.matcher(img);

            String src = null;
            if (srcMatcher.find()) {
                src = getTextBetweenQuotation(srcMatcher.group().trim().substring(4));
            }
            if (TextUtils.isEmpty(src)) {
                continue;
            }

            holder = new ImageHolder(src, position);

            wMatcher = PATTERN_IMG_WIDTH.matcher(img);
            if (wMatcher.find()) {
                holder.setWidth(str2Int(getTextBetweenQuotation(wMatcher.group().trim().substring(6))));
            }

            hMatcher = PATTERN_IMG_HEIGHT.matcher(img);
            if (hMatcher.find()) {
                holder.setHeight(str2Int(getTextBetweenQuotation(hMatcher.group().trim().substring(6))));
            }

            imageHolderMap.put(holder.src, holder);
            position++;
        }
    }


    /**
     * 当获取到bitmap后调用此方法
     *
     * @param drawable
     * @param holder
     * @param rawBmp
     */
    public void fillBmp(UrlDrawable drawable, ImageHolder holder, Bitmap rawBmp) {
        if (drawable == null || holder == null || rawBmp == null || richWidth <= 0) {
            return;
        }
        if (callback != null) {
            callback.onFix(holder);
        }

        Bitmap destBmp = holder.valid(rawBmp, richWidth);
        if (destBmp == null) {
            return;
        }
        wrapDrawable(drawable, holder, destBmp);
    }

    private void wrapDrawable(UrlDrawable drawable, ImageHolder holder, Bitmap destBmp) {
        if (destBmp.getWidth() > richWidth) return;

        Rect rect = null;
        int left = 0;
        switch (holder.style) {
            case LEFT:
                rect = new Rect(left, 0, destBmp.getWidth(), destBmp.getHeight());
                break;

            case CENTER:
                left = (richWidth - destBmp.getWidth()) / 2;
                if (left < 0) left = 0;
                rect = new Rect(left, 0, left + destBmp.getWidth(), destBmp.getHeight());
                break;

            case RIGHT:
                left = richWidth - destBmp.getWidth();
                if (left < 0) left = 0;
                rect = new Rect(left, 0, richWidth, destBmp.getHeight());
                break;
        }
        drawable.setBounds(0, 0, destBmp.getWidth(), destBmp.getHeight());
        drawable.setBitmap(destBmp, rect);
        setText(getText());
    }

    /**
     * 从双引号之间取出字符串
     */
    private static String getTextBetweenQuotation(String text) {
        Pattern pattern = Pattern.compile("\"(.*?)\"");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 将String转化成Int
     *
     * @param text
     * @return
     */
    private int str2Int(String text) {
        int result = -1;
        try {
            result = Integer.valueOf(text);
        } catch (Exception e) {
        }
        return result;
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    @TargetApi(16)
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    /**
     * 设置回调
     *
     * @param callback
     * @return
     */
    public XRichText callback(Callback callback) {
        this.callback = callback;
        return this;
    }

    /**
     * 设置自定义图片下载器
     *
     * @param loader
     * @return
     */
    public XRichText imageDownloader(ImageLoader loader) {
        this.downLoader = loader;
        return this;
    }

    @Override
    public void onGlobalLayout() {
        if (isInit) {
            richWidth = getWidth() - getPaddingLeft() - getPaddingRight();
            if (richWidth > 0) isInit = false;
        }
    }


    private class LocalImageGetter implements Html.ImageGetter {

        @Override
        public Drawable getDrawable(String source) {
            final UrlDrawable urlDrawable = new UrlDrawable();

            final ImageHolder holder = imageHolderMap.get(source);
            if (holder == null) return null;

            if (downLoader == null) {
                downLoader = new BaseImageLoader(getContext());
            }

            Runnable loadRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        final Bitmap rawBmp = downLoader.getBitmap(holder.getSrc());
                        if (rawBmp != null) {
                            ILoad loadImpl = new ILoad() {
                                @Override
                                public void afterLoad() {
                                    fillBmp(urlDrawable, holder, rawBmp);
                                }
                            };
                            LoaderTask.getMainHandler().obtainMessage(LoaderTask.MSG_POST_RESULT, loadImpl).sendToTarget();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            };
            LoaderTask.getThreadPoolExecutor().execute(loadRunnable);

            return urlDrawable;
        }
    }


    public interface ILoad {
        void afterLoad();
    }


    public static class ImageHolder {
        private String src;
        private int position;
        private int width = -1;
        private int height = -1;
        private Style style = Style.CENTER;

        public ImageHolder(String src, int position) {
            this.src = src;
            this.position = position;
        }

        public String getSrc() {
            return src;
        }

        public void setSrc(String src) {
            this.src = src;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public Style getStyle() {
            return style;
        }

        public void setStyle(Style style) {
            this.style = style;
        }

        /**
         * 修正参数
         *
         * @param rawBmp
         * @return
         */
        public Bitmap valid(Bitmap rawBmp, int maxWidth) {
            if (rawBmp == null) return null;

            int reqWidth = width;
            int reqHeight = height;

            if (reqWidth == -1 || reqHeight == -1) {
                reqWidth = rawBmp.getWidth();
                reqHeight = rawBmp.getHeight();
            }

            if (reqWidth >= maxWidth) {
                float ratio = maxWidth * 1.0f / reqWidth;
                reqWidth = maxWidth;
                reqHeight = (int) (reqHeight * ratio);
            }

            width = reqWidth;
            height = reqHeight;
            return Kit.scaleImageTo(rawBmp, reqWidth, reqHeight, false);
        }
    }

    public enum Style {
        LEFT,       //左对齐
        CENTER,     //居中
        RIGHT       //右对齐
    }

    private static class Kit {

        private static Bitmap scaleImageTo(Bitmap org, int newWidth, int newHeight, boolean needRecycle) {
            return scaleImage(org, (float) newWidth / org.getWidth(), (float) newHeight / org.getHeight(), needRecycle);
        }

        private static Bitmap scaleImage(Bitmap org, float scaleWidth, float scaleHeight, boolean needRecycle) {
            if (org == null) {
                return null;
            }

            Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleHeight);
            Bitmap bitmap = Bitmap.createBitmap(org, 0, 0, org.getWidth(), org.getHeight(), matrix, true);

            if (needRecycle && !org.isRecycled()) {
                org.recycle();
            }
            return bitmap;
        }
    }

    public static class LinkSpan extends URLSpan {

        private Callback callback;

        public LinkSpan(String url, Callback callback) {
            super(url);
            this.callback = callback;
        }

        @Override
        public void onClick(View widget) {
            if (callback != null && callback.onLinkClick(getURL()))
                return;
            super.onClick(widget);
        }
    }

    public static class UrlDrawable extends BitmapDrawable {
        private Bitmap bitmap;
        private Rect rect;
        private Paint paint;

        public UrlDrawable() {
            paint = new Paint();
        }

        @Override
        public void draw(Canvas canvas) {
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, rect.left, rect.top, paint);
            }
        }

        public void setBitmap(Bitmap bitmap, Rect rect) {
            this.bitmap = bitmap;
            this.rect = rect;
        }
    }

    public interface Callback {
        void onImageClick(List<String> urlList, int position);

        boolean onLinkClick(String url);

        void onFix(ImageHolder holder);
    }


    public static class BaseClickCallback implements Callback {

        @Override
        public void onImageClick(List<String> urlList, int position) {

        }

        @Override
        public boolean onLinkClick(String url) {
            return false;
        }

        @Override
        public void onFix(ImageHolder holder) {

        }
    }


}
