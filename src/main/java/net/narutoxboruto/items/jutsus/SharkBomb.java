package net.narutoxboruto.items.jutsus;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.narutoxboruto.entities.jutsus.SharkBombEntity;

/**
 * Shark Bomb Jutsu - Water Release
 * 
 * Creates a shark-shaped projectile that homes in on nearby enemies.
 * Player can influence the targeting by aiming their crosshair.
 * 
 * - Medium range (10 blocks)
 * - Homing with player guidance
 * - Water Release required
 */
public class SharkBomb extends AbstractJutsuItem {
    
    public SharkBomb(Properties properties) {
        super(properties);
    }
    
    @Override
    public int getChakraCost() {
        return 30; // Medium chakra cost for medium range jutsu
    }
    
    @Override
    public int getCooldownTicks() {
        return 60; // 3 seconds cooldown
    }
    
    @Override
    public String getRequiredRelease() {
        return "water";
    }
    
    @Override
    public String getJutsuName() {
        return "Shark Bomb";
    }
    
    @Override
    protected boolean executeJutsu(ServerPlayer player, Level level) {
        // Create and spawn the shark projectile
        SharkBombEntity shark = new SharkBombEntity(level, player);
        level.addFreshEntity(shark);
        return true;
    }
    
    @Override
    protected void playCastSound(Level level, Player player) {
        // No cast sound for shark bomb
    }
}
