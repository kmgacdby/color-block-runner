package com.example.colorblockrunner;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class UtilitySuiteScreen extends Screen {
 private final Screen parent; private int selected=0; private int binding=-1; private int dragging=-1;
 private final String[] names={"Mushroom Stew","Color Block Runner","Arrow Dodge"};
 protected UtilitySuiteScreen(Screen parent){super(Text.literal("Utility Suite"));this.parent=parent;}

 @Override public void render(DrawContext d,int mx,int my,float delta){
  d.fill(0,0,width,height,0xE916111E);
  int left=30,top=52,w=235,row=58;
  d.fill(left-8,top-32,left+w+8,top+3,0xFF28212D);
  d.drawTextWithShadow(textRenderer,"UTILITY SUITE",left,top-22,0xFFFFC4EA);
  d.drawTextWithShadow(textRenderer,"Left: toggle   Right: details   Middle: bind",left+w+25,top-22,0xFFBDB4C0);

  for(int i=0;i<3;i++){
   int y=top+i*row; boolean on=isEnabled(i);
   int bg=i==selected?0xFFF09BD9:(on?0xFF76516F:0xFF332C3A);
   d.fill(left,y,left+w,y+46,bg);
   d.drawTextWithShadow(textRenderer,names[i],left+12,y+8,0xFFFFFFFF);
   d.drawTextWithShadow(textRenderer,on?"ON":"OFF",left+12,y+27,on?0xFFFFFFFF:0xFFB9B0BA);
   d.drawTextWithShadow(textRenderer,keyName(keyFor(i)),left+w-58,y+17,0xFFFFFFFF);
  }

  int rx=295;
  d.drawTextWithShadow(textRenderer,names[selected],rx,top,0xFFFFFFFF);
  d.fill(rx,top+22,width-30,top+23,0xFF5B4C5A);
  drawDetails(d,rx,top+45,mx,my);

  int bindY=height-78;
  d.fill(rx,bindY,width-30,bindY+30,binding>=0?0xFFF09BD9:0xFF403544);
  String bindText=binding>=0?"PRESS A KEY... (ESC CANCEL)":"BIND KEY: "+keyName(keyFor(selected));
  d.drawCenteredTextWithShadow(textRenderer,bindText,(rx+width-30)/2,bindY+10,binding>=0?0xFF241A24:0xFFFFFFFF);
  d.drawTextWithShadow(textRenderer,"Middle-click the module or click this button to bind.",rx,bindY+38,0xFFAAA1AB);

  int menuY=height-78;
  d.drawTextWithShadow(textRenderer,"Menu key: "+keyName(UnifiedConfig.get().menuKey),left,menuY,0xFFD9D0D9);
  d.drawTextWithShadow(textRenderer,"Click the key label below to change it",left,menuY+18,0xFF938A94);
 }

 private void drawDetails(DrawContext d,int x,int y,int mx,int my){
  UnifiedConfig c=UnifiedConfig.get();
  if(selected==0){
   toggleRow(d,x,y,"Enabled",c.stewEnabled,mx,my,0);
   slider(d,x,y+38,"Move delay",c.stewDelay,0,20,mx,my,1," ticks");
   d.drawTextWithShadow(textRenderer,"Lower delay = faster transfers",x,y+70,0xFFAAA1AB);
  } else if(selected==1){
   toggleRow(d,x,y,"Enabled",c.runnerEnabled,mx,my,0);
   slider(d,x,y+38,"Search range",c.runnerRange,4,64,mx,my,2," blocks");
   toggleRow(d,x,y+76,"Auto sprint",c.runnerSprint,mx,my,3);
   toggleRow(d,x,y+114,"Auto jump",c.runnerJump,mx,my,4);
  } else {
   toggleRow(d,x,y,"Enabled",c.dodgeEnabled,mx,my,0);
   slider(d,x,y+38,"Detection range",c.dodgeRange,4,64,mx,my,2," blocks");
   d.drawTextWithShadow(textRenderer,"Side-step first",x,y+76,0xFFEDE7ED);
   d.drawTextWithShadow(textRenderer,"ALWAYS",x+230,y+76,0xFFFFB8E5);
   toggleRow(d,x,y+114,"Auto jump",c.dodgeJump,mx,my,3);
   toggleRow(d,x,y+152,"Auto sneak",c.dodgeSneak,mx,my,4);
   toggleRow(d,x,y+190,"Ignore own arrows",c.ignoreOwnArrows,mx,my,5);
   toggleRow(d,x,y+228,"Show landing point",c.showLanding,mx,my,6);
  }
 }

 private void toggleRow(DrawContext d,int x,int y,String label,boolean value,int mx,int my,int id){
  boolean hover=mx>=x&&mx<=width-30&&my>=y-4&&my<=y+24;
  if(hover)d.fill(x-6,y-7,width-30,y+27,0x443F3643);
  d.drawTextWithShadow(textRenderer,label,x,y,0xFFEDE7ED);
  d.drawTextWithShadow(textRenderer,value?"ON":"OFF",x+230,y,value?0xFFFFB8E5:0xFF9F959F);
 }

 private void slider(DrawContext d,int x,int y,String label,int value,int min,int max,int mx,int my,int id,String suffix){
  d.drawTextWithShadow(textRenderer,label,x,y,0xFFEDE7ED);
  d.drawTextWithShadow(textRenderer,value+suffix,x+230,y,0xFFFFB8E5);
  int sy=y+18, sx=x, sw=width-30-x;
  d.fill(sx,sy,sx+sw,sy+4,0xFF514653);
  int knob=sx+(int)((value-min)/(double)(max-min)*sw);
  d.fill(sx,sy,knob,sy+4,0xFFE9A0D8);
  d.fill(knob-4,sy-4,knob+4,sy+12,0xFFF5B5E3);
 }

 private boolean isEnabled(int i){UnifiedConfig c=UnifiedConfig.get();return i==0?c.stewEnabled:i==1?c.runnerEnabled:c.dodgeEnabled;}
 private int keyFor(int i){UnifiedConfig c=UnifiedConfig.get();return i==0?c.stewKey:i==1?c.runnerKey:c.dodgeKey;}
 private String keyName(int key){try{return InputUtil.fromKeyCode(key,0).getLocalizedText().getString();}catch(Exception e){return "NONE";}}
 private void toggle(int i){UnifiedConfig c=UnifiedConfig.get();if(i==0)c.stewEnabled=!c.stewEnabled;else if(i==1)c.runnerEnabled=!c.runnerEnabled;else c.dodgeEnabled=!c.dodgeEnabled;c.save();}
 private void toggleDetail(UnifiedConfig c,int id){
  if(selected==0)return;
  if(selected==1){if(id==3)c.runnerSprint=!c.runnerSprint;else if(id==4)c.runnerJump=!c.runnerJump;}
  else {if(id==3)c.dodgeJump=!c.dodgeJump;else if(id==4)c.dodgeSneak=!c.dodgeSneak;else if(id==5)c.ignoreOwnArrows=!c.ignoreOwnArrows;else if(id==6)c.showLanding=!c.showLanding;}
  c.save();
 }
 private void setSlider(UnifiedConfig c,int id,double mouseX){
  int x=295, sw=width-30-x; double t=Math.max(0,Math.min(1,(mouseX-x)/(double)sw));
  if(id==1)c.stewDelay=(int)Math.round(t*20);
  else if(id==2){int min=4,max=64;if(selected==1)c.runnerRange=min+(int)Math.round(t*(max-min));else c.dodgeRange=min+(int)Math.round(t*(max-min));}
  c.save();
 }
 private void startBinding(int i){binding=i;}

 @Override public boolean mouseClicked(double mx,double my,int button){
  int left=30,top=52,row=58,w=235;
  if(mx>=left&&mx<=left+w){int i=(int)((my-top)/row);if(i>=0&&i<3){if(button==0){toggle(i);selected=i;}else if(button==1){selected=i;}else if(button==2){selected=i;startBinding(i);}return true;}}
  int rx=295,bindY=height-78;
  if(mx>=rx&&mx<=width-30&&my>=bindY&&my<=bindY+30){startBinding(selected);return true;}
  if(mx>=rx&&mx<=width-30){
   UnifiedConfig c=UnifiedConfig.get();
   if(selected==0){if(my>=97&&my<=125){setSlider(c,1,mx);dragging=1;return true;}}
   else if(selected==1){
    if(my>=135&&my<=170){setSlider(c,2,mx);dragging=2;return true;}
    if(my>=171&&my<=203){toggleDetail(c,3);return true;}
    if(my>=209&&my<=241){toggleDetail(c,4);return true;}
   } else {
    if(my>=135&&my<=170){setSlider(c,2,mx);dragging=2;return true;}
    if(my>=209&&my<=241){toggleDetail(c,3);return true;}
    if(my>=247&&my<=279){toggleDetail(c,4);return true;}
    if(my>=285&&my<=317){toggleDetail(c,5);return true;}
    if(my>=323&&my<=355){toggleDetail(c,6);return true;}
   }
  }
  return super.mouseClicked(mx,my,button);
 }

 @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){
  if(dragging>=0){setSlider(UnifiedConfig.get(),dragging,mx);return true;}
  return super.mouseDragged(mx,my,button,dx,dy);
 }
 @Override public boolean mouseReleased(double mx,double my,int button){dragging=-1;return super.mouseReleased(mx,my,button);}

 @Override public boolean keyPressed(int key,int scan,int mods){
  if(binding>=0){if(key==GLFW.GLFW_KEY_ESCAPE){binding=-1;return true;}UnifiedConfig c=UnifiedConfig.get();if(binding==0)c.stewKey=key;else if(binding==1)c.runnerKey=key;else c.dodgeKey=key;c.save();binding=-1;return true;}
  if(key==GLFW.GLFW_KEY_ESCAPE){close();return true;}
  return super.keyPressed(key,scan,mods);
 }
 @Override public void close(){if(client!=null)client.setScreen(parent);}
}
