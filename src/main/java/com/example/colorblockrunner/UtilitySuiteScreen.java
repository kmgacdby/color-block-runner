package com.example.colorblockrunner;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class UtilitySuiteScreen extends Screen {
 private final Screen parent; private int selected=0; private int binding=-1;
 private final String[] names={"Mushroom Stew","Color Block Runner","Arrow Dodge"};
 protected UtilitySuiteScreen(Screen parent){super(Text.literal("Utility Suite"));this.parent=parent;}

 @Override public void render(DrawContext d,int mx,int my,float delta){
  // Do not call renderBackground() or super.render() here: both can redraw the
  // vanilla screen background after our custom UI and cause the menu to appear blurred.
  // Draw our own full-screen backdrop first so the UI remains on the top layer.
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
  drawDetails(d,rx,top+45);

  int bindY=height-78;
  d.fill(rx,bindY,width-30,bindY+30,binding>=0?0xFFF09BD9:0xFF403544);
  String bindText=binding>=0?"PRESS A KEY... (ESC CANCEL)":"BIND KEY: "+keyName(keyFor(selected));
  d.drawCenteredTextWithShadow(textRenderer,bindText,(rx+width-30)/2,bindY+10,binding>=0?0xFF241A24:0xFFFFFFFF);
  d.drawTextWithShadow(textRenderer,"Middle-click the module or click this button to bind.",rx,bindY+38,0xFFAAA1AB);

  int menuY=height-78;
  d.drawTextWithShadow(textRenderer,"Menu key: "+keyName(UnifiedConfig.get().menuKey),left,menuY,0xFFD9D0D9);
  d.drawTextWithShadow(textRenderer,"Click the key label below to change it",left,menuY+18,0xFF938A94);
 }

 private void drawDetails(DrawContext d,int x,int y){
  UnifiedConfig c=UnifiedConfig.get();
  if(selected==0){line(d,x,y,"Enabled",c.stewEnabled);line(d,x,y+30,"Move delay",c.stewDelay*50+" ms");}
  else if(selected==1){line(d,x,y,"Enabled",c.runnerEnabled);line(d,x,y+30,"Search range",c.runnerRange+" blocks");line(d,x,y+60,"Auto sprint",c.runnerSprint);line(d,x,y+90,"Auto jump",c.runnerJump);}
  else{line(d,x,y,"Enabled",c.dodgeEnabled);line(d,x,y+30,"Detection range",c.dodgeRange+" blocks");line(d,x,y+60,"Side-step first",true);line(d,x,y+90,"Auto jump",c.dodgeJump);line(d,x,y+120,"Auto sneak",c.dodgeSneak);line(d,x,y+150,"Ignore own arrows",c.ignoreOwnArrows);line(d,x,y+180,"Show landing point",c.showLanding);}
 }
 private void line(DrawContext d,int x,int y,String a,Object b){d.drawTextWithShadow(textRenderer,a,x,y,0xFFEDE7ED);d.drawTextWithShadow(textRenderer,String.valueOf(b),x+230,y,0xFFFFB8E5);}
 private boolean isEnabled(int i){UnifiedConfig c=UnifiedConfig.get();return i==0?c.stewEnabled:i==1?c.runnerEnabled:c.dodgeEnabled;}
 private int keyFor(int i){UnifiedConfig c=UnifiedConfig.get();return i==0?c.stewKey:i==1?c.runnerKey:c.dodgeKey;}
 private String keyName(int key){try{return InputUtil.fromKeyCode(key,0).getLocalizedText().getString();}catch(Exception e){return "NONE";}}
 private void toggle(int i){UnifiedConfig c=UnifiedConfig.get();if(i==0)c.stewEnabled=!c.stewEnabled;else if(i==1)c.runnerEnabled=!c.runnerEnabled;else c.dodgeEnabled=!c.dodgeEnabled;c.save();}
 private void startBinding(int i){binding=i;}

 @Override public boolean mouseClicked(double mx,double my,int button){
  int left=30,top=52,row=58,w=235;
  if(mx>=left&&mx<=left+w){int i=(int)((my-top)/row);if(i>=0&&i<3){if(button==0){toggle(i);selected=i;}else if(button==1){selected=i;}else if(button==2){selected=i;startBinding(i);}return true;}}
  int rx=295,bindY=height-78;
  if(mx>=rx&&mx<=width-30&&my>=bindY&&my<=bindY+30){startBinding(selected);return true;}
  return super.mouseClicked(mx,my,button);
 }
 @Override public boolean keyPressed(int key,int scan,int mods){
  if(binding>=0){if(key==GLFW.GLFW_KEY_ESCAPE){binding=-1;return true;}UnifiedConfig c=UnifiedConfig.get();if(binding==0)c.stewKey=key;else if(binding==1)c.runnerKey=key;else c.dodgeKey=key;c.save();binding=-1;return true;}
  if(key==GLFW.GLFW_KEY_ESCAPE){close();return true;}
  return super.keyPressed(key,scan,mods);
 }
 @Override public void close(){if(client!=null)client.setScreen(parent);}
}
