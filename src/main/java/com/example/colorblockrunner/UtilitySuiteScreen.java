package com.example.colorblockrunner;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Modern card-style settings UI. */
public final class UtilitySuiteScreen extends Screen {
    private final Screen parent;
    private int selected = 0, binding = -1, dragging = -1;
    private final String[] names = {"蘑菇煲自动搬运", "同色方块跑酷", "方块记忆虚影"};

    protected UtilitySuiteScreen(Screen parent) { super(Text.literal("Utility Suite")); this.parent = parent; }

    @Override public void render(DrawContext d, int mx, int my, float delta) {
        d.fill(0, 0, width, height, 0xEE0E1017);
        int left = 34, top = 48, w = 250, row = 62, right = 310;
        d.fill(left - 10, top - 34, width - 30, height - 34, 0xCC171A23);
        d.drawTextWithShadow(textRenderer, "UTILITY", left, top - 21, 0xFFF2F4F8);
        d.drawTextWithShadow(textRenderer, "功能中心", left + 57, top - 21, 0xFF9EA7B7);
        d.drawTextWithShadow(textRenderer, "左键 开关   ·   右键 详情   ·   中键 绑定", left + 250, top - 21, 0xFF8E97A7);

        for (int i = 0; i < 3; i++) {
            int y = top + i * row; boolean on = isEnabled(i); boolean sel = i == selected;
            d.fill(left, y, left + w, y + 50, sel ? 0xFF303744 : 0xFF20242D);
            d.fill(left, y, left + 4, y + 50, on ? 0xFF7FD4B1 : 0xFF5C6471);
            d.drawTextWithShadow(textRenderer, names[i], left + 16, y + 9, 0xFFF2F4F8);
            d.drawTextWithShadow(textRenderer, on ? "已开启" : "已关闭", left + 16, y + 29, on ? 0xFF8FE1BC : 0xFF8D95A3);
            d.drawTextWithShadow(textRenderer, keyName(keyFor(i)), left + w - 56, y + 19, 0xFFDDE2EA);
        }

        int rx = right, ry = top;
        d.drawTextWithShadow(textRenderer, names[selected], rx, ry, 0xFFF2F4F8);
        d.drawTextWithShadow(textRenderer, "DETAILS", rx, ry + 20, 0xFF737D8C);
        d.fill(rx, ry + 38, width - 48, ry + 39, 0xFF343A45);
        drawDetails(d, rx, ry + 57, mx, my);

        int bindY = height - 67;
        d.fill(rx, bindY, width - 48, bindY + 28, binding >= 0 ? 0xFF4B6D62 : 0xFF292E38);
        d.drawCenteredTextWithShadow(textRenderer, binding >= 0 ? "按下键盘按键 · ESC 取消" : "中键绑定：" + keyName(keyFor(selected)), (rx + width - 48) / 2, bindY + 9, 0xFFEAF0F4);
    }

    private void drawDetails(DrawContext d, int x, int y, int mx, int my) {
        UnifiedConfig c = UnifiedConfig.get();
        if (selected == 0) {
            rowText(d, x, y, "状态", c.stewEnabled); slider(d, x, y + 42, "搬运间隔", c.stewDelay, 50, 2000, 1, " ms");
            d.drawTextWithShadow(textRenderer, "仅在实际打开背包界面时工作；副手不参与", x, y + 77, 0xFF87909E);
        } else if (selected == 1) {
            rowText(d, x, y, "状态", c.runnerEnabled); slider(d, x, y + 42, "搜索范围", c.runnerRange, 4, 64, 2, " 格");
            rowTextAt(d, x, y + 84, "疾跑", c.runnerSprint); rowTextAt(d, x, y + 120, "跳跃", c.runnerJump);
        } else {
            rowText(d, x, y, "状态", c.ghostEnabled); slider(d, x, y + 42, "记录范围", c.ghostRange, 4, 32, 3, " 格");
            d.drawTextWithShadow(textRenderer, "原方块消失后显示记忆虚影", x, y + 82, 0xFF87909E);
            d.drawTextWithShadow(textRenderer, "中键点按虚影可暂时取消；恢复后再次消失会重现", x, y + 100, 0xFF87909E);
            d.fill(x, y + 118, width - 48, y + 148, 0xFF303640);
            d.drawCenteredTextWithShadow(textRenderer, "清除全部虚影", (x + width - 48) / 2, y + 128, 0xFFE8EDF3);
        }
    }
    private void rowText(DrawContext d, int x, int y, String label, boolean on) { rowTextAt(d,x,y,label,on); }
    private void rowTextAt(DrawContext d, int x, int y, String label, boolean on) { d.drawTextWithShadow(textRenderer,label,x,y,0xFFE5E9EF); d.drawTextWithShadow(textRenderer,on?"ON":"OFF",x+220,y,on?0xFF8FE1BC:0xFF858E9B); }
    private void slider(DrawContext d,int x,int y,String label,int value,int min,int max,int id,String suffix){
        d.drawTextWithShadow(textRenderer,label,x,y,0xFFE5E9EF); d.drawTextWithShadow(textRenderer,value+suffix,x+205,y,0xFFAFC6FF);
        int sy=y+18, sw=width-48-x; d.fill(x,sy,x+sw,sy+3,0xFF3C424D); int knob=x+(int)((value-min)/(double)(max-min)*sw); d.fill(x,sy,knob,sy+3,0xFF86A9FF); d.fill(knob-4,sy-4,knob+4,sy+11,0xFFDDE8FF);
    }
    private boolean isEnabled(int i){UnifiedConfig c=UnifiedConfig.get();return i==0?c.stewEnabled:i==1?c.runnerEnabled:c.ghostEnabled;}
    private int keyFor(int i){UnifiedConfig c=UnifiedConfig.get();return i==0?c.stewKey:i==1?c.runnerKey:c.ghostKey;}
    private String keyName(int key){try{return InputUtil.fromKeyCode(key,0).getLocalizedText().getString();}catch(Exception e){return "NONE";}}
    private void toggle(int i){UnifiedConfig c=UnifiedConfig.get();if(i==0)c.stewEnabled=!c.stewEnabled;else if(i==1)c.runnerEnabled=!c.runnerEnabled;else c.ghostEnabled=!c.ghostEnabled;c.save();}
    private void setSlider(UnifiedConfig c,int id,double mouseX){int x=310,sw=width-48-x;double t=Math.max(0,Math.min(1,(mouseX-x)/(double)sw));if(id==1)c.stewDelay=50+(int)Math.round(t*1950);else if(id==2)c.runnerRange=4+(int)Math.round(t*60);else c.ghostRange=4+(int)Math.round(t*28);c.save();}
    private void toggleDetail(UnifiedConfig c,int id){if(selected==1){if(id==4)c.runnerSprint=!c.runnerSprint;else if(id==5)c.runnerJump=!c.runnerJump;}c.save();}
    private void startBinding(){binding=selected;}

    @Override public boolean mouseClicked(double mx,double my,int button){
        int left=34,top=48,w=250,row=62;
        if(mx>=left&&mx<=left+w){int i=(int)((my-top)/row);if(i>=0&&i<3){if(button==0){toggle(i);selected=i;}else if(button==1)selected=i;else if(button==2){selected=i;startBinding();}return true;}}
        int x=310;
        if(mx>=x&&mx<=width-48){UnifiedConfig c=UnifiedConfig.get();int y=top+57;
            if(selected==0&&my>=y+35&&my<=y+75){setSlider(c,1,mx);dragging=1;return true;}
            if(selected==1){if(my>=y+35&&my<=y+75){setSlider(c,2,mx);dragging=2;return true;}if(my>=y+76&&my<=y+112){toggleDetail(c,4);return true;}if(my>=y+113&&my<=y+149){toggleDetail(c,5);return true;}}
            if(selected==2){if(my>=y+35&&my<=y+75){setSlider(c,3,mx);dragging=3;return true;}if(my>=y+118&&my<=y+150){BlockMemoryGhost.clearAll();if(client!=null&&client.player!=null)client.player.sendMessage(Text.literal("方块记忆虚影：已清除全部"),true);return true;}}
        }
        return super.mouseClicked(mx,my,button);
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){if(dragging>0){setSlider(UnifiedConfig.get(),dragging,mx);return true;}return super.mouseDragged(mx,my,button,dx,dy);}
    @Override public boolean mouseReleased(double mx,double my,int button){dragging=-1;return super.mouseReleased(mx,my,button);}
    @Override public boolean keyPressed(int key,int scan,int mods){if(binding>=0){if(key==GLFW.GLFW_KEY_ESCAPE){binding=-1;return true;}UnifiedConfig c=UnifiedConfig.get();if(binding==0)c.stewKey=key;else if(binding==1)c.runnerKey=key;else c.ghostKey=key;c.save();binding=-1;return true;}if(key==GLFW.GLFW_KEY_ESCAPE){close();return true;}return super.keyPressed(key,scan,mods);}
    @Override public void close(){if(client!=null)client.setScreen(parent);}
}
