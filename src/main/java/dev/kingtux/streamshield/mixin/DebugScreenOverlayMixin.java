package dev.kingtux.streamshield.mixin;

import dev.kingtux.streamshield.ObfuscationState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.CLIENT)
@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {

    @Redirect(
        method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/debug/DebugScreenEntries;getEntry(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/gui/components/debug/DebugScreenEntry;"
        )
    )
    private DebugScreenEntry streamShield$filterDebugEntry(Identifier id) {
        if (ObfuscationState.INSTANCE.getEnabled() && isHiddenEntry(id)) {
            return null;
        }
        return DebugScreenEntries.getEntry(id);
    }

    private static boolean isHiddenEntry(Identifier id) {
        return id.equals(DebugScreenEntries.PLAYER_POSITION)
            || id.equals(DebugScreenEntries.PLAYER_SECTION_POSITION);
    }
}
