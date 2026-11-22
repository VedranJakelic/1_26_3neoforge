package net.beamex.shamaschizm.grimoire.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

/** Recto/verso leaves: tears remove both faces and expose the next physical sheet. */
public class GrimoireScreen extends Screen {
    private static final int PAGE_W=144,PAGE_H=178,SPREAD_W=292,TEXT_W=110,LINES=13;
    private static final Identifier PAPER=Shamaschizm.id("textures/gui/grimoire/book_ui_1.png");
    private record Page(String artwork,List<FormattedCharSequence> lines,boolean chapter) {}
    private record Layer(int page,int depth,List<int[]> rectangles) {}
    private final List<Page> pages=new ArrayList<>();
    private final List<boolean[]> holes=new ArrayList<>();
    private List<Layer> leftLayers=List.of(),rightLayers=List.of();
    private int spread,left,top;
    private float scale;
    private Button previous,next;
    public GrimoireScreen(){super(Component.translatableWithFallback("item.shamaschizm.grimoire_of_nerodemus","Grimoire of Nerodemus"));}
    @Override protected void init(){
        if(pages.isEmpty())buildPages();
        scale=Math.min(1.9F,Math.min((width-12F)/SPREAD_W,(height-52F)/PAGE_H));
        left=(width-Math.round(SPREAD_W*scale))/2;top=Math.max(20,(height-32-Math.round(PAGE_H*scale))/2);
        int y=Math.min(height-24,top+Math.round(PAGE_H*scale)+4);
        previous=addRenderableWidget(new PageButton(width/2-88,y+3,false,b->turn(-1),true));
        addFooterActions(y);
        next=addRenderableWidget(new PageButton(width/2+65,y+3,true,b->turn(1),true));
        refresh();
    }
    protected void addFooterActions(int y){
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(width/2-54,y,108,20).build());
    }
    private Page blank(){return new Page(null,List.of(),false);}
    private void flush(List<FormattedCharSequence> pending){
        if(!pending.isEmpty()){pages.add(new Page(null,List.copyOf(pending),false));pending.clear();}
    }
    private void append(List<FormattedCharSequence> pending,FormattedCharSequence line){
        if(pending.size()==LINES)flush(pending);
        pending.add(line);
    }
    private void buildPages(){
        for(var chapter:NerodemusContents.CHAPTERS){
            // Each chapter opens on a right-hand recto, with only its centered title.
            if((pages.size()&1)!=0)pages.add(blank());
            pages.add(new Page(null,font.split(Component.literal(chapter.title()),TEXT_W),true));
            List<FormattedCharSequence> pending=new ArrayList<>();
            for(String block:chapter.blocks()){
                if(block.startsWith("[")){
                    flush(pending);
                    String name=block.substring(1,block.length()-1);
                    if(name.equals("Lost Carcosa")){
                        for(String[] stanzaPage:NerodemusContents.POEM){
                            for(String line:stanzaPage){
                                if(line.isEmpty())append(pending,FormattedCharSequence.EMPTY);
                                else for(var wrapped:font.split(Component.literal(line),TEXT_W))append(pending,wrapped);
                            }
                            flush(pending);
                        }
                    }else pages.add(new Page(name,List.of(),false));
                }else{
                    if(!pending.isEmpty()&&pending.size()<LINES-1)append(pending,FormattedCharSequence.EMPTY);
                    for(var line:font.split(Component.literal(block),TEXT_W))append(pending,line);
                }
            }
            flush(pending);
        }
        if((pages.size()&1)!=0)pages.add(blank());
        for(int i=0;i<pages.size();i++){
            String face=pages.get(i).artwork(),back=pages.get(i^1).artwork();
            holes.add(NerodemusPaper.leafMask(NerodemusTears.mask(face==null?"":face),NerodemusTears.mask(back==null?"":back)));
        }
    }
    private List<Layer> layers(int index,int direction){
        List<Layer> layers=new ArrayList<>();boolean[] visible=new boolean[PAGE_W*PAGE_H];Arrays.fill(visible,true);
        for(int depth=0;;depth++,index+=direction){
            boolean valid=index>=0&&index<pages.size();
            boolean[] mask=valid?holes.get(index):new boolean[PAGE_W*PAGE_H];
            var rectangles=NerodemusPaper.rectangles(NerodemusPaper.intersect(visible,mask,false));
            if(!rectangles.isEmpty())layers.add(new Layer(valid?index:-1,depth,rectangles));
            visible=NerodemusPaper.intersect(visible,mask,true);
            if(!valid||NerodemusPaper.rectangles(visible).isEmpty())break;
        }
        return layers;
    }
    private void refresh(){
        previous.active=spread>0;next.active=spread<pages.size()/2;
        leftLayers=layers(spread*2-1,-2);rightLayers=layers(spread*2,2);
    }
    private void turn(int direction){spread=Math.max(0,Math.min(pages.size()/2,spread+direction));refresh();}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isAllowedInPortal(){return true;}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){extractTransparentBackground(g);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        g.centeredText(font,title,width/2,Math.max(3,top-15),0xFFFFE8C1);
        g.pose().pushMatrix();g.pose().translate(left,top);g.pose().scale(scale,scale);
        drawStack(g,leftLayers,0);drawStack(g,rightLayers,PAGE_W+4);
        g.pose().popMatrix();super.extractRenderState(g,mx,my,delta);
    }
    private void drawStack(GuiGraphicsExtractor g,List<Layer> stack,int x){
        for(Layer layer:stack){
            Page page=layer.page()<0?blank():pages.get(layer.page());
            Identifier texture=page.artwork()==null?PAPER:Shamaschizm.id("textures/gui/grimoire/nerodemus/"+page.artwork());
            boolean mirror=page.artwork()==null&&x==0;
            for(int[] r:layer.rectangles()){
                float u0=(mirror?165-r[0]:21+r[0])/256F,u1=(mirror?165-r[2]:21+r[2])/256F;
                g.blit(texture,x+r[0],r[1],x+r[2],r[3],u0,u1,(2+r[1])/256F,(2+r[3])/256F);
            }
            // Clip all ink, including page numbers, against the same physical tear mask.
            for(int[] r:layer.rectangles()){
                g.enableScissor(x+r[0],r[1],x+r[2],r[3]);
                int y=page.chapter()?(PAGE_H-page.lines().size()*10)/2:18;
                for(var line:page.lines()){
                    if(y+font.lineHeight>r[1]&&y<r[3]){
                        int tx=page.chapter()?x+(PAGE_W-font.width(line))/2:x+17;
                        g.text(font,line,tx,y,0xFF362419,false);
                    }
                    y+=10;
                }
                if(layer.page()>=0&&!page.chapter()&&(!page.lines().isEmpty()||page.artwork()!=null)){
                    String number=Integer.toString(layer.page()+1);int nx=x+(PAGE_W-font.width(number))/2,ny=PAGE_H-16;
                    if(ny+font.lineHeight>r[1]&&ny<r[3]){
                        for(int f=2;f>=0;f--)g.fill(nx-2-f,ny-1-f,nx+font.width(number)+2+f,ny+font.lineHeight+f,((18+(2-f)*10)<<24)|0x00FFF1D6);
                        g.text(font,number,nx,ny,0xFF62452C,false);
                    }
                }
                g.disableScissor();
            }
            // Slight shadow per sheet of depth, applied to the image and its lettering alike.
            if(layer.depth()>0){
                g.nextStratum();int alpha=Math.min(90,Math.round(255*(1-(float)Math.pow(.84,layer.depth()))));
                for(int[] r:layer.rectangles())g.fill(x+r[0],r[1],x+r[2],r[3],alpha<<24);
            }
            g.nextStratum();
        }
    }
}
