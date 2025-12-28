package net.narutoxboruto.items.jutsus;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.narutoxboruto.entities.jutsus.FireBallEntity;

/**
 * Fire Ball Jutsu Item - Launches a rotating fireball projectile that
 * ignites entities on contact and creates explosive craters on ground impact.
 * Requires Fire release affinity.
 */
public class FireBall extends AbstractJutsuItem {

    public FireBall(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public String getJutsuName() {
        return "Fire Ball";
    }

    @Override
    public String getRequiredRelease() {
        return "fire";
    }

    @Override
    public int getChakraCost() {
        return 5;
    }

    @Override
    public int getCooldownTicks() {
        return 60; // 3 seconds cooldown
    }

    @Override
    protected boolean executeJutsu(ServerPlayer serverPlayer, Level level) {
        if (!(level instanceof ServerLevel)) {
            return false;
        }

        // Create and spawn the fireball entity
        FireBallEntity fireBall = new FireBallEntity(level, serverPlayer);
        level.addFreshEntity(fireBall);

        // Play casting sound
        level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);

        return true;
    }
}
