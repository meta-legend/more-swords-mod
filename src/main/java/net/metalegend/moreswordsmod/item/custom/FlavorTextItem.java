package net.metalegend.moreswordsmod.item.custom;

import net.metalegend.moreswordsmod.item.TooltipHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

// simple ingredient/component item that shows one translated flavor line
public class FlavorTextItem extends Item {
    private final String flavorKey;

    public FlavorTextItem(String itemName, Properties properties) {
        super(properties);
        this.flavorKey = "tooltip.moreswordsmod." + itemName + ".flavor";
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        TooltipHelper.addTooltipLine(builder, flavorKey, ChatFormatting.GRAY);
    }
}
