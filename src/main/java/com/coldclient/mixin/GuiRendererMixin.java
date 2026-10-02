package com.coldclient.mixin;

import com.coldclient.gui.ClickGuiRenderer;
import com.coldclient.gui.ClickGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void coldClient$renderClickGui(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();

        if (client.gui == null) {
            return;
        }

        if (client.gui.screen() instanceof ClickGuiScreen screen) {
            ClickGuiRenderer.render(screen);
        }
    }
}