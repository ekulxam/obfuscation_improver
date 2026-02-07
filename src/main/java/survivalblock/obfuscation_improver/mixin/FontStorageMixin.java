package survivalblock.obfuscation_improver.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
//? if =1.21.1
/*import com.mojang.blaze3d.font.GlyphInfo;*/
import com.mojang.blaze3d.font.GlyphProvider;
//? if >=1.21.9
import com.mojang.blaze3d.font.UnbakedGlyph;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.SpecialGlyphs;
import net.minecraft.client.gui.font.providers.UnihexProvider;
import net.minecraft.util.Mth;

//@Debug(export = true)
@Mixin(FontSet.class)
public class FontStorageMixin {

    @Unique
    private final Int2ObjectMap<IntList> obfuscation_improver$charactersByWidth = new Int2ObjectOpenHashMap<>();

    @WrapOperation(method = "getRandomGlyph", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;get(I)Ljava/lang/Object;", remap = false))
    private Object reduceObfuscationLag(Int2ObjectMap<?> instance, int i, Operation<Object> original) {
        return original.call(obfuscation_improver$charactersByWidth, i);
    }

    @Inject(method = "resetTextures", at = @At("RETURN"))
    private void clearObfuscationImprover(CallbackInfo ci) {
        this.obfuscation_improver$charactersByWidth.clear();
    }

    @ModifyReturnValue(method = "selectProviders", at = @At("RETURN"))
    private List<GlyphProvider> setDefault(List<GlyphProvider> original, @Local IntSet intSet, @Local(argsOnly = true)List<GlyphProvider.Conditional> allFonts) {
        if (original.isEmpty()) {
            return original;
        }
        List<GlyphProvider> obfuscationFonts = new ArrayList<>();
        for (GlyphProvider font : original) {
            if (font instanceof UnihexProvider) {
                continue;
            }
            obfuscationFonts.add(font);
        }
        Collections.reverse(obfuscationFonts);
        intSet.forEach(
                codePoint -> {
                    for (GlyphProvider font : obfuscationFonts) {
                        /*? <1.21.9 {*/ /*GlyphInfo *//*?} else {*/ UnbakedGlyph /*?}*/ glyph = font.getGlyph(codePoint);
                        if (glyph != null && glyph/*? >=1.21.9 {*/ .info() /*?}*/ != SpecialGlyphs.MISSING) {
                            this.obfuscation_improver$charactersByWidth
                                    .computeIfAbsent(Mth.ceil(glyph/*? >=1.21.9 {*/ .info() /*?}*/.getAdvance(false)), (Int2ObjectFunction<? extends IntList>)(i -> new IntArrayList()))
                                    .add(codePoint);
                        }
                        break;
                    }
                }
        );
        return original;
    }
}