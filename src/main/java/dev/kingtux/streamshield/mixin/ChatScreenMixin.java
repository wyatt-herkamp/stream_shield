package dev.kingtux.streamshield.mixin;

import dev.kingtux.streamshield.ObfuscationState;
import dev.kingtux.streamshield.window.SecondaryWindow;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {

    @Shadow
    protected EditBox input;

    protected ChatScreenMixin(net.minecraft.network.chat.Component title) {
        super(title);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("HEAD"))
    private void streamShield$preExtract(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!ObfuscationState.INSTANCE.getEnabled()) {
            return;
        }
        if (this.input != null) {
            // Inline gray auto-complete preview bypasses the input's TextFormatter (EditBox.java:444),
            // so wipe it each frame while obfuscation is on.
            this.input.setSuggestion(null);
            SecondaryWindow.INSTANCE.setComposing(this.input.getValue());
        }
    }

    @Inject(
        method = "formatChat(Ljava/lang/String;I)Lnet/minecraft/util/FormattedCharSequence;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void streamShield$maskFormatter(String text, int offset, CallbackInfoReturnable<FormattedCharSequence> cir) {
        if (!ObfuscationState.INSTANCE.getEnabled()) {
            return;
        }
        String mask = ObfuscationState.INSTANCE.getMaskCharacter();
        if (mask == null || mask.isEmpty()) {
            mask = "•";
        }
        StringBuilder sb = new StringBuilder(text.length() * mask.length());
        for (int i = 0; i < text.length(); i++) {
            sb.append(mask);
        }
        cir.setReturnValue(FormattedCharSequence.forward(sb.toString(), Style.EMPTY));
    }

    @Redirect(
        method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/CommandSuggestions;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"
        )
    )
    private void streamShield$skipSuggestionsPopup(CommandSuggestions instance, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!ObfuscationState.INSTANCE.getEnabled()) {
            instance.extractRenderState(graphics, mouseX, mouseY);
        }
    }
}
