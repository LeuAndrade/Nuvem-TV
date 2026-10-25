package br.com.nuvemtv;
import android.content.Context;import android.media.*;import android.os.Build;
/** Reprodução no fluxo de mídia. Não altera volume, mute global ou rota escolhida pelo usuário. */
final class GameAudio {
 final AudioManager manager;final AudioManager.OnAudioFocusChangeListener listener=change->{};AudioFocusRequest request;boolean requested;
 GameAudio(Context context){manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);}
 static AudioAttributes attributes(){return new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();}
 @SuppressWarnings("deprecation") void start(boolean preserveSocialFocus){if(manager==null||preserveSocialFocus)return;requested=true;if(Build.VERSION.SDK_INT>=26){request=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes()).setOnAudioFocusChangeListener(listener).setWillPauseWhenDucked(false).build();manager.requestAudioFocus(request);}else manager.requestAudioFocus(listener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);}
 @SuppressWarnings("deprecation") void close(){if(!requested||manager==null)return;requested=false;if(Build.VERSION.SDK_INT>=26&&request!=null)manager.abandonAudioFocusRequest(request);else manager.abandonAudioFocus(listener);}
}
