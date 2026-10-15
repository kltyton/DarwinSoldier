package com.kltyton.darwin_soldier.client.aim.compat.tacz;

import com.kltyton.darwin_soldier.client.aim.ballistics.ProjectileLaunchState;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * TaCZ 1.1.8-hotfix（官方源码 commit b43eb84）弹道只读解析。
 * <p>
 * 只读取枪械数据与玩家配件属性缓存，不调用射击脚本、不改变枪械/玩家状态。
 * 速度公式与官方 {@code ModernKineticGunScriptAPI.shootOnce} 一致：
 * {@code cacheSpeed * AmmoConfig.GLOBAL_BULLET_SPEED_MODIFIER / 20}。
 */
public final class TaczBallisticsResolver {
    private static final double TACZ_WATER_DRAG = 1.0D - 0.4D;

    private TaczBallisticsResolver() {
    }

    public static ProjectileLaunchState resolve(LocalPlayer player, ItemStack stack, float partialTick) {
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) {
            return null;
        }
        ResourceLocation gunId = gun.getGunId(stack);
        GunData gunData = TimelessAPI.getClientGunIndex(gunId)
                .map(index -> index.getGunData())
                .orElse(null);
        if (gunData == null) {
            return null;
        }
        AttachmentCacheProperty cacheProperty = IGunOperator.fromLivingEntity(player).getCacheProperty();
        if (cacheProperty == null) {
            return null;
        }
        BulletData bulletData = gunData.getBulletData();
        if (bulletData == null) {
            return null;
        }

        // 配件修改后的弹速（m/s），来自玩家缓存
        float cacheSpeed = cacheProperty.<Float>getCache("ammo_speed");
        double speed = cacheSpeed * AmmoConfig.GLOBAL_BULLET_SPEED_MODIFIER.get() / 20.0D;

        // 当前姿态散布（度），来自配件缓存
        Map<InaccuracyType, Float> inaccuracyCache = cacheProperty.<Map<InaccuracyType, Float>>getCache("inaccuracy");
        double spreadDegrees = inaccuracyCache == null ? 0.0D
                : inaccuracyCache.getOrDefault(InaccuracyType.getInaccuracyType(player), 0.0F);

        Vec3 origin = interpolatedEyePosition(player);
        Vec3 inherited = inheritedVelocity(player);
        Vec3 velocity = direction(player).scale(speed).add(inherited);

        double gravity = bulletData.getGravity();
        double airDrag = 1.0D - bulletData.getFriction();

        return new ProjectileLaunchState("tacz_kinetic", origin, velocity, inherited,
                gravity, airDrag, TACZ_WATER_DRAG, false, spreadDegrees);
    }

    public static boolean isSupported(ItemStack stack) {
        return IGun.getIGunOrNull(stack) != null;
    }

    /**
     * 模仿 TaCZ {@code EntityKineticBullet} 构造时的初始位置：
     * 半 tick 位置插值（xOld 与 x 的中点）+ 眼睛高度。
     */
    private static Vec3 interpolatedEyePosition(LocalPlayer player) {
        double x = player.xOld + (player.getX() - player.xOld) / 2.0D;
        double y = player.yOld + (player.getY() - player.yOld) / 2.0D + player.getEyeHeight();
        double z = player.zOld + (player.getZ() - player.zOld) / 2.0D;
        return new Vec3(x, y, z);
    }

    /**
     * 与 TaCZ {@code shootFromRotation} 一致：水平方向全继承玩家速度，垂直方向仅离地时继承。
     */
    private static Vec3 inheritedVelocity(LocalPlayer player) {
        Vec3 movement = player.getDeltaMovement();
        return new Vec3(movement.x, player.onGround() ? 0.0D : movement.y, movement.z);
    }

    private static Vec3 direction(LocalPlayer player) {
        double pitch = player.getXRot() * (Math.PI / 180.0D);
        double yaw = player.getYRot() * (Math.PI / 180.0D);
        return new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch)).normalize();
    }
}
