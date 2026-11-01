package net.beamex.shamaschizm.ritual;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class RitualClient {
    private RitualClient() {}

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RitualRegistry.QUESTION_VILLAGER.get(), QuestionRenderer::new);
    }

    public static final class QuestionRenderer extends VillagerRenderer {
        private static final Identifier EYES = Shamaschizm.id("textures/entity/question_villager.png");

        public QuestionRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(VillagerRenderState state) {
            return EYES;
        }
    }
}
