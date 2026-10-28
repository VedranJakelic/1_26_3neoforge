package net.beamex.shamaschizm.localportal.client;

import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.beamex.shamaschizm.localportal.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;

/** Appearance-only copies: never inserted in the client world and never ticked. */
final class PortalBlockEntityScene {
    private record Replica(BlockEntity entity,PortalBlockAppearance appearance) {}
    private final PortalTerrain terrain;
    private final List<Replica> replicas=new ArrayList<>();
    PortalBlockEntityScene(PortalTerrain terrain) {
        this.terrain=terrain;var mc=Minecraft.getInstance();
        for(var appearance:terrain.blockEntities())try {
            int i=appearance.index();if(i<0||i>=terrain.states().length)continue;
            BlockPos pos=terrain.min().offset(i%terrain.sizeX(),i/(terrain.sizeX()*terrain.sizeZ()),(i/terrain.sizeX())%terrain.sizeZ());
            var state=Block.stateById(terrain.states()[i]);var type=BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(appearance.type());
            if(type==null||!type.isValid(state))continue;
            var entity=type.create(pos,state);if(entity==null)continue;
            entity.setLevel(mc.level);
            entity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,mc.level.registryAccess(),appearance.tag().copy()));
            replicas.add(new Replica(entity,appearance));
        }catch(RuntimeException failure){PortalEntityScene.warn("block entity "+appearance.type(),failure);}
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    void submit(PortalEntityMesh collector,CameraRenderState camera,float partialTick) {
        var mc=Minecraft.getInstance();var dispatcher=mc.getBlockEntityRenderDispatcher();
        for(var replica:replicas)try {
            var entity=replica.entity();var pos=entity.getBlockPos();
            var live=mc.level.getBlockEntity(pos);
            boolean useLive=live!=null&&live.getType()==entity.getType()&&live.getBlockState().equals(entity.getBlockState());
            if(useLive)entity=live;
            BlockEntityRenderer renderer=dispatcher.getRenderer(entity);if(renderer==null)continue;
            var state=renderer.createRenderState();renderer.extractRenderState(entity,state,partialTick,camera.pos,null);
            int index=terrain.index(pos),light=index<0?0:terrain.lights()[index]&255;
            state.lightCoords=((light&15)<<4)|((light>>4)<<20);
            if(!useLive&&state instanceof ChestRenderState chest)chest.open=replica.appearance().openness();
            if(!useLive&&state instanceof net.minecraft.client.renderer.blockentity.state.BeaconRenderState beacon) {
                int[] beam=replica.appearance().tag().getIntArray("shamaschizm:portal_beam_sections").orElse(new int[0]);
                var sections=new ArrayList<net.minecraft.client.renderer.blockentity.state.BeaconRenderState.Section>();
                for(int j=0;j+1<beam.length&&j<128;j+=2)
                    sections.add(new net.minecraft.client.renderer.blockentity.state.BeaconRenderState.Section(beam[j],Math.clamp(beam[j+1],1,4096)));
                beacon.sections=List.copyOf(sections);
            }
            var origin=terrain.target().origin();var pose=new PoseStack();
            pose.translate(pos.getX()-origin.x,pos.getY()-origin.y,pos.getZ()-origin.z);
            renderer.submit(state,pose,collector,camera);
        }catch(RuntimeException failure){PortalEntityScene.warn("block entity "+replica.appearance().type(),failure);}
    }
}
