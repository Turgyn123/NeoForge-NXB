package net.narutoxboruto.entities.jutsus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.narutoxboruto.entities.ModEntities;

/**
 * FireBall Jutsu Entity - A rotating fireball projectile that ignites entities
 * and creates small explosive craters on ground impact.
 */
public class FireBallEntity extends Projectile {

    private int age = 0;
    private float rotation = 0;
    
    // Configuration
    private static final float EXPLOSION_RADIUS = 1.5F;
    private static final float FIRE_DAMAGE = 8.0F;
    private static final int FIRE_SECONDS = 5;
    private static final int MAX_LIFETIME = 100; // 5 seconds max flight time

    public FireBallEntity(EntityType<? extends FireBallEntity> entityType, Level level) {
        super(entityType, level);
    }

    public FireBallEntity(Level level, LivingEntity shooter) {
        this(ModEntities.FIRE_BALL.get(), level);
        this.setOwner(shooter);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
        
        // Set initial velocity in the direction the player is looking
        Vec3 lookVec = shooter.getLookAngle();
        this.setDeltaMovement(lookVec.scale(1.5)); // Projectile speed
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No additional synced data needed
    }

    @Override
    public void tick() {
        super.tick();
        
        this.age++;
        
        // Update rotation for visual spinning effect
        this.rotation += 15.0F; // Rotate 15 degrees per tick
        if (this.rotation >= 360.0F) {
            this.rotation -= 360.0F;
        }
        
        // Check for max lifetime
        if (this.age > MAX_LIFETIME) {
            this.discard();
            return;
        }
        
        // Perform hit detection
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onHit(hitResult);
        }
        
        // Update position based on velocity
        Vec3 velocity = this.getDeltaMovement();
        this.setPos(this.getX() + velocity.x, this.getY() + velocity.y, this.getZ() + velocity.z);
        
        // Apply slight gravity
        this.setDeltaMovement(velocity.x, velocity.y - 0.03, velocity.z);
        
        // Spawn fire particles while flying
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    this.getX(), this.getY(), this.getZ(),
                    3, 0.1, 0.1, 0.1, 0.02);
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    this.getX(), this.getY(), this.getZ(),
                    1, 0.05, 0.05, 0.05, 0.01);
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        
        if (!this.level().isClientSide()) {
            if (hitResult.getType() == HitResult.Type.ENTITY) {
                this.onHitEntity((EntityHitResult) hitResult);
            } else if (hitResult.getType() == HitResult.Type.BLOCK) {
                this.onHitBlock((BlockHitResult) hitResult);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        
        if (target instanceof LivingEntity livingTarget) {
            // Don't damage the caster
            if (livingTarget == this.getOwner()) {
                return;
            }
            
            // Deal fire damage
            livingTarget.hurt(this.damageSources().onFire(), FIRE_DAMAGE);
            
            // Set entity on fire
            livingTarget.setRemainingFireTicks(FIRE_SECONDS * 20);
            
            // Play hit sound
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        
        // Create explosion on entity hit (smaller than block hit)
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.explode(
                    this.getOwner(),
                    this.getX(), this.getY(), this.getZ(),
                    EXPLOSION_RADIUS * 0.8F, // Slightly smaller explosion on entity hit
                    true, // causes fire
                    Level.ExplosionInteraction.MOB
            );
        }
        
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        // Create crater explosion on ground impact
        createCraterExplosion(result.getBlockPos());
        
        // Play explosion sound
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
        
        this.discard();
    }

    /**
     * Creates a small explosive crater at the impact location.
     */
    private void createCraterExplosion(BlockPos impactPos) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        // Create explosion that damages blocks
        serverLevel.explode(
                this.getOwner(), // source entity
                this.getX(), this.getY(), this.getZ(),
                EXPLOSION_RADIUS,
                true, // causes fire
                Level.ExplosionInteraction.MOB
        );
        
        // Spawn extra fire particles
        serverLevel.sendParticles(ParticleTypes.LAVA,
                this.getX(), this.getY(), this.getZ(),
                10, 0.5, 0.5, 0.5, 0.1);
    }

    /**
     * Creates a visual explosion effect without block damage.
     */
    private void createExplosionEffect() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        // Spawn explosion particles
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                this.getX(), this.getY(), this.getZ(),
                1, 0, 0, 0, 0);
        serverLevel.sendParticles(ParticleTypes.FLAME,
                this.getX(), this.getY(), this.getZ(),
                20, 0.5, 0.5, 0.5, 0.1);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        // Don't hit the owner
        if (target == this.getOwner()) {
            return false;
        }
        return super.canHitEntity(target);
    }

    /**
     * Get the current rotation for rendering.
     */
    public float getRotation(float partialTicks) {
        return this.rotation + (15.0F * partialTicks);
    }

    public int getAge() {
        return this.age;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Age", this.age);
        tag.putFloat("Rotation", this.rotation);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.age = tag.getInt("Age");
        this.rotation = tag.getFloat("Rotation");
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false; // Fireball cannot be damaged
    }
}
