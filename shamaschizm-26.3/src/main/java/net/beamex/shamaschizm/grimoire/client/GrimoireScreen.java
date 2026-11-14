package net.beamex.shamaschizm.grimoire.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Two illustrated pages, then two pages of Cassilda's Song (Chambers, 1895). */
public final class GrimoireScreen extends Screen {
    private static final int PAGE_W=192,PAGE_H=238,SPREAD_W=388;
    private static final Identifier[] IMAGES={image(1),image(2),image(3)};
    private static final String[][] POEM={
        {"Cassilda's Song","","Along the shore the cloud waves break,","The twin suns sink behind the lake,","The shadows lengthen","    In Carcosa.","","Strange is the night where black stars rise,","And strange moons circle through the skies,","But stranger still is","    Lost Carcosa."},
        {"Cassilda's Song","","Songs that the Hyades shall sing,","Where flap the tatters of the King,","Must die unheard in","    Dim Carcosa.","","Song of my soul, my voice is dead,","Die thou, unsung, as tears unshed","Shall dry and die in","    Lost Carcosa.","","Robert W. Chambers, 1895"}
    };
    private int spread;
    private float scale;
    private int left,top;
    private Button previous,next;
    private static Identifier image(int n){return Shamaschizm.id("textures/gui/grimoire/book_ui_"+n+".png");}
    public GrimoireScreen(){super(Component.translatableWithFallback("item.shamaschizm.grimoire_of_nerodemus","Grimoire of Nerodemus"));}
    @Override protected void init(){
        scale=Math.min(1.4F,Math.min((width-12F)/SPREAD_W,(height-52F)/PAGE_H));
        left=(width-Math.round(SPREAD_W*scale))/2;top=Math.max(20,(height-32-Math.round(PAGE_H*scale))/2);
        int y=Math.min(height-24,top+Math.round(PAGE_H*scale)+4);
        previous=addRenderableWidget(new PageButton(width/2-88,y+3,false,b->turn(-1),true));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(width/2-54,y,108,20).build());
        next=addRenderableWidget(new PageButton(width/2+65,y+3,true,b->turn(1),true));
        buttons();
    }
    private void buttons(){previous.active=spread>0;next.active=spread<1;}
    private void turn(int direction){spread=Math.max(0,Math.min(1,spread+direction));buttons();}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isAllowedInPortal(){return true;}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){extractTransparentBackground(g);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        g.centeredText(font,title,width/2,Math.max(3,top-15),0xFFFFE8C1);
        g.pose().pushMatrix();g.pose().translate(left,top);g.pose().scale(scale,scale);
        for(int side=0;side<2;side++){
            int x=side*(PAGE_W+4);
            // Sample only the authored page rectangle; retain original PNG bytes and transparency.
            // Reverse U only for the left artwork: no negative pose scale or mirrored text.
            float u0=(side==0?165F:21F)/256F,u1=(side==0?21F:165F)/256F;
            g.blit(IMAGES[spread==0?side+1:0],x,0,x+PAGE_W,PAGE_H,u0,u1,2F/256F,180F/256F);
            if(spread==1){
                int y=24;
                for(String line:POEM[side]){
                    if(line.isEmpty()){y+=7;continue;}
                    for(var wrapped:font.split(Component.literal(line),148)){
                        g.text(font,wrapped,x+22,y,0xFF362419,false);y+=10;
                    }
                }
            }
            String number=Integer.toString(spread*2+side+1);
            int numberX=x+(PAGE_W-font.width(number))/2,numberY=PAGE_H-20;
            // Four translucent layers feather the tan backing into the page (about 43% at its center).
            for(int feather=3;feather>=0;feather--){
                int alpha=18+(3-feather)*10;
                g.fill(numberX-3-feather,numberY-2-feather,
                        numberX+font.width(number)+3+feather,numberY+font.lineHeight+1+feather,
                        (alpha<<24)|0x00FFF1D6);
            }
            g.text(font,number,numberX,numberY,0xFF62452C,false);
        }
        g.pose().popMatrix();super.extractRenderState(g,mx,my,delta);
    }
}
