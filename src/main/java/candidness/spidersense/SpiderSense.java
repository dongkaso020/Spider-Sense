package candidness.spidersense;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpiderSense implements ModInitializer {
    private static final float SOUND_RANGE = 10.0F;
    private final Map<PlayerEntity, Long> playersWithSounds = new HashMap<>();
    private final Set<Entity> detectedEntities = new HashSet<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            server.getWorlds().forEach(world -> {
                world.getPlayers().forEach(player -> {
                    Vec3d playerPos = player.getPos();
                    boolean soundPlayed = false;
                    Set<Entity> currentDetectedEntities = new HashSet<>();
                    for (Entity nearbyEntity : world.getOtherEntities(player, player.getBoundingBox().expand(SOUND_RANGE))) {
                        if ((nearbyEntity instanceof ProjectileEntity || nearbyEntity instanceof MobEntity) &&
                                !(nearbyEntity instanceof AnimalEntity) &&
                                shouldPlaySound(player) && !detectedEntities.contains(nearbyEntity)) {
                            Vec3d entitySoundPos = nearbyEntity.getPos().add(0, nearbyEntity.getStandingEyeHeight() / 2, 0);
                            world.playSound(null, entitySoundPos.getX(), entitySoundPos.getY(), entitySoundPos.getZ(), SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.MASTER, 1F, 1F);
                            soundPlayed = true;
                            playersWithSounds.put(player, System.currentTimeMillis());
                        }
                        currentDetectedEntities.add(nearbyEntity);
                        if (soundPlayed) {
                            break;
                        }
                    }
                    detectedEntities.clear();
                    detectedEntities.addAll(currentDetectedEntities);
                });
            });
        });
    }

    private boolean shouldPlaySound(PlayerEntity player) {
        if (!playersWithSounds.containsKey(player)) {
            return true;
        }
        long lastPlayedTime = playersWithSounds.get(player);
        return System.currentTimeMillis() - lastPlayedTime > 1000;
    }
}