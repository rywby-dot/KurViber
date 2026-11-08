package io.github.vibernoslop;
import android.view.View;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
final class Appearance {
 static int accent(){try{return Color.parseColor(Options.text("accent",Options.DEFAULT_ACCENT));}catch(Exception e){return 0xffffffff;}}
 static int accentLight(){float[] h=new float[3];Color.colorToHSV(accent(),h);h[1]*=.7f;h[2]=Math.min(1,h[2]*1.15f);return Color.HSVToColor(h);}
 static void install(ClassLoader loader) {
  try {
   Class<?> cls=Class.forName("com.viber.voip.core.ui.widget.GradientImageView",false,loader);
   java.lang.reflect.Field gradient=null;
   for(java.lang.reflect.Field f:cls.getDeclaredFields())if(f.getType()==android.graphics.drawable.Drawable.class&&!java.lang.reflect.Modifier.isStatic(f.getModifiers()))gradient=f;
   if(gradient!=null){final java.lang.reflect.Field field=gradient;field.setAccessible(true);
    de.robv.android.xposed.XposedBridge.hookMethod(cls.getDeclaredMethod("onDraw",android.graphics.Canvas.class),new de.robv.android.xposed.XC_MethodHook(){protected void beforeHookedMethod(MethodHookParam p)throws Throwable{
     if(!Options.on(Options.Option.FLAT)&&!Options.on(Options.Option.ACCENT))return;
     android.graphics.drawable.Drawable drawable;
     if(Options.on(Options.Option.FLAT))drawable=new android.graphics.drawable.ColorDrawable(accent());
     else drawable=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{accent(),accentLight()});
     field.set(p.thisObject,drawable);
    }});HookLog.add("Appearance: gradient image mask installed");
   }
  }catch(Throwable e){HookLog.add("Gradient mask unavailable: "+e.getClass().getSimpleName());}
  for(String name:new String[]{"com.viber.voip.core.ui.dynamicblur.DynamicBlurLayout","com.viber.voip.core.ui.BlurFrameLayout"})try{
   Class<?> cls=Class.forName(name,false,loader);
   final boolean dynamic=name.contains("dynamicblur");
   java.lang.reflect.Method method=dynamic?cls.getDeclaredMethod("draw",android.graphics.Canvas.class):cls.getDeclaredMethod("drawChild",android.graphics.Canvas.class,View.class,long.class);
   final java.lang.reflect.Method base=dynamic?View.class.getDeclaredMethod("draw",android.graphics.Canvas.class):android.view.ViewGroup.class.getDeclaredMethod("drawChild",android.graphics.Canvas.class,View.class,long.class);
   base.setAccessible(true);
   de.robv.android.xposed.XposedBridge.hookMethod(method,new de.robv.android.xposed.XC_MethodHook(){protected void beforeHookedMethod(MethodHookParam p)throws Throwable{
    if(Options.on(Options.Option.NO_BLUR))p.setResult(de.robv.android.xposed.XposedBridge.invokeOriginalMethod(base,p.thisObject,p.args));
   }});HookLog.add("Appearance: blur bypass installed / "+name);
  }catch(Throwable e){HookLog.add("Blur bypass unavailable: "+name);}
  de.robv.android.xposed.XposedBridge.hookAllMethods(android.content.res.TypedArray.class,"getColor",new de.robv.android.xposed.XC_MethodHook(){protected void afterHookedMethod(MethodHookParam p){
   if(Options.on(Options.Option.ACCENT)&&p.getResult() instanceof Integer&&purple((Integer)p.getResult()))p.setResult(accent());
  }});
 }
 static boolean purple(int c) { float[] hsv=new float[3];Color.colorToHSV(c,hsv);return hsv[0]>=245&&hsv[0]<=290&&hsv[1]>.25f; }
 static void apply(View view) {
  if(!(view.getBackground() instanceof GradientDrawable))return;
  GradientDrawable bg=(GradientDrawable)view.getBackground();int[] colors=bg.getColors();
  if(colors==null||colors.length==0)return;
  boolean match=false;for(int c:colors)match|=purple(c);if(!match)return;
  if(Options.on(Options.Option.FLAT)||Options.on(Options.Option.ACCENT)) {
   int color=colors[0];try{if(Options.on(Options.Option.ACCENT))color=Color.parseColor(Options.text("accent",Options.DEFAULT_ACCENT));}catch(IllegalArgumentException ignored){}
   GradientDrawable copy=(GradientDrawable)bg.mutate();
   if(Options.on(Options.Option.FLAT))copy.setColor(color);
   else {int[] changed=colors.clone();for(int i=0;i<changed.length;i++)if(purple(changed[i]))changed[i]=color;copy.setColors(changed);}
  }
 }
}
