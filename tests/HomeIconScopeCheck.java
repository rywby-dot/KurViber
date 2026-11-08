package io.github.vibernoslop;
public final class HomeIconScopeCheck {
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);}
 public static void main(String[] args){
  String home="com.viber.voip.HomeActivity";
  for(String container:new String[]{"activity_home_toolbar","bottom_nav_bar","fab_container"})check(HomeIconScope.allowed(home,new String[]{"icon",container}),container);
  for(String[] chain:new String[][]{{"iconView","moreContainer"},{"chatInfoIcon","group_settings"},{"textMessageView","balloonView"},{"avatarView","homePager"},{"reactionView","homePager"}})check(!HomeIconScope.allowed(home,chain),"Preserve unrelated UI");
  check(!HomeIconScope.allowed("com.viber.voip.settings.ui.SettingsHeadersActivity",new String[]{"toolbar","icon"}),"Stock settings");
  check(!HomeIconScope.allowed("com.viber.voip.messages.ui.ConversationActivity",new String[]{"fab_container"}),"Do not trust same ID in a different activity");
  System.out.println("Home-only icon scope checks passed");
 }
}
