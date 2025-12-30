package net.narutoxboruto.items.swords;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.LazyLoadedValue;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public enum SwordCustomTiers implements Tier {
    GENERAL(0, 1561, -4.0F, -1.0F, 0, () -> Ingredient.EMPTY),
    SAMEHADA(0, 1561, -7.5F, 15.0F, 0, () -> Ingredient.EMPTY),
    SHIBUKI(0, 1561, -4.5F, 12.0F, 0, () -> Ingredient.EMPTY),
    KIBA(0, 1561, -2.0F, 8.0F, 0, () -> Ingredient.EMPTY), // Fast dual lightning blades
    KUBIKIRIBOCHO(0, 1561, -9.5F, 17.0F, 0, () -> Ingredient.EMPTY),
    NUIBARI(0, 1561, -3.5F, 7.0F, 0, () -> Ingredient.EMPTY),
    KABUTOWARI(0, 1561, -3.0F, 12.0F, 0, () -> Ingredient.EMPTY); // Heavy bluntsword
    // HIRAMEKAREI()

    private final int level;
    private final int uses;
    private final float speed;
    private final float damage;
    private final int enchantmentValue;
    private final LazyLoadedValue<Ingredient> repairIngredient;

    SwordCustomTiers(int pLevel, int pUses, float pSpeed, float pDamage, int pEnchantmentValue, Supplier<Ingredient> pRepairIngredient) {
        this.level = pLevel;
        this.uses = pUses;
        this.speed = pSpeed;
        this.damage = pDamage;
        this.enchantmentValue = pEnchantmentValue;
        this.repairIngredient = new LazyLoadedValue<>(pRepairIngredient);
    }

    @Override
    public int getUses() {
        return this.uses;
    }

    @Override
    public float getSpeed() {
        return this.speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return this.damage;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return BlockTags.INCORRECT_FOR_STONE_TOOL;
    }


    public int getLevel() {
        return this.level;
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }
}
