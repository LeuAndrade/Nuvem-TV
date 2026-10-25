package br.com.nuvemtv;

import android.view.*;
import android.widget.*;
import android.text.TextUtils;
import android.graphics.Typeface;
import org.json.*;
import java.util.*;

/** Carrossel com quatro células reaproveitadas, mesmo quando a lista contém centenas de jogos. */
final class TvShelf extends LinearLayout {
    final MainActivity activity;final String key;final JSONArray items;final boolean genres;
    final List<LinearLayout> cells=new ArrayList<>();final List<ImageView> covers=new ArrayList<>();final List<TextView> titles=new ArrayList<>();
    final TextView count;int first,index;TvShelf previous,next;View top;
    TvShelf(MainActivity a,String key,String label,JSONArray items,boolean genres){
        super(a);activity=a;this.key=key;this.items=items;this.genres=genres;setOrientation(VERTICAL);setPadding(a.dp(2),a.dp(8),a.dp(2),a.dp(7));
        LinearLayout heading=new LinearLayout(a);heading.setGravity(Gravity.CENTER_VERTICAL);TextView title=a.text(label,17);title.setTypeface(null,Typeface.BOLD);heading.addView(title,new LayoutParams(0,-2,1));
        count=a.text("",10);count.setTextColor(MainActivity.MUTED);heading.addView(count);
        Button left=a.button("‹",()->focusAt(index-1)),right=a.button("›",()->focusAt(index+1));heading.addView(left,new LayoutParams(a.dp(32),a.dp(28)));heading.addView(right,new LayoutParams(a.dp(32),a.dp(28)));addView(heading);
        if(items.length()==0){TextView empty=a.text(a.feed.optLong("updatedAt")==0||a.feed.optBoolean("partial")?"Lista temporariamente indisponível. Tentaremos novamente automaticamente.":"Nenhum jogo disponível nesta categoria agora.",12);empty.setTextColor(MainActivity.MUTED);addView(empty);return;}
        LinearLayout row=new LinearLayout(a);addView(row,new LayoutParams(-1,-2));int slots=Math.min(items.length(),Math.max(3,Math.min(4,(a.viewportWidth()-220)/145)));
        for(int i=0;i<slots;i++){
            final int slot=i;LinearLayout cell=a.column();cell.setId(View.generateViewId());cell.setFocusable(true);cell.setFocusableInTouchMode(true);cell.setBackground(a.focus());cell.setPadding(a.dp(4),a.dp(4),a.dp(4),a.dp(4));
            LayoutParams size=new LayoutParams(0,-2,1);size.setMargins(a.dp(3),a.dp(4),a.dp(3),a.dp(4));row.addView(cell,size);cells.add(cell);
            ImageView cover=new ImageView(a);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);if(!genres)cell.addView(cover,new LayoutParams(-1,a.dp(82)));covers.add(cover);
            TextView name=a.text("",genres?14:11);name.setTypeface(null,genres?Typeface.BOLD:Typeface.NORMAL);name.setMaxLines(genres?2:1);name.setEllipsize(TextUtils.TruncateAt.END);name.setGravity(Gravity.CENTER);cell.addView(name,new LayoutParams(-1,a.dp(genres?76:29)));titles.add(name);
            cell.setOnClickListener(v->{int at=first+slot;JSONObject game=items.optJSONObject(at);if(game==null)return;if(genres)a.openGenre(game.optString("name"));else a.details(game);});
            cell.setOnFocusChangeListener((v,has)->{if(has){index=first+slot;cell.setTag("shelf:"+key);remember();count.setText((index+1)+" / "+items.length());}});
            cell.setOnKeyListener((v,k,e)->{if(e.getAction()!=KeyEvent.ACTION_DOWN)return false;index=first+slot;
                if(k==KeyEvent.KEYCODE_DPAD_RIGHT){focusAt(index+1);return true;}
                if(k==KeyEvent.KEYCODE_DPAD_LEFT){if(index==0)a.navButtons.get("home").requestFocus();else focusAt(index-1);return true;}
                if(k==KeyEvent.KEYCODE_DPAD_DOWN){if(next!=null)next.focusAt(Math.min(index,next.items.length()-1));return true;}
                if(k==KeyEvent.KEYCODE_DPAD_UP){if(previous!=null)previous.focusAt(Math.min(index,previous.items.length()-1));else if(top!=null)top.requestFocus();return true;}return false;});
        }
        index=Math.max(0,Math.min(a.shelfPositions.getOrDefault(key,0),items.length()-1));first=Math.min(index,Math.max(0,items.length()-cells.size()));bind();
    }
    void remember(){activity.shelfPositions.put(key,index);activity.homeFocus="shelf:"+key;}
    void bind(){for(int i=0;i<cells.size();i++){JSONObject game=items.optJSONObject(first+i);titles.get(i).setText(game==null?"":game.optString("name"));cells.get(i).setContentDescription(game==null?"":game.optString("name"));if(!genres)activity.images.bind(covers.get(i),game==null?"":game.optString("cover"));}count.setText((index+1)+" / "+items.length());}
    void focusAt(int desired){if(cells.isEmpty()){if(next!=null)next.focusAt(0);else if(top!=null)top.requestFocus();return;}index=Math.max(0,Math.min(desired,items.length()-1));int old=first;if(index<first)first=index;if(index>=first+cells.size())first=index-cells.size()+1;if(old!=first)bind();remember();cells.get(index-first).requestFocus();count.setText((index+1)+" / "+items.length());}
}
