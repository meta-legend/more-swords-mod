package net.metalegend.moreswordsmod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class TooltipHelper {
    private TooltipHelper() {
    }

    public static void addTooltipLine(Consumer<Component> builder, String key, ChatFormatting color) {
        builder.accept(Component.translatable(key).withStyle(color));
    }

    public static void addAbilitySection(Consumer<Component> builder, String titleKey, String... descriptionKeys) {
        addTooltipLine(builder, titleKey, ChatFormatting.AQUA);
        for (String descriptionKey : descriptionKeys) {
            addTooltipLine(builder, descriptionKey, ChatFormatting.DARK_AQUA);
        }
    }
}
