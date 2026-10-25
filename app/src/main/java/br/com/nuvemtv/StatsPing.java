package br.com.nuvemtv;
import org.webrtc.*;import java.util.*;
/** RTT do par ICE selecionado, em ms; ausência não vira zero. Sem sonda de rede extra. */
final class StatsPing {
 static Double milliseconds(Map<String,RTCStats> map){
  String pair="";for(RTCStats stat:map.values())if("inbound-rtp".equals(stat.getType())&&("video".equals(stat.getMembers().get("kind"))||"video".equals(stat.getMembers().get("mediaType")))){Object id=stat.getMembers().get("transportId");RTCStats transport=id==null?null:map.get(String.valueOf(id));if(transport!=null){Object selected=transport.getMembers().get("selectedCandidatePairId");if(selected!=null)pair=String.valueOf(selected);}break;}
  if(pair.isEmpty())for(RTCStats stat:map.values())if("transport".equals(stat.getType())){Object selected=stat.getMembers().get("selectedCandidatePairId");if(selected!=null){pair=String.valueOf(selected);break;}}
  if(!pair.isEmpty())return rtt(map.get(pair));
  for(RTCStats stat:map.values())if("candidate-pair".equals(stat.getType())){Map<String,Object> m=stat.getMembers();if(Boolean.TRUE.equals(m.get("selected"))||(Boolean.TRUE.equals(m.get("nominated"))&&"succeeded".equals(m.get("state")))){Double value=rtt(stat);if(value!=null)return value;}}return null;
 }
 static Double rtt(RTCStats stat){if(stat==null||!"candidate-pair".equals(stat.getType()))return null;Object n=stat.getMembers().get("currentRoundTripTime");if(!(n instanceof Number))return null;double value=((Number)n).doubleValue();return Double.isFinite(value)&&value>=0?value*1000:null;}
 static String label(Map<String,RTCStats> map){Double value=milliseconds(map);return value==null?"—":String.format(Locale.US,"%.0f",value);}
}
