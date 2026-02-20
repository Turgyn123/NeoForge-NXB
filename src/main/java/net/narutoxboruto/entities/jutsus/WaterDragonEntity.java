package net.narutoxboruto.entities.jutsus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.narutoxboruto.entities.ModEntities;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Water Dragon Projectile - Powerful Water Release Jutsu
 * 
 * - Phase 1 (Rising): Spawns below ground, rises vertically while facing straight up.
 *   Spawn animation plays, base stays near ground while head emerges skyward.
 * - Phase 2 (Hovering): Smoothly transitions from vertical to horizontal ("straightens out"),
 *   rotating to face the target. Dramatic buildup moment.
 * - Phase 3 (Flight): Launches toward target at high speed in a straight line.
 * - Impact: Massive AOE explosion + water damage + temporary water blocks.
 * - Rendered at 5x scale for an imposing visual.
 */
public class WaterDragonEntity extends Projectile implements GeoEntity {
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    private int age = 0;
    private boolean launched = false;
    private Vec3 launchDir = null;     // Direction to launch, calculated during hover
    private Vec3 startPos;             // Set when launched, for range check
    private double yOrigin = Double.MIN_VALUE; // Y position when spawned (set on first tick)
    private boolean yOriginInitialized = false;
    
    // === Configuration (inspired by reference mod values) ===
    private static final float DAMAGE = 20.0F;          // Direct hit damage (devastating)
    private static final float AOE_DAMAGE = 10.0F;      // AOE splash damage at center
    private static final float SPEED = 0.5F;             // Fast but visible flight speed
    private static final int RISE_TICKS = 40;            // 2s rising phase (longer for drama)
    private static final int HOVER_TICKS = 20;           // 1s hovering phase
    private static final int WINDUP_TICKS = RISE_TICKS + HOVER_TICKS; // 3s total windup
    private static final int MAX_FLIGHT_TICKS = 60;      // ~3s flight after launch
    private static final int MAX_TOTAL_TICKS = 130;      // ~6.5s total lifetime
    private static final double MAX_RANGE = 50.0;        // Max travel distance after launch
    private static final float AOE_RADIUS = 4.0F;        // Large splash radius
    private static final float EXPLOSION_POWER = 5.0F;   // Massive visual explosion
    private static final double RISE_HEIGHT = 5.0;       // How many blocks to rise total
    private static final double START_BELOW = 2.0;       // Start 2 blocks below ground for emergence
    
    public WaterDragonEntity(EntityType<? extends WaterDragonEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true; // No collision during windup
    }
    
    public WaterDragonEntity(Level level, LivingEntity shooter) {
        this(ModEntities.WATER_DRAGON.get(), level);
        this.setOwner(shooter);
        
        // Spawn below the shooter's feet (emerges from ground)
        this.setPos(shooter.getX(), shooter.getY() - START_BELOW, shooter.getZ());
        this.yOrigin = shooter.getY() - START_BELOW;
        this.yOriginInitialized = true;
        
        // Face the same direction as the shooter initially
        this.setYRot(shooter.getYHeadRot());
        this.setXRot(0); // Level pitch during rise
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        
        // No movement yet - will rise vertically first
        this.setDeltaMovement(Vec3.ZERO);
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No additional synced data needed
    }
    
    @Override
    public void tick() {
        super.tick();
        this.age++;
        
        Entity owner = this.getOwner();
        
        // If owner dies/disconnects, discard
        if (owner == null || !owner.isAlive()) {
            if (this.launched) explodeWithWater();
            this.discard();
            return;
        }
        
        // Max lifetime check
        if (this.age > MAX_TOTAL_TICKS) {
            this.explodeWithWater();
            this.discard();
            return;
        }
        
        // ==================== PHASE 1: RISING ====================
        // Rise vertically from below ground, facing straight up
        // Spawn animation plays while pitch is constant - no animation fighting
        if (this.age <= RISE_TICKS) {
            // Initialize yOrigin on first tick if not set (fixes client-side sync)
            if (!this.yOriginInitialized) {
                this.yOrigin = this.getY();
                this.yOriginInitialized = true;
            }
            
            // Rise at a constant rate to reach RISE_HEIGHT blocks above origin
            double riseSpeed = RISE_HEIGHT / (double) RISE_TICKS;
            double newY = this.yOrigin + (riseSpeed * this.age);
            this.setPos(this.getX(), newY, this.getZ());
            
            // Face straight up during rise - stable rotation prevents "bent" look
            this.setXRot(90.0f);
            // Keep yRot as initialized (shooter's direction) - don't change it
            
            // Rising water wake particles (increasing density)
            if (this.level() instanceof ServerLevel sl) {
                int particleCount = this.age * 5; // Gets denser over time
                sl.sendParticles(ParticleTypes.SPLASH,
                    this.getX(), this.getY(), this.getZ(),
                    particleCount, 0.5, 0.3, 0.5, 0.05);
            }
            return;
        }
        
        // ==================== PHASE 2: HOVERING ====================
        // Straighten out from vertical (up) to horizontal (toward target)
        if (this.age <= WINDUP_TICKS) {
            // Stay at the risen position
            double hoverY = this.yOrigin + RISE_HEIGHT;
            this.setPos(this.getX(), hoverY, this.getZ());
            
            // Calculate target direction for launch
            computeLaunchDirection(owner);
            
            // Smoothly transition pitch from 90 (up) to target pitch (straightening out)
            int hoverAge = this.age - RISE_TICKS;
            float hoverProgress = Mth.clamp((float) hoverAge / (float) HOVER_TICKS, 0.0f, 1.0f);
            
            float targetPitch = 0.0f;
            float targetYaw = this.getYRot();
            if (this.launchDir != null) {
                double horizDist = this.launchDir.horizontalDistance();
                targetPitch = (float)(Mth.atan2(this.launchDir.y, horizDist) * (180.0 / Math.PI));
                targetYaw = (float)(Mth.atan2(this.launchDir.x, this.launchDir.z) * (180.0 / Math.PI));
            }
            
            // Lerp pitch from 90 (up) to target angle
            this.setXRot(Mth.lerp(hoverProgress, 90.0f, targetPitch));
            
            // Approach yaw toward target direction
            float yawDiff = Mth.wrapDegrees(targetYaw - this.getYRot());
            float maxYawStep = 360.0f / (float) HOVER_TICKS;
            this.setYRot(this.getYRot() + Mth.clamp(yawDiff, -maxYawStep, maxYawStep));
            
            // Heavy hovering particles
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SPLASH,
                    this.getX(), this.getY(), this.getZ(),
                    this.age * 5, 0.5, 0.5, 0.5, 0.05);
                sl.sendParticles(ParticleTypes.DRIPPING_WATER,
                    this.getX(), this.getY() - 1, this.getZ(),
                    20, 0.3, 1.0, 0.3, 0.01);
            }
            return;
        }
        
        // ==================== PHASE 3: LAUNCH & FLIGHT ====================
        if (!this.launched) {
            this.launched = true;
            this.noPhysics = false; // Enable collision for flight
            this.startPos = this.position();
            
            // Calculate launch direction toward target
            if (this.launchDir != null) {
                this.setDeltaMovement(this.launchDir.scale(SPEED));
            } else if (owner instanceof LivingEntity shooter) {
                this.setDeltaMovement(shooter.getLookAngle().scale(SPEED));
            }
        }
        
        // Check max flight time / range
        int flightAge = this.age - WINDUP_TICKS;
        if (flightAge > MAX_FLIGHT_TICKS ||
            (this.startPos != null && this.startPos.distanceTo(this.position()) > MAX_RANGE)) {
            this.explodeWithWater();
            this.discard();
            return;
        }
        
        // Hit detection
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onHit(hitResult);
            return;
        }
        
        // Update position
        Vec3 velocity = this.getDeltaMovement();
        this.setPos(this.getX() + velocity.x, this.getY() + velocity.y, this.getZ() + velocity.z);
        
        // Update rotation to face movement direction (only when moving)
        double horizSpeed = velocity.horizontalDistance();
        if (horizSpeed > 0.001) {
            this.setYRot((float)(Mth.atan2(velocity.x, velocity.z) * (180.0 / Math.PI)));
            this.setXRot((float)(Mth.atan2(velocity.y, horizSpeed) * (180.0 / Math.PI)));
        }
        
        // Heavy flight particles - massive water trail (200 particles like reference)
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FALLING_WATER,
                this.getX(), this.getY(), this.getZ(),
                100, 0.5, 0.5, 0.5, 0.15);
            sl.sendParticles(ParticleTypes.SPLASH,
                this.getX(), this.getY(), this.getZ(),
                100, 0.5, 0.5, 0.5, 0.1);
        }
    }
    
    /**
     * Compute the launch direction toward a target entity or the shooter's look.
     * Only calculates launchDir, does NOT set rotation (handled by phase code).
     */
    private void computeLaunchDirection(Entity owner) {
        if (!(owner instanceof LivingEntity shooter)) return;
        
        Vec3 targetPos = null;
        
        if (owner instanceof Player player) {
            List<LivingEntity> nearby = this.level().getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(50.0),
                e -> e != player && e.isAlive() && !e.isSpectator()
            );
            
            if (!nearby.isEmpty()) {
                Vec3 look = player.getLookAngle();
                Vec3 eye = player.getEyePosition();
                LivingEntity best = null;
                double bestDot = -1;
                
                for (LivingEntity e : nearby) {
                    Vec3 toE = e.position().subtract(eye).normalize();
                    double dot = look.dot(toE);
                    if (dot > bestDot) {
                        bestDot = dot;
                        best = e;
                    }
                }
                
                if (best != null && bestDot > 0.5) {
                    targetPos = best.getEyePosition();
                }
            }
        }
        
        if (targetPos == null) {
            targetPos = shooter.getEyePosition().add(shooter.getLookAngle().scale(50.0));
        }
        
        this.launchDir = targetPos.subtract(this.position()).normalize();
    }
    
    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity != this.getOwner();
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
        if (target instanceof LivingEntity living && living != this.getOwner()) {
            // Devastating direct hit damage
            living.hurt(this.damageSources().mobProjectile(this,
                this.getOwner() instanceof LivingEntity le ? le : null), DAMAGE);
            
            // Strong knockback
            Vec3 knockback = this.getDeltaMovement().normalize().scale(1.5);
            living.setDeltaMovement(living.getDeltaMovement().add(knockback));
        }
        
        // Massive explosion on impact
        explodeWithWater();
        this.discard();
    }
    
    @Override
    protected void onHitBlock(BlockHitResult result) {
        explodeWithWater();
        this.discard();
    }
    
    /**
     * Create a massive water explosion on impact:
     * - Large AOE damage to all nearby entities
     * - Dramatic explosion visual (power 5)
     * - Temporary water blocks (removed after 1 second)
     * - Massive water particle effects
     */
    private void explodeWithWater() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        
        // --- AOE Damage (hits ANY entity, not target-specific) ---
        for (LivingEntity nearby : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(AOE_RADIUS))) {
            if (nearby != this.getOwner() && nearby.isAlive()) {
                double dist = nearby.distanceTo(this);
                if (dist <= AOE_RADIUS) {
                    float dmg = AOE_DAMAGE * (float)(1.0 - dist / AOE_RADIUS);
                    nearby.hurt(this.damageSources().mobProjectile(this,
                        this.getOwner() instanceof LivingEntity le ? le : null), dmg);
                    
                    // Strong knockback away from explosion
                    Vec3 kb = nearby.position().subtract(this.position()).normalize().scale(1.0);
                    nearby.setDeltaMovement(nearby.getDeltaMovement().add(kb));
                }
            }
        }
        
        // --- Massive visual explosion (no block damage) ---
        serverLevel.explode(null, this.getX(), this.getY(), this.getZ(),
            EXPLOSION_POWER, Level.ExplosionInteraction.NONE);
        
        // --- Water particles ---
        serverLevel.sendParticles(ParticleTypes.SPLASH,
            this.getX(), this.getY(), this.getZ(), 100, 2.0, 2.0, 2.0, 0.5);
        serverLevel.sendParticles(ParticleTypes.FALLING_WATER,
            this.getX(), this.getY() + 2, this.getZ(), 60, 2.0, 0.5, 2.0, 0.3);
        serverLevel.sendParticles(ParticleTypes.RAIN,
            this.getX(), this.getY() + 3, this.getZ(), 40, 2.5, 0.5, 2.5, 0.3);
        
        // --- Temporary water blocks ---
        placeTemporaryWater(serverLevel);
        
        // --- Impact sound ---
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 2.0F, 0.6F);
    }
    
    /**
     * Place temporary water blocks at the impact site.
     * Covers a larger area than shark bomb. Removed after ~1 second.
     */
    private void placeTemporaryWater(ServerLevel level) {
        BlockPos center = BlockPos.containing(this.position());
        List<BlockPos> waterPositions = new ArrayList<>();
        
        // Fill a larger area with water (3 block radius)
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-3, 0, -3), center.offset(3, 0, 3))) {
            if (level.getBlockState(pos).isAir()) {
                level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
                waterPositions.add(pos.immutable());
            }
        }
        
        // Schedule removal after 20 ticks (1 second)
        if (!waterPositions.isEmpty() && level.getServer() != null) {
            int removeAt = level.getServer().getTickCount() + 20;
            level.getServer().tell(new TickTask(removeAt, () -> {
                for (BlockPos pos : waterPositions) {
                    if (level.getBlockState(pos).is(Blocks.WATER)) {
                        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    }
                }
            }));
        }
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.age = compound.getInt("Age");
        this.launched = compound.getBoolean("Launched");
        if (compound.contains("YOrigin")) {
            this.yOrigin = compound.getDouble("YOrigin");
            this.yOriginInitialized = true;
        }
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Age", this.age);
        compound.putBoolean("Launched", this.launched);
        compound.putDouble("YOrigin", this.yOrigin);
    }
    
    // === GeckoLib Animation ===
    
    private static final RawAnimation SPAWN_ANIM = RawAnimation.begin().thenPlay("Spawn");
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("Idle");
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 5, state -> {
            // Play Spawn animation during rise (unfurls the dragon), then Idle for flight
            if (this.age <= RISE_TICKS) {
                return state.setAndContinue(SPAWN_ANIM);
            }
            return state.setAndContinue(IDLE_ANIM);
        }));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
