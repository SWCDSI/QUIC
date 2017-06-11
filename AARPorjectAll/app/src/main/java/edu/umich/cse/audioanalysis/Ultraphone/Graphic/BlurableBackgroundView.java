package edu.umich.cse.audioanalysis.Ultraphone.Graphic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

import edu.umich.cse.audioanalysis.C;

/**
 * Created by eddyxd on 6/12/16.
 */
public class BlurableBackgroundView extends View {

    Bitmap bitmapOri = null;
    Bitmap bitmapBlurred = null;
    double blurScale = 0.0;

    public BlurableBackgroundView(Context context) {
        super(context);
        init();
    }

    public BlurableBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BlurableBackgroundView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }


    // default setting to avoid null point error
    private void init() {

    }

    public void setUpBitmap(Bitmap bitmap){
        this.bitmapOri = bitmap;
        this.bitmapBlurred = bitmap.copy(bitmapOri.getConfig(), true);

        w = bitmapOri.getWidth();
        h = bitmapOri.getHeight();
        pix = new int[w * h];

        // *** just for debug ***
        blurScale = 0.5;
        fastblur(30);

        this.invalidate();
    }

    static double BLUR_MIN_SHRINK_SCALE = 0.3;
    public void setBlurScale(double scale){
        /*
        blurScale = scale;
        // build blurred image
        if(scale > 0.0) {
            double scaleToShrink = BLUR_MIN_SHRINK_SCALE;
            if(scale<0.99){
                scaleToShrink = BLUR_MIN_SHRINK_SCALE+ (1.0-BLUR_MIN_SHRINK_SCALE)*(1-scale);
            }

            int shrinkedWidth = (int)(bitmap.getWidth()*scaleToShrink);
            int shrinkedHeight = (int)(bitmap.getHeight()*scaleToShrink);
            Bitmap bitmapShrinked = Bitmap.createScaledBitmap(bitmap, shrinkedWidth, shrinkedHeight, true);
            bitmapBlurred = Bitmap.createScaledBitmap(bitmapShrinked, bitmap.getWidth(), bitmap.getHeight(), true);

            fastblur(100);

        } else {

        }
        this.invalidate();
        */
    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(bitmapOri!=null && blurScale <= 0.000001){
            canvas.drawBitmap(bitmapOri, 0, 0, new Paint());
        } else {
            if(bitmapBlurred!=null){
                canvas.drawBitmap(bitmapBlurred, 0, 0, new Paint());
            }
        }
    }

    // ref: http://stackoverflow.com/questions/2067955/fast-bitmap-blur-for-android-sdk
    int w, h;
    int[] pix;
    public void fastblur(int radius) {
        //Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        Log.e(C.LOG_TAG, w + " " + h + " " + pix.length);
        bitmapOri.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = ( 0xff000000 & pix[yi] ) | ( dv[rsum] << 16 ) | ( dv[gsum] << 8 ) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        Log.e("pix", w + " " + h + " " + pix.length);
        bitmapBlurred.setPixels(pix, 0, w, 0, 0, w, h);
    }
}
