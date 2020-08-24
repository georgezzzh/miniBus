package com.sonydafa.minibus;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;


public class MyLine extends Drawable {
    int width;
    int line_len;
    int begin_height;
    int radius;
    //{1:到达,2:将至,3:超出当前站且到达,4:超出当前站且降至,5:不存在}
    int flag;
    //{0:其他,1:开头,2：结尾}
    int begin_Or_end;
    int color;
    Resources res;
    public MyLine(int line_len, int begin_height, int width, int radius, Resources res,int flag,int color,int begin_Or_end){
        this.line_len =line_len;
        this.begin_height = begin_height;
        this.radius = radius;
        this.width = width;
        this.res = res;
        this.flag= flag;
        this.color = color;
        this.begin_Or_end = begin_Or_end;
    }
    @Override
    public void draw(Canvas canvas) {
        //绘制图形，准备
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        //int y = begin_height;
        int y = 0;
        int start_x = 60;
        if(begin_Or_end != 1)
            canvas.drawLine(start_x,y,start_x,line_len/2,paint);
        canvas.drawCircle(start_x,y+line_len/2+radius,radius,paint);
        //offset += width;
        y += 2*radius;
        if(begin_Or_end != 2)
            canvas.drawLine(start_x,y+line_len/2,start_x,line_len,paint);
        Bitmap bitmap =BitmapFactory.decodeResource(res,R.drawable.bus_overtime);
        if(flag==1){
            bitmap = BitmapFactory.decodeResource(res,R.drawable.bus_intime);
        }else if(flag == 2){
            bitmap = BitmapFactory.decodeResource(res,R.drawable.bus_waittime);
        }
        switch (flag){
            case 1:
            case 3:
                 int pic_y =line_len/2-bitmap.getHeight()/2;
                 pic_y = Math.max(pic_y, 0);
                 canvas.drawBitmap(bitmap,0,pic_y,paint);
                 break;
            case 2:
            case 4:
                 canvas.drawBitmap(bitmap,0,0,paint);
                 break;
        }

    }

    @Override
    public void setAlpha(int i) {

    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }
}
