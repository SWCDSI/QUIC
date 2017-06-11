package edu.umich.cse.audioanalysis.Ultraphone.Graphic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by eddyxd on 11/19/15.
 */
public class CircleView extends View {


    Paint paint, paintSmall;
    private Point start = null;
    private Point cursorAtMouseDown = null;
    private Point startAtMouseDown = null;
    private Point endAtMouseDown = null;
    private boolean movingStart = false;
    private boolean movingEnd = false;
    private boolean movingLine = false;


    public CircleView(Context context) {
        super(context);
        init();
    }

    public CircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint = new Paint();
        start = new Point(100, 100);
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);
    }

    public void updateStartPoint(Point p){
        start = p;
        this.invalidate();
    }

    public void updateColor(int colorCode){ // colorCode, ex: Color.BLUE
        paint.setColor(colorCode);
    }


    public void moveBallByScale(double scale){
        start = new Point(100, (int)(200*scale)+100);
        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(start.x, start.y, 80, paint);

        canvas.drawCircle(start.x, start.y, 10, paint);

    }

}
