package net.narutoxboruto.attachments.climber;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.narutoxboruto.util.Orientation;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Handles movement for wall-climbing players.
 *
 * Ported from AWCAPI's ClimberComponent.travelOnGround():
 *   probe move → get collision normal → project movement → real move.
 *
 * Runs on the CLIENT only.  The server skips custom movement (it accepts
 * the client's authoritative position packets) but still cancels vanilla
 * travel so gravity is not applied server-side.
 */
public class ClimberMoveController {

    private static final double BASE_STICKING_FORCE = 0.08;

    // Attached-side tracking for detach handling (AWCAPI pattern)
    private static Vec3 attachedSides = Vec3.ZERO;
    private static Vec3 prevAttachedSides = Vec3.ZERO;

    /**
     * Handle complete travel logic for climbing.
     *
     * @return true  → skip vanilla travel()
     *         false → use vanilla travel()
     */
    public static boolean handleTravel(Player player, Vec3 travelVector, ClimberComponent climber) {
        if (!climber.isClimbing()) {
            attachedSides = Vec3.ZERO;
            prevAttachedSides = Vec3.ZERO;
            return false;
        }

        // Server: skip vanilla travel (prevents gravity) but do NOT compute custom
        // movement – the server trusts the client's position packets.
        if (!player.level().isClientSide) {
            return true;
        }

        // ── Orientation ──────────────────────────────────────────────
        Orientation orientation = climber.getMovementOrientation();
        Direction groundSide = climber.getCurrentWallDirection();
        if (groundSide == null) groundSide = Direction.DOWN;

        Vec3 groundNormal = new Vec3(groundSide.getStepX(), groundSide.getStepY(), groundSide.getStepZ());
        Pair<Direction, Vec3> groundDirection = Pair.of(groundSide, groundNormal);

        // Orientation basis vectors
        Vec3 forwardVector = orientation.getGlobal(player.getYRot(), player.getXRot());
        Vec3 strafeVector  = orientation.getGlobal(player.getYRot() + 90.0f, 0);
        Vec3 upVector      = orientation.getGlobal(player.getYRot(), -90.0f);

        // Sticking force
        Vec3 stickingForce = calcStickingForce(player, orientation.normal, groundDirection);

        // Input
        float forward = (float) travelVector.z;
        float strafe  = (float) -travelVector.x;  // negated to fix left/right

        // ── Movement calculation (AWCAPI probe-move pattern) ─────────
        if (forward != 0 || strafe != 0) {
            float slipperiness = 0.91f;
            if (player.onGround()) {
                BlockPos offsetPos = player.blockPosition().relative(groundSide);
                slipperiness = player.level().getBlockState(offsetPos)
                        .getFriction(player.level(), offsetPos, player);
            }

            float f = forward * forward + strafe * strafe;
            if (f >= 1.0E-4F) {
                f = Math.max(Mth.sqrt(f), 1.0f);
                f = moveFactor(player, slipperiness) / f;
                forward *= f;
                strafe  *= f;

                Vec3 movementOffset = new Vec3(
                        forwardVector.x * forward + strafeVector.x * strafe,
                        forwardVector.y * forward + strafeVector.y * strafe,
                        forwardVector.z * forward + strafeVector.z * strafe
                );

                // Probe move → collision direction
                double px = player.getX(), py = player.getY(), pz = player.getZ();
                Vec3 motion = player.getDeltaMovement();
                AABB aabb = player.getBoundingBox();

                player.move(MoverType.SELF, movementOffset);

                Vec3 movementDir = new Vec3(
                        player.getX() - px, player.getY() - py, player.getZ() - pz
                ).normalize();

                // Reset
                player.setBoundingBox(aabb);
                setLocFromBB(player);
                player.setDeltaMovement(motion);

                // Collision probe
                Vec3 probeVector = new Vec3(
                        Math.abs(movementDir.x) < 0.001 ? -Math.signum(upVector.x) : 0,
                        Math.abs(movementDir.y) < 0.001 ? -Math.signum(upVector.y) : 0,
                        Math.abs(movementDir.z) < 0.001 ? -Math.signum(upVector.z) : 0
                ).normalize().scale(0.0001);
                player.move(MoverType.SELF, probeVector);

                Vec3 collisionNormal = new Vec3(
                        Math.abs(player.getX() - px - probeVector.x) > 1E-6 ? Math.signum(-probeVector.x) : 0,
                        Math.abs(player.getY() - py - probeVector.y) > 1E-6 ? Math.signum(-probeVector.y) : 0,
                        Math.abs(player.getZ() - pz - probeVector.z) > 1E-6 ? Math.signum(-probeVector.z) : 0
                ).normalize();

                // Reset again
                player.setBoundingBox(aabb);
                setLocFromBB(player);
                player.setDeltaMovement(motion);

                // Project movement along surface
                Vec3 surfaceDir = movementDir
                        .subtract(collisionNormal.scale(collisionNormal.dot(movementDir)))
                        .normalize();

                boolean isInnerCorner =
                        Math.abs(collisionNormal.x) + Math.abs(collisionNormal.y)
                                + Math.abs(collisionNormal.z) > 1.0001f;
                if (!isInnerCorner) movementDir = surfaceDir;

                stickingForce = stickingForce
                        .subtract(surfaceDir.scale(surfaceDir.normalize().dot(stickingForce)));

                float moveSpeed = Mth.sqrt(forward * forward + strafe * strafe);
                player.setDeltaMovement(player.getDeltaMovement().add(movementDir.scale(moveSpeed)));
            }
        }

        // ── Sticking force ───────────────────────────────────────────
        player.setDeltaMovement(player.getDeltaMovement().add(stickingForce));

        // ── Real move ────────────────────────────────────────────────
        double px = player.getX(), py = player.getY(), pz = player.getZ();
        Vec3 motion = player.getDeltaMovement();
        player.move(MoverType.SELF, motion);

        // Track attached sides
        prevAttachedSides = attachedSides;
        attachedSides = new Vec3(
                Math.abs(player.getX() - px - motion.x) > 0.001 ? -Math.signum(motion.x) : 0,
                Math.abs(player.getY() - py - motion.y) > 0.001 ? -Math.signum(motion.y) : 0,
                Math.abs(player.getZ() - pz - motion.z) > 0.001 ? -Math.signum(motion.z) : 0
        );

        // ── Friction ─────────────────────────────────────────────────
        if (forward == 0 && strafe == 0) {
            player.setDeltaMovement(Vec3.ZERO);
        } else {
            float friction = 0.91f;
            if (player.onGround()) player.fallDistance = 0;

            motion = player.getDeltaMovement();
            Vec3 orthogonal = upVector.scale(upVector.dot(motion));
            Vec3 tangential = motion.subtract(orthogonal);
            player.setDeltaMovement(
                    tangential.x * friction + orthogonal.x * 0.98,
                    tangential.y * friction + orthogonal.y * 0.98,
                    tangential.z * friction + orthogonal.z * 0.98
            );
        }

        // ── Detach handling (step onto adjacent surface) ─────────────
        boolean detX = attachedSides.x != prevAttachedSides.x && Math.abs(attachedSides.x) < 0.001;
        boolean detY = attachedSides.y != prevAttachedSides.y && Math.abs(attachedSides.y) < 0.001;
        boolean detZ = attachedSides.z != prevAttachedSides.z && Math.abs(attachedSides.z) < 0.001;

        if (detX || detY || detZ) {
            float stepHeight = player.maxUpStep();
            AttributeInstance stepAttr = player.getAttribute(Attributes.STEP_HEIGHT);
            if (stepAttr != null) stepAttr.setBaseValue(0);

            boolean prevOnGround = player.onGround();
            boolean prevHCol     = player.horizontalCollision;
            boolean prevVCol     = player.verticalCollision;

            player.move(MoverType.SELF, new Vec3(
                    detX ? -prevAttachedSides.x * 0.25f : 0,
                    detY ? -prevAttachedSides.y * 0.25f : 0,
                    detZ ? -prevAttachedSides.z * 0.25f : 0
            ));

            Vec3 axis = prevAttachedSides.normalize();
            Vec3 attachVec = upVector.scale(-1)
                    .subtract(axis.scale(axis.dot(upVector.scale(-1))));

            if (Math.abs(attachVec.x) > Math.abs(attachVec.y)
                    && Math.abs(attachVec.x) > Math.abs(attachVec.z)) {
                attachVec = new Vec3(Math.signum(attachVec.x), 0, 0);
            } else if (Math.abs(attachVec.y) > Math.abs(attachVec.z)) {
                attachVec = new Vec3(0, Math.signum(attachVec.y), 0);
            } else {
                attachVec = new Vec3(0, 0, Math.signum(attachVec.z));
            }

            double attachDst = motion.length() + 0.1;
            AABB aabb = player.getBoundingBox();
            motion = player.getDeltaMovement();

            for (int i = 0; i < 2 && !player.onGround(); i++) {
                player.move(MoverType.SELF, attachVec.scale(attachDst));
            }

            if (stepAttr != null) stepAttr.setBaseValue(stepHeight);

            if (!player.onGround()) {
                player.setBoundingBox(aabb);
                setLocFromBB(player);
                player.setDeltaMovement(motion);
                player.setOnGround(prevOnGround);
                player.horizontalCollision = prevHCol;
                player.verticalCollision   = prevVCol;
            } else {
                player.setDeltaMovement(Vec3.ZERO);
            }
        }

        // ── Finish ───────────────────────────────────────────────────
        player.fallDistance = 0;
        player.calculateEntityAnimation(true);
        return true;
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private static float moveFactor(Player player, float slipperiness) {
        float speed = player.getSpeed();
        return speed * (0.16277136F / (slipperiness * slipperiness * slipperiness));
    }

    private static Vec3 calcStickingForce(Player player, Vec3 attachmentNormal,
                                          Pair<Direction, Vec3> walkingSide) {
        double uprightness = Math.max(attachmentNormal.y, 0);
        double gravity = player.getGravity();
        double force = gravity * uprightness + BASE_STICKING_FORCE * (1 - uprightness);
        return walkingSide.getRight().scale(force);
    }

    private static void setLocFromBB(Player player) {
        AABB bb = player.getBoundingBox();
        player.setPosRaw((bb.minX + bb.maxX) / 2.0, bb.minY, (bb.minZ + bb.maxZ) / 2.0);
    }
}
