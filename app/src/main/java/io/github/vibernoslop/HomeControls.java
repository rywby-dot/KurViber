package io.github.vibernoslop;
import android.app.Activity;
import android.view.*;
import android.widget.ImageButton;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import de.robv.android.xposed.XposedHelpers;
final class HomeControls {
 static java.lang.ref.WeakReference<View> lastToolbar = new java.lang.ref.WeakReference<>(null);
 static final int GEAR=0x6e6f7301;
 static void install(View toolbar, Activity a) {
  lastToolbar = new java.lang.ref.WeakReference<>(toolbar);
  HookLog.add("Home toolbar attached: " + toolbar.getClass().getName());
  toolbar.getViewTreeObserver().addOnGlobalLayoutListener(()->refresh(toolbar,a));
  toolbar.post(()->refresh(toolbar,a));
 }
 static void refresh(View toolbar,Activity a) {
  try {
   String title=Options.text("title",Options.DEFAULT_TITLE);
   if(!title.isEmpty()) {
    Object current=XposedHelpers.callMethod(toolbar,"getTitle");
    if(!(current instanceof String)||!title.equals(current))XposedHelpers.callMethod(toolbar,"setTitle",title);
    if(XposedHelpers.callMethod(toolbar,"getLogo")!=null)XposedHelpers.callMethod(toolbar,"setLogo",(Object)null);
   }
   Menu menu=(Menu)XposedHelpers.callMethod(toolbar,"getMenu");
   MenuItem camera=menu.findItem(resource(a,"menu_camera"));
   if(camera!=null&&Options.on(Options.Option.CAMERA)&&camera.isVisible()){camera.setVisible(false);HookLog.add("Home: camera menu item hidden");}
   if(Options.on(Options.Option.MORE_BUTTON)&&menu.findItem(GEAR)==null) {
    MenuItem gear=menu.add(0,GEAR,1000,"More");
    gear.setIcon(new Gear());gear.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    gear.setOnMenuItemClickListener(item->{View more=a.findViewById(resource(a,"bottom_nav_tab_more"));if(more!=null)more.performClick();return true;});
    HookLog.add("Home: gear menu action installed");
   }
   MenuItem search=menu.findItem(resource(a,"menu_search"));
   if(search!=null&&Options.on(Options.Option.SEARCH_FAB))wire(toolbar,a,search);
  } catch(Throwable e){HookLog.add("Home toolbar: "+e.getClass().getSimpleName()+": "+e.getMessage());}
 }
 static int resource(Activity a,String name){return a.getResources().getIdentifier(name,"id","com.viber.voip");}
 static void wire(View v,Activity a,MenuItem search){
  String id=UiHooks.name(v);
  if(id.equals("search_button")||id.equals("menu_search")) {
   HookLog.add("Home: search actions wired / "+id);
   v.setOnClickListener(button -> {
    // The parent is hidden by NoSlop; child visibility still identifies Viber's
    // current action. isShown() would reject every child of that hidden parent.
    for (String name : new String[]{"fab_compose", "fab_open_keypad", "fab_compose_video_call"}) {
     View target = a.findViewById(resource(a, name));
     if (target != null && target.getVisibility() == View.VISIBLE && target.isEnabled() && target.hasOnClickListeners()) {
      target.performClick();
      return;
     }
    }
    HookLog.add("Home: no active floating action available on this screen");
   });
  }
  Appearance.apply(v);
  if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)wire(((ViewGroup)v).getChildAt(i),a,search);
 }
 static final class Gear extends Drawable {
  final Paint paint=new Paint(3);
  public void draw(Canvas c){Rect b=getBounds();float x=b.exactCenterX(),y=b.exactCenterY(),r=Math.min(b.width(),b.height())*.32f;
   paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(r*.26f);paint.setColor(Appearance.accent());
   if(!Options.on(Options.Option.FLAT))paint.setShader(new LinearGradient(b.left,b.top,b.right,b.bottom,new int[]{Appearance.accent(),Appearance.accentLight()},null,Shader.TileMode.CLAMP));else paint.setShader(null);
   c.drawCircle(x,y,r*.72f,paint);c.drawCircle(x,y,r*.25f,paint);
   for(int i=0;i<8;i++){double t=i*Math.PI/4;c.drawLine(x+(float)Math.cos(t)*r*.8f,y+(float)Math.sin(t)*r*.8f,x+(float)Math.cos(t)*r*1.12f,y+(float)Math.sin(t)*r*1.12f,paint);}
  }
  public int getIntrinsicWidth(){return 72;}public int getIntrinsicHeight(){return 72;}
  public void setAlpha(int a){paint.setAlpha(a);}public void setColorFilter(ColorFilter f){paint.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 }
}
