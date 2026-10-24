package br.com.nuvemtv;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Ícones geométricos locais, sem fonte de ícones, downloads ou bitmaps extras. */
final class TvIcon extends Drawable {
    private final Paint pen=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String kind;private final int size;
    TvIcon(String kind,int color,int size){this.kind=kind;this.size=size;pen.setColor(color);pen.setStrokeWidth(1.8f);pen.setStrokeCap(Paint.Cap.ROUND);pen.setStrokeJoin(Paint.Join.ROUND);setBounds(0,0,size,size);}
    @Override public int getIntrinsicWidth(){return size;}
    @Override public int getIntrinsicHeight(){return size;}
    @Override public void draw(Canvas c){
        int save=c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/24f,getBounds().height()/24f);pen.setStyle(Paint.Style.STROKE);
        Path p=new Path();
        switch(kind){
            case "home":p.moveTo(3,11);p.lineTo(12,3);p.lineTo(21,11);p.moveTo(6,10);p.lineTo(6,21);p.lineTo(10,21);p.lineTo(10,15);p.lineTo(14,15);p.lineTo(14,21);p.lineTo(18,21);p.lineTo(18,10);c.drawPath(p,pen);break;
            case "search":c.drawCircle(10,10,6,pen);c.drawLine(15,15,21,21,pen);break;
            case "settings":c.drawCircle(12,12,6,pen);c.drawCircle(12,12,2,pen);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine(12+(float)Math.cos(a)*7,12+(float)Math.sin(a)*7,12+(float)Math.cos(a)*9,12+(float)Math.sin(a)*9,pen);}break;
            case "favorite":p.moveTo(6,3);p.lineTo(18,3);p.lineTo(18,21);p.lineTo(12,17);p.lineTo(6,21);p.close();c.drawPath(p,pen);break;
            case "library":c.drawRoundRect(3,4,10,21,1,1,pen);c.drawRoundRect(14,4,21,21,1,1,pen);c.drawLine(6,8,7,8,pen);c.drawLine(17,8,18,8,pen);break;
            case "account":c.drawCircle(12,8,4,pen);c.drawArc(4,12,20,27,190,160,false,pen);break;
            case "play":pen.setStyle(Paint.Style.FILL);p.moveTo(6,3);p.lineTo(21,12);p.lineTo(6,21);p.close();c.drawPath(p,pen);break;
            case "help":c.drawCircle(12,12,9,pen);c.drawArc(8,5,16,13,190,250,false,pen);c.drawLine(12,12,12,14,pen);c.drawPoint(12,18,pen);break;
            default:c.drawRoundRect(2,6,22,19,5,5,pen);c.drawLine(6,12,11,12,pen);c.drawLine(8.5f,9.5f,8.5f,14.5f,pen);c.drawPoint(16,10,pen);c.drawPoint(19,14,pen);break;
        }
        c.restoreToCount(save);
    }
    @Override public void setAlpha(int alpha){pen.setAlpha(alpha);}
    @Override public void setColorFilter(ColorFilter filter){pen.setColorFilter(filter);}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
