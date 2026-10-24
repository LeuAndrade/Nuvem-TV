package org.webrtc;

import android.media.*;
import android.os.*;
import android.view.Surface;
import java.nio.ByteBuffer;
import java.util.*;

/** Adapter for the pinned WebRTC SDK. No reflection, no software fallback. */
public final class LowLatencyDecoderFactory implements VideoDecoderFactory {
 private final EglBase.Context context;
 private final String selected;
 private final boolean lowLatency;
 private final HardwareVideoDecoderFactory supported;
 public LowLatencyDecoderFactory(EglBase.Context context,String selected,boolean lowLatency){this.context=context;this.selected=selected;this.lowLatency=lowLatency;supported=new HardwareVideoDecoderFactory(context);}
 public VideoCodecInfo[] getSupportedCodecs(){List<VideoCodecInfo> list=new ArrayList<>();for(VideoCodecInfo info:supported.getSupportedCodecs())if(info.name.equalsIgnoreCase(selected))list.add(info);return list.toArray(new VideoCodecInfo[0]);}
 public VideoDecoder createDecoder(VideoCodecInfo info){
  if(!info.name.equalsIgnoreCase(selected))return null;
  VideoCodecMimeType type=VideoCodecMimeType.valueOf(info.name);
  for(MediaCodecInfo candidate:new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()){
   if(candidate.isEncoder()||!MediaCodecUtils.isHardwareAccelerated(candidate)||!MediaCodecUtils.codecSupportsType(candidate,type))continue;
   MediaCodecInfo.CodecCapabilities caps=candidate.getCapabilitiesForType(type.mimeType());Integer color=MediaCodecUtils.selectColorFormat(MediaCodecUtils.DECODER_COLOR_FORMATS,caps);if(color==null)continue;
   boolean enable=lowLatency&&Build.VERSION.SDK_INT>=30&&caps.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_LowLatency);
   MediaCodecWrapperFactory wrapper=name->new Codec(new MediaCodecWrapperFactoryImpl().createByCodecName(name),enable);
   return new AndroidVideoDecoder(wrapper,candidate.getName(),type,color,context);
  }
  return null;
 }
 private static final class Codec implements MediaCodecWrapper {
  private final MediaCodecWrapper base;private final boolean low;
  Codec(MediaCodecWrapper base,boolean low){this.base=base;this.low=low;}
  public void configure(MediaFormat format,Surface surface,MediaCrypto crypto,int flags){
   if(Build.VERSION.SDK_INT>=30&&low)format.setInteger(MediaFormat.KEY_LOW_LATENCY,1);
   base.configure(format,surface,crypto,flags);
  }
  public void start(){base.start();}public void flush(){base.flush();}public void stop(){base.stop();}public void release(){base.release();}
  public int dequeueInputBuffer(long timeout){return base.dequeueInputBuffer(timeout);}
  public void queueInputBuffer(int index,int offset,int size,long pts,int flags){base.queueInputBuffer(index,offset,size,pts,flags);}
  public int dequeueOutputBuffer(MediaCodec.BufferInfo info,long timeout){return base.dequeueOutputBuffer(info,timeout);}
  public void releaseOutputBuffer(int index,boolean render){base.releaseOutputBuffer(index,render);}
  public MediaFormat getInputFormat(){return base.getInputFormat();}public MediaFormat getOutputFormat(){return base.getOutputFormat();}public MediaFormat getOutputFormat(int index){return base.getOutputFormat(index);}
  public ByteBuffer getInputBuffer(int index){return base.getInputBuffer(index);}public ByteBuffer getOutputBuffer(int index){return base.getOutputBuffer(index);}
  public Surface createInputSurface(){return base.createInputSurface();}public void setParameters(Bundle params){base.setParameters(params);}public MediaCodecInfo getCodecInfo(){return base.getCodecInfo();}
 }
}
