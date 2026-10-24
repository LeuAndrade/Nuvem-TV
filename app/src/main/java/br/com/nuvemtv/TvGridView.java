package br.com.nuvemtv;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.GridView;

/** Foco do controle remoto desenhado acima da capa, inclusive após fechar filtros. */
final class TvGridView extends GridView {
    private final Paint outline=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds=new RectF();
    private final float density;
    private View upTarget,leftTarget;
    private int remembered;
    TvGridView(Context context){
        super(context);
        density=getResources().getDisplayMetrics().density;
        setFocusable(true);setFocusableInTouchMode(false);
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
        setDrawSelectorOnTop(true);
        // O seletor nativo é neutro: o contorno abaixo não depende de state_selected da célula.
        setSelector(new ColorDrawable(Color.TRANSPARENT));
        outline.setStyle(Paint.Style.STROKE);outline.setStrokeWidth(3*density);outline.setColor(0xff9bff24);
        edge.setStyle(Paint.Style.STROKE);edge.setStrokeWidth(5*density);edge.setColor(0xff163c18);
        setOnScrollListener(new android.widget.AbsListView.OnScrollListener(){
            public void onScrollStateChanged(android.widget.AbsListView view,int state){invalidate();}
            public void onScroll(android.widget.AbsListView view,int first,int visible,int total){invalidate();}
        });
    }
    void anchors(View up,View left){upTarget=up;leftTarget=left;}
    void focusPosition(int position){
        remembered=Math.max(0,Math.min(position,Math.max(0,getCount()-1)));
        requestFocusFromTouch();setSelection(remembered);
        post(()->{if(hasFocus()){if(getSelectedItemPosition()<0)setSelection(remembered);invalidate();}});
    }
    @Override protected void onFocusChanged(boolean gain,int direction,Rect previous){
        if(!gain&&getSelectedItemPosition()>=0)remembered=getSelectedItemPosition();
        super.onFocusChanged(gain,direction,previous);
        if(gain)post(()->{if(hasFocus()&&getCount()>0){if(getSelectedItemPosition()<0)setSelection(Math.min(remembered,getCount()-1));invalidate();}});
        invalidate();
    }
    @Override public boolean onKeyDown(int key,KeyEvent event){
        int position=Math.max(0,getSelectedItemPosition());int columns=Math.max(1,getNumColumns());
        if(key==KeyEvent.KEYCODE_DPAD_UP&&position<columns&&upTarget!=null){upTarget.requestFocus();return true;}
        if(key==KeyEvent.KEYCODE_DPAD_LEFT&&position%columns==0&&leftTarget!=null){leftTarget.requestFocus();return true;}
        boolean handled=super.onKeyDown(key,event);invalidate();return handled;
    }
    @Override protected void dispatchDraw(Canvas canvas){
        super.dispatchDraw(canvas);
        if(!hasFocus())return;
        View cell=getSelectedView();
        if(cell==null)return;
        float inset=2.5f*density;
        bounds.set(cell.getLeft()+inset,cell.getTop()+inset,cell.getRight()-inset,cell.getBottom()-inset);
        canvas.drawRoundRect(bounds,9*density,9*density,edge);
        canvas.drawRoundRect(bounds,9*density,9*density,outline);
    }
    boolean hasVisibleSelection(){return hasFocus()&&getSelectedView()!=null&&getSelectedItemPosition()>=0;}
}
