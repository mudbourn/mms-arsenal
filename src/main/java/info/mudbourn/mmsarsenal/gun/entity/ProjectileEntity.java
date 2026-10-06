package info.mudbourn.mmsarsenal.gun.entity;

import info.mudbourn.mmsarsenal.MmsArsenal;
import info.mudbourn.mmsarsenal.config.ArsenalConfig;
import info.mudbourn.mmsarsenal.gun.Gun;
import info.mudbourn.mmsarsenal.gun.GunNetwork;
import info.mudbourn.mmsarsenal.gun.GunSounds;
import info.mudbourn.mmsarsenal.gun.GunState;
import info.mudbourn.mmsarsenal.gun.GunGameRules;
import info.mudbourn.mmsarsenal.gun.GunTags;
import info.mudbourn.mmsarsenal.gun.SpreadTracker;
import info.mudbourn.mmsarsenal.gun.net.ProjectileHitPayloads;
import info.mudbourn.mmsweapons.headshot.HeadshotBoxes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

// A fired bullet: flies in a straight line or under gravity, hits the first thing on its path, and expires after its life.
public class ProjectileEntity extends Entity {

    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(ProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GRAVITY = SynchedEntityData.defineId(ProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(ProjectileEntity.class, EntityDataSerializers.INT);
    private static final ResourceKey<DamageType> BULLET = ResourceKey.create(Registries.DAMAGE_TYPE, MmsArsenal.id("bullet"));
    private static final Predicate<Entity> TARGETS = entity -> entity != null && entity.isPickable() && !entity.isSpectator();

    protected LivingEntity shooter;
    protected int shooterId;
    protected Gun gun;
    protected ItemStack weapon = ItemStack.EMPTY;

    public ProjectileEntity(EntityType<? extends ProjectileEntity> type, Level level) {
        super(type, level);
    }

    public ProjectileEntity(EntityType<? extends ProjectileEntity> type, Level level, LivingEntity shooter, ItemStack weapon, Gun gun) {
        this(type, level);
        this.shooter = shooter;
        this.shooterId = shooter.getId();
        this.gun = gun;
        this.weapon = weapon.copy();
        Gun.Projectile projectile = gun.projectile();
        this.entityData.set(SIZE, projectile.size());
        this.entityData.set(GRAVITY, projectile.gravity() ? -0.04F : 0.0F);
        this.entityData.set(LIFE, projectile.life());
        this.refreshDimensions();
        Vec3 direction = this.direction(shooter, weapon, gun);
        double speed = projectile.speed();
        this.setDeltaMovement(direction.x * speed, direction.y * speed, direction.z * speed);
        this.updateHeading();
        this.setPos(
            shooter.xOld + (shooter.getX() - shooter.xOld) / 2.0,
            shooter.yOld + (shooter.getY() - shooter.yOld) / 2.0 + shooter.getEyeHeight(),
            shooter.zOld + (shooter.getZ() - shooter.zOld) / 2.0
        );
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SIZE, 0.25F);
        builder.define(GRAVITY, 0.0F);
        builder.define(LIFE, 60);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float size = this.entityData.get(SIZE);
        return EntityDimensions.scalable(size, size);
    }

    public LivingEntity getShooter() {
        return this.shooter;
    }

    public ItemStack getWeapon() {
        return this.weapon;
    }

    public double projectileGravity() {
        return this.entityData.get(GRAVITY);
    }

    public int getLife() {
        return this.entityData.get(LIFE);
    }

    // The shot's direction: straight ahead, or a random point in a cone that widens with the gun's current spread.
    private Vec3 direction(LivingEntity shooter, ItemStack weapon, Gun gun) {
        float spread = gun.general().spread();
        if (spread == 0.0F) {
            return rotationVector(shooter.getXRot(), shooter.getYRot());
        }
        if (shooter instanceof Player player) {
            if (!gun.general().alwaysSpread()) {
                spread *= SpreadTracker.get(player).spread(weapon.getItem());
            }
            if (GunState.isAiming(player)) {
                spread *= 0.5F;
            }
        } else {
            spread *= shooter.level().getDifficulty() != Difficulty.HARD ? 10.0F : 5.0F;
        }
        spread = Math.min(spread, 170.0F) * 0.5F * Mth.DEG_TO_RAD;
        Vec3 forwards = rotationVector(shooter.getXRot(), shooter.getYRot());
        Vec3 upwards = rotationVector(shooter.getXRot() + 90.0F, shooter.getYRot());
        Vec3 sideways = forwards.cross(upwards);
        float theta = this.random.nextFloat() * 2.0F * Mth.PI;
        float radius = Mth.sqrt(this.random.nextFloat()) * (float) Math.tan(spread);
        return forwards
            .add(sideways.scale(Mth.cos(theta) * radius))
            .add(upwards.scale(Mth.sin(theta) * radius))
            .normalize();
    }

    @Override
    public void tick() {
        super.tick();
        this.updateHeading();
        this.onProjectileTick();
        if (this.level() instanceof ServerLevel level && this.gun != null) {
            Vec3 start = this.position();
            Vec3 end = start.add(this.getDeltaMovement());
            BlockHitResult blockHit = clipBlocks(level, new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (blockHit.getType() != HitResult.Type.MISS) {
                end = blockHit.getLocation();
            }
            List<EntityHit> hits = new ArrayList<>();
            if (this.gun.projectile().collateral()) {
                hits.addAll(this.findEntitiesOnPath(start, end));
            } else {
                EntityHit hit = this.findEntityOnPath(start, end);
                if (hit != null) {
                    hits.add(hit);
                }
            }
            if (hits.isEmpty()) {
                this.onHitBlockResult(level, blockHit);
            } else {
                for (EntityHit hit : hits) {
                    if (hit.entity() instanceof Player target && this.shooter instanceof Player player && !player.canHarmPlayer(target)) {
                        continue;
                    }
                    this.onHitEntityResult(level, hit, start, end);
                }
            }
        }
        this.setPos(this.getX() + this.getDeltaMovement().x, this.getY() + this.getDeltaMovement().y, this.getZ() + this.getDeltaMovement().z);
        if (this.projectileGravity() != 0.0) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, this.projectileGravity(), 0.0));
        }
        if (!this.level().isClientSide() && this.tickCount >= this.getLife()) {
            if (this.isAlive()) {
                this.onExpired();
            }
            this.discard();
        }
    }

    // Per-tick effects such as trailing particles.
    protected void onProjectileTick() {
        if (this.level() instanceof ServerLevel level && this.isUnderWater()) {
            GunNetwork.particlesToAll(level, ParticleTypes.BUBBLE, this.getX() - this.getDeltaMovement().x, this.getY() - this.getDeltaMovement().y, this.getZ() - this.getDeltaMovement().z, 2, 0.1, 0.1, 0.1, 0.0);
        }
    }

    // Effects when the projectile is removed, such as an explosion flash.
    protected void impactEffect() {
    }

    // Called when the projectile runs out of life without hitting anything.
    protected void onExpired() {
    }

    private EntityHit findEntityOnPath(Vec3 start, Vec3 end) {
        EntityHit closest = null;
        double closestDistance = Double.MAX_VALUE;
        AABB area = this.getBoundingBox().expandTowards(this.getDeltaMovement());
        for (Entity entity : this.level().getEntities(this, area.inflate(1.0), TARGETS)) {
            if (entity.equals(this.shooter)) {
                continue;
            }
            EntityHit hit = this.hitResult(entity, start, end);
            if (hit != null) {
                double distance = start.distanceTo(hit.position());
                if (distance < closestDistance) {
                    closest = hit;
                    closestDistance = distance;
                }
            }
        }
        for (Entity entity : this.level().getEntities(this, area.inflate(3.0), TARGETS)) {
            if (entity != this.shooter && entity instanceof Player) {
                this.playPassBySound();
            }
        }
        return closest;
    }

    private List<EntityHit> findEntitiesOnPath(Vec3 start, Vec3 end) {
        List<EntityHit> hits = new ArrayList<>();
        AABB area = this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0);
        for (Entity entity : this.level().getEntities(this, area, TARGETS)) {
            if (!entity.equals(this.shooter)) {
                EntityHit hit = this.hitResult(entity, start, end);
                if (hit != null) {
                    hits.add(hit);
                }
            }
        }
        return hits;
    }

    private void playPassBySound() {
        if (this.shooter instanceof Player player) {
            this.level().playSound(player, this, GunSounds.BULLET_CLOSE, SoundSource.PLAYERS, 0.7F, 0.0F);
        } else {
            this.playSound(GunSounds.BULLET_CLOSE, 0.7F, 1.0F);
        }
    }

    // Where the path crosses the entity, grown slightly so near misses still count, and whether it crossed the head.
    private EntityHit hitResult(Entity entity, Vec3 start, Vec3 end) {
        double grow = ArsenalConfig.get().growBoundingBoxAmount;
        double expandHeight = entity instanceof Player && !entity.isCrouching() ? 0.0625 : 0.0;
        AABB box = entity.getBoundingBox().expandTowards(0.0, expandHeight, 0.0);
        Vec3 hitPos = box.clip(start, end).orElse(null);
        Vec3 grownHitPos = box.inflate(grow, 0.0, grow).clip(start, end).orElse(null);
        if (hitPos == null && grownHitPos != null) {
            BlockHitResult blocked = clipBlocks(this.level(), new ClipContext(start, grownHitPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (blocked.getType() == HitResult.Type.BLOCK) {
                return null;
            }
            hitPos = grownHitPos;
        }
        boolean headshot = false;
        if (ArsenalConfig.get().enableHeadShots && entity instanceof LivingEntity living) {
            HeadshotBoxes.HeadshotBox headBox = HeadshotBoxes.get(entity.getType());
            AABB head = headBox == null ? null : headBox.box(living);
            if (head != null) {
                head = head.move(box.getCenter().x, box.minY, box.getCenter().z);
                Vec3 headHit = head.clip(start, end).orElse(null);
                if (headHit == null) {
                    headHit = head.inflate(grow, 0.0, grow).clip(start, end).orElse(null);
                }
                if (headHit != null && (hitPos == null || headHit.distanceTo(hitPos) < 0.5)) {
                    hitPos = headHit;
                    headshot = true;
                }
            }
            if (headshot) {
                ItemStack helmet = living.getItemBySlot(EquipmentSlot.HEAD);
                if (!helmet.isEmpty()) {
                    this.helmetHit(living, helmet);
                }
            }
        }
        return hitPos == null ? null : new EntityHit(entity, hitPos, headshot);
    }

    // A headshot on a helmeted mob may knock the helmet off and blind it briefly, wearing the helmet down to its last point.
    private void helmetHit(LivingEntity living, ItemStack helmet) {
        if (living instanceof Player || !ArsenalConfig.get().mobsDropHelmets) {
            return;
        }
        boolean veryHeavyRound = this.advantage().equals("very_heavy");
        if (living.getType().is(GunTags.VERY_HEAVY) || !(this.random.nextFloat() < 0.4F || veryHeavyRound)) {
            return;
        }
        if (this.random.nextBoolean() || veryHeavyRound) {
            this.removeHelmet(living, helmet);
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
        }
        int durabilityLeft = helmet.getMaxDamage() - helmet.getDamageValue();
        if (durabilityLeft <= 1) {
            living.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        } else if (helmet.isDamageableItem()) {
            helmet.setDamageValue(helmet.getMaxDamage() - 1);
        }
    }

    // Knocks a helmet off, sending it flying, unless it is cursed with binding.
    private void removeHelmet(LivingEntity living, ItemStack helmet) {
        if (this.ignoreEntity(living) || helmet.isEmpty() || hasBindingCurse(helmet)) {
            return;
        }
        living.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        this.level().playSound(null, living.getOnPos(), GunSounds.HEADSHOT, SoundSource.PLAYERS, 1.0F, 1.0F);
        ItemEntity flying = new ItemEntity(this.level(), living.getX(), living.getEyeY() - 0.5, living.getZ(), helmet.copy());
        flying.setDeltaMovement((this.random.nextDouble() - 0.5) * 0.3, 0.5 + this.random.nextDouble() * 0.2, (this.random.nextDouble() - 0.5) * 0.3);
        this.level().addFreshEntity(flying);
    }

    // Knocks a player's helmet off into their own inventory, leaving it worn when there is no room or it is cursed with binding.
    private void stowHelmet(Player player, ItemStack helmet) {
        if (helmet.isEmpty() || hasBindingCurse(helmet)) {
            return;
        }
        ItemStack stowed = helmet.copy();
        if (!player.getInventory().add(stowed)) {
            return;
        }
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        this.level().playSound(null, player.getOnPos(), GunSounds.HEADSHOT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static boolean hasBindingCurse(ItemStack stack) {
        return stack.getEnchantments().keySet().stream().anyMatch(holder -> holder.is(Enchantments.BINDING_CURSE));
    }

    private void onHitEntityResult(ServerLevel level, EntityHit hit, Vec3 start, Vec3 end) {
        Entity entity = hit.entity();
        if (entity.getId() == this.shooterId || entity.hasPassenger(this.shooter)) {
            return;
        }
        if (this.shooter instanceof Player player && entity.hasIndirectPassenger(player)) {
            return;
        }
        if (this.ignoreEntity(entity)) {
            return;
        }
        this.onHitEntity(level, entity, hit.position(), start, end, hit.headshot());
        if (!this.ignoreEntity(entity) && !(this instanceof RocketEntity) && !this.gun.projectile().collateral()) {
            this.discard();
        }
        if (this.shooter instanceof Player) {
            entity.invulnerableTime = 0;
        }
    }

    private void onHitBlockResult(ServerLevel level, BlockHitResult result) {
        if (result.getType() == HitResult.Type.MISS || this.gun.projectile().ignoresBlocks()) {
            return;
        }
        BlockPos pos = result.getBlockPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        Vec3 hitVec = result.getLocation();
        if (this.shooter instanceof Player) {
            this.breakBlocks(level, pos, state);
        }
        if (!state.canBeReplaced()) {
            this.discard();
        }
        this.onHitBlock(level, state, pos, result.getDirection(), hitVec);
        if (block instanceof BellBlock bell) {
            bell.attemptToRing(level, pos, result.getDirection());
        }
    }

    // Glass shatters; wood splinters under heavy or burning rounds.
    private void breakBlocks(ServerLevel level, BlockPos pos, BlockState state) {
        if (!level.getGameRules().get(GunGameRules.GUN_GRIEFING)) {
            return;
        }
        ArsenalConfig config = ArsenalConfig.get();
        if (config.enableGlassBreaking && state.is(GunTags.FRAGILE)) {
            float destroySpeed = state.getDestroySpeed(level, pos);
            if (destroySpeed >= 0.0F && this.random.nextFloat() < config.fragileBaseBreakChance / (destroySpeed + 1.0F)) {
                if (level.getGameRules().get(GameRules.BLOCK_DROPS)) {
                    this.dropFragileExtras(level, pos, state);
                }
                boolean pumpkin = state.is(Blocks.PUMPKIN) || state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN);
                level.destroyBlock(pos, !pumpkin && config.fragileBlockDrops);
                if (state.is(Blocks.ICE)) {
                    this.meltIce(level, pos);
                }
            }
        }
        String advantage = this.advantage();
        boolean heavyRound = config.gunAdvantage ? advantage.equals("heavy") || advantage.equals("very_heavy") : true;
        boolean fireRound = this instanceof BlazeProjectileEntity;
        boolean wood = state.is(BlockTags.MINEABLE_WITH_AXE) && state.is(BlockTags.DRAGON_IMMUNE) || state.is(GunTags.WOOD);
        if (config.enableWoodBreaking && wood && (fireRound || heavyRound)) {
            float destroySpeed = state.getDestroySpeed(level, pos);
            if (destroySpeed >= 0.0F && this.random.nextFloat() < config.woodBaseBreakChance / (destroySpeed + 1.0F)) {
                level.destroyBlock(pos, false);
            }
        }
    }

    private void dropFragileExtras(ServerLevel level, BlockPos pos, BlockState state) {
        Vec3 center = pos.getCenter();
        if (state.is(Blocks.MELON)) {
            this.drop(level, center, new ItemStack(Items.MELON_SLICE, this.random.nextInt(6) + 1));
            this.drop(level, center, new ItemStack(Items.MELON_SEEDS, this.random.nextInt(2) + 1));
        }
        if (state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN)) {
            this.drop(level, center, new ItemStack(Items.PUMPKIN_SEEDS, this.random.nextInt(6) + 1));
        }
        if (state.is(Blocks.PUMPKIN)) {
            this.drop(level, center, new ItemStack(Items.PUMPKIN_SEEDS, this.random.nextInt(2) + 1));
            this.drop(level, center, new ItemStack(Items.CARVED_PUMPKIN));
        }
        if (state.is(Blocks.COCOA)) {
            this.drop(level, center, new ItemStack(Items.COCOA_BEANS, this.random.nextInt(3) + 1));
        }
        if (state.is(Blocks.BEEHIVE) || state.is(Blocks.BEE_NEST)) {
            this.drop(level, center, new ItemStack(Items.HONEYCOMB, this.random.nextInt(3) + 1));
        }
    }

    private void drop(ServerLevel level, Vec3 position, ItemStack stack) {
        level.addFreshEntity(new ItemEntity(level, position.x, position.y, position.z, stack));
    }

    // Shot ice over water, or packed among other ice, turns back into water.
    private void meltIce(ServerLevel level, BlockPos pos) {
        boolean water = level.getBlockState(pos.below()).is(Blocks.WATER);
        if (!water) {
            int neighbours = 0;
            for (Direction direction : Direction.values()) {
                if (level.getBlockState(pos.relative(direction)).is(Blocks.ICE)) {
                    neighbours++;
                }
            }
            water = neighbours > 2;
        }
        if (water) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 0);
        }
    }

    // Bullet hole, sparks and impact sound for nearby players, plus any round-specific block effects.
    protected void onHitBlock(ServerLevel level, BlockState state, BlockPos pos, Direction face, Vec3 hitVec) {
        GunNetwork.sendTrackingChunk(level, pos, new ProjectileHitPayloads.Block(hitVec, pos, face));
        if (state.getBlock() instanceof FallingBlock) {
            level.updateNeighborsAt(pos, state.getBlock());
            level.scheduleTick(pos, state.getBlock(), 1);
        }
    }

    // Sets fire on the face a burning round struck, when fire spreading is allowed.
    protected void igniteFace(ServerLevel level, BlockPos pos, Direction face, Vec3 hitVec) {
        if (!ArsenalConfig.get().setFireToBlocks || !level.getGameRules().get(GunGameRules.GUN_GRIEFING)) {
            return;
        }
        BlockPos firePos = pos.relative(face);
        if (level.getRandom().nextFloat() > 0.5F && BaseFireBlock.canBePlacedAt(level, firePos, face)) {
            level.setBlock(firePos, BaseFireBlock.getState(level, firePos), 11);
            level.sendParticles(ParticleTypes.LAVA, hitVec.x - 1.0 + this.random.nextDouble() * 2.0, hitVec.y, hitVec.z - 1.0 + this.random.nextDouble() * 2.0, 4, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public String advantage() {
        if (!ArsenalConfig.get().gunAdvantage || this.gun == null) {
            return "none";
        }
        return this.gun.projectile().advantage().getPath();
    }

    // Damage scaling from the round's advantage against light, heavy, very heavy and undead targets.
    protected float advantageMultiplier(Entity entity) {
        String advantage = this.advantage();
        float multiplier = 1.0F;
        if (advantage.equals("none")) {
            return multiplier;
        }
        boolean heavyRound = advantage.equals("heavy") || advantage.equals("very_heavy");
        if (entity.getType().is(GunTags.HEAVY)) {
            multiplier = heavyRound ? 1.25F : 0.5F;
        } else if (entity.getType().is(GunTags.VERY_HEAVY)) {
            multiplier = advantage.equals("very_heavy") ? 1.0F : advantage.equals("heavy") ? 0.5F : 0.25F;
        }
        if (advantage.equals("undead")) {
            if (isUndead(entity) || entity.getType().is(GunTags.GHOST)) {
                multiplier = 1.25F;
                entity.igniteForSeconds(2.0F);
            } else {
                multiplier = 0.75F;
            }
        }
        return multiplier;
    }

    // Targets a projectile passes through: the dying, ghosts hit by anything but spectral rounds, and a mob shooter's own kind most of the time.
    protected boolean ignoreEntity(Entity entity) {
        if (entity instanceof LivingEntity living && living.isDeadOrDying()) {
            return true;
        }
        if (entity.getType().is(GunTags.GHOST) && !this.advantage().equals("undead")) {
            return !(this instanceof SpectreProjectileEntity);
        }
        return this.shooter instanceof PathfinderMob mob
            && mob.getType() == entity.getType()
            && this.level().getRandom().nextFloat() < 0.9F;
    }

    protected static boolean isUndead(Entity entity) {
        return entity.getType().is(EntityTypeTags.UNDEAD) || entity.getType().is(GunTags.UNDEAD);
    }

    // Deals the round's damage, scaled by advantage and headshots, and reports the hit to the shooter.
    protected void onHitEntity(ServerLevel level, Entity entity, Vec3 hitVec, Vec3 start, Vec3 end, boolean headshot) {
        if (this.ignoreEntity(entity)) {
            return;
        }
        DamageSource source = new BulletDamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(BULLET), this, this.shooter);
        if (entity instanceof EnderMan) {
            source = level.damageSources().mobProjectile(this, this.shooter);
        }
        float damage = this.getDamage();
        if (ArsenalConfig.get().gunAdvantage) {
            damage *= this.advantageMultiplier(entity);
        }
        if (headshot) {
            damage *= this.gun.projectile().headshotMultiplier();
        }
        if (!(this.shooter instanceof Player)) {
            damage /= this.shooter.level().getDifficulty() != Difficulty.HARD ? 2.0F : 1.5F;
        }
        if (headshot && entity instanceof Player player && ArsenalConfig.get().playersDropHelmets) {
            damage = this.turtleHelmetHeadshot(level, player, damage);
        }
        entity.hurtServer(level, source, damage);
        if (!ArsenalConfig.get().enableKnockback) {
            entity.setDeltaMovement(0.0, 0.0, 0.0);
        }
        this.applyPotionEffect(entity);
        if (entity instanceof LivingEntity) {
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, entity.getX(), entity.getY(), entity.getZ(), (int) damage / 2, entity.getBbWidth() / 2.0F, entity.getBbHeight() / 2.0F, entity.getBbWidth() / 2.0F, 0.1);
        }
        if (this.shooter instanceof ServerPlayer player && entity instanceof LivingEntity && !(entity instanceof EnderMan)) {
            ServerPlayNetworking.send(player, new ProjectileHitPayloads.Entity(hitVec, headshot ? 1 : 0, entity instanceof Player));
        }
    }

    // A very heavy round to a turtle helmet wears it to its last point and, if it would kill a healthy player, leaves them on half a heart.
    private float turtleHelmetHeadshot(ServerLevel level, Player player, float damage) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.is(Items.TURTLE_HELMET) || !this.advantage().equals("very_heavy")) {
            return damage;
        }
        int durabilityLeft = helmet.getMaxDamage() - helmet.getDamageValue();
        if (durabilityLeft <= 1) {
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        } else {
            helmet.setDamageValue(helmet.getMaxDamage() - 1);
        }
        if (this.gun.projectile().damage() > player.getHealth() && player.getHealth() > 10.0F) {
            if (level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
                this.stowHelmet(player, helmet);
            } else {
                this.removeHelmet(player, helmet);
            }
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
            return player.getHealth() - 0.5F;
        }
        return damage;
    }

    private void applyPotionEffect(Entity entity) {
        Gun.PotionEffect effect = this.gun.potionEffect();
        if (effect.effect() == null || effect.selfApplied() || !(entity instanceof LivingEntity living)) {
            return;
        }
        Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.get(effect.effect()).orElse(null);
        if (holder != null) {
            living.addEffect(new MobEffectInstance(holder, effect.duration(), effect.strength()));
        }
    }

    // The round's damage split across its pellets, fading over its life when the gun says so.
    public float getDamage() {
        Gun.Projectile projectile = this.gun.projectile();
        float damage = projectile.damage();
        if (projectile.damageReduceOverLife()) {
            float modifier = ((float) projectile.life() - (this.tickCount - 1)) / projectile.life();
            damage *= Math.min(modifier, 1.0F);
        }
        damage /= this.gun.general().projectileAmount();
        return Math.max(0.0F, damage);
    }

    public void updateHeading() {
        Vec3 motion = this.getDeltaMovement();
        this.setYRot((float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG));
        this.setXRot((float) (Mth.atan2(motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    private static Vec3 rotationVector(float pitch, float yaw) {
        float cosYaw = Mth.cos(-yaw * Mth.DEG_TO_RAD - Mth.PI);
        float sinYaw = Mth.sin(-yaw * Mth.DEG_TO_RAD - Mth.PI);
        float cosPitch = -Mth.cos(-pitch * Mth.DEG_TO_RAD);
        float sinPitch = Mth.sin(-pitch * Mth.DEG_TO_RAD);
        return new Vec3(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
    }

    // A block raytrace that passes through leaves.
    protected static BlockHitResult clipBlocks(Level level, ClipContext context) {
        boolean ignoreLeaves = ArsenalConfig.get().ignoreLeaves;
        return BlockGetter.traverseBlocks(context.getFrom(), context.getTo(), context, (clip, pos) -> {
            BlockState state = level.getBlockState(pos);
            if (ignoreLeaves && state.getBlock() instanceof LeavesBlock) {
                return null;
            }
            VoxelShape shape = clip.getBlockShape(state, level, pos);
            return level.clipWithInteractionOverride(clip.getFrom(), clip.getTo(), pos, shape, state);
        }, clip -> {
            Vec3 delta = clip.getFrom().subtract(clip.getTo());
            return BlockHitResult.miss(clip.getTo(), Direction.getApproximateNearest(delta.x, delta.y, delta.z), BlockPos.containing(clip.getTo()));
        });
    }

    @Override
    public void remove(RemovalReason reason) {
        if (this.level() instanceof ServerLevel level) {
            this.impactEffect();
            GunNetwork.sendNear(level, this.position(), 256.0, new ProjectileHitPayloads.Remove(this.getId()));
        }
        super.remove(reason);
    }

    @Override
    public void onClientRemoval() {
        this.impactEffect();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    // One entity a projectile's path crosses, where, and whether at the head.
    protected record EntityHit(Entity entity, Vec3 position, boolean headshot) {
    }
}
