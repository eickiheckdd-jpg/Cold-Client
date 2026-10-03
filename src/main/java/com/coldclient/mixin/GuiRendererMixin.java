package com.coldclient.mixin;

import com.coldclient.feature.Features;
import com.coldclient.gui.ClickGuiRenderer;
import com.coldclient.gui.ClickGuiScreen;
import com.coldclient.hud.HudEditorScreen;
import com.coldclient.hud.HudRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void coldClient$renderClickGui(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();

        if (client.gui == null) {
            return;
        }

        try {
            if (client.gui.screen() instanceof ClickGuiScreen screen) {
                ClickGuiRenderer.render(screen);
            } else if (client.gui.screen() instanceof HudEditorScreen editor) {
                HudRenderer.renderEditor(editor);
            } else {
                // In-game: per-frame features, then the overlay.
                Features.frame(client);
                HudRenderer.render(client);
            }
        } catch (Throwable t) {
            // Never let an overlay bug crash the render thread.
            Features.reportRenderError(t);
        }
    }
}