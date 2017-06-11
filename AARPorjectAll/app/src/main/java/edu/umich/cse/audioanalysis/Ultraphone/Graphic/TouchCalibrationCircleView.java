package edu.umich.cse.audioanalysis.Ultraphone.Graphic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by eddyxd on 11/19/15.
 * 2016/05/19: add function to show text
 * 2016/05/21: decide to use this class to store calib values
 */
public class TouchCalibrationCircleView extends View {
    Paint paintOutline, paintColorIdle, paintColorPressed, paintText, paintColorTrained;
    public Point start = null;
    private Point cursorAtMouseDown = null;
    private Point startAtMouseDown = null;
    private Point endAtMouseDown = null;
    private boolean movingStart = false;
    private boolean movingEnd = false;
    private boolean movingLine = false;
    public boolean pressed = false;
    private int radius;

    // run-time calibration related variables
    public int distSquare = 0; // square of distance to the touch point
    public double calibWeight = 0.0;

    // class used to store calibration information
    double CALIB_RATIO_USED_DEFAULT = 1.0;
    List<Double> calibRatioList;
    double calibRatioSum;
    String calibText;
    public double calibRatioUsed = CALIB_RATIO_USED_DEFAULT; // init value
    boolean isTrained;

    public TouchCalibrationCircleView(Context context) {
        super(context);
        init();
    }

    public TouchCalibrationCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TouchCalibrationCircleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    // default setting to avoid null point error
    private void init() {
        start = new Point(100, 100);
        radius = 80;

        calibText = "None";
        isTrained = false;

        // paints init
        paintOutline = new Paint();
        paintOutline.setColor(Color.GRAY);
        paintOutline.setStrokeWidth(10);
        paintOutline.setStyle(Paint.Style.STROKE);

        paintColorIdle = new Paint();
        paintColorIdle.setColor(Color.YELLOW);
        paintColorIdle.setStyle(Paint.Style.FILL);

        paintColorPressed = new Paint();
        paintColorPressed.setColor(Color.RED);
        paintColorPressed.setStyle(Paint.Style.FILL);

        paintColorTrained = new Paint();
        paintColorTrained.setColor(Color.GREEN);
        paintColorTrained.setStyle(Paint.Style.FILL);

        paintText = new Paint();
        paintText.setColor(Color.BLACK);
        paintText.setTextSize(40);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setAntiAlias(true);


        calibRatioList = new ArrayList<>();
    }

    public void updateStartPoint(Point p){
        start = p;
        this.invalidate();
    }

    public void updateRadius(int r){
        radius = r;
        this.invalidate();
    }

    public boolean contain(int x,int y){
        double dis = Math.sqrt((x-start.x)*(x-start.x) + (y-start.y)*(y-start.y));
        return dis < radius;
    }

    public void moveBallByScale(double scale){
        start = new Point(100, (int)(200*scale)+100);
        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(!pressed){ // normal
            if(!isTrained) { // not trained yet
                canvas.drawCircle(start.x, start.y, radius, paintColorIdle);
            } else { // trained
                canvas.drawCircle(start.x, start.y, radius, paintColorTrained);
            }
        } else { // pressed
            canvas.drawCircle(start.x, start.y, radius*2, paintColorPressed); // increase to indicate the press clearly
        }

        canvas.drawCircle(start.x, start.y, radius, paintOutline);
        //canvas.drawCircle(start.x, start.y, 10, paint);

        // show calib texts
        canvas.drawText(calibText, start.x, start.y, paintText);
    }

    public void clearCalibRatioAndUpdateRatio(double ratioIn){
        calibRatioList.clear();
        calibRatioSum = 0;
        addCalibRatio(ratioIn);
    }

    // calibration functions
    public void addCalibRatio(double ratioIn){
        calibRatioList.add(new Double(ratioIn));
        calibRatioSum += ratioIn;
    }

    // update calib info based on the current array data
    public void estimateAndUpdateCalibRatio(){
        if(calibRatioList.size() == 0){
            // no thing inputted yet -> keep it as the default value
            calibRatioUsed = CALIB_RATIO_USED_DEFAULT;
        } else {
            // use mean value as the data
            calibRatioUsed = calibRatioSum/((double)calibRatioList.size());
            isTrained = true;
        }

        calibText = String.format("%.2f/%d", calibRatioUsed, calibRatioList.size());


        this.invalidate();
    }

    // TODO: make different calib method
    public double calib(double data){
        return data*calibRatioUsed;
    }

}
