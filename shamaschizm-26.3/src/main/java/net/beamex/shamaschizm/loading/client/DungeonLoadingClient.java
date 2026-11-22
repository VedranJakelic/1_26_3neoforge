package net.beamex.shamaschizm.loading.client;

import java.util.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.loading.DungeonLoadingNetwork;
import net.beamex.shamaschizm.mixin.TrippingPostChainAccessor;
import net.beamex.shamaschizm.mixin.TrippingPostPassAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@EventBusSubscriber(modid=Shamaschizm.MOD_ID,value=Dist.CLIENT)
public final class DungeonLoadingClient {
    private static final Map<UUID,UUID> HIDDEN=new HashMap<>();
    private static DungeonLoadingNetwork.State own;
    private static long start;
    private static int frames,arrivalFrames;
    private static boolean ready,arrivalSent,failed;
    private static final ByteBuffer DATA=ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
    public static boolean active(){return own!=null;}
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        Minecraft mc=Minecraft.getInstance();
        if(mc.getConnection()==null){own=null;HIDDEN.clear();DungeonLoadingNetwork.PENDING.clear();return;}
        DungeonLoadingNetwork.State s;
        while((s=DungeonLoadingNetwork.PENDING.poll())!=null){
            if(s.phase()==2){
                if(s.token().equals(HIDDEN.get(s.player())))HIDDEN.remove(s.player());
                if(own!=null&&own.token().equals(s.token())){own=null;if(mc.gui.screen() instanceof WaitingScreen)mc.gui.setScreen(null);}
            }else{
                HIDDEN.put(s.player(),s.token());
                if(mc.player!=null&&s.player().equals(mc.player.getUUID())){
                    if(own==null||!own.token().equals(s.token())){start=System.nanoTime();frames=arrivalFrames=0;ready=arrivalSent=failed=false;}
                    own=s;
                }
            }
        }
        if(own!=null&&!(mc.gui.screen() instanceof WaitingScreen))mc.gui.setScreen(new WaitingScreen());
    }
    @SubscribeEvent public static void hide(RenderPlayerEvent.Pre<?> e){
        var mc=Minecraft.getInstance();
        if(mc.level==null)return;
        var entity=mc.level.getEntity(e.getRenderState().id);
        if(entity!=null&&HIDDEN.containsKey(entity.getUUID()))e.setCanceled(true);
    }
    /** Called after all GUI/world effects, before the renderer releases frame resources. */
    public static void render(RenderTarget target,GraphicsResourceAllocator allocator){
        if(own==null)return;
        Minecraft mc=Minecraft.getInstance();
        float seconds=(System.nanoTime()-start)/1_000_000_000F;
        int frame=((int)(seconds*20))&31;
        var chain=mc.getShaderManager().getPostChain(Shamaschizm.id(String.format("dungeon_loading_%02d",frame)),LevelTargetBundle.MAIN_TARGETS);
        if(chain==null){
            if(!failed){failed=true;ClientPacketDistributor.sendToServer(new DungeonLoadingNetwork.Ack(own.token(),3));}
            return;
        }
        DATA.clear();DATA.putFloat(seconds).putFloat(target.width/(float)Math.max(1,target.height)).putFloat(0).putFloat(0).flip();
        for(var pass:((TrippingPostChainAccessor)(Object)chain).shamaschizm$passes()){
            var uniforms=((TrippingPostPassAccessor)(Object)pass).shamaschizm$uniforms();
            GpuBuffer b=uniforms.get("LoadingConfig");if(b==null)continue;
            if((b.usage()&GpuBuffer.USAGE_COPY_DST)==0){
                GpuBuffer next=RenderSystem.getDevice().createBuffer(()->"Dungeon loading clock",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,DATA.duplicate());
                uniforms.put("LoadingConfig",next);b.close();
            }else RenderSystem.getDevice().createCommandEncoder().writeToBuffer(b.slice(),DATA.duplicate());
        }
        chain.process(target,allocator);
        // Two completed render invocations give the first frame time to be presented.
        if(!ready&&++frames>=2){ready=true;ClientPacketDistributor.sendToServer(new DungeonLoadingNetwork.Ack(own.token(),1));}
        if(own.phase()==1&&!arrivalSent&&mc.player!=null&&mc.level!=null
                &&mc.level.dimension().identifier().toString().equals(own.dimension())
                &&mc.level.hasChunkAt(own.position())
                &&mc.player.blockPosition().distSqr(own.position())<64){
            if(++arrivalFrames>=3){arrivalSent=true;ClientPacketDistributor.sendToServer(new DungeonLoadingNetwork.Ack(own.token(),2));}
        }else if(own.phase()!=1)arrivalFrames=0;
    }
    private static final class WaitingScreen extends Screen {
        WaitingScreen(){super(Component.empty());}
        @Override public boolean isPauseScreen(){return false;}
        @Override public boolean shouldCloseOnEsc(){return false;}
        @Override public void onClose(){}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float partial){g.fill(0,0,width,height,0xff000000);}
        @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float partial){}
    }
}
