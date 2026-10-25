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
    static final int BG=0xff020f15,PANEL=0xff0b202d,GREEN=0xff9bff24,WHITE=0xfff3f7fa,MUTED=0xff9ab1bd;
    static final int PAGE_SIZE=60;
    final ExecutorService io=Executors.newSingleThreadExecutor(),licenseIo=Executors.newSingleThreadExecutor();
    final Handler ui=new Handler(Looper.getMainLooper());final Pad pad=new Pad();
    final Map<String,Button> navButtons=new HashMap<>();
    android.content.SharedPreferences prefs;Auth auth;CoverLoader images;CatalogCache cache;
    LinearLayout root,body,tabs,sidebar;
    TextView notice,hud,counter;Button accountButton,heroButton,sortButton,genreButton;
    JSONObject feed=new JSONObject();
    final Map<String,Integer> shelfPositions=new HashMap<>();final List<TvShelf> shelves=new ArrayList<>();
    String homeFocus="hero";long lastFeedAttempt;boolean foreground;volatile int licenseRequest;
    final Map<String,Entitlements.State> licenses=new HashMap<>();final Map<String,Long> licenseTimes=new HashMap<>();
    String heroUrl="";ImageView heroImage;EditText searchBox;TvGridView grid;GameAdapter adapter;
    NativePlayer nativePlayer;WebPlayer webPlayer;AlertDialog loginDialog,choiceDialog;
    JSONArray catalog=new JSONArray(),recent=new JSONArray();List<JSONObject> filtered=new ArrayList<>();JSONObject detailGame;
    String tab="home",screen="home",query="",genre="",order="az",sessionStatus="",settingsPage="stream",pendingFocus="";
    int page,selected,epoch,layoutVersion;volatile boolean busy;boolean playing,signedIn;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);prefs=getSharedPreferences("settings",0);
        // Mantém a identificação interna já usada pela sessão; não há editor de Client ID na interface.
        auth=new Auth(this,prefs.getString("client",Auth.DEFAULT_CLIENT));images=new CoverLoader(this);cache=new CatalogCache(this);signedIn=auth.hasSavedSession();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        home();if(signedIn)restoreSession();
    }
    int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    int viewportWidth(){return Math.round(getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density);}
    TextView text(String value,int size){TextView view=new TextView(this);view.setText(value);view.setTextColor(WHITE);view.setTextSize(size);view.setPadding(dp(5),dp(3),dp(5),dp(3));return view;}
    LinearLayout column(){LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);return layout;}
    GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(8));return d;}
    android.graphics.drawable.Drawable focus(){return getDrawable(R.drawable.tv_control_background);}
    Button button(String label,Runnable action){
        Button b=new Button(this);b.setId(View.generateViewId());b.setText(label);b.setAllCaps(false);b.setTextSize(13);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setSingleLine(true);b.setHorizontallyScrolling(false);b.setEllipsize(TextUtils.TruncateAt.END);
        b.setGravity(Gravity.CENTER);b.setIncludeFontPadding(false);b.setTextColor(WHITE);b.setBackground(focus());b.setPadding(dp(12),dp(3),dp(12),dp(3));b.setFocusable(true);b.setFocusableInTouchMode(true);b.setStateListAnimator(null);b.setElevation(0);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-2,dp(40));params.setMargins(dp(3),dp(3),dp(3),dp(3));b.setLayoutParams(params);b.setOnClickListener(v->action.run());return b;
    }
    void icon(Button b,String name,int color){b.setCompoundDrawablesWithIntrinsicBounds(new TvIcon(name,color,dp(19)),null,null,null);b.setCompoundDrawablePadding(dp(9));}
    void primary(Button b){
        StateListDrawable d=new StateListDrawable();GradientDrawable focused=shape(GREEN);focused.setStroke(dp(3),WHITE);d.addState(new int[]{android.R.attr.state_focused},focused);d.addState(new int[]{},shape(GREEN));b.setBackground(d);b.setTextColor(BG);b.setTypeface(null,Typeface.BOLD);
    }
    String focusedTag(){if(root==null)return "";View focus=root.findFocus();Object tag=focus==null?null:focus.getTag();return tag instanceof String?(String)tag:"";}
    void restoreFocus(String tag){
        final int version=layoutVersion;ui.post(()->{if(playing||root==null||version!=layoutVersion)return;if(tag.equals("grid")&&grid!=null&&grid.getCount()>0){focusGrid();return;}if(tag.startsWith("shelf:")){for(TvShelf shelf:shelves)if(tag.equals("shelf:"+shelf.key)){shelf.focusAt(shelfPositions.getOrDefault(shelf.key,0));return;}}View target=tag.isEmpty()?navButtons.get(screen.equals("settings")?"settings":tab):root.findViewWithTag(tag);if(target!=null)target.requestFocus();});
    }
    void navigate(String target){tab=target;screen="home";detailGame=null;page=0;selected=0;genre="";order=target.equals("recent")?"recent":"az";pendingFocus="nav:"+target;home();if(target.equals("search")){pendingFocus="";restoreFocus("search");}}
    void addNav(String label,String id,String glyph,Runnable action){
        Button item=button(label,action);item.setTag("nav:"+id);item.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);icon(item,glyph,WHITE);
        item.setActivated(screen.equals("settings")?id.equals("settings"):screen.equals("help")?id.equals("help"):id.equals(tab));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(43));lp.setMargins(0,dp(3),0,dp(3));sidebar.addView(item,lp);navButtons.put(id,item);
        item.setOnKeyListener((v,key,event)->{if(event.getAction()==KeyEvent.ACTION_DOWN&&key==KeyEvent.KEYCODE_DPAD_RIGHT){enterContent();return true;}return false;});
    }
    void shell(){
        layoutVersion++;licenseRequest++;ui.removeCallbacks(searchUpdate);images.screen();grid=null;adapter=null;sortButton=null;genreButton=null;searchBox=null;heroButton=null;heroImage=null;heroUrl="";navButtons.clear();shelves.clear();
        root=column();root.setPadding(dp(16),dp(10),dp(16),dp(5));root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff021c1c,BG}));setContentView(root);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView mark=new ImageView(this);mark.setImageResource(R.drawable.icon);header.addView(mark,new LinearLayout.LayoutParams(dp(37),dp(37)));
        LinearLayout brand=column();TextView name=text("BXbox Cloud TV",22);name.setTypeface(null,Typeface.BOLD);name.setPadding(dp(5),0,dp(5),0);brand.addView(name);TextView tagline=text("SEU PRÓXIMO JOGO ESTÁ AQUI",9);tagline.setTextColor(GREEN);tagline.setLetterSpacing(.12f);tagline.setPadding(dp(5),0,dp(5),0);brand.addView(tagline);header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        accountButton=button(signedIn?"●  Conectado":"Entrar na conta",()->{if(signedIn){settingsPage="account";settings();}else login();});accountButton.setTag("account");header.addView(accountButton);root.addView(header,new LinearLayout.LayoutParams(-1,dp(53)));
        LinearLayout workspace=new LinearLayout(this);root.addView(workspace,new LinearLayout.LayoutParams(-1,0,1));
        sidebar=column();sidebar.setPadding(0,dp(10),dp(12),dp(2));int rail=viewportWidth()<800?148:174;workspace.addView(sidebar,new LinearLayout.LayoutParams(dp(rail),-1));
        LinearLayout profile=new LinearLayout(this);profile.setGravity(Gravity.CENTER_VERTICAL);ImageView avatar=new ImageView(this);avatar.setImageDrawable(new TvIcon("account",WHITE,dp(28)));profile.addView(avatar,new LinearLayout.LayoutParams(dp(30),dp(32)));LinearLayout profileText=column();TextView label=text("Sua conta Xbox",12);label.setTypeface(null,Typeface.BOLD);label.setTextColor(GREEN);profileText.addView(label);TextView status=text(signedIn?"Sessão salva":"Conecte para jogar",10);status.setTextColor(MUTED);profileText.addView(status);profile.addView(profileText);sidebar.addView(profile,new LinearLayout.LayoutParams(-1,dp(48)));
        addNav("Início","home","home",()->navigate("home"));addNav("Volte a jogar","recent","gamepad",()->navigate("recent"));addNav("Pesquisar","search","search",()->navigate("search"));addNav("Configurações","settings","settings",this::settings);
        View divider=new View(this);divider.setBackgroundColor(0xff24404b);LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(-1,dp(1));line.setMargins(dp(3),dp(15),dp(3),dp(11));sidebar.addView(divider,line);
        addNav("Biblioteca","library","library",()->navigate("library"));addNav("Favoritos","favorites","favorite",()->navigate("favorites"));sidebar.addView(new View(this),new LinearLayout.LayoutParams(1,0,1));addNav("Ajuda","help","help",this::help);
        body=column();body.setPadding(dp(8),dp(6),0,0);workspace.addView(body,new LinearLayout.LayoutParams(0,-1,1));tabs=sidebar;
        notice=text(sessionStatus,10);notice.setTextColor(MUTED);notice.setMaxLines(1);notice.setHorizontallyScrolling(false);notice.setEllipsize(TextUtils.TruncateAt.END);root.addView(notice,new LinearLayout.LayoutParams(-1,dp(19)));
    }
    void enterContent(){if(tab.equals("home")&&!shelves.isEmpty()){shelves.get(0).focusAt(shelfPositions.getOrDefault(shelves.get(0).key,0));return;}if(grid!=null&&grid.getCount()>0)focusGrid();else if(heroButton!=null)heroButton.requestFocus();else if(searchBox!=null)searchBox.requestFocus();else{View target=body.findViewWithTag("first-control");if(target!=null)target.requestFocus();}}
    void focusGrid(){if(grid!=null&&grid.getCount()>0){grid.focusPosition(selected);updateSelectionHint();}}
    void wireFilter(View view){
        view.setOnKeyListener((v,key,event)->{if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;if(key==KeyEvent.KEYCODE_DPAD_DOWN&&grid!=null&&grid.getCount()>0){focusGrid();return true;}if(key==KeyEvent.KEYCODE_DPAD_UP&&heroButton!=null){heroButton.requestFocus();return true;}return false;});
        view.setOnFocusChangeListener((v,has)->{if(has&&grid!=null)grid.anchors(v,navButtons.get(tab));});
    }
    void hero(){
        JSONObject game=recent.optJSONObject(0);if(game==null)for(int i=0;i<catalog.length();i++){JSONObject candidate=catalog.optJSONObject(i);if(candidate!=null&&candidate.optBoolean("hasEntitlement")){game=candidate;break;}}final JSONObject featured=game;
        FrameLayout banner=new FrameLayout(this);banner.setBackground(shape(PANEL));banner.setClipToOutline(true);
        LinearLayout.LayoutParams box=new LinearLayout.LayoutParams(-1,dp(132));box.setMargins(dp(2),dp(2),dp(2),dp(8));body.addView(banner,box);
        heroImage=new ImageView(this);heroImage.setScaleType(ImageView.ScaleType.CENTER_CROP);FrameLayout.LayoutParams heroArt=new FrameLayout.LayoutParams(dp(Math.max(280,(viewportWidth()-(viewportWidth()<800?148:174)-56)*3/5)),-1,Gravity.END);banner.addView(heroImage,heroArt);heroUrl=game==null?"":game.optString("cover");
        View veil=new View(this);veil.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xff061c27,0xe0061c27,0x20061c27}));banner.addView(veil,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout info=column();info.setPadding(dp(15),dp(8),dp(12),dp(6));int available=viewportWidth()-(viewportWidth()<800?148:174)-56;FrameLayout.LayoutParams infoSize=new FrameLayout.LayoutParams(dp(Math.max(260,available*2/3)),-1,Gravity.START);banner.addView(info,infoSize);
        TextView overline=text(game==null?"BEM-VINDO À SUA BIBLIOTECA":"EM DESTAQUE NA SUA BIBLIOTECA",9);overline.setTextColor(GREEN);info.addView(overline);
        TextView title=text(game==null?"Seu próximo jogo começa aqui":game.optString("name"),22);title.setTypeface(null,Typeface.BOLD);title.setMaxLines(1);title.setEllipsize(TextUtils.TruncateAt.END);info.addView(title);
        TextView meta=text(game==null?(signedIn?"Explore as categorias abaixo":"Entre na sua conta para explorar o catálogo"):Catalog.genres(game),11);meta.setTextColor(MUTED);meta.setMaxLines(1);meta.setHorizontallyScrolling(false);meta.setEllipsize(TextUtils.TruncateAt.END);info.addView(meta);
        heroButton=button(game==null?(signedIn?"Atualizar biblioteca":"Entrar na conta"):"Exibir detalhes",()->{if(featured!=null)details(featured);else if(signedIn)load();else login();});heroButton.setTag("hero");primary(heroButton);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(31));lp.setMargins(dp(4),dp(3),0,0);info.addView(heroButton,lp);
        heroButton.setOnKeyListener((v,key,event)->{if(key==KeyEvent.KEYCODE_DPAD_DOWN&&event.getAction()==KeyEvent.ACTION_DOWN&&searchBox!=null){searchBox.requestFocus();return true;}return false;});
    }
    void home(){
        if(playing)return;String wanted=!pendingFocus.isEmpty()?pendingFocus:screen.equals("home")?focusedTag():tab.equals("home")?homeFocus:"grid";pendingFocus="";
        screen="home";detailGame=null;images.resume();shell();if(tab.equals("home")){homeCategories(wanted);return;}
        LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);String title=tab.equals("recent")?"Volte a jogar":tab.equals("favorites")?"Minha seleção":tab.equals("search")?"Encontre seu próximo jogo":"Todos os jogos";
        TextView label=text(title,19);label.setTypeface(null,Typeface.BOLD);heading.addView(label,new LinearLayout.LayoutParams(0,-2,1));Button refresh=button("Atualizar",this::load);refresh.setTag("refresh");heading.addView(refresh);body.addView(heading,new LinearLayout.LayoutParams(-1,dp(38)));
        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER_VERTICAL);body.addView(controls,new LinearLayout.LayoutParams(-1,dp(40)));
        searchBox=new EditText(this);searchBox.setId(View.generateViewId());searchBox.setTag("search");searchBox.setSingleLine(true);searchBox.setTextSize(12);searchBox.setTextColor(WHITE);searchBox.setHintTextColor(MUTED);searchBox.setHint("Buscar por nome");searchBox.setText(query);searchBox.setPadding(dp(12),0,dp(10),0);searchBox.setBackground(focus());
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(34),1);sp.setMargins(dp(3),dp(2),dp(6),dp(2));controls.addView(searchBox,sp);
        searchBox.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){query=s.toString();page=0;selected=0;ui.removeCallbacks(searchUpdate);ui.postDelayed(searchUpdate,160);}public void afterTextChanged(Editable value){}});
        String[] sortNames=tab.equals("recent")?new String[]{"Mais recentes","A–Z","Z–A"}:new String[]{"A–Z","Z–A"};String[] sortValues=tab.equals("recent")?new String[]{"recent","az","za"}:new String[]{"az","za"};
        sortButton=filterButton("Ordenar jogos",sortNames,sortValues,order,value->{order=value;page=0;selected=0;renderGrid();});sortButton.setTag("sort");controls.addView(sortButton,new LinearLayout.LayoutParams(dp(104),dp(34)));
        TreeSet<String> categories=new TreeSet<>();JSONArray source=tab.equals("recent")?recent:catalog;for(int i=0;i<source.length();i++){JSONObject g=source.optJSONObject(i);JSONArray a=g==null?null:g.optJSONArray("genres");if(a!=null)for(int j=0;j<a.length();j++)categories.add(a.optString(j));}
        List<String> labels=new ArrayList<>(),values=new ArrayList<>();labels.add("Todos os gêneros");values.add("");labels.addAll(categories);values.addAll(categories);if(!values.contains(genre))genre="";
        genreButton=filterButton("Filtrar por gênero",labels.toArray(new String[0]),values.toArray(new String[0]),genre,value->{genre=value;page=0;selected=0;renderGrid();});genreButton.setTag("genre");LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(dp(170),dp(34));gp.setMargins(dp(6),0,dp(2),0);controls.addView(genreButton,gp);
        int width=viewportWidth()-(viewportWidth()<800?148:174)-56;int columns=Math.max(3,Math.min(5,width/156));
        grid=new TvGridView(this);grid.setId(View.generateViewId());grid.setTag("grid");grid.setNumColumns(columns);grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);grid.setHorizontalSpacing(dp(9));grid.setVerticalSpacing(dp(10));grid.setPadding(dp(3),dp(5),dp(3),dp(4));grid.setClipToPadding(false);grid.setCacheColorHint(BG);
        adapter=new GameAdapter(Math.max(64,(width-(columns-1)*9)/columns*9/16));grid.setAdapter(adapter);grid.anchors(sortButton,navButtons.get(tab));
        grid.setOnItemClickListener((parent,view,pos,id)->{selected=pos;int at=page*PAGE_SIZE+pos;if(at<filtered.size())details(filtered.get(at));});
        grid.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){selected=pos;updateSelectionHint();grid.invalidate();}});
        body.addView(grid,new LinearLayout.LayoutParams(-1,0,1));
        TextView empty=text(signedIn?"Nenhum jogo encontrado. Ajuste os filtros ou atualize a biblioteca.":"Entre na sua conta para explorar os jogos.",14);empty.setGravity(Gravity.CENTER);empty.setTextColor(MUTED);body.addView(empty,new LinearLayout.LayoutParams(-1,0,1));grid.setEmptyView(empty);
        LinearLayout footer=new LinearLayout(this);footer.setGravity(Gravity.CENTER_VERTICAL);body.addView(footer,new LinearLayout.LayoutParams(-1,dp(35)));counter=text("",10);counter.setTextColor(MUTED);counter.setMaxLines(1);counter.setHorizontallyScrolling(false);counter.setEllipsize(TextUtils.TruncateAt.END);footer.addView(counter,new LinearLayout.LayoutParams(0,-2,1));
        Button prev=button("‹ Anterior",()->{if(page>0){page--;selected=0;renderGrid();focusGrid();}});prev.setTag("previous");footer.addView(prev,new LinearLayout.LayoutParams(dp(94),dp(30)));Button next=button("Próxima ›",()->{if((page+1)*PAGE_SIZE<filtered.size()){page++;selected=0;renderGrid();focusGrid();}});next.setTag("next");footer.addView(next,new LinearLayout.LayoutParams(dp(94),dp(30)));
        wireFilter(searchBox);wireFilter(sortButton);wireFilter(genreButton);sortButton.setNextFocusDownId(grid.getId());genreButton.setNextFocusDownId(grid.getId());searchBox.setNextFocusDownId(grid.getId());renderGrid();restoreFocus(wanted);
    }
    void openGenre(String name){query="";navigate("library");genre=name;renderGrid();focusGrid();}
    void homeCategories(String wanted){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setClipToPadding(false);LinearLayout content=column();scroll.addView(content);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout outer=body;body=content;hero();body=outer;if(heroImage!=null&&!heroUrl.isEmpty())images.bindHero(heroImage,heroUrl);
        LinearLayout bar=new LinearLayout(this);TextView hint=text("Descubra sua próxima partida",16);hint.setTypeface(null,Typeface.BOLD);bar.addView(hint,new LinearLayout.LayoutParams(0,-2,1));Button refresh=button("Atualizar",this::load);refresh.setTag("refresh");bar.addView(refresh);content.addView(bar);
        if(!signedIn){caption(content,"Entre na conta para carregar as categorias da nuvem.");restoreFocus(wanted);return;}
        addShelf(content,"popular","Mais populares em nuvem",HomeFeed.select(feed,"popular",catalog),false);
        addShelf(content,"added","Adicionados recentemente",HomeFeed.select(feed,"added",catalog),false);
        addShelf(content,"leaving","Saindo de cena em breve",HomeFeed.select(feed,"leaving",catalog),false);
        TreeSet<String> names=new TreeSet<>();for(int i=0;i<catalog.length();i++){JSONObject game=catalog.optJSONObject(i);JSONArray gs=game==null?null:game.optJSONArray("genres");if(gs!=null)for(int j=0;j<gs.length();j++){String name=gs.optString(j);if(!name.isEmpty())names.add(name);}}
        JSONArray genres=new JSONArray();for(String name:names)genres.put(Json.obj("name",name));addShelf(content,"genres","Explore por gênero",genres,true);
        for(int i=0;i<shelves.size();i++){TvShelf shelf=shelves.get(i);shelf.top=heroButton;if(i>0)shelf.previous=shelves.get(i-1);if(i+1<shelves.size())shelf.next=shelves.get(i+1);}
        heroButton.setOnKeyListener((v,key,event)->{if(event.getAction()==KeyEvent.ACTION_DOWN&&key==KeyEvent.KEYCODE_DPAD_DOWN&&!shelves.isEmpty()){shelves.get(0).focusAt(shelfPositions.getOrDefault(shelves.get(0).key,0));return true;}return false;});
        caption(content,"Listas oficiais da Microsoft · atualização automática a cada 6 horas com o catálogo aberto. As listas podem variar por região e disponibilidade na nuvem.");
        restoreFocus(wanted.equals("grid")?homeFocus:wanted);
    }
    void addShelf(LinearLayout content,String key,String label,JSONArray games,boolean genres){TvShelf shelf=new TvShelf(this,key,label,games,genres);content.addView(shelf,new LinearLayout.LayoutParams(-1,-2));if(games.length()>0)shelves.add(shelf);}
    final Runnable refreshFeed=new Runnable(){public void run(){if(!foreground||playing)return;long now=System.currentTimeMillis();if(signedIn&&!busy&&HomeFeed.due(feed.optLong("updatedAt"),now)&&(lastFeedAttempt==0||now-lastFeedAttempt>=15*60*1000L))load();ui.postDelayed(this,60000);}};
    final Runnable searchUpdate=()->{if(!playing&&screen.equals("home"))renderGrid();};
    Button filterButton(String title,String[] labels,String[] values,String current,java.util.function.Consumer<String> action){
        int at=0;for(int i=0;i<values.length;i++)if(values[i].equals(current))at=i;final String[] chosen={values[at]};
        Button b=button(labels[at]+"  ▾",()->{});b.setTextSize(11);b.setContentDescription(title);b.setOnClickListener(v->{int active=0;for(int i=0;i<values.length;i++)if(values[i].equals(chosen[0]))active=i;showChoices(title,labels,active,b,index->{chosen[0]=values[index];b.setText(labels[index]+"  ▾");action.accept(values[index]);});});return b;
    }
    void showChoices(String title,String[] labels,int active,View anchor,java.util.function.IntConsumer apply){
        LinearLayout list=column();list.setPadding(dp(4),dp(6),dp(4),dp(6));ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.addView(list);Button[] rows=new Button[labels.length];
        for(int i=0;i<labels.length;i++){final int index=i;Button row=button(labels[i]+(i==active?"  ✓":""),()->{if(choiceDialog!=null)choiceDialog.dismiss();apply.accept(index);});row.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);row.setActivated(i==active);list.addView(row,new LinearLayout.LayoutParams(-1,dp(42)));rows[i]=row;}
        FrameLayout container=new FrameLayout(this);container.addView(scroll,new FrameLayout.LayoutParams(-1,dp(Math.min(280,Math.max(46,labels.length*44)))));
        choiceDialog=new AlertDialog.Builder(this).setTitle(title).setView(container).setNegativeButton("Voltar",null).create();choiceDialog.setOnDismissListener(d->{if(anchor!=null&&!playing)anchor.requestFocus();});choiceDialog.show();if(rows.length>0)rows[Math.max(0,Math.min(active,rows.length-1))].requestFocus();
    }
    Set<String> favorites(){return new HashSet<>(prefs.getStringSet("favorites",Collections.emptySet()));}
    void renderGrid(){
        if(grid==null||adapter==null||playing||!screen.equals("home"))return;boolean hadFocus=grid.hasFocus();JSONArray source=tab.equals("recent")?recent:catalog;filtered=Catalog.filter(source,query,genre,order,favorites(),tab.equals("favorites"));page=Math.min(page,Math.max(0,(filtered.size()-1)/PAGE_SIZE));int first=page*PAGE_SIZE,end=Math.min(filtered.size(),first+PAGE_SIZE);images.screen();if(heroImage!=null)images.bindHero(heroImage,heroUrl);adapter.items=new ArrayList<>(filtered.subList(first,end));adapter.notifyDataSetChanged();selected=Math.min(selected,Math.max(0,adapter.items.size()-1));if(!adapter.items.isEmpty())grid.setSelection(selected);if(hadFocus)focusGrid();updateSelectionHint();
    }
    void updateSelectionHint(){if(counter==null)return;String hint=filtered.size()+" jogos · "+(page+1)+"/"+Math.max(1,(filtered.size()+PAGE_SIZE-1)/PAGE_SIZE);int at=page*PAGE_SIZE+selected;if(grid!=null&&grid.hasFocus()&&at<filtered.size())hint+=" · "+filtered.get(at).optString("name");counter.setText(hint);}
    final class GameAdapter extends BaseAdapter{
        List<JSONObject> items=new ArrayList<>();final int imageHeight;GameAdapter(int h){imageHeight=h;}
        public int getCount(){return items.size();}public JSONObject getItem(int i){return items.get(i);}public long getItemId(int i){return i;}
        public View getView(int position,View reused,ViewGroup parent){
            LinearLayout card;Card holder;if(reused==null){card=column();card.setPadding(dp(2),dp(2),dp(2),dp(3));GradientDrawable frame=shape(PANEL);frame.setStroke(dp(1),0xff163c4d);card.setBackground(frame);card.setClipToOutline(true);card.setFocusable(false);holder=new Card();holder.image=new ImageView(MainActivity.this);holder.image.setScaleType(ImageView.ScaleType.CENTER_CROP);holder.image.setBackgroundColor(0xff12303c);card.addView(holder.image,new LinearLayout.LayoutParams(-1,dp(imageHeight)));holder.title=text("",11);holder.title.setMaxLines(1);holder.title.setHorizontallyScrolling(false);holder.title.setEllipsize(TextUtils.TruncateAt.END);holder.title.setGravity(Gravity.CENTER_VERTICAL);card.addView(holder.title,new LinearLayout.LayoutParams(-1,dp(28)));card.setTag(holder);}else{card=(LinearLayout)reused;holder=(Card)card.getTag();}
            JSONObject game=getItem(position);holder.title.setText(game.optString("name"));holder.image.setContentDescription(game.optString("name"));images.bind(holder.image,game.optString("cover"));return card;
        }
    }
    static final class Card{ImageView image;TextView title;}
    void details(JSONObject game){
        screen="detail";detailGame=game;shell();TextView label=text("SUA PRÓXIMA PARTIDA",10);label.setTextColor(GREEN);label.setLetterSpacing(.13f);body.addView(label);
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);body.addView(row,new LinearLayout.LayoutParams(-1,0,1));
        FrameLayout art=new FrameLayout(this);art.setBackground(shape(PANEL));art.setClipToOutline(true);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(222),5);ap.setMargins(dp(2),dp(12),dp(18),dp(12));row.addView(art,ap);ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);art.addView(image,new FrameLayout.LayoutParams(-1,-1));images.bindHero(image,game.optString("cover"));
        LinearLayout info=column();row.addView(info,new LinearLayout.LayoutParams(0,-2,5));TextView title=text(game.optString("name"),25);title.setTypeface(null,Typeface.BOLD);title.setMaxLines(3);title.setEllipsize(TextUtils.TruncateAt.END);info.addView(title);TextView genreLabel=text(Catalog.genres(game),12);genreLabel.setTextColor(GREEN);genreLabel.setMaxLines(2);info.addView(genreLabel);if(!game.optString("publisher").isEmpty()){TextView publisher=text(game.optString("publisher"),11);publisher.setTextColor(MUTED);info.addView(publisher);}
        TextView note=text(signedIn?"Consultando acesso e licença da sua conta…":"Entre na conta para consultar sua biblioteca.",12);note.setTextColor(MUTED);info.addView(note);
        Button play=button("Jogar",()->{if(signedIn)launch(game);else login();});play.setTag("first-control");primary(play);icon(play,"play",BG);info.addView(play,new LinearLayout.LayoutParams(-1,dp(41)));
        boolean saved=favorites().contains(game.optString("id"));Button favorite=button(saved?"Remover dos favoritos":"Adicionar aos favoritos",()->{Set<String> ids=favorites();if(!ids.add(game.optString("id")))ids.remove(game.optString("id"));prefs.edit().putStringSet("favorites",ids).apply();details(game);});favorite.setTag("favorite-action");icon(favorite,"favorite",WHITE);LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,dp(39));fp.setMargins(0,dp(8),0,dp(4));info.addView(favorite,fp);
        Button back=button("Voltar ao início",()->{if(tab.equals("home"))home();else navigate("home");focusGrid();});info.addView(back,new LinearLayout.LayoutParams(-1,dp(37)));play.requestFocus();
        if(signedIn)checkLicense(game,note,play);

    }
    void showLicense(JSONObject game,Entitlements.State state,TextView note,Button play){
        boolean access=game.optBoolean("hasEntitlement");
        switch(state){
            case OWNED:note.setText("Na sua biblioteca · licença de compra ou resgate confirmada.");break;
            case ENTITLED:note.setText("Acesso confirmado na conta · pode ser assinatura, pacote ou teste.");break;
            case NOT_OWNED:
                if(access)note.setText("Disponível para sua conta na nuvem. A compra individual não foi identificada.");
                else note.setText("Não foi identificada uma licença válida deste jogo na conta. Se você não possui o título, é necessário comprá-lo na loja oficial Xbox para jogar. Se estiver no Game Pass, verifique sua assinatura e a região.");break;
            default:note.setText(access?"Acesso à nuvem identificado. Não foi possível confirmar a compra individual.":"Não foi possível confirmar a licença agora. Verifique sua biblioteca e assinatura na Microsoft.");
        }
        // A Microsoft é a autoridade final: não bloqueia jogo por ausência, edições alternativas ou licenças compartilhadas.
        if(state==Entitlements.State.NOT_OWNED&&!access)play.setText("Verificar acesso / jogar");
    }
    void checkLicense(JSONObject game,TextView note,Button play){
        String productId=game.optString("productId");long now=System.currentTimeMillis();Entitlements.State cached=licenses.get(productId);
        if(cached!=null&&now-licenseTimes.getOrDefault(productId,0L)<15*60*1000L){showLicense(game,cached,note,play);return;}
        final int version=layoutVersion,ticket=epoch,request=++licenseRequest;final Auth current=auth;
        final java.lang.ref.WeakReference<TextView> label=new java.lang.ref.WeakReference<>(note);final java.lang.ref.WeakReference<Button> action=new java.lang.ref.WeakReference<>(play);
        licenseIo.execute(()->{if(request!=licenseRequest)return;Entitlements.State state;try{state=Entitlements.check(current,productId);}catch(Exception error){state=Entitlements.State.UNKNOWN;}final Entitlements.State result=state;
            runOnUiThread(()->{if(ticket!=epoch||isDestroyed()||playing)return;licenses.put(productId,result);licenseTimes.put(productId,System.currentTimeMillis());if(version==layoutVersion&&screen.equals("detail")){TextView target=label.get();Button button=action.get();if(target!=null&&button!=null)showLicense(game,result,target,button);}});});
    }
    boolean microphoneGranted(){return checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED;}
    boolean microphoneInput(){AudioManager manager=(AudioManager)getSystemService(AUDIO_SERVICE);return manager!=null&&manager.getDevices(AudioManager.GET_DEVICES_INPUTS).length>0;}
    void microphoneSetting(LinearLayout content){
        toggle(content,"Ativar microfone","microphone",false);
        Switch control=(Switch)content.findViewWithTag("setting:microphone");control.setOnCheckedChangeListener((button,enabled)->{
            if(enabled&&!microphoneGranted()){prefs.edit().putBoolean("microphone",false).apply();requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},40);}
            else prefs.edit().putBoolean("microphone",enabled).apply();
        });
        caption(content,"(É necessário um microfone ou dispositivo de entrada de áudio conectado e reconhecido pela TV.) A captura começa com a partida e termina ao sair. O jogo também precisa permitir chat de voz.");
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){
        super.onRequestPermissionsResult(requestCode,permissions,results);if(requestCode!=40)return;
        boolean allowed=results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED;prefs.edit().putBoolean("microphone",allowed).apply();
        if(root!=null){Switch control=root.findViewWithTag("setting:microphone");if(control!=null)control.setChecked(allowed);}
        Toast.makeText(this,allowed?"Microfone autorizado para a próxima partida":"Microfone desativado: permissão não concedida",Toast.LENGTH_LONG).show();
    }
    void say(String message){runOnUiThread(()->{if(notice!=null)notice.setText(message);});}
    void fail(Exception error){
        say("Falha: "+error.getMessage());runOnUiThread(()->{if(isFinishing()||isDestroyed())return;new AlertDialog.Builder(this).setTitle("Não foi possível concluir").setMessage(error.getMessage()).setPositiveButton("OK",null).show();});
    }
    void refreshScreen(){if(playing||isDestroyed()||isFinishing())return;if(screen.equals("settings"))settings();else if(screen.equals("detail")&&detailGame!=null)details(detailGame);else if(screen.equals("help"))help();else home();}
    void restoreSession(){
        if(busy)return;busy=true;int ticket=epoch;Auth current=auth;sessionStatus="Restaurando sua sessão…";say(sessionStatus);
        io.execute(()->{
            JSONObject saved=cache.read();
            runOnUiThread(()->{if(ticket!=epoch)return;JSONArray all=saved.optJSONArray("catalog"),history=saved.optJSONArray("recent");if(all!=null)catalog=all;if(history!=null)recent=history;JSONObject storedFeed=saved.optJSONObject("feed");if(storedFeed!=null)feed=storedFeed;refreshScreen();});
            try{current.ensure();runOnUiThread(()->{if(ticket!=epoch)return;busy=false;signedIn=true;sessionStatus="Conta conectada";refreshScreen();load();});}
            catch(Exception error){runOnUiThread(()->{if(ticket!=epoch)return;busy=false;if(error instanceof Auth.SessionExpired){signedIn=false;catalog=new JSONArray();recent=new JSONArray();feed=new JSONObject();licenses.clear();licenseTimes.clear();cache.clear();sessionStatus="Sessão desconectada. Entre na conta.";}else{signedIn=current.hasSavedSession();sessionStatus="Sessão salva · sem conexão. Tente Atualizar.";}refreshScreen();});}
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
        if(playing)return;if(!signedIn){login();return;}if(busy){say("Atualização em andamento…");return;}
        busy=true;lastFeedAttempt=System.currentTimeMillis();int ticket=epoch;Auth current=auth;JSONObject previousFeed=feed;say("Atualizando sua biblioteca…");
        io.execute(()->{try{
            Cloud api=new Cloud(current,prefs.getString("region",""));JSONObject freshFeed=api.feed(previousFeed);JSONArray all=api.catalog(api.withFeed(api.allTitles(),freshFeed));JSONArray history;
            try{history=api.catalog(api.titles(true));}catch(Exception ignored){history=recent;}
            cache.write(all,history,freshFeed);JSONArray finalHistory=history;
            runOnUiThread(()->{if(ticket!=epoch)return;busy=false;catalog=all;recent=finalHistory;feed=freshFeed;licenses.clear();licenseTimes.clear();sessionStatus=freshFeed.optBoolean("partial")?"Conta conectada · algumas categorias aguardam atualização":"Conta conectada · biblioteca atualizada";refreshScreen();});
        }catch(Exception error){runOnUiThread(()->{if(ticket!=epoch)return;busy=false;if(error instanceof Auth.SessionExpired){signedIn=false;catalog=new JSONArray();recent=new JSONArray();feed=new JSONObject();licenses.clear();licenseTimes.clear();cache.clear();sessionStatus="Sessão desconectada. Entre novamente.";}else sessionStatus="Não foi possível atualizar · sua sessão foi mantida";refreshScreen();fail(error);});}});
    }
    void confirmLogout(){new AlertDialog.Builder(this).setTitle("Sair da conta?").setMessage("O login e a biblioteca desta conta serão removidos deste aparelho.").setNegativeButton("Cancelar",null).setPositiveButton("Sair da conta",(dialog,which)->logout()).show();}
    void logout(){
        epoch++;auth.cancelled=true;signedIn=false;busy=true;catalog=new JSONArray();recent=new JSONArray();feed=new JSONObject();licenses.clear();licenseTimes.clear();query="";genre="";page=0;selected=0;prefs.edit().remove("favorites").apply();
        if(loginDialog!=null)loginDialog.dismiss();images.pause();Auth current=auth;
        io.execute(()->{current.logout();cache.clear();runOnUiThread(()->{busy=false;sessionStatus="Você saiu da conta";navigate("home");});});
    }

    void settings(){
        if(playing)return;String wanted=screen.equals("settings")?focusedTag():"nav:settings";screen="settings";detailGame=null;shell();
        TextView title=text("Configurações",24);title.setTypeface(null,Typeface.BOLD);body.addView(title);TextView subtitle=text("Ajuste a imagem, a conexão e sua conta.",12);subtitle.setTextColor(MUTED);body.addView(subtitle);
        LinearLayout sections=new LinearLayout(this);body.addView(sections);
        for(String[] section:new String[][]{{"stream","Streaming"},{"network","Conexão"},{"account","Conta e aplicativo"}}){Button b=button(section[1],()->{settingsPage=section[0];settings();});b.setActivated(settingsPage.equals(section[0]));b.setTag("settings-tab:"+section[0]);sections.addView(b);}
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setClipToPadding(false);LinearLayout content=column();content.setPadding(dp(3),dp(9),dp(3),dp(12));scroll.addView(content);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(settingsPage.equals("stream")){
            choose(content,"Motor de streaming","engine",new String[]{"Native • somente hardware","WebView • WebRTC do sistema"},new String[]{"native","webview"},"native");
            choose(content,"Resolução solicitada","resolution",new String[]{"720p • recomendado","1080p"},new String[]{"720","1080"},"720");
            choose(content,"Limite de bitrate solicitado","bitrate",new String[]{"3 Mb/s","5 Mb/s","8 Mb/s","10 Mb/s","15 Mb/s"},new String[]{"3000","5000","8000","10000","15000"},"5000");
            choose(content,"Codec","codec",new String[]{"H264 • recomendado","VP8","VP9","H265 • se disponível","AV1 • se disponível"},new String[]{"H264","VP8","VP9","H265","AV1"},"H264");
            toggle(content,"Solicitar baixa latência no Native","lowLatency",true);caption(content,"Android 11 ou superior, quando o decodificador oferece suporte.");
            toggle(content,"Exibir estatísticas do streaming","stats",false);microphoneSetting(content);
            Button profile=button("Aplicar perfil leve",()->{prefs.edit().putString("engine","native").putString("resolution","720").putString("bitrate","5000").putString("codec","H264").putBoolean("stats",false).apply();settings();say("Perfil leve aplicado: Native · 720p · H264 · 5 Mb/s");});profile.setTag("profile");content.addView(profile,new LinearLayout.LayoutParams(-1,dp(43)));
            caption(content,"As alterações valem na próxima partida. O servidor pode ajustar resolução, bitrate e codec conforme a sessão.");
        }else if(settingsPage.equals("network")){
            Button server=button("Servidor: "+(prefs.getString("region","").isEmpty()?"Automático":prefs.getString("region",""))+"   ▾",this::regions);server.setTag("first-control");content.addView(server,new LinearLayout.LayoutParams(-1,dp(46)));caption(content,"Escolha entre as regiões disponíveis na sua conta.");
            Button diagnostics=button("Diagnóstico dos decodificadores",this::diagnostics);icon(diagnostics,"settings",WHITE);content.addView(diagnostics,new LinearLayout.LayoutParams(-1,dp(45)));caption(content,"Veja os codecs, o suporte de hardware e a memória informados pelo aparelho.");
        }else{
            LinearLayout accountCard=column();accountCard.setPadding(dp(14),dp(12),dp(14),dp(12));accountCard.setBackground(shape(PANEL));TextView account=text(signedIn?"Sua conta está conectada":"Conecte sua conta Xbox",18);account.setTypeface(null,Typeface.BOLD);accountCard.addView(account);caption(accountCard,signedIn?"Seu login é lembrado automaticamente neste aparelho.":"Autorize o acesso pelo celular para abrir sua biblioteca.");
            Button sign=button(signedIn?"Sair da conta":"Entrar na conta",signedIn?this::confirmLogout:this::login);sign.setTag("first-control");accountCard.addView(sign);content.addView(accountCard);
            Button credits=button("Créditos e licenças",this::licenses);credits.setTag("licenses");content.addView(credits,new LinearLayout.LayoutParams(-1,dp(46)));caption(content,"BXbox Cloud TV · projeto independente, sem vínculo com Microsoft ou Xbox. Código baseado em adaptações de protocolo do XStreaming, sob AGPL-3.0.");
        }
        View first=content.findViewWithTag("setting:engine");if(first!=null)first.setTag("first-control");
        restoreFocus(wanted);
    }
    void caption(LinearLayout parent,String value){TextView hint=text(value,11);hint.setTextColor(MUTED);hint.setPadding(dp(10),dp(6),dp(8),dp(12));parent.addView(hint);}
    void choose(LinearLayout parent,String label,String key,String[] labels,String[] values,String fallback){
        String stored=prefs.getString(key,fallback);int at=0;for(int i=0;i<values.length;i++)if(values[i].equals(stored))at=i;final String[] current={values[at]};
        TextView heading=text(label,11);heading.setTextColor(MUTED);parent.addView(heading);
        Button control=button(labels[at]+"    ▾",()->{});control.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);control.setTextSize(14);control.setTag("setting:"+key);control.setContentDescription(label);
        control.setOnClickListener(v->{int selected=0;for(int i=0;i<values.length;i++)if(values[i].equals(current[0]))selected=i;showChoices(label,labels,selected,control,index->{current[0]=values[index];prefs.edit().putString(key,values[index]).apply();control.setText(labels[index]+"    ▾");});});
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(44));p.setMargins(dp(2),dp(1),dp(2),dp(9));parent.addView(control,p);
    }
    void toggle(LinearLayout parent,String label,String key,boolean fallback){
        Switch control=new Switch(this);control.setId(View.generateViewId());control.setTag("setting:"+key);control.setText(label);control.setTextColor(WHITE);control.setTextSize(13);control.setPadding(dp(13),dp(8),dp(13),dp(8));control.setBackground(focus());control.setFocusable(true);control.setChecked(prefs.getBoolean(key,fallback));control.setOnCheckedChangeListener((v,checked)->prefs.edit().putBoolean(key,checked).apply());LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,dp(49));params.setMargins(dp(2),dp(3),dp(2),dp(4));parent.addView(control,params);
    }
    void licenses(){
        try{StringBuilder terms=new StringBuilder();for(String name:new String[]{"CREDITOS.txt","AGPL-3.0.txt","WebRTC-SDK-MIT.txt","WebRTC-BSD.txt"})try(java.io.InputStream stream=getAssets().open(name);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] bytes=new byte[4096];int n;while((n=stream.read(bytes))!=-1)out.write(bytes,0,n);terms.append(out.toString("UTF-8")).append("\n\n");}showText("Créditos e licenças",terms.toString());}catch(Exception error){fail(error);}
    }
    void showText(String title,String value){TextView content=text(value,12);content.setTextIsSelectable(true);content.setPadding(dp(10),dp(5),dp(10),dp(10));ScrollView scroll=new ScrollView(this);scroll.addView(content);new AlertDialog.Builder(this).setTitle(title).setView(scroll).setPositiveButton("Fechar",null).show();}
    void regions(){
        if(busy){say("Aguarde a operação em andamento");return;}busy=true;int ticket=epoch;io.execute(()->{try{JSONArray regions=auth.regions();String[] names=new String[regions.length()+1],values=new String[regions.length()+1];names[0]="Automático";values[0]="";int selected=0;String current=prefs.getString("region","");for(int i=0;i<regions.length();i++){names[i+1]=regions.getJSONObject(i).getString("name");values[i+1]=names[i+1];if(current.equals(values[i+1]))selected=i+1;}int active=selected;runOnUiThread(()->{if(ticket!=epoch||isDestroyed())return;busy=false;View anchor=root.findViewWithTag("first-control");showChoices("Servidor de streaming",names,active,anchor,index->{prefs.edit().putString("region",values[index]).apply();settings();});});}catch(Exception error){runOnUiThread(()->{if(ticket!=epoch)return;busy=false;fail(error);});}});
    }
    void diagnostics(){
        StringBuilder result=new StringBuilder();ActivityManager manager=(ActivityManager)getSystemService(ACTIVITY_SERVICE);ActivityManager.MemoryInfo memory=new ActivityManager.MemoryInfo();manager.getMemoryInfo(memory);result.append("ABIs: ").append(Arrays.toString(Build.SUPPORTED_ABIS)).append("\nRAM total: ").append(memory.totalMem/1048576).append(" MB\nRAM disponível: ").append(memory.availMem/1048576).append(" MB\n\n");
        for(MediaCodecInfo info:new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()){if(info.isEncoder())continue;for(String type:info.getSupportedTypes())if(type.startsWith("video/")){result.append(info.getName()).append(" · ").append(type);if(Build.VERSION.SDK_INT>=29)result.append(info.isHardwareAccelerated()?" · hardware":" · software");try{if(Build.VERSION.SDK_INT>=30&&info.getCapabilitiesForType(type).isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_LowLatency))result.append(" · baixa latência");}catch(Exception ignored){}result.append('\n');}}
        showText("Diagnóstico do aparelho",result.toString());
    }
    void help(){
        screen="help";detailGame=null;shell();TextView title=text("Pronto para a próxima partida?",24);title.setTypeface(null,Typeface.BOLD);body.addView(title);LinearLayout card=column();card.setPadding(dp(16),dp(14),dp(16),dp(14));card.setBackground(shape(PANEL));body.addView(card,new LinearLayout.LayoutParams(-1,-2));caption(card,"Use as setas do controle para navegar. A borda verde mostra o foco atual. Pressione OK para abrir um jogo ou uma opção.");caption(card,"Abaixo dos filtros, pressione ↓ para entrar nos jogos. Na primeira linha, ↑ volta ao filtro. Na primeira coluna, ← volta ao menu lateral.");caption(card,"Durante uma partida, use Voltar para encerrar. As configurações de streaming estão em Streaming; servidor e diagnóstico ficam em Conexão.");Button back=button("Abrir biblioteca",()->navigate("library"));back.setTag("first-control");primary(back);card.addView(back);back.requestFocus();
    }

    void releaseLibraryViews(){
        licenseRequest++;images.pause();ui.removeCallbacks(searchUpdate);ui.removeCallbacks(refreshFeed);shelves.clear();
        if(choiceDialog!=null){choiceDialog.setOnDismissListener(null);choiceDialog.dismiss();choiceDialog=null;}
        if(loginDialog!=null){loginDialog.dismiss();loginDialog=null;}
        grid=null;adapter=null;detailGame=null;root=null;body=null;tabs=null;sidebar=null;counter=null;
        accountButton=null;heroButton=null;heroImage=null;heroUrl="";sortButton=null;genreButton=null;searchBox=null;navButtons.clear();
    }
 void launch(JSONObject game){if(busy){say("Aguarde a operação em andamento");return;}playing=true;releaseLibraryViews();pad.reset();FrameLayout stage=new FrameLayout(this);stage.setBackgroundColor(Color.BLACK);setContentView(stage);hud=text("Preparando sessão…",14);hud.setBackgroundColor(0x99000000);FrameLayout.LayoutParams overlay=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.LEFT);notice=hud;String title=game.optString("id");Auth current=auth;int height=Integer.parseInt(prefs.getString("resolution","720")),kbps=Integer.parseInt(prefs.getString("bitrate","5000"));String codec=prefs.getString("codec","H264");boolean stats=prefs.getBoolean("stats",false);
  boolean wantedMic=prefs.getBoolean("microphone",false);final boolean microphone=wantedMic&&microphoneGranted()&&microphoneInput();
  if(wantedMic&&!microphone)Toast.makeText(this,"Partida sem microfone: verifique a permissão e conecte uma entrada de áudio reconhecida pela TV.",Toast.LENGTH_LONG).show();
  NativePlayer.Ui callbacks=new NativePlayer.Ui(){public void status(String s){if(playing){say(s);if(s.startsWith("Microfone"))runOnUiThread(()->{if(playing)Toast.makeText(MainActivity.this,s,Toast.LENGTH_LONG).show();});}}public void stats(String s){if(playing&&stats)say(s);}public void error(Exception e){runOnUiThread(()->{if(!playing)return;stop();fail(e);});}public void connected(){runOnUiThread(()->{if(!playing)return;if(!stats)hud.setVisibility(View.GONE);});}};
  busy=true;int ticket=++epoch;io.execute(()->{try{Cloud cloud=new Cloud(current,prefs.getString("region",""));runOnUiThread(()->{if(!playing||ticket!=epoch){busy=false;return;}try{if(prefs.getString("engine","native").equals("native")){nativePlayer=new NativePlayer(this,cloud,pad,height,kbps,codec,stats,microphone,callbacks);stage.addView(nativePlayer.view,new FrameLayout.LayoutParams(-1,-1));nativePlayer.start(title);}else{webPlayer=new WebPlayer(this,cloud,pad,height,kbps,codec,stats,microphone,title,callbacks);stage.addView(webPlayer.view,new FrameLayout.LayoutParams(-1,-1));}stage.addView(hud,overlay);busy=false;}catch(Exception e){busy=false;callbacks.error(e);}});}catch(Exception e){busy=false;callbacks.error(e);}});
 }


    void stop(){epoch++;playing=false;if(nativePlayer!=null){nativePlayer.close();nativePlayer=null;}if(webPlayer!=null){webPlayer.close();webPlayer=null;}images.resume();screen="home";pendingFocus=tab.equals("home")?homeFocus:"grid";home();if(foreground){ui.removeCallbacks(refreshFeed);ui.postDelayed(refreshFeed,60000);}}
    @Override public boolean dispatchKeyEvent(KeyEvent event){if(playing&&(event.getSource()&InputDevice.SOURCE_GAMEPAD)==InputDevice.SOURCE_GAMEPAD&&pad.key(event))return true;return super.dispatchKeyEvent(event);}
    @Override public boolean onGenericMotionEvent(MotionEvent event){if(playing&&pad.motion(event))return true;return super.onGenericMotionEvent(event);}
    @Override public void onBackPressed(){
        if(playing)new AlertDialog.Builder(this).setTitle("Encerrar a partida?").setPositiveButton("Encerrar",(dialog,which)->stop()).setNegativeButton("Continuar",null).show();
        else if(!screen.equals("home")){pendingFocus=screen.equals("detail")?(tab.equals("home")?homeFocus:"grid"):"nav:"+tab;home();}
        else if(!tab.equals("home"))navigate("home");
        else super.onBackPressed();
    }
    @Override protected void onPause(){super.onPause();pad.reset();}
    @Override protected void onStart(){super.onStart();foreground=true;ui.removeCallbacks(refreshFeed);ui.postDelayed(refreshFeed,60000);if(images!=null&&!playing){images.resume();if(adapter!=null)adapter.notifyDataSetChanged();for(TvShelf shelf:shelves)shelf.bind();if(heroImage!=null&&!heroUrl.isEmpty())images.bindHero(heroImage,heroUrl);}}
    @Override protected void onStop(){super.onStop();foreground=false;ui.removeCallbacks(refreshFeed);if(playing&&!isChangingConfigurations())stop();if(images!=null)images.pause();}
    @Override protected void onDestroy(){epoch++;auth.cancelled=true;ui.removeCallbacksAndMessages(null);if(loginDialog!=null)loginDialog.dismiss();if(choiceDialog!=null)choiceDialog.dismiss();if(nativePlayer!=null)nativePlayer.close();if(webPlayer!=null)webPlayer.close();images.close();io.shutdownNow();licenseIo.shutdownNow();super.onDestroy();}

}
