package net.beamex.shamaschizm.grimoire.client;

import net.beamex.shamaschizm.grimoire.GrimoireRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.LecternMenu;

public final class GrimoireLecternScreen extends GrimoireScreen implements MenuAccess<LecternMenu> {
    private final LecternMenu menu;
    public GrimoireLecternScreen(LecternMenu menu){this.menu=menu;}
    @Override public LecternMenu getMenu(){return menu;}
    @Override protected void addFooterActions(int y){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||!mc.player.mayBuild()){super.addFooterActions(y);return;}
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose())
                .bounds(width/2-54,y,52,20).build());
        addRenderableWidget(Button.builder(Component.translatable("lectern.take_book"),b->{
            if(mc.player!=null && mc.player.containerMenu==menu && mc.gameMode!=null){
                b.active=false;
                // Vanilla validates permission and returns the exact stored stack, including components.
                mc.gameMode.handleInventoryButtonClick(menu.containerId,LecternMenu.BUTTON_TAKE_BOOK);
            }
        }).bounds(width/2+2,y,52,20).build());
    }
    @Override public void tick(){
        super.tick();Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.player.containerMenu!=menu||!GrimoireRegistration.isBook(menu.getBook()))onClose();
    }
    @Override public void onClose(){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null&&mc.player.containerMenu==menu)mc.player.closeContainer();
        super.onClose();
    }
}
