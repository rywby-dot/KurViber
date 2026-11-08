package io.github.vibernoslop;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
final class RuntimeDiagnostics {
 static String report(Activity activity) {
  StringBuilder out=new StringBuilder("Runtime activity: ").append(activity.getClass().getName()).append('\n');
  // Only class/resource names; never message text, contact names or input values.
  View home=HomeControls.lastToolbar.get();
  if(home!=null){out.append("Home toolbar snapshot:\n");visit(home,out,0,true);}else out.append("Home toolbar reference missing\n");
  visit(activity.getWindow().getDecorView(),out,0,false);
  return out.toString();
 }
 static void visit(View view,StringBuilder out,int depth,boolean toolbar) {
  if(depth>24||out.length()>16000)return;
  String name=UiHooks.name(view);
  boolean inside=toolbar||name.equals("activity_home_toolbar");
  if(inside||name.contains("toolbar")||name.contains("fab_"))out.append(depth).append(' ').append(view.getClass().getName()).append(" id=").append(name).append(" visibility=").append(view.getVisibility()).append(" attached=").append(view.isAttachedToWindow()).append('\n');
  if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)visit(((ViewGroup)view).getChildAt(i),out,depth+1,inside);
 }
}
