package net.beamex.shamaschizm.maiden;
import net.minecraft.world.phys.Vec3;
/** Skull, ribcage, pelvis and separate upper/lower limb bones; vanilla skeleton texture. */
public final class MaidenBones {
 public static final float[][] PARTS={{-4,25,-5,4,33,3,0,0},{-4,15,-3,4,25,1,16,16},{-3,13,-3,3,15,1,16,26},{-6,19,-2,-4,25,0,40,16},{-6,13,-2,-4,19,0,40,22},{4,19,-2,6,25,0,40,16},{4,13,-2,6,19,0,40,22},{-3,7,-2,-1,13,0,0,16},{-3,1,-2,-1,7,0,0,22},{1,7,-2,3,13,0,0,16},{1,1,-2,3,7,0,0,22}};
 public static Vec3 center(int part){var p=PARTS[part];return new Vec3((p[0]+p[3])/32,(p[1]+p[4])/32,(p[2]+p[5])/32);}
}
