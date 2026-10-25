package br.com.nuvemtv;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.util.*;

/** Área oficial remota, sem ponte JavaScript para credenciais ou comandos nativos. */
final class OfficialSocial {
    final MainActivity activity;final Runnable onClose;
    final FrameLayout panel,webHost;final LinearLayout content;WebView web;final Button reopen;final TextView status;
    WebView popup;PermissionRequest pending;boolean expanded=true,closed,microphoneRequested,rendererGone;FrameLayout container;String target="";
    static final Set<String> AUTH_HOSTS=new HashSet<>(Arrays.asList("login.live.com","account.live.com","signup.live.com","consent.live.com","oauth.live.com","login.microsoftonline.com","login.microsoft.com","login.windows.net","account.microsoft.com","account.xbox.com","sisu.xboxlive.com"));
    static boolean permitted(String value){try{Uri uri=Uri.parse(value);String host=uri.getHost();return "https".equals(uri.getScheme())&&uri.getUserInfo()==null&&uri.getPort()==-1&&(host!=null&&(host.equals("www.xbox.com")||host.equals("xbox.com")||AUTH_HOSTS.contains(host)));}catch(Exception ignored){return false;}}
    static boolean microphoneOrigin(String value){try{Uri u=Uri.parse(value);return "https".equals(u.getScheme())&&u.getPort()==-1&&u.getUserInfo()==null&&"www.xbox.com".equals(u.getHost())&&(u.getPath()==null||u.getPath().isEmpty()||u.getPath().equals("/"));}catch(Exception ignored){return false;}}
    static String desktopAgent(String original){java.util.regex.Matcher m=java.util.regex.Pattern.compile("Chrome/[0-9.]+").matcher(original);String version=m.find()?m.group():"Chrome/120.0.0.0";return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "+version+" Safari/537.36";}
    OfficialSocial(MainActivity a,Runnable onClose){
        activity=a;this.onClose=onClose;panel=new FrameLayout(a);panel.setBackgroundColor(MainActivity.BG);panel.setClipChildren(true);content=a.column();panel.addView(content,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout controls=new LinearLayout(a);controls.setGravity(Gravity.CENTER_VERTICAL);controls.setPadding(a.dp(5),a.dp(3),a.dp(5),a.dp(3));
        controls.addView(a.button(a.tr("Voltar ao aplicativo"),this::minimize));controls.addView(a.button(a.tr("Voltar página"),()->{if(popup!=null){if(popup.canGoBack())popup.goBack();else closePopup();}else if(web.canGoBack())web.goBack();}));
        controls.addView(a.button(a.tr("Atualizar"),()->{if(popup!=null)popup.reload();else web.reload();}));controls.addView(a.button(a.tr("Fechar social"),this::close));
        content.addView(controls,new LinearLayout.LayoutParams(-1,a.dp(46)));status=a.text(a.tr("Social Xbox · entre na página oficial para usar grupos e recursos da conta."),10);status.setMaxLines(2);content.addView(status,new LinearLayout.LayoutParams(-1,a.dp(35)));
        webHost=new FrameLayout(a);content.addView(webHost,new LinearLayout.LayoutParams(-1,0,1));web=new WebView(a);configure(web);webHost.addView(web,new FrameLayout.LayoutParams(-1,-1));
        reopen=a.button(a.tr("Social Xbox"),this::expand);reopen.setTextSize(11);reopen.setOnLongClickListener(v->{close();return true;});
    }
    @SuppressLint("SetJavaScriptEnabled") void configure(WebView view){
        view.setBackgroundColor(MainActivity.BG);WebSettings settings=view.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setMediaPlaybackRequiresUserGesture(false);settings.setUseWideViewPort(true);settings.setLoadWithOverviewMode(true);settings.setBuiltInZoomControls(true);settings.setDisplayZoomControls(false);settings.setSupportMultipleWindows(true);settings.setJavaScriptCanOpenWindowsAutomatically(false);settings.setOffscreenPreRaster(false);settings.setUserAgentString(desktopAgent(settings.getUserAgentString()));
        CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(view,true);
        if(Build.VERSION.SDK_INT>=26)view.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT,false);
        view.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest request){if(!request.isForMainFrame())return false;String value=request.getUrl().toString();if(!permitted(value)){status.setText(activity.tr("Esta navegação não pertence ao Xbox ou à autenticação Microsoft."));return true;}if(request.getUrl().getPath()!=null&&request.getUrl().getPath().contains("/play/launch/")){status.setText(activity.tr("Abra os jogos pela biblioteca do aplicativo."));return true;}return false;}
            @Override public void onPageFinished(WebView v,String url){CookieManager.getInstance().flush();if(closed)return;status.setText(activity.tr("Use as opções do Xbox nesta página para amizade, mensagem, convite, grupo e bloqueio."));}
            @Override public void onReceivedError(WebView v,WebResourceRequest request,WebResourceError error){if(request.isForMainFrame()&&!closed)status.setText(activity.tr("Não foi possível carregar o Social Xbox: ")+error.getDescription());}
            @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail detail){rendererGone=true;activity.runOnUiThread(()->{android.widget.Toast.makeText(activity,activity.tr("A área social foi encerrada pelo sistema. Abra-a novamente."),android.widget.Toast.LENGTH_LONG).show();close();});return true;}
        });
        view.setWebChromeClient(new WebChromeClient(){
            @Override public void onPermissionRequest(PermissionRequest request){activity.runOnUiThread(()->requestMicrophone(request));}
            @Override public void onPermissionRequestCanceled(PermissionRequest request){if(pending==request)pending=null;}
            @Override public boolean onCreateWindow(WebView opener,boolean dialog,boolean userGesture,Message message){if(closed||!userGesture)return false;closePopup();popup=new WebView(activity);configure(popup);webHost.addView(popup,new FrameLayout.LayoutParams(-1,-1));WebView.WebViewTransport transport=(WebView.WebViewTransport)message.obj;transport.setWebView(popup);message.sendToTarget();return true;}
            @Override public void onCloseWindow(WebView window){if(window==popup)closePopup();}
        });
    }
    void requestMicrophone(PermissionRequest request){
        if(closed||!microphoneOrigin(request.getOrigin().toString())||!Arrays.asList(request.getResources()).contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)){request.deny();return;}
        if(activity.gameMicrophoneActive()){request.deny();status.setText(activity.tr("Encerre a partida para transferir o microfone do jogo para o grupo."));return;}
        if(activity.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){if(activity.gameAudio!=null)activity.gameAudio.close();microphoneRequested=true;request.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});return;}
        if(pending!=null)pending.deny();pending=request;activity.requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},42);
    }
    void permissionResult(){PermissionRequest request=pending;pending=null;if(request!=null)requestMicrophoneWithResult(request);}
    void requestMicrophoneWithResult(PermissionRequest request){if(!closed&&activity.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED&&microphoneOrigin(request.getOrigin().toString())){if(activity.gameAudio!=null)activity.gameAudio.close();microphoneRequested=true;request.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});}else request.deny();}
    void attach(FrameLayout parent){if(closed)return;container=parent;detach(panel);detach(reopen);parent.addView(panel,new FrameLayout.LayoutParams(-1,-1));FrameLayout.LayoutParams shortcut=new FrameLayout.LayoutParams(activity.dp(112),activity.dp(34),Gravity.TOP|Gravity.CENTER_HORIZONTAL);shortcut.setMargins(0,activity.dp(2),activity.dp(6),0);parent.addView(reopen,shortcut);applySize();}
    void open(String gamertag){if(closed)return;expand();String next="https://www.xbox.com/"+I18n.locale(activity)+"/play/user"+(gamertag.isEmpty()?"":"/"+Uri.encode(gamertag));if(hasMicrophone()&&!target.isEmpty()){status.setText(activity.tr("Social mantido aberto para preservar o grupo. Use a busca e as ações do Xbox nesta página.")+" "+gamertag);return;}if(target.isEmpty()||!gamertag.isEmpty()&&!next.equals(target)){target=next;web.loadUrl(next);}}
    void minimize(){if(closed)return;activity.pad.reset();expanded=false;applySize();activity.getWindow().getDecorView().requestFocus();}
    void expand(){if(closed)return;activity.pad.reset();expanded=true;applySize();web.requestFocus();}
    void applySize(){if(container==null)return;FrameLayout.LayoutParams params=(FrameLayout.LayoutParams)panel.getLayoutParams();params.width=expanded?-1:1;params.height=expanded?-1:1;params.gravity=Gravity.BOTTOM|Gravity.END;panel.setLayoutParams(params);content.setVisibility(expanded?View.VISIBLE:View.INVISIBLE);reopen.setVisibility(expanded?View.GONE:View.VISIBLE);if(expanded)panel.bringToFront();else reopen.bringToFront();}
    boolean isExpanded(){return expanded&&!closed;}boolean hasMicrophone(){return microphoneRequested&&!closed;}
    void closePopup(){if(popup!=null){detach(popup);popup.stopLoading();popup.destroy();popup=null;}}
    void close(){if(closed)return;closed=true;if(pending!=null){pending.deny();pending=null;}closePopup();detach(panel);detach(reopen);try{if(!rendererGone){web.stopLoading();web.loadUrl("about:blank");}}finally{web.destroy();}CookieManager.getInstance().flush();microphoneRequested=false;container=null;onClose.run();}
    static void detach(View view){if(view.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)view.getParent()).removeView(view);}
}
