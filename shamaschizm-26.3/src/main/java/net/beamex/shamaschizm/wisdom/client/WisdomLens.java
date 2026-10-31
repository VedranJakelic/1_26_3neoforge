package net.beamex.shamaschizm.wisdom.client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.mixin.TrippingPostChainAccessor;
import net.beamex.shamaschizm.mixin.TrippingPostPassAccessor;
import net.beamex.shamaschizm.wisdom.WisdomGaze;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Pre-HUD passes over the completed scene, including portals; no render history or recursive effects. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class WisdomLens {
    private static final ByteBuffer DATA = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
    private static final WisdomExposure EXPOSURE=new WisdomExposure();
    private static Object world, player;
    private static long received, frame;
    private static float targetStrength, strength, targetProximity, proximity, effectTime;
    private static Vec3 position;
    @SubscribeEvent public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(WisdomGaze.Lens.TYPE, (packet, context) -> context.enqueueWork(() -> {
            var mc = Minecraft.getInstance();
            if (world != mc.level || player != mc.player) reset(mc);
            targetStrength = finiteUnit(packet.strength());targetProximity=finiteUnit(packet.proximity());
            received = System.nanoTime();
        }));
    }
    private static float finiteUnit(float x){return Float.isFinite(x)?Math.clamp(x,0,1):0;}
    private static boolean valid(Minecraft mc){return mc.player!=null&&mc.level!=null&&mc.player.isAlive()
            &&!mc.player.isSpectator()&&mc.getCameraEntity()==mc.player;}
    private static void reset(Minecraft mc) {
        world=mc.level;player=mc.player;strength=targetStrength=targetProximity=proximity=effectTime=0;
        received=frame=0;position=null;EXPOSURE.reset();
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();
        if(world!=mc.level||player!=mc.player||!valid(mc)){reset(mc);return;}
        if(mc.isPaused())return;
        if(position!=null&&position.distanceToSqr(mc.player.position())>16){
            // Same-world portal crossings clear stale spatial input but retain the personal three-second release.
            targetStrength=targetProximity=strength=proximity=0;received=0;
        }
        position=mc.player.position();
        boolean fresh=System.nanoTime()-received<350_000_000L;
        EXPOSURE.tick(fresh&&targetStrength>0,.05F);
        proximity+=((fresh?targetProximity:0)-proximity)*.33F;
        effectTime=(effectTime+.05F)%1000;
    }
    public static float soundGain(){
        var mc=Minecraft.getInstance();
        return world==mc.level&&player==mc.player&&valid(mc)?EXPOSURE.soundGain():1;
    }
    public static void render(RenderTarget output, GraphicsResourceAllocator allocator) {
        var mc=Minecraft.getInstance();long now=System.nanoTime();
        if(world!=mc.level||player!=mc.player)reset(mc);
        if(!valid(mc)){reset(mc);return;}
        float dt=frame==0?1F/60:(float)Math.min(.1,(now-frame)/1e9);frame=now;
        if(!mc.isPaused()){
            float wanted=now-received<350_000_000L?targetStrength:0;
            strength+=(wanted-strength)*(1-(float)Math.exp(-dt*12));
        }
        float lens=EXPOSURE.lensStrength(strength);
        if(lens>=.002F)pass(mc,"wisdom_lens",lens,output,allocator);
        float acid=EXPOSURE.visual(proximity);
        if(acid>=.001F)pass(mc,"wisdom_psychedelic",acid,output,allocator);
    }
    private static void pass(Minecraft mc,String id,float amount,RenderTarget output,GraphicsResourceAllocator allocator){
        var chain=mc.getShaderManager().getPostChain(Shamaschizm.id(id),LevelTargetBundle.MAIN_TARGETS);
        if(chain==null)return;
        DATA.clear();DATA.putFloat(amount).putFloat(effectTime).putFloat((float)output.width/Math.max(1,output.height)).putFloat(0).flip();
        for(var pass:((TrippingPostChainAccessor)(Object)chain).shamaschizm$passes()){
            var uniforms=((TrippingPostPassAccessor)(Object)pass).shamaschizm$uniforms();
            var buffer=uniforms.get("WisdomConfig");if(buffer==null)continue;
            if((buffer.usage()&GpuBuffer.USAGE_COPY_DST)==0){
                var dynamic=RenderSystem.getDevice().createBuffer(()->"Tainted Wisdom "+id,GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,DATA.duplicate());
                uniforms.put("WisdomConfig",dynamic);buffer.close();
            }else RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(),DATA.duplicate());
        }
        chain.process(output,allocator);
    }
}
