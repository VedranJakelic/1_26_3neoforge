package net.beamex.shamaschizm.client;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.centipede.BiteVisuals;
import net.beamex.shamaschizm.mixin.TrippingPostChainAccessor;
import net.beamex.shamaschizm.mixin.TrippingPostPassAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.world.phys.Vec3;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
/** Brief radial impact, then frame-rate-adjusted motion trails, before the HUD. */
public final class CentipedeBiteBlur {
 private static final ByteBuffer DATA=ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
 private static Object world,player;
 private static PostChain previous;
 private static Vec3 position;
 private static int width,height;
 private static long frame;
 private static boolean history;
 public static void render(RenderTarget target,GraphicsResourceAllocator allocator){
  var mc=Minecraft.getInstance();long now=System.nanoTime();
  float dt=frame==0?1F/60:(float)Math.min(.1,(now-frame)/1e9);frame=now;
  if(world!=mc.level||player!=mc.player){world=mc.level;player=mc.player;history=false;position=null;}
  float age=(float)((now-BiteVisuals.receivedNanos)/1e9);
  if(mc.player==null||mc.level==null||!mc.player.isAlive()||mc.player.getId()!=BiteVisuals.playerId
   ||mc.getCameraEntity()!=mc.player||mc.isPaused()||age<0||age>1.25F){history=false;return;}
  PostChain chain=mc.getShaderManager().getPostChain(Shamaschizm.id("centipede_bite"),LevelTargetBundle.MAIN_TARGETS);
  if(chain==null){history=false;return;}
  if(chain!=previous||width!=target.width||height!=target.height
   ||(position!=null&&position.distanceToSqr(mc.player.position())>16))history=false;
  previous=chain;width=target.width;height=target.height;position=mc.player.position();
  float radial=(float)Math.pow(Math.max(0,1-age/.24F),2);
  float fade=Math.max(0,Math.min(1,(1.25F-age)/.65F));fade=fade*fade*(3-2*fade);
  float trail=history?(float)Math.exp(-dt/.14F)*fade:0;
  DATA.clear();DATA.putFloat(radial).putFloat(trail).putFloat(0).putFloat(0).flip();
  for(var pass:((TrippingPostChainAccessor)(Object)chain).shamaschizm$passes()){
   var uniforms=((TrippingPostPassAccessor)(Object)pass).shamaschizm$uniforms();var buffer=uniforms.get("BiteConfig");
   if(buffer==null)continue;
   if((buffer.usage()&GpuBuffer.USAGE_COPY_DST)==0){
    var dynamic=RenderSystem.getDevice().createBuffer(()->"Centipede bite blur",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,DATA.duplicate());
    uniforms.put("BiteConfig",dynamic);buffer.close();
   }else RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(),DATA.duplicate());
  }
  chain.process(target,allocator);history=true;
 }
}
