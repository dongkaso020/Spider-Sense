package candidness.spidersense;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpiderSense implements ModInitializer {
    private static final float SOUND_RANGE_ENTITY = 10.0F;
    private static final float SOUND_RANGE_ARROW = 30.0F;
    private final Map<PlayerEntity, Long> playersWithSounds = new HashMap<>();
    private final Set<Entity> detectedEntities = new HashSet<>();

    @Override
    public void onInitialize() {
        registerSoundEvents();
    }

    private void registerSoundEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null && client.player != null) {
                processPlayerSenseEvents(client.player);
            }
        });
    }

    private void processPlayerSenseEvents(PlayerEntity player) {
        Set<Entity> currentDetectedEntities = findNearbyEntities(player);
        boolean soundPlayed = playSoundForDetectedEntities(player, currentDetectedEntities);

        if (soundPlayed) {
            playersWithSounds.put(player, System.currentTimeMillis());
        }

        detectedEntities.clear();
        detectedEntities.addAll(currentDetectedEntities);
    }

    private Set<Entity> findNearbyEntities(PlayerEntity player) {
        Set<Entity> currentDetectedEntities = new HashSet<>(player.getWorld().getOtherEntities(player, player.getBoundingBox().expand(SOUND_RANGE_ENTITY)));
        player.getWorld().getOtherEntities(player, player.getBoundingBox().expand(SOUND_RANGE_ARROW)).stream().filter(entity -> entity instanceof ArrowEntity).forEach(currentDetectedEntities::add);
        return currentDetectedEntities;
    }

    private boolean playSoundForDetectedEntities(PlayerEntity player, Set<Entity> currentDetectedEntities) {
        boolean soundPlayed = false;
        for (Entity nearbyEntity : currentDetectedEntities) {
            if (isEntityToBeSensed(player, nearbyEntity)) {
                playSoundAtEntityPosition(player, nearbyEntity);
                soundPlayed = true;
                break;
            }
        }
        return soundPlayed;
    }

    private boolean isEntityToBeSensed(PlayerEntity player, Entity nearbyEntity) {
        if (nearbyEntity instanceof ArrowEntity arrow) {
            return !arrow.isOnGround() && shouldPlaySound(player) && !detectedEntities.contains(arrow);
        } else {
            return (nearbyEntity instanceof MobEntity || nearbyEntity instanceof PlayerEntity) &&
                    !(nearbyEntity instanceof AnimalEntity) &&
                    !(nearbyEntity instanceof BatEntity) &&
                    !(nearbyEntity instanceof VillagerEntity) &&
                    shouldPlaySound(player) &&
                    !detectedEntities.contains(nearbyEntity);
        }
    }

    private void playSoundAtEntityPosition(PlayerEntity player, Entity nearbyEntity) {
        Vec3d entitySoundPos = nearbyEntity.getPos().add(0, nearbyEntity.getStandingEyeHeight() / 2, 0);
        if (nearbyEntity instanceof ArrowEntity) {
            player.getWorld().playSound(entitySoundPos.getX(), entitySoundPos.getY(), entitySoundPos.getZ(),
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1F, 1F, false);
        } else {
            player.getWorld().playSound(entitySoundPos.getX(), entitySoundPos.getY(), entitySoundPos.getZ(),
                    SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.MASTER, 1F, 1F, true);
        }
    }

    private boolean shouldPlaySound(PlayerEntity player) {
        if (!playersWithSounds.containsKey(player)) {
            return true;
        }
        long lastPlayedTime = playersWithSounds.get(player);
        return System.currentTimeMillis() - lastPlayedTime > 0;
    }
}