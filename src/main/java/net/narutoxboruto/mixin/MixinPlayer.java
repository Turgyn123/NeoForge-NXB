package net.narutoxboruto.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.modes.ChakraControl;
import net.narutoxboruto.attachments.modes.NarutoRun;
import net.narutoxboruto.effect.ModEffects;
import net.narutoxboruto.entities.ModeHandler;
import net.narutoxboruto.items.PreventSlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MixinPlayer extends LivingEntity implements ModeHandler {
    @Unique
    private static final EntityDataAccessor<Boolean> DATA_NARUTO_RUNNING = SynchedEntityData.defineId(
            MixinPlayer.class, EntityDataSerializers.BOOLEAN);
    @Unique private static final EntityDataAccessor<Boolean> DATA_CHAKRA_CONTROL = SynchedEntityData.defineId(
            MixinPlayer.class, EntityDataSerializers.BOOLEAN);

    protected MixinPlayer(EntityType<? extends LivingEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Shadow
    public abstract Abilities getAbilities();

    @Override
    public boolean canStandOnFluid(FluidState fluidState) {
        return this.$getChakraControl() && !this.isCrouching() && fluidState.is(Fluids.WATER) && this.fallDistance < 3
                && !this.isInWater();
    }

    @Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
    private void getDefaultDimensions(Pose pPose, CallbackInfoReturnable<EntityDimensions> cir) {
        if ($isNarutoRunning()) {
            cir.setReturnValue(EntityDimensions.scalable(1.2F, 1.7F));
            EntityDimensions newDimensions = EntityDimensions.scalable(0.6F, 1.8F)
                    .withEyeHeight(1.4F);
            cir.setReturnValue(newDimensions);

        }
    }

        @Inject(method = "defineSynchedData", at = @At("HEAD"))
    private void defineSynchedData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(DATA_NARUTO_RUNNING, false);
        builder.define(DATA_CHAKRA_CONTROL, false);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void readAdditionalSaveData(CompoundTag pCompound, CallbackInfo ci) {
        if (pCompound.contains("NarutoRunning")) {
            this.entityData.set(DATA_NARUTO_RUNNING, pCompound.getBoolean("NarutoRunning"));
        }
        if (pCompound.contains("ChakraControl")) {
            this.entityData.set(DATA_CHAKRA_CONTROL, pCompound.getBoolean("ChakraControl"));
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void addAdditionalSaveData(CompoundTag pCompound, CallbackInfo ci) {
        pCompound.putBoolean("NarutoRunning", this.getEntityData().get(DATA_NARUTO_RUNNING));
        pCompound.putBoolean("ChakraControl", this.getEntityData().get(DATA_CHAKRA_CONTROL));
    }

    @Unique
    public boolean $isNarutoRunning() {
        return this.entityData.get(DATA_NARUTO_RUNNING);
    }

    @Unique
    public void $setNarutoRunning(boolean b) {
        this.entityData.set(DATA_NARUTO_RUNNING, b);
    }

    @Unique
    public boolean $getChakraControl() {
        return this.entityData.get(DATA_CHAKRA_CONTROL);
    }

    @Unique
    public void $setChakraControl(boolean b) {
        this.entityData.set(DATA_CHAKRA_CONTROL, b);
        this.removeEffect(ModEffects.CHAKRA_CONTROL);
        if (b) {
            this.addEffect(new MobEffectInstance(
                    ModEffects.CHAKRA_CONTROL,
                    -1,      // Use -1 for infinite duration
                    0,       // amplifier
                    false,   // ambient = false
                    true,    // visible = true
                    true     // show icon = true
            ));
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> entityDataAccessor) {
        if (DATA_NARUTO_RUNNING.equals(entityDataAccessor)) {
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(entityDataAccessor);
    }

    @Override
    protected void playStepSound(BlockPos pPos, BlockState pState) {
        if (this.canStandOnFluid(pState.getFluidState())) {
            SoundType soundtype = pState.getSoundType(level(), pPos, this);
            soundtype.getPitch();
        }
        super.playStepSound(pPos, pState);
    }

    @Inject(method = "serverAiStep", at = @At("HEAD"))
    public void serverAiStep(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer serverPlayer) {
            NarutoRun narutoRun = serverPlayer.getData(MainAttachment.NARUTO_RUN);
            ChakraControl chakraControl = serverPlayer.getData(MainAttachment.CHAKRA_CONTROL);

            narutoRun.setValue(
                    chakraControl.isActive() && !this.isInWater() && this.isSprinting() && !this.isCrouching()
                            && (this.getUseItem().getItem() instanceof PreventSlow || !this.isUsingItem())
                            && !this.isFallFlying() && !this.getAbilities().flying && this.fallDistance <= 3,
                    serverPlayer);
        }
    }
}
