package edu.umich.cse.audioanalysis.Ultraphone.Graphic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by eddyxd on 11/19/15.
 */
public class BallMovingView extends View {
    Rect viewSize;
    float gridOffset;

    int boxCnt;
    int selectIdx;
    int ballRadius;
    Paint paintBoxNormal;
    Paint paintBoxSelected;
    Paint paintBall;
    private Point ballOrigin = null;

    ArrayList<Rect> boxRects;


    public BallMovingView(Context context) {
        super(context);
        init();
    }

    public BallMovingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BallMovingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        boxCnt = 0;
        selectIdx = 0;


        float density = getResources().getDisplayMetrics().density;

        gridOffset = 5*density;
        ballRadius = (int)(25*density);


        viewSize = new Rect(0,0,(int)(120*density),(int)(450*density)); // NOTE: this setting needs to compile with the layout setting
        setBackgroundColor(Color.GRAY);

        boxRects = new ArrayList<Rect>();

        ballOrigin = new Point(0, 0);

        // init pants

        paintBall = new Paint();
        paintBall.setStyle(Paint.Style.FILL);
        //paintBall.setStyle(Paint.Style.FILL_AND_STROKE);
        //paintBall.setColor(Color.parseColor("#00B2FF"));
        paintBall.setColor(Color.WHITE);
        paintBall.setStrokeWidth(20);

        paintBoxNormal = new Paint();
        paintBoxNormal.setStyle(Paint.Style.FILL);
        paintBoxNormal.setColor(Color.GREEN);

        paintBoxSelected = new Paint();
        paintBoxSelected.setStyle(Paint.Style.FILL);
        paintBoxSelected.setColor(Color.RED);

    }

    public void showBoxs(int boxCnt, int selectIdx){
        // remove old boxs
        for (int i=0; i<boxRects.size(); i++) {
            // dont need to anything here
        }
        boxRects.clear();


        // add new boxs
        for (int i=0; i<boxCnt; i++) {
            int boxOffsetY = viewSize.height()/boxCnt;
            int boxW =(int)( viewSize.width() - gridOffset*2);
            int boxH =(int)( viewSize.height()/boxCnt - gridOffset*2);
            int boxX =(int)( gridOffset);
            int boxY =(int)( boxOffsetY*i+gridOffset);

            Rect r = new Rect(boxX, boxY, boxX+boxW, boxY+boxH);
            boxRects.add(r);
        }

        moveBallByScale(0);


        this.boxCnt = boxCnt;
        this.selectIdx = selectIdx;

        this.invalidate();
    }

    public void moveBallByScale(double scale){
        int maxY =(int)( viewSize.height()-ballRadius-gridOffset);
        int minY =(int)( ballRadius+gridOffset);

        int y = (int)( viewSize.height()*(1-scale));
        y = Math.max(y, minY);
        y = Math.min(y, maxY);


        ballOrigin = new Point(viewSize.width()/2, y);

        this.invalidate();
    }


    public boolean isBallInTheSelectedIdx() {
        if (selectIdx >= 0){
            Rect r = boxRects.get(selectIdx);
            return r.contains(ballOrigin.x, ballOrigin.y);
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);


        for (int i=0;i<boxRects.size();i++){
            if (i==selectIdx){
                canvas.drawRect(boxRects.get(i), paintBoxSelected);
            } else {
                canvas.drawRect(boxRects.get(i), paintBoxNormal);
            }
        }

        canvas.drawCircle(ballOrigin.x, ballOrigin.y, ballRadius, paintBall);

    }





}
