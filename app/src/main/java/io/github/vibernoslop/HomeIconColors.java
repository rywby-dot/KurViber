package io.github.vibernoslop;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import de.robv.android.xposed.*;

final class HomeIconColors {
 private static final java.util.Map<View,Drawable> tinted=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
 private static final ThreadLocal<Boolean> applying=ThreadLocal.withInitial(()->false);
 static boolean eligible(View view){
  if(!Options.on(Options.Option.WHITE_HOME_ICONS))return false;
  Activity activity=UiHooks.activity(view.getContext());if(activity==null)return false;
  ArrayList<String> ids=new ArrayList<>();
  for(View current=view;current!=null;){ids.add(UiHooks.name(current));android.view.ViewParent parent=current.getParent();current=parent instanceof View?(View)parent:null;}
  return HomeIconScope.allowed(activity.getClass().getName(),ids.toArray(new String[0]));
 }
 private static boolean fab(View view){return UiHooks.name(view).startsWith("fab_")&&view instanceof ImageView;}
 static void apply(View view){
  if(applying.get()||!eligible(view))return;
  applying.set(true);
  try{
   if(view instanceof ImageView){
    ImageView image=(ImageView)view;Drawable d=image.getDrawable();
    if(d!=null&&tinted.get(view)==d)return;
    if(fab(view)){
     // Keep the original FAB background; tint only its foreground glyph layer.
     if(d instanceof LayerDrawable){LayerDrawable layers=(LayerDrawable)d.mutate();image.setImageTintList(null);image.clearColorFilter();for(int i=1;i<layers.getNumberOfLayers();i++)layers.getDrawable(i).mutate().setTint(Color.WHITE);}
    }else if(d!=null){d.mutate().setTint(Color.WHITE);image.setImageTintList(ColorStateList.valueOf(Color.WHITE));image.setColorFilter(Color.WHITE,PorterDuff.Mode.SRC_IN);}
    if(d!=null)tinted.put(view,d);
   }
   if(view instanceof TextView&&view.getClass().getName().contains("ActionMenuItemView")){
    TextView text=(TextView)view;
    for(Drawable[] icons:new Drawable[][]{text.getCompoundDrawables(),text.getCompoundDrawablesRelative()})for(Drawable d:icons)if(d!=null){d.mutate().setTintMode(PorterDuff.Mode.SRC_IN);d.setTint(Color.WHITE);d.setColorFilter(Color.WHITE,PorterDuff.Mode.SRC_IN);}
    try{XposedHelpers.callMethod(text,"setSupportCompoundDrawablesTintList",ColorStateList.valueOf(Color.WHITE));XposedHelpers.callMethod(text,"setSupportCompoundDrawablesTintMode",PorterDuff.Mode.SRC_IN);}catch(NoSuchMethodError ignored){}
    text.setCompoundDrawableTintMode(PorterDuff.Mode.SRC_IN);
    text.setCompoundDrawableTintList(ColorStateList.valueOf(Color.WHITE));
   }
  }catch(Throwable error){HookLog.add("Home icon tint failed: "+error.getClass().getSimpleName());}
  finally{applying.set(false);}
 }
 static void install(ClassLoader loader){
  XC_MethodHook refresh=new XC_MethodHook(){protected void afterHookedMethod(MethodHookParam p){if(!applying.get())tinted.remove((View)p.thisObject);apply((View)p.thisObject);}};
  for(String method:new String[]{"setImageDrawable","setImageResource","setImageTintList","setColorFilter"})XposedBridge.hookAllMethods(ImageView.class,method,refresh);
  for(String method:new String[]{"setCompoundDrawables","setCompoundDrawablesRelative","setCompoundDrawableTintList"})XposedBridge.hookAllMethods(TextView.class,method,refresh);
  XposedHelpers.findAndHookMethod(ImageView.class,"onDraw",android.graphics.Canvas.class,new XC_MethodHook(){protected void beforeHookedMethod(MethodHookParam p){apply((View)p.thisObject);}});
  // Some home toolbar icons use Viber's separate gradient mask instead of image tint.
  try{
   Class<?> gradient=Class.forName("com.viber.voip.core.ui.widget.GradientImageView",false,loader);
   java.lang.reflect.Field found=null;
   for(java.lang.reflect.Field field:gradient.getDeclaredFields())if(field.getType()==Drawable.class&&!java.lang.reflect.Modifier.isStatic(field.getModifiers())){if(found!=null)throw new IllegalStateException("Ambiguous gradient mask");found=field;}
   if(found!=null){final java.lang.reflect.Field mask=found;mask.setAccessible(true);XposedBridge.hookMethod(gradient.getDeclaredMethod("onDraw",android.graphics.Canvas.class),new XC_MethodHook(){protected void beforeHookedMethod(MethodHookParam p)throws Throwable{if(eligible((View)p.thisObject))mask.set(p.thisObject,new android.graphics.drawable.ColorDrawable(Color.WHITE));}});}
  }catch(Throwable error){HookLog.add("Optional home icon mask unavailable: "+error.getClass().getSimpleName());}
  HookLog.add("Home-only white icon hooks installed; global color hooks removed");
 }
}
