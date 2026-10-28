package net.beamex.shamaschizm.localportal;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** No client supplied target, position, reach, or damage. */
public record PortalInteractionPacket(Action action) implements CustomPacketPayload {
    public enum Action { ATTACK, HOLD, RELEASE, USE }
    public static final Type<PortalInteractionPacket> TYPE=new Type<>(Shamaschizm.id("portal_interaction"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PortalInteractionPacket> CODEC=new StreamCodec<>() {
        public PortalInteractionPacket decode(RegistryFriendlyByteBuf b){return new PortalInteractionPacket(b.readEnum(Action.class));}
        public void encode(RegistryFriendlyByteBuf b,PortalInteractionPacket p){b.writeEnum(p.action);}
    };
    public Type<PortalInteractionPacket> type(){return TYPE;}
}
