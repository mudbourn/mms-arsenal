package info.mudbourn.mmsarsenal.gun.mixin;

import info.mudbourn.mmsarsenal.gun.throwable.SmokeGrenadeEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Mobs cannot see anything inside a smoke grenade's column or behind it, so smoke hides targets and threats alike.
@Mixin(LivingEntity.class)
public abstract class MobSmokeSightMixin {

    @Inject(method = "hasLineOfSight(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/ClipContext$Block;Lnet/minecraft/world/level/ClipContext$Fluid;D)Z", at = @At("HEAD"), cancellable = true)
    private void mmsArsenal$smokeBlocksSight(Entity target, ClipContext.Block block, ClipContext.Fluid fluid, double eyeY,
                                             CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Mob && !self.level().isClientSide()
            && SmokeGrenadeEntity.blocksSight(self.level(), self.getEyePosition(), new Vec3(target.getX(), eyeY, target.getZ()))) {
            cir.setReturnValue(false);
        }
    }
}
