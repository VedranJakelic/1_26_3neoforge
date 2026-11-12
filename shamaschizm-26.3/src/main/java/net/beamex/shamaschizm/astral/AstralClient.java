package net.beamex.shamaschizm.astral;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.client.SurfaceEyeEffects;
import net.beamex.shamaschizm.client.TripVisionClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID,value=Dist.CLIENT)
public final class AstralClient {
 private static ArmorStand ghost;private static Object world;private static long session;private static CameraType oldCamera;
 private static Vec3 target;private static int age;private static float danger;
 public static boolean active(){return ghost!=null;}
 public static float seconds(){return age/20F;}
 public static float danger(){return danger;}
 @SubscribeEvent public static void register(RegisterClientPayloadHandlersEvent e){e.register(AstralNetwork.State.TYPE,(p,c)->c.enqueueWork(()->receive(p)));}
 private static void receive(AstralNetwork.State p){
  var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
  if(!p.active()){if(p.session()==session)reset();return;}if(!mc.player.isAlive())return;
  if(ghost==null||p.session()!=session){
   reset();world=mc.level;session=p.session();oldCamera=mc.options.getCameraType();mc.options.setCameraType(CameraType.FIRST_PERSON);
   ghost=new ArmorStand(mc.level,p.x(),p.y(),p.z()){@Override public boolean isSpectator(){return true;}};
   ghost.setInvisible(true);ghost.setNoGravity(true);ghost.absSnapTo(p.x(),p.y()-ghost.getEyeHeight(),p.z(),p.yaw(),p.pitch());mc.setCameraEntity(ghost);
   var screen=mc.gui.screen();if(screen!=null&&(screen instanceof AbstractContainerScreen||screen.getClass().getSimpleName().contains("InBed")))mc.gui.setScreen(null);
   TripVisionClient.reset();
  }
  target=new Vec3(p.x(),p.y(),p.z());age=p.age();danger=p.danger();
  if(p.crossed())ghost.absSnapTo(p.x(),p.y()-ghost.getEyeHeight(),p.z(),p.yaw(),p.pitch());
 }
 @SubscribeEvent public static void tick(ClientTickEvent.Pre e){
  var mc=Minecraft.getInstance();if(!active())return;
  if(mc.level!=world||mc.player==null||!mc.player.isAlive()){reset();return;}if(mc.isPaused())return;
  mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(ghost);ghost.setOldPosAndRot();if(target!=null)ghost.setPos(target.subtract(0,ghost.getEyeHeight(),0));
  boolean move=mc.gui.screen()==null;
  float forward=move?(mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0):0;
  float strafe=move?(mc.options.keyRight.isDown()?1:0)-(mc.options.keyLeft.isDown()?1:0):0;
  float vertical=move?(mc.options.keyJump.isDown()?1:0)-(mc.options.keyShift.isDown()?1:0):0;
  ClientPacketDistributor.sendToServer(new AstralNetwork.Input(session,forward,strafe,vertical,ghost.getYRot(),ghost.getXRot(),move&&mc.options.keySprint.isDown()));
 }
 public static void turn(double x,double y){if(ghost!=null)ghost.turn(x,y);}
 public static Vec3 celestial(boolean moon){var mc=Minecraft.getInstance();float angle=mc.gameRenderer.mainCamera().attributeProbe().getValue(moon?EnvironmentAttributes.MOON_ANGLE:EnvironmentAttributes.SUN_ANGLE,1F);double a=Math.toRadians(angle);return new Vec3(-Math.sin(a),Math.cos(a),0);}
 public static Vec3 projected(boolean moon){return SurfaceEyeEffects.projectDirection(celestial(moon));}
 @SubscribeEvent public static void hand(RenderHandEvent e){if(active())e.setCanceled(true);}
 @SubscribeEvent public static void hud(RenderGuiLayerEvent.Pre e){if(active())e.setCanceled(true);}
 @SubscribeEvent public static void screen(ScreenEvent.Opening e){if(active()&&e.getNewScreen()!=null&&(e.getNewScreen() instanceof AbstractContainerScreen||e.getNewScreen().getClass().getSimpleName().contains("InBed")))e.setCanceled(true);}
 @SubscribeEvent public static void interact(InputEvent.InteractionKeyMappingTriggered e){if(active()){e.setCanceled(true);e.setSwingHand(false);}}
 @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){reset();}
 public static void reset(){if(ghost!=null){var mc=Minecraft.getInstance();if(mc.player!=null)mc.setCameraEntity(mc.player);if(oldCamera!=null)mc.options.setCameraType(oldCamera);}ghost=null;target=null;world=null;oldCamera=null;age=0;danger=0;}
}
