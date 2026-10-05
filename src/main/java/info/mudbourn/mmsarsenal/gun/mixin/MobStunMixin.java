package info.mudbourn.mmsarsenal.gun.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import info.mudbourn.mmsarsenal.gun.GunMobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// A stunned mob, blinded by a stun grenade, loses its senses: its goals and brain stop, it cannot take a target, and it wanders aimlessly instead.
@Mixin(Mob.class)
public abstract class MobStunMixin {

    // How far a stunned mob strays on each aimless leg.
    private static final int WANDER_RANGE = 6;
    private static final double WANDER_SPEED = 1.2;

    private boolean mmsArsenal$stunned() {
        return ((Mob) (Object) this).hasEffect(GunMobEffects.BLINDED);
    }

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void mmsArsenal$noTargetWhileStunned(LivingEntity target, CallbackInfo ci) {
        if (target != null && this.mmsArsenal$stunned()) {
            ci.cancel();
        }
    }

    @Inject(method = "serverAiStep", at = @At("HEAD"))
    private void mmsArsenal$wanderWhileStunned(CallbackInfo ci) {
        if (!this.mmsArsenal$stunned()) {
            return;
        }
        Mob self = (Mob) (Object) this;
        self.setTarget(null);
        if (self.getNavigation().isDone() || self.getRandom().nextInt(40) == 0) {
            Vec3 pos = self instanceof PathfinderMob pathfinder
                ? DefaultRandomPos.getPos(pathfinder, WANDER_RANGE, 3)
                : self.position().add(self.getRandom().nextGaussian() * WANDER_RANGE, 0.0, self.getRandom().nextGaussian() * WANDER_RANGE);
            if (pos != null) {
                self.getNavigation().moveTo(pos.x, pos.y, pos.z, WANDER_SPEED);
            }
        }
        if (self.getRandom().nextInt(10) == 0) {
            self.getLookControl().setLookAt(self.getEyePosition().add(self.getRandom().nextGaussian(), self.getRandom().nextGaussian() * 0.5, self.getRandom().nextGaussian()));
        }
    }

    @WrapOperation(method = "serverAiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/goal/GoalSelector;tick()V"))
    private void mmsArsenal$skipGoalsWhileStunned(GoalSelector selector, Operation<Void> original) {
        if (!this.mmsArsenal$stunned()) {
            original.call(selector);
        }
    }

    @WrapOperation(method = "serverAiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/goal/GoalSelector;tickRunningGoals(Z)V"))
    private void mmsArsenal$skipRunningGoalsWhileStunned(GoalSelector selector, boolean everyTick, Operation<Void> original) {
        if (!this.mmsArsenal$stunned()) {
            original.call(selector, everyTick);
        }
    }

    // Brain-driven mobs think in their custom AI step, so it is skipped too.
    @WrapOperation(method = "serverAiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;customServerAiStep(Lnet/minecraft/server/level/ServerLevel;)V"))
    private void mmsArsenal$skipBrainWhileStunned(Mob mob, ServerLevel level, Operation<Void> original) {
        if (!this.mmsArsenal$stunned()) {
            original.call(mob, level);
        }
    }
}
