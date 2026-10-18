package net.beamex.shamaschizm.saints.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.saints.CatacombEncounterEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Four synchronized health bars belonging to the nearest active arena. */
public final class SaintsHud {
    private static final Identifier SKULL=Shamaschizm.id("textures/gui/saints_skull.png");
    private static final Identifier BACKGROUND=Identifier.withDefaultNamespace("boss_bar/red_background");
    private static final Identifier PROGRESS=Identifier.withDefaultNamespace("boss_bar/red_progress");
    private SaintsHud(){}
    public static CatacombEncounterEntity active(){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.gui.hud.isHidden())return null;
        return mc.level.getEntitiesOfClass(CatacombEncounterEntity.class,mc.player.getBoundingBox().inflate(64),
                e->e.bossHudActive() && e.distanceToSqr(mc.player)<4096).stream()
                .min(java.util.Comparator.comparingDouble(e->e.distanceToSqr(mc.player))).orElse(null);
    }
    public static void render(GuiGraphicsExtractor graphics,DeltaTracker delta){
        var encounter=active();if(encounter==null)return;
        int middle=graphics.guiWidth()/2;
        int width=Math.min(144,Math.max(40,(graphics.guiWidth()-44)/2));
        for(int i=0;i<4;i++){
            int x=i%2==0?middle-16-width:middle+16;int y=i<2?12:22;
            int filled=Math.round(width*encounter.bossHealth(i));
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED,BACKGROUND,x,y,width,5);
            if(filled>0)graphics.blitSprite(RenderPipelines.GUI_TEXTURED,PROGRESS,width,5,0,0,x,y,filled,5);
        }
        // Preserve the supplied 32x64 image's proportions and pixel detail.
        graphics.blit(RenderPipelines.GUI_TEXTURED,SKULL,middle-64,2,0,0,128,64,128,64);
    }
}
