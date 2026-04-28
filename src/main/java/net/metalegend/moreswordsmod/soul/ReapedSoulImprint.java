package net.metalegend.moreswordsmod.soul;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

// captured soul payload stored on the scythe
// an imprint keeps only the data needed to recreate a summon later: which profile to
// spawn copied equipment state and the special-case slime size needed for
// split-capable summons
public record ReapedSoulImprint(BoneScytheSoulProfile profile, EnumMap<EquipmentSlot, ItemStack> equipment, int slimeSize) {
    private static final String PROFILE_ID_KEY = "ProfileId";
    private static final String EQUIPMENT_KEY = "Equipment";
    private static final String SLIME_SIZE_KEY = "SlimeSize";

    public ReapedSoulImprint {
        EnumMap<EquipmentSlot, ItemStack> equipmentCopy = new EnumMap<>(EquipmentSlot.class);
        for (Map.Entry<EquipmentSlot, ItemStack> entry : equipment.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                equipmentCopy.put(entry.getKey(), entry.getValue().copy());
            }
        }
        equipment = equipmentCopy;
        slimeSize = Math.max(1, slimeSize);
    }

    public static ReapedSoulImprint bare(BoneScytheSoulProfile profile) {
        return new ReapedSoulImprint(profile, new EnumMap<>(EquipmentSlot.class), 1);
    }

    public static ReapedSoulImprint capture(BoneScytheSoulProfile profile, LivingEntity source) {
        EnumMap<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack stack = source.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                equipment.put(slot, stack.copy());
            }
        }

        int slimeSize = source instanceof Slime slime ? Math.max(1, slime.getSize()) : 1;
        return new ReapedSoulImprint(profile, equipment, slimeSize);
    }

    // serializes full ItemStack data so enchantments damage and custom components survive harvest
    public CompoundTag toTag(HolderLookup.Provider registries) {
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        CompoundTag tag = new CompoundTag();
        tag.putString(PROFILE_ID_KEY, profile.id());
        tag.putInt(SLIME_SIZE_KEY, slimeSize);

        CompoundTag equipmentTag = new CompoundTag();
        for (Map.Entry<EquipmentSlot, ItemStack> entry : equipment.entrySet()) {
            Tag encoded = ItemStack.OPTIONAL_CODEC.encodeStart(ops, entry.getValue()).result().orElse(null);
            if (encoded != null) {
                equipmentTag.put(entry.getKey().getName(), encoded);
            }
        }

        if (!equipmentTag.isEmpty()) {
            tag.put(EQUIPMENT_KEY, equipmentTag);
        }

        return tag;
    }

    // accepts older or partial data defensively so existing stored imprints do not hard-fail on load
    public static @Nullable ReapedSoulImprint fromTag(CompoundTag tag, HolderLookup.Provider registries) {
        BoneScytheSoulProfile profile = BoneScytheSoulProfile.fromId(tag.getStringOr(PROFILE_ID_KEY, ""));
        if (profile == null) {
            return null;
        }

        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        EnumMap<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
        CompoundTag equipmentTag = tag.getCompoundOrEmpty(EQUIPMENT_KEY);
        for (String key : equipmentTag.keySet()) {
            try {
                EquipmentSlot slot = EquipmentSlot.byName(key);
                ItemStack stack = ItemStack.OPTIONAL_CODEC.parse(ops, equipmentTag.get(key)).result().orElse(ItemStack.EMPTY);
                if (!stack.isEmpty()) {
                    equipment.put(slot, stack);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        return new ReapedSoulImprint(profile, equipment, tag.getIntOr(SLIME_SIZE_KEY, 1));
    }
}
