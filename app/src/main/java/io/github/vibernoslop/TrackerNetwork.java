package io.github.vibernoslop;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
final class TrackerNetwork {
 static boolean blocked(String host) {
  if(host==null)return false;host=host.toLowerCase(Locale.ROOT);while(host.endsWith("."))host=host.substring(0,host.length()-1);
  for(String domain:new String[]{"graph.facebook.com","graph.facebook.net","connect.facebook.net","www.facebook.com","app-measurement.com","adjust.com","adjust.net","mixpanel.com","amplitude.com","amplreq.com","braze.com","appboy.com"})
   if(host.equals(domain)||host.endsWith("."+domain))return true;
  return false;
 }
 static void install() {
  for(java.lang.reflect.Method m:InetAddress.class.getDeclaredMethods()) {
   if(!(m.getName().equals("getAllByName")||m.getName().equals("getByName")))continue;
   if(m.getParameterCount()==0||m.getParameterTypes()[0]!=String.class)continue;
   XposedBridge.hookMethod(m,new XC_MethodHook(){protected void beforeHookedMethod(MethodHookParam p){
    if(Options.on(Options.Option.ANALYTICS)&&blocked((String)p.args[0]))p.setThrowable(new UnknownHostException("Blocked tracking endpoint"));
   }});
  }
  HookLog.add("Java DNS tracker endpoint blocking installed");
 }
}
