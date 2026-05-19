package net.metalegend.moreswordsmod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

// shared tooltip formatting helpers so item classes stay focused on behavior
public final class TooltipHelper {
    private TooltipHelper() {
    }

    // adds one translated line with a consistent color wrapper
    public static void addTooltipLine(Consumer<Component> builder, String key, ChatFormatting color) {
        builder.accept(Component.translatable(key).withStyle(color));
    }

    // ability sections always use an aqua title followed by darker description lines
    public static void addAbilitySection(Consumer<Component> builder, String titleKey, String... descriptionKeys) {
        builder.accept(Component.empty());
        addTooltipLine(builder, titleKey, ChatFormatting.AQUA);
        for (String descriptionKey : descriptionKeys) {
            addTooltipLine(builder, descriptionKey, ChatFormatting.DARK_AQUA);
        }
    }

    // vanilla enchantments render after custom item text, so separate them only when present
    public static void addEnchantmentSeparatorIfNeeded(ItemStack stack, Consumer<Component> builder) {
        if (stack.isEnchanted()) {
            builder.accept(Component.empty());
        }
    }
}
