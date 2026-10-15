package com.kltyton.darwin_soldier.client.battle;

import com.kltyton.darwin_soldier.network.BattleInstinctVisualPacket;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class BattleInstinctClientVisuals {
    // TODO: Add the optional five-tick player afterimage without creating a client entity.
    private static final DustParticleOptions WHITE_TRAIL =
            new DustParticleOptions(new Vector3f(0.95F, 0.95F, 0.95F), 0.9F);
    private static final DustParticleOptions RED_TRAIL =
            new DustParticleOptions(new Vector3f(0.9F, 0.08F, 0.06F), 0.75F);
    private static final Map<Integer, VisualState> ACTIVE = new HashMap<>();

    private BattleInstinctClientVisuals() {
    }

    public static void start(BattleInstinctVisualPacket packet) {
        ACTIVE.put(packet.playerId(), new VisualState(packet.start(), packet.end(),
                packet.blockPoseTicks(), packet.trailTicks()));
        RuntimeDiagnostics.info("battle_visual_start", "playerId=" + packet.playerId()
                + " activeVisuals=" + ACTIVE.size() + " distance=" + packet.start().distanceTo(packet.end())
                + " blockPoseTicks=" + packet.blockPoseTicks() + " trailTicks=" + packet.trailTicks());
    }

    public static void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null) {
            ACTIVE.clear();
            return;
        }

        Iterator<Map.Entry<Integer, VisualState>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, VisualState> entry = iterator.next();
            Entity entity = level.getEntity(entry.getKey());
            VisualState state = entry.getValue();
            if (!(entity instanceof AbstractClientPlayer player)
                    || state.age >= Math.max(state.trailTicks, state.blockPoseTicks)) {
                RuntimeDiagnostics.info("battle_visual_end", "playerId=" + entry.getKey()
                        + " entityPresent=" + (entity != null) + " age=" + state.age
                        + " duration=" + Math.max(state.trailTicks, state.blockPoseTicks));
                iterator.remove();
                continue;
            }

            clearHitFeedback(player);
            if (state.age < state.trailTicks) {
                spawnTrail(level, state);
            }
            state.age++;
        }
    }

    public static boolean isBlocking(AbstractClientPlayer player) {
        VisualState state = ACTIVE.get(player.getId());
        return state != null && state.age < state.blockPoseTicks;
    }

    private static void clearHitFeedback(AbstractClientPlayer player) {
        player.hurtTime = 0;
        player.hurtDuration = 0;
        player.setArrowCount(0);
    }

    private static void spawnTrail(ClientLevel level, VisualState state) {
        Vec3 delta = state.end.subtract(state.start);
        for (int sample = 0; sample < 4; sample++) {
            double progress = (state.age * 4.0D + sample + 0.5D) / (state.trailTicks * 4.0D);
            Vec3 point = state.start.add(delta.scale(progress)).add(
                    (level.random.nextDouble() - 0.5D) * 0.18D,
                    0.45D + level.random.nextDouble() * 0.8D,
                    (level.random.nextDouble() - 0.5D) * 0.18D);
            level.addParticle(WHITE_TRAIL, point.x, point.y, point.z, 0.0D, 0.0D, 0.0D);
            if (sample == 1) {
                level.addParticle(RED_TRAIL, point.x, point.y, point.z, 0.0D, 0.0D, 0.0D);
            }
            if (sample == 3 && state.age % 2 == 0) {
                level.addParticle(ParticleTypes.SMOKE, point.x, point.y, point.z, 0.0D, 0.015D, 0.0D);
            }
        }
    }

    private static final class VisualState {
        private final Vec3 start;
        private final Vec3 end;
        private final int blockPoseTicks;
        private final int trailTicks;
        private int age;

        private VisualState(Vec3 start, Vec3 end, int blockPoseTicks, int trailTicks) {
            this.start = start;
            this.end = end;
            this.blockPoseTicks = Math.max(0, blockPoseTicks);
            this.trailTicks = Math.max(1, trailTicks);
        }
    }
}
