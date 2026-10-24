package br.com.nuvemtv;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.content.res.ColorStateList;
import android.view.*;
import android.widget.*;
import android.text.*;
import android.media.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    static final int BG=0xff090e15, PANEL=0xff18232f, GREEN=0xff72edab, WHITE=0xfff2f6f5, MUTED=0xffa6b5bd;
    static final int PAGE_SIZE=60;
    final ExecutorService io=Executors.newSingleThreadExecutor();
    final Handler ui=new Handler(Looper.getMainLooper());
    final Pad pad=new Pad();
    android.content.SharedPreferences prefs;
    Auth auth;
    CoverLoader images;
    CatalogCache cache;
    LinearLayout root,body,tabs;
    TextView notice,hud,counter;
    Button accountButton;
    GridView grid;
    GameAdapter adapter;
    NativePlayer nativePlayer;
    WebPlayer webPlayer;
    AlertDialog loginDialog;
    JSONArray catalog=new JSONArray(),recent=new JSONArray();
    List<JSONObject> filtered=new ArrayList<>();
    JSONObject detailGame;
    String tab="home",screen="home",query="",genre="",order="az",sessionStatus="";
    int page,selected,epoch;
    volatile boolean busy;
    boolean playing,signedIn;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences("settings",0);
        auth=new Auth(this,prefs.getString("client",Auth.DEFAULT_CLIENT));
        images=new CoverLoader(this);
        cache=new CatalogCache(this);
        signedIn=auth.hasSavedSession();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        home();
        if(signedIn) restoreSession();
    }
    int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    TextView text(String value,int size){
        TextView view=new TextView(this);view.setText(value);view.setTextColor(WHITE);view.setTextSize(size);
        view.setPadding(dp(6),dp(3),dp(6),dp(3));return view;
    }
    GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(9));return d;}
    StateListDrawable focus(){
        StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused},shape(GREEN));
        states.addState(new int[]{android.R.attr.state_pressed},shape(GREEN));
        states.addState(new int[]{},shape(PANEL));return states;
    }
    StateListDrawable cardBackground(){
        StateListDrawable states=new StateListDrawable();
        GradientDrawable selected=shape(0xff203d35);selected.setStroke(dp(2),GREEN);
        states.addState(new int[]{android.R.attr.state_selected},selected);
        states.addState(new int[]{},shape(PANEL));return states;
    }
    Button button(String label,Runnable action){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setMinWidth(0);b.setMinimumWidth(0);
        b.setTextColor(new ColorStateList(new int[][]{new int[]{android.R.attr.state_focused},new int[]{}},new int[]{BG,WHITE}));
        b.setBackground(focus());b.setPadding(dp(15),dp(3),dp(15),dp(3));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-2,dp(42));params.setMargins(dp(3),dp(3),dp(3),dp(3));
        b.setLayoutParams(params);b.setOnClickListener(v->action.run());return b;
    }
    LinearLayout column(){LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);return layout;}
    void navigate(String target){tab=target;screen="home";detailGame=null;page=0;selected=0;genre="";order=target.equals("recent")?"recent":"az";home();}
    void shell(){
        images.screen();grid=null;adapter=null;
        root=column();root.setPadding(dp(24),dp(12),dp(24),dp(8));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff132329,BG}));
        setContentView(root);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout brand=column();TextView name=text("BXbox Cloud TV",24);name.setTypeface(null,Typeface.BOLD);brand.addView(name);
        TextView tagline=text("SEU PRÓXIMO JOGO ESTÁ AQUI",10);tagline.setTextColor(GREEN);tagline.setLetterSpacing(.13f);brand.addView(tagline);
        top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        accountButton=button(signedIn?"●  Conta conectada":"Entrar na conta",()->{if(signedIn)settings();else login();});top.addView(accountButton);root.addView(top);
        tabs=new LinearLayout(this);root.addView(tabs);
        tabs.addView(button("Início",()->navigate("home")));
        tabs.addView(button("Volte a jogar",()->navigate("recent")));
        tabs.addView(button("Favoritos",()->navigate("favorites")));
        tabs.addView(button("Configurações",this::settings));
        body=column();root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        notice=text(sessionStatus,11);notice.setTextColor(MUTED);notice.setSingleLine(true);root.addView(notice);
    }
    void home(){
        if(playing)return;
        screen="home";detailGame=null;images.resume();shell();
        String title=tab.equals("recent")?"Volte à sua última aventura":tab.equals("favorites")?"Sua seleção de favoritos":"Todos os jogos";
        LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView label=text(title,21);label.setTypeface(null,Typeface.BOLD);heading.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        heading.addView(button("Atualizar",this::load));body.addView(heading);
        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER_VERTICAL);body.addView(controls);
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(14);search.setTextColor(WHITE);search.setHintTextColor(MUTED);search.setHint("Buscar por nome");search.setText(query);search.setPadding(dp(12),dp(3),dp(10),dp(3));search.setBackground(shape(PANEL));
        LinearLayout.LayoutParams searchParams=new LinearLayout.LayoutParams(0,dp(42),1);searchParams.setMargins(dp(3),dp(4),dp(8),dp(6));controls.addView(search,searchParams);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){query=s.toString();page=0;selected=0;ui.removeCallbacks(searchUpdate);ui.postDelayed(searchUpdate,160);}public void afterTextChanged(Editable value){}});
        String[] sortNames=tab.equals("recent")?new String[]{"Mais recentes","A–Z","Z–A"}:new String[]{"A–Z","Z–A"};
        String[] sortValues=tab.equals("recent")?new String[]{"recent","az","za"}:new String[]{"az","za"};
        controls.addView(filterSpinner(sortNames,sortValues,order,value->{order=value;page=0;selected=0;renderGrid();}),new LinearLayout.LayoutParams(dp(135),dp(42)));
        TreeSet<String> categories=new TreeSet<>();JSONArray source=tab.equals("recent")?recent:catalog;
        for(int i=0;i<source.length();i++){JSONArray a=source.optJSONObject(i)==null?null:source.optJSONObject(i).optJSONArray("genres");if(a!=null)for(int j=0;j<a.length();j++)categories.add(a.optString(j));}
        List<String> genreNames=new ArrayList<>();genreNames.add("Todos os gêneros");genreNames.addAll(categories);
        List<String> genreValues=new ArrayList<>();genreValues.add("");genreValues.addAll(categories);
        controls.addView(filterSpinner(genreNames.toArray(new String[0]),genreValues.toArray(new String[0]),genre,value->{genre=value;page=0;selected=0;renderGrid();}),new LinearLayout.LayoutParams(dp(185),dp(42)));
        int width=(int)(getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density);
        int columns=Math.max(3,Math.min(6,(width-48)/175));
        grid=new GridView(this);grid.setNumColumns(columns);grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);grid.setHorizontalSpacing(dp(10));grid.setVerticalSpacing(dp(10));grid.setPadding(dp(3),dp(5),dp(3),dp(5));grid.setClipToPadding(false);grid.setCacheColorHint(BG);grid.setSelector(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        adapter=new GameAdapter(Math.max(80,(width-48-(columns-1)*10)/columns*9/16));grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent,view,position,id)->{selected=position;int index=page*PAGE_SIZE+position;if(index<filtered.size())details(filtered.get(index));});
        body.addView(grid,new LinearLayout.LayoutParams(-1,0,1));
        TextView empty=text(signedIn?"Nenhum jogo encontrado. Ajuste os filtros ou atualize o catálogo.":"Entre na sua conta para explorar os jogos.",15);empty.setTextColor(MUTED);empty.setGravity(Gravity.CENTER);body.addView(empty,new LinearLayout.LayoutParams(-1,0,1));grid.setEmptyView(empty);
        LinearLayout footer=new LinearLayout(this);footer.setGravity(Gravity.CENTER_VERTICAL);body.addView(footer);
        counter=text("",12);counter.setTextColor(MUTED);footer.addView(counter,new LinearLayout.LayoutParams(0,-2,1));
        footer.addView(button("‹ Anterior",()->{if(page>0){page--;selected=0;renderGrid();grid.requestFocus();}}));
        footer.addView(button("Próxima ›",()->{if((page+1)*PAGE_SIZE<filtered.size()){page++;selected=0;renderGrid();grid.requestFocus();}}));
        renderGrid();
    }
    final Runnable searchUpdate=()->{if(!playing&&screen.equals("home"))renderGrid();};
    Spinner filterSpinner(String[] labels,String[] values,String current,java.util.function.Consumer<String> action){
        Spinner spinner=new Spinner(this);spinner.setContentDescription(labels.length>0?labels[0]:"Filtro");
        ArrayAdapter<String> options=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labels){
            @Override public View getView(int p,View view,ViewGroup parent){TextView t=(TextView)super.getView(p,view,parent);t.setTextColor(WHITE);t.setTextSize(13);return t;}
            @Override public View getDropDownView(int p,View view,ViewGroup parent){TextView t=(TextView)super.getDropDownView(p,view,parent);t.setTextColor(WHITE);t.setBackgroundColor(PANEL);t.setTextSize(14);return t;}
        };
        spinner.setAdapter(options);int initial=0;for(int i=0;i<values.length;i++)if(values[i].equals(current))initial=i;spinner.setSelection(initial);
        final String[] last={values[initial]};spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> parent){}public void onItemSelected(AdapterView<?> parent,View view,int position,long id){String value=values[position];if(!last[0].equals(value)){last[0]=value;action.accept(value);}}});return spinner;
    }
    Set<String> favorites(){return new HashSet<>(prefs.getStringSet("favorites",Collections.emptySet()));}
    void renderGrid(){
        if(grid==null||adapter==null||playing||!screen.equals("home"))return;
        JSONArray source=tab.equals("recent")?recent:catalog;
        filtered=Catalog.filter(source,query,genre,order,favorites(),tab.equals("favorites"));
        page=Math.min(page,Math.max(0,(filtered.size()-1)/PAGE_SIZE));
        int start=page*PAGE_SIZE,end=Math.min(filtered.size(),start+PAGE_SIZE);
        images.screen();adapter.items=new ArrayList<>(filtered.subList(start,end));adapter.notifyDataSetChanged();
        counter.setText(filtered.size()+" jogos  ·  Página "+(page+1)+" de "+Math.max(1,(filtered.size()+PAGE_SIZE-1)/PAGE_SIZE));
        if(!adapter.items.isEmpty())grid.setSelection(Math.min(selected,adapter.items.size()-1));
    }
    final class GameAdapter extends BaseAdapter {
        List<JSONObject> items=new ArrayList<>();final int imageHeight;
        GameAdapter(int imageHeight){this.imageHeight=imageHeight;}
        public int getCount(){return items.size();}public JSONObject getItem(int i){return items.get(i);}public long getItemId(int i){return i;}
        public View getView(int position,View reused,ViewGroup parent){
            LinearLayout card;Card holder;
            if(reused==null){card=column();card.setPadding(dp(3),dp(3),dp(3),dp(5));card.setBackground(cardBackground());
                holder=new Card();holder.image=new ImageView(MainActivity.this);holder.image.setScaleType(ImageView.ScaleType.CENTER_CROP);holder.image.setBackground(shape(0xff23353b));holder.image.setClipToOutline(true);card.addView(holder.image,new LinearLayout.LayoutParams(-1,dp(imageHeight)));
                holder.title=text("",13);holder.title.setTypeface(null,Typeface.BOLD);holder.title.setMaxLines(2);holder.title.setMinLines(2);holder.title.setEllipsize(TextUtils.TruncateAt.END);card.addView(holder.title,new LinearLayout.LayoutParams(-1,dp(42)));card.setTag(holder);
            }else{card=(LinearLayout)reused;holder=(Card)card.getTag();}
            JSONObject game=getItem(position);holder.title.setText(game.optString("name"));holder.image.setContentDescription(game.optString("name"));images.bind(holder.image,game.optString("cover"));return card;
        }
    }
    static final class Card {ImageView image;TextView title;}
    void details(JSONObject game){
        screen="detail";detailGame=game;shell();
        TextView eyebrow=text("ESCOLHA SUA PRÓXIMA PARTIDA",11);eyebrow.setTextColor(GREEN);eyebrow.setLetterSpacing(.12f);body.addView(eyebrow);
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);body.addView(row,new LinearLayout.LayoutParams(-1,0,1));
        ImageView cover=new ImageView(this);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);cover.setBackground(shape(PANEL));cover.setClipToOutline(true);cover.setContentDescription(game.optString("name"));
        LinearLayout.LayoutParams art=new LinearLayout.LayoutParams(0,dp(210),4);art.setMargins(dp(4),dp(12),dp(20),dp(12));row.addView(cover,art);images.bind(cover,game.optString("cover"));
        LinearLayout info=column();row.addView(info,new LinearLayout.LayoutParams(0,-2,6));
        TextView title=text(game.optString("name"),28);title.setTypeface(null,Typeface.BOLD);title.setMaxLines(3);title.setEllipsize(TextUtils.TruncateAt.END);info.addView(title);
        TextView genres=text(Catalog.genres(game),14);genres.setTextColor(GREEN);genres.setMaxLines(2);info.addView(genres);
        String publisher=game.optString("publisher");if(!publisher.isEmpty()){TextView publisherView=text(publisher,13);publisherView.setTextColor(MUTED);info.addView(publisherView);}
        TextView note=text("Sua biblioteca, pronta para jogar na nuvem.",14);note.setTextColor(MUTED);info.addView(note);
        LinearLayout actions=new LinearLayout(this);info.addView(actions);
        Button play=button("▶  Jogar",()->{if(signedIn)launch(game);else login();});actions.addView(play);
        actions.addView(button("Voltar ao início",()->{if(tab.equals("home"))home();else navigate("home");if(grid!=null)grid.requestFocus();}));
        boolean favorite=favorites().contains(game.optString("id"));
        info.addView(button(favorite?"★  Remover dos favoritos":"☆  Adicionar aos favoritos",()->{Set<String> ids=favorites();if(!ids.add(game.optString("id")))ids.remove(game.optString("id"));prefs.edit().putStringSet("favorites",ids).apply();details(game);}));
        play.requestFocus();
    }
    void say(String message){runOnUiThread(()->{if(notice!=null)notice.setText(message);});}
    void fail(Exception error){
        say("Falha: "+error.getMessage());runOnUiThread(()->{if(isFinishing()||isDestroyed())return;new AlertDialog.Builder(this).setTitle("Não foi possível concluir").setMessage(error.getMessage()).setPositiveButton("OK",null).show();});
    }
    void refreshScreen(){if(playing||isDestroyed()||isFinishing())return;if(screen.equals("settings"))settings();else if(screen.equals("detail")&&detailGame!=null)details(detailGame);else home();}
    void restoreSession(){
        if(busy)return;busy=true;int ticket=epoch;Auth current=auth;sessionStatus="Restaurando sua sessão…";say(sessionStatus);
        io.execute(()->{
            JSONObject saved=cache.read();
            runOnUiThread(()->{if(ticket!=epoch)return;JSONArray all=saved.optJSONArray("catalog"),history=saved.optJSONArray("recent");if(all!=null)catalog=all;if(history!=null)recent=history;refreshScreen();});
            try{current.ensure();runOnUiThread(()->{if(ticket!=epoch)return;busy=false;signedIn=true;sessionStatus="Conta conectada";refreshScreen();load();});}
            catch(Exception error){runOnUiThread(()->{if(ticket!=epoch)return;busy=false;if(error instanceof Auth.SessionExpired){signedIn=false;catalog=new JSONArray();recent=new JSONArray();cache.clear();sessionStatus="Sessão desconectada. Entre na conta.";}else{signedIn=current.hasSavedSession();sessionStatus="Sessão salva · sem conexão. Tente Atualizar.";}refreshScreen();});}
        });
    }
    void login(){
        if(signedIn||auth.hasSavedSession()){signedIn=true;sessionStatus="Sua conta já está salva";say(sessionStatus);if(!busy)restoreSession();return;}
        if(busy){say("Aguarde a operação em andamento");return;}
        busy=true;int ticket=epoch;Auth current=auth;
        io.execute(()->{try{
            JSONObject device=current.device();
            runOnUiThread(()->{if(ticket!=epoch||isFinishing()||isDestroyed())return;loginDialog=new AlertDialog.Builder(this).setTitle("Entre pelo celular").setMessage("Acesse: "+device.optString("verification_uri")+"\n\nCódigo: "+device.optString("user_code")+"\n\nO aplicativo lembrará sua conta nas próximas vezes.").setNegativeButton("Cancelar",(dialog,which)->current.cancelled=true).setOnCancelListener(dialog->current.cancelled=true).setPositiveButton("Concluir no celular",null).show();});
            current.poll(device);current.ensure();
            runOnUiThread(()->{if(ticket!=epoch)return;busy=false;signedIn=true;sessionStatus="Conta conectada";if(loginDialog!=null)loginDialog.dismiss();refreshScreen();load();});
        }catch(Exception error){runOnUiThread(()->{if(ticket!=epoch)return;busy=false;signedIn=current.hasSavedSession();sessionStatus=signedIn?"Sessão salva. Tente Atualizar.":"Entre para explorar sua biblioteca";if(loginDialog!=null)loginDialog.dismiss();refreshScreen();if(!current.cancelled)fail(error);});}});
    }
    void load(){
        if(!signedIn){login();return;}if(busy){say("Atualização em andamento…");return;}
        busy=true;int ticket=epoch;Auth current=auth;say("Atualizando sua biblioteca…");
        io.execute(()->{try{
            Cloud api=new Cloud(current,prefs.getString("region",""));JSONArray all=api.catalog(api.allTitles());JSONArray history;
            try{history=api.catalog(api.titles(true));}catch(Exception ignored){history=recent;}
            cache.write(all,history);JSONArray finalHistory=history;
            runOnUiThread(()->{if(ticket!=epoch)return;busy=false;catalog=all;recent=finalHistory;sessionStatus="Conta conectada · biblioteca atualizada";refreshScreen();});
        }catch(Exception error){runOnUiThread(()->{if(ticket!=epoch)return;busy=false;if(error instanceof Auth.SessionExpired){signedIn=false;catalog=new JSONArray();recent=new JSONArray();cache.clear();sessionStatus="Sessão desconectada. Entre novamente.";}else sessionStatus="Não foi possível atualizar · sua sessão foi mantida";refreshScreen();fail(error);});}});
    }
    void confirmLogout(){new AlertDialog.Builder(this).setTitle("Sair da conta?").setMessage("O login e a biblioteca desta conta serão removidos deste aparelho.").setNegativeButton("Cancelar",null).setPositiveButton("Sair da conta",(dialog,which)->logout()).show();}
    void logout(){
        epoch++;auth.cancelled=true;signedIn=false;busy=true;catalog=new JSONArray();recent=new JSONArray();query="";genre="";page=0;selected=0;prefs.edit().remove("favorites").apply();
        if(loginDialog!=null)loginDialog.dismiss();images.pause();Auth current=auth;
        io.execute(()->{current.logout();cache.clear();runOnUiThread(()->{busy=false;sessionStatus="Você saiu da conta";navigate("home");});});
    }
 void settings(){screen="settings";detailGame=null;shell();ScrollView scroll=new ScrollView(this);LinearLayout content=column();scroll.addView(content);body.addView(scroll,new LinearLayout.LayoutParams(-1,-1));content.addView(text("Ajustes para uma TV com 1 GB",22));
  choose(content,"Motor de streaming","engine",new String[]{"Native • somente hardware","WebView • WebRTC do sistema"},new String[]{"native","webview"},"native");
  choose(content,"Resolução solicitada","resolution",new String[]{"720p • recomendado","1080p • exige mais do decoder"},new String[]{"720","1080"},"720");
  choose(content,"Limite de bitrate solicitado","bitrate",new String[]{"3 Mb/s","5 Mb/s • inicial","8 Mb/s","10 Mb/s","15 Mb/s"},new String[]{"3000","5000","8000","10000","15000"},"5000");
  choose(content,"Codec (depende do motor e servidor)","codec",new String[]{"H264 • recomendado","VP8 • experimental","VP9 • experimental","H265 • se disponível","AV1 • se disponível"},new String[]{"H264","VP8","VP9","H265","AV1"},"H264");
  Switch low=new Switch(this);low.setText("Native: solicitar baixa latência (Android 11+, quando suportado)");low.setTextColor(WHITE);low.setChecked(prefs.getBoolean("lowLatency",true));low.setOnCheckedChangeListener((b,v)->prefs.edit().putBoolean("lowLatency",v).apply());content.addView(low);
  Switch stats=new Switch(this);stats.setText("Exibir estatísticas do streaming");stats.setTextSize(18);stats.setTextColor(WHITE);stats.setPadding(dp(10),dp(10),dp(10),dp(10));stats.setChecked(prefs.getBoolean("stats",false));stats.setOnCheckedChangeListener((b,v)->prefs.edit().putBoolean("stats",v).apply());content.addView(stats);
  content.addView(button("Servidor: "+prefs.getString("region","Automático")+" • escolher",this::regions));content.addView(button("Diagnóstico dos decodificadores",this::diagnostics));content.addView(button("Perfil leve: Native / 720p / H264 / 5 Mb/s",()->{prefs.edit().putString("engine","native").putString("resolution","720").putString("bitrate","5000").putString("codec","H264").putBoolean("stats",false).apply();settings();}));
  content.addView(button("Client ID de autenticação (avançado)",()->{EditText input=new EditText(this);input.setSingleLine(true);input.setText(prefs.getString("client",Auth.DEFAULT_CLIENT));new AlertDialog.Builder(this).setTitle("Aplicação pública OAuth").setMessage("O padrão é o Client ID público usado na referência XStreaming. Para publicar sua própria distribuição, configure seu registro OAuth autorizado. Trocar desconecta a conta.").setView(input).setPositiveButton("Salvar",(d,w)->{if(busy){say("Aguarde o login terminar");return;}try{String id=input.getText().toString().trim();UUID.fromString(id);epoch++;auth.logout();prefs.edit().putString("client",id).apply();auth=new Auth(this,id);signedIn=false;catalog=new JSONArray();recent=new JSONArray();cache.clear();say("Client ID atualizado. Entre na conta.");}catch(Exception e){say("Client ID deve ser um UUID válido");}}).setNegativeButton("Cancelar",null).show();}));
  content.addView(button("Créditos e licenças",()->{try{StringBuilder terms=new StringBuilder();for(String name:new String[]{"CREDITOS.txt","AGPL-3.0.txt","WebRTC-SDK-MIT.txt","WebRTC-BSD.txt"})try(java.io.InputStream stream=getAssets().open(name);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] data=new byte[4096];int read;while((read=stream.read(data))!=-1)out.write(data,0,read);terms.append(out.toString("UTF-8")).append("\n\n");}TextView license=text(terms.toString(),13);ScrollView sc=new ScrollView(this);sc.addView(license);new AlertDialog.Builder(this).setTitle("BXbox Cloud TV • AGPL-3.0").setView(sc).setPositiveButton("Fechar",null).show();}catch(Exception e){fail(e);}}));
  content.addView(button(signedIn?"Sair da conta":"Entrar na conta",signedIn?this::confirmLogout:this::login));
  content.addView(text("Limites: servidor e codec dependem da conta, região e hardware. O bitrate é uma solicitação SDP, não uma garantia. Native não usa decodificador por software. O WebView decide o próprio decoder. Não há FSR, geração de quadros nem pós-processamento. Reinicie a partida após mudar ajustes.\n\nProjeto experimental e independente, sem vínculo com Microsoft ou Xbox. Protocolo adaptado do XStreaming sob AGPL-3.0. Código e créditos acompanham o APK.",14));
 }
 void choose(LinearLayout parent,String label,String key,String[] labels,String[] values,String def){parent.addView(text(label,17));Spinner s=new Spinner(this);ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels);s.setAdapter(a);String value=prefs.getString(key,def);for(int i=0;i<values.length;i++)if(values[i].equals(value))s.setSelection(i);s.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> v){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){prefs.edit().putString(key,values[pos]).apply();}});parent.addView(s,new LinearLayout.LayoutParams(-1,dp(48)));}
 void regions(){if(busy){say("Aguarde a operação em andamento");return;}busy=true;io.execute(()->{try{JSONArray a=auth.regions();String[] names=new String[a.length()+1],values=new String[a.length()+1];names[0]="Automático";values[0]="";for(int i=0;i<a.length();i++){names[i+1]=a.getJSONObject(i).getString("name");values[i+1]=names[i+1];}runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Regiões disponíveis na conta").setItems(names,(d,i)->{prefs.edit().putString("region",values[i]).apply();settings();}).setNegativeButton("Cancelar",null).show());}catch(Exception e){fail(e);}finally{busy=false;}});}
 void diagnostics(){StringBuilder s=new StringBuilder("RAM e codecs não garantem desempenho. Confira durante uma partida.\n\n");ActivityManager am=(ActivityManager)getSystemService(ACTIVITY_SERVICE);ActivityManager.MemoryInfo memory=new ActivityManager.MemoryInfo();am.getMemoryInfo(memory);s.append("ABIs: ").append(java.util.Arrays.toString(Build.SUPPORTED_ABIS)).append("\nRAM total: ").append(memory.totalMem/1048576).append(" MB; disponível: ").append(memory.availMem/1048576).append(" MB\n\n");
  for(MediaCodecInfo info:new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()){if(info.isEncoder())continue;for(String type:info.getSupportedTypes())if(type.startsWith("video/")){s.append(info.getName()).append(" • ").append(type);if(Build.VERSION.SDK_INT>=29)s.append(info.isHardwareAccelerated()?" • hardware":" • software");else s.append(" • hardware não confirmado nesta API");try{if(Build.VERSION.SDK_INT>=30)s.append(info.getCapabilitiesForType(type).isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_LowLatency)?" • baixa latência suportada":"");}catch(Exception ignored){}s.append('\n');}}
  TextView t=text(s.toString(),14);ScrollView scroll=new ScrollView(this);scroll.addView(t);new AlertDialog.Builder(this).setTitle("Diagnóstico").setView(scroll).setPositiveButton("OK",null).show();}
 void launch(JSONObject game){if(busy){say("Aguarde a operação em andamento");return;}playing=true;images.pause();grid=null;adapter=null;detailGame=null;root=null;body=null;tabs=null;accountButton=null;pad.reset();FrameLayout stage=new FrameLayout(this);stage.setBackgroundColor(Color.BLACK);setContentView(stage);hud=text("Preparando sessão…",14);hud.setBackgroundColor(0x99000000);FrameLayout.LayoutParams overlay=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.LEFT);notice=hud;String title=game.optString("id");Auth current=auth;int height=Integer.parseInt(prefs.getString("resolution","720")),kbps=Integer.parseInt(prefs.getString("bitrate","5000"));String codec=prefs.getString("codec","H264");boolean stats=prefs.getBoolean("stats",false);
  NativePlayer.Ui callbacks=new NativePlayer.Ui(){public void status(String s){if(playing)say(s);}public void stats(String s){if(playing&&stats)say(s);}public void error(Exception e){runOnUiThread(()->{if(!playing)return;stop();fail(e);});}public void connected(){runOnUiThread(()->{if(!playing)return;if(!stats)hud.setVisibility(View.GONE);});}};
  busy=true;int ticket=++epoch;io.execute(()->{try{Cloud cloud=new Cloud(current,prefs.getString("region",""));runOnUiThread(()->{if(!playing||ticket!=epoch){busy=false;return;}try{if(prefs.getString("engine","native").equals("native")){nativePlayer=new NativePlayer(this,cloud,pad,height,kbps,codec,stats,callbacks);stage.addView(nativePlayer.view,new FrameLayout.LayoutParams(-1,-1));nativePlayer.start(title);}else{webPlayer=new WebPlayer(this,cloud,pad,height,kbps,codec,stats,title,callbacks);stage.addView(webPlayer.view,new FrameLayout.LayoutParams(-1,-1));}stage.addView(hud,overlay);busy=false;}catch(Exception e){busy=false;callbacks.error(e);}});}catch(Exception e){busy=false;callbacks.error(e);}});
 }

    void stop(){epoch++;playing=false;if(nativePlayer!=null){nativePlayer.close();nativePlayer=null;}if(webPlayer!=null){webPlayer.close();webPlayer=null;}images.resume();screen="home";home();}
    @Override public boolean dispatchKeyEvent(KeyEvent event){if(playing&&(event.getSource()&InputDevice.SOURCE_GAMEPAD)==InputDevice.SOURCE_GAMEPAD&&pad.key(event))return true;return super.dispatchKeyEvent(event);}
    @Override public boolean onGenericMotionEvent(MotionEvent event){if(playing&&pad.motion(event))return true;return super.onGenericMotionEvent(event);}
    @Override public void onBackPressed(){
        if(playing)new AlertDialog.Builder(this).setTitle("Encerrar a partida?").setPositiveButton("Encerrar",(dialog,which)->stop()).setNegativeButton("Continuar",null).show();
        else if(!screen.equals("home")){screen="home";home();if(grid!=null)grid.requestFocus();}
        else if(!tab.equals("home"))navigate("home");
        else super.onBackPressed();
    }
    @Override protected void onPause(){super.onPause();pad.reset();}
    @Override protected void onStart(){super.onStart();if(images!=null&&!playing){images.resume();if(adapter!=null)adapter.notifyDataSetChanged();}}
    @Override protected void onStop(){super.onStop();if(playing&&!isChangingConfigurations())stop();if(images!=null)images.pause();}
    @Override protected void onDestroy(){epoch++;auth.cancelled=true;ui.removeCallbacksAndMessages(null);if(loginDialog!=null)loginDialog.dismiss();if(nativePlayer!=null)nativePlayer.close();if(webPlayer!=null)webPlayer.close();images.close();io.shutdownNow();super.onDestroy();}
}
