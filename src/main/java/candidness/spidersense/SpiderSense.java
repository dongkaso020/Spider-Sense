package candidness.spidersense;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpiderSense implements ModInitializer {
    private static final float SOUND_RANGE_ENTITY = 8.0F;
    private static final float SOUND_RANGE_ARROW = 30.0F;
    private static final int ENTITY_DETECTION_INTERVAL_MS = 20000;
    private final Map<PlayerEntity, Set<Entity>> detectedEntitiesByPlayer = new HashMap<>();
    private final Map<Entity, Long> sensedEntitiesWithTimestamps = new HashMap<>();

    public static final Identifier MY_SOUND_ID = new Identifier("spider-sense:my_sound");
    public static SoundEvent MY_SOUND_EVENT = SoundEvent.of(MY_SOUND_ID);

    @Override
    public void onInitialize() {
        Registry.register(Registries.SOUND_EVENT, SpiderSense.MY_SOUND_ID, MY_SOUND_EVENT);
        registerClientTickEvent();
    }

    private void registerClientTickEvent() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null && client.player != null) {
                processPlayerSensingEvents(client.player);
            }
        });
    }

    private void processPlayerSensingEvents(PlayerEntity player) {
        Set<Entity> currentDetectedEntities = findNearbyEntities(player);
        playSoundForDetectedEntities(player, currentDetectedEntities);

        detectedEntitiesByPlayer.put(player, currentDetectedEntities);
    }

    private Set<Entity> findNearbyEntities(PlayerEntity player) {
        Set<Entity> currentDetectedEntities = new HashSet<>(player.getWorld().getOtherEntities(player, player.getBoundingBox().expand(SOUND_RANGE_ENTITY)));
        player.getWorld().getOtherEntities(player, player.getBoundingBox().expand(SOUND_RANGE_ARROW)).stream().filter(entity -> entity instanceof ArrowEntity).forEach(currentDetectedEntities::add);
        return currentDetectedEntities;
    }

    private void playSoundForDetectedEntities(PlayerEntity player, Set<Entity> currentDetectedEntities) {
        for (Entity nearbyEntity : currentDetectedEntities) {
            if (shouldSenseEntity(player, nearbyEntity)) {
                playSoundAtEntityPosition(player, nearbyEntity);
                sensedEntitiesWithTimestamps.put(nearbyEntity, System.currentTimeMillis());
            }
        }
    }

    private boolean shouldSenseEntity(PlayerEntity player, Entity nearbyEntity) {
        if (nearbyEntity instanceof ArrowEntity arrow) {
            return !arrow.isOnGround() && isTimeForNextSound(nearbyEntity) && isNotYetDetectedByPlayer(player, arrow);
        } else {
            return (nearbyEntity instanceof MobEntity || nearbyEntity instanceof PlayerEntity) &&
                    !(nearbyEntity instanceof AnimalEntity || nearbyEntity instanceof BatEntity || nearbyEntity instanceof VillagerEntity || nearbyEntity instanceof FishEntity || nearbyEntity instanceof SquidEntity) &&
                    isTimeForNextSound(nearbyEntity) && isNotYetDetectedByPlayer(player, nearbyEntity);
        }
    }

    private void playSoundAtEntityPosition(PlayerEntity player, Entity nearbyEntity) {
        Vec3d entitySoundPos = nearbyEntity.getPos().add(0, nearbyEntity.getStandingEyeHeight() / 2, 0);
        SoundEvent soundEvent = nearbyEntity instanceof ArrowEntity ? SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP : SpiderSense.MY_SOUND_EVENT;
        player.getWorld().playSound(entitySoundPos.getX(), entitySoundPos.getY(), entitySoundPos.getZ(), soundEvent, SoundCategory.MASTER, 1F, 1F, false);
    }

    private boolean isTimeForNextSound(Entity entity) {
        return !sensedEntitiesWithTimestamps.containsKey(entity) || System.currentTimeMillis() - sensedEntitiesWithTimestamps.get(entity) > ENTITY_DETECTION_INTERVAL_MS;
    }

    private boolean isNotYetDetectedByPlayer(PlayerEntity player, Entity nearbyEntity) {
        return !detectedEntitiesByPlayer.get(player).contains(nearbyEntity);
    }
}