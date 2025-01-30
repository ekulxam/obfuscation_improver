package survivalblock.obfuscation_improver.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.BitmapFont;
import net.minecraft.client.font.BuiltinEmptyGlyph;
import net.minecraft.client.font.Font;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.font.UnihexFont;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Debug(export = true)
@Mixin(FontStorage.class)
public class FontStorageMixin {

    @Shadow @Final private Identifier id;
    @Unique
    private final Int2ObjectMap<IntList> obfuscation_improver$charactersByWidth = new Int2ObjectOpenHashMap<>();

    @WrapOperation(method = "getObfuscatedGlyphRenderer", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;get(I)Ljava/lang/Object;", remap = false))
    private Object reduceObfuscationLag(Int2ObjectMap<?> instance, int i, Operation<Object> original) {
        //return original.call(instance, i);
        return original.call(obfuscation_improver$charactersByWidth, i);
    }

    @Inject(method = "clear", at = @At("RETURN"))
    private void clearObfuscationImprover(CallbackInfo ci) {
        this.obfuscation_improver$charactersByWidth.clear();
    }

    @ModifyReturnValue(method = "applyFilters", at = @At("RETURN"))
    private List<Font> setDefault(List<Font> original, @Local IntSet intSet) {
        if (original.isEmpty()) {
            return original;
        }
        if (!this.id.equals(MinecraftClient.DEFAULT_FONT_ID)) {
            return original;
        }
        List<Font> obfuscationFonts = new ArrayList<>();
        for (Font font : original) {
            if (font instanceof UnihexFont) {
                continue;
            }
            obfuscationFonts.add(font);
        }
        Collections.reverse(obfuscationFonts);
        intSet.forEach(
                codePoint -> {
                    for (Font font : obfuscationFonts) {
                        Glyph glyph = font.getGlyph(codePoint);
                        if (glyph != null && glyph != BuiltinEmptyGlyph.MISSING) {
                            this.obfuscation_improver$charactersByWidth
                                    .computeIfAbsent(MathHelper.ceil(glyph.getAdvance(false)), (Int2ObjectFunction<? extends IntList>)(i -> new IntArrayList()))
                                    .add(codePoint);
                        }
                        break;
                    }
                }
        );
        return original;
    }
}