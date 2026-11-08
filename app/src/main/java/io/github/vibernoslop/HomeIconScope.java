package io.github.vibernoslop;
/** Only these containers may have their icon colors changed. */
final class HomeIconScope {
 static boolean allowed(String activity,String[] ancestors){
  if(!"com.viber.voip.HomeActivity".equals(activity))return false;
  for(String id:ancestors)if("activity_home_toolbar".equals(id)||"bottom_nav_bar".equals(id)||"fab_container".equals(id))return true;
  return false;
 }
}
