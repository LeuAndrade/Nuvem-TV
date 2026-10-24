package br.com.nuvemtv;
import android.view.*;
final class Pad {
 int buttons; final float[] axes=new float[6];
 boolean key(KeyEvent e){if(e.getKeyCode()==KeyEvent.KEYCODE_BUTTON_L2||e.getKeyCode()==KeyEvent.KEYCODE_BUTTON_R2){axes[e.getKeyCode()==KeyEvent.KEYCODE_BUTTON_L2?4:5]=e.getAction()==KeyEvent.ACTION_DOWN?1:0;return true;}int mask=switch(e.getKeyCode()){
 case KeyEvent.KEYCODE_BUTTON_MODE->2;case KeyEvent.KEYCODE_BUTTON_START->4;case KeyEvent.KEYCODE_BUTTON_SELECT->8;
 case KeyEvent.KEYCODE_BUTTON_A->16;case KeyEvent.KEYCODE_BUTTON_B->32;case KeyEvent.KEYCODE_BUTTON_X->64;case KeyEvent.KEYCODE_BUTTON_Y->128;
 case KeyEvent.KEYCODE_DPAD_UP->256;case KeyEvent.KEYCODE_DPAD_DOWN->512;case KeyEvent.KEYCODE_DPAD_LEFT->1024;case KeyEvent.KEYCODE_DPAD_RIGHT->2048;
 case KeyEvent.KEYCODE_BUTTON_L1->4096;case KeyEvent.KEYCODE_BUTTON_R1->8192;case KeyEvent.KEYCODE_BUTTON_THUMBL->16384;case KeyEvent.KEYCODE_BUTTON_THUMBR->32768;default->0;};
 if(mask==0)return false;if(e.getAction()==KeyEvent.ACTION_DOWN)buttons|=mask;else if(e.getAction()==KeyEvent.ACTION_UP)buttons&=~mask;return true;}
 boolean motion(MotionEvent e){if((e.getSource()&InputDevice.SOURCE_JOYSTICK)!=InputDevice.SOURCE_JOYSTICK)return false;axes[0]=dead(e.getAxisValue(MotionEvent.AXIS_X));axes[1]=-dead(e.getAxisValue(MotionEvent.AXIS_Y));axes[2]=dead(e.getAxisValue(MotionEvent.AXIS_Z));axes[3]=-dead(e.getAxisValue(MotionEvent.AXIS_RZ));axes[4]=Math.max(e.getAxisValue(MotionEvent.AXIS_LTRIGGER),e.getAxisValue(MotionEvent.AXIS_BRAKE));axes[5]=Math.max(e.getAxisValue(MotionEvent.AXIS_RTRIGGER),e.getAxisValue(MotionEvent.AXIS_GAS));
 int[] bits={1024,2048,256,512};float x=e.getAxisValue(MotionEvent.AXIS_HAT_X),y=e.getAxisValue(MotionEvent.AXIS_HAT_Y);boolean[] on={x<-.5,x>.5,y<-.5,y>.5};for(int i=0;i<4;i++)if(on[i])buttons|=bits[i];else buttons&=~bits[i];return true;}
 float dead(float f){return Math.abs(f)<.12?0:f;}void reset(){buttons=0;java.util.Arrays.fill(axes,0);}
}
