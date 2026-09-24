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
  renderBackground(d,mx,my,delta); int left=30, top=35, w=220, row=52;
  d.fill(20,20,width-20,height-20,0xE916111E); d.drawTextWithShadow(textRenderer,"UTILITY SUITE",left,25,0xFFFFFF);
  for(int i=0;i<3;i++){int y=top+i*row; boolean on=isEnabled(i); int bg=i==selected?0xFFF09BD9:(on?0xFF8E6687:0xFF332C3A); d.fill(left,y,left+w,y+42,bg); d.drawTextWithShadow(textRenderer,names[i],left+12,y+8,0xFFFFFF);d.drawTextWithShadow(textRenderer,on?"ON":"OFF",left+12,y+25,on?0xFFE8BFE0:0xFF999099);}
  int rx=285; d.drawTextWithShadow(textRenderer,names[selected],rx,48,0xFFFFFF); d.drawTextWithShadow(textRenderer,"Left click: toggle    Right click: settings    Middle: bind",rx,68,0xFFB7AEB8);
  drawDetails(d,rx,100);
  if(binding>=0)d.drawCenteredTextWithShadow(textRenderer,"Press a key to bind  (ESC cancels)",width/2,height-45,0xFFFFAA);
  super.render(d,mx,my,delta);
 }
 private void drawDetails(DrawContext d,int x,int y){UnifiedConfig c=UnifiedConfig.get();if(selected==0){line(d,x,y,"Enabled",c.stewEnabled);line(d,x,y+30,"Move delay",c.stewDelay*50+" ms");line(d,x,y+60,"Key",InputUtil.fromKeyCode(c.stewKey,0).getLocalizedText().getString());}else if(selected==1){line(d,x,y,"Enabled",c.runnerEnabled);line(d,x,y+30,"Search range",c.runnerRange+" blocks");line(d,x,y+60,"Auto sprint",c.runnerSprint);line(d,x,y+90,"Auto jump",c.runnerJump);line(d,x,y+120,"Key",InputUtil.fromKeyCode(c.runnerKey,0).getLocalizedText().getString());}else{line(d,x,y,"Enabled",c.dodgeEnabled);line(d,x,y+30,"Detection range",c.dodgeRange+" blocks");line(d,x,y+60,"Side-step first",true);line(d,x,y+90,"Auto jump",c.dodgeJump);line(d,x,y+120,"Auto sneak",c.dodgeSneak);line(d,x,y+150,"Ignore own arrows",c.ignoreOwnArrows);line(d,x,y+180,"Show landing point",c.showLanding);line(d,x,y+210,"Key",InputUtil.fromKeyCode(c.dodgeKey,0).getLocalizedText().getString());}}
 private void line(DrawContext d,int x,int y,String a,Object b){d.drawTextWithShadow(textRenderer,a,x,y,0xFFEDE7ED);d.drawTextWithShadow(textRenderer,String.valueOf(b),x+230,y,0xFFFFB8E5);}
 private boolean isEnabled(int i){UnifiedConfig c=UnifiedConfig.get();return i==0?c.stewEnabled:i==1?c.runnerEnabled:c.dodgeEnabled;}
 private void toggle(int i){UnifiedConfig c=UnifiedConfig.get();if(i==0)c.stewEnabled=!c.stewEnabled;else if(i==1)c.runnerEnabled=!c.runnerEnabled;else c.dodgeEnabled=!c.dodgeEnabled;c.save();}
 @Override public boolean mouseClicked(double mx,double my,int button){int left=30,top=35,row=52,w=220;if(mx>=left&&mx<=left+w){int i=(int)((my-top)/row);if(i>=0&&i<3){if(button==0){toggle(i);selected=i;}else if(button==1){selected=i;}else if(button==2){selected=i;binding=i;}return true;}}return super.mouseClicked(mx,my,button);}
 @Override public boolean keyPressed(int key,int scan,int mods){if(binding>=0){if(key==GLFW.GLFW_KEY_ESCAPE){binding=-1;return true;}UnifiedConfig c=UnifiedConfig.get();if(binding==0)c.stewKey=key;else if(binding==1)c.runnerKey=key;else c.dodgeKey=key;c.save();binding=-1;return true;}return super.keyPressed(key,scan,mods);}
 @Override public void close(){if(client!=null)client.setScreen(parent);}
}
