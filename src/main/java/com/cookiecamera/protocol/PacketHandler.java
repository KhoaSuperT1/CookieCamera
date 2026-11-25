package com.cookiecamera.protocol;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCamera;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMoveAndRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

public final class PacketHandler {
  private static final Map<UUID, List<ArmorStand>> markerEntities = new ConcurrentHashMap<>();
  private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(100000);
  private static final Map<UUID, GameMode> savedGameModes = new ConcurrentHashMap<>();
  private static final Map<UUID, Integer> cameraEntityIds = new ConcurrentHashMap<>();
  private static final Map<UUID, long[]> encodedPositions = new ConcurrentHashMap<>();
  private static final String CAMERA_MARKER_TEXTURE =
      "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWE3ZDJhN2ZiYjRkMzdiNGQ1M2ZlODc3NTcxMjhlNWVmNjZlYzIzZDdmZjRmZTk5NDQ1NDZkYmM4Y2U3NzcifX19";

  private PacketHandler() {}

  private static int nextEntityId() {
    return ENTITY_ID_COUNTER.getAndIncrement();
  }

  private static Object channel(Player player) {
    PacketEventsAPI<?> api = PacketEvents.getAPI();
    return api != null && player != null ? api.getPlayerManager().getChannel(player) : null;
  }

  private static void send(Player player, PacketWrapper<?> packet) {
    try {
      PacketEventsAPI<?> api = PacketEvents.getAPI();
      if (api == null || player == null) {
        return;
      }

      Object ch = api.getPlayerManager().getChannel(player);
      if (ch != null) {
        api.getProtocolManager().sendPacket(ch, packet);
      }
    } catch (Exception exception) {
      Bukkit.getLogger().warning("[CookieCamera] Packet send failed: " + exception.getMessage());
    }
  }

  private static long enc(double v) {
    return Math.round(v * 4096.0);
  }

  public static int startCamera(Player player, Location loc) {
    UUID uid = player.getUniqueId();
    savedGameModes.put(uid, player.getGameMode());
    player.setGameMode(GameMode.SPECTATOR);
    int eid = nextEntityId();
    cameraEntityIds.put(uid, eid);
    encodedPositions.put(uid, new long[] {enc(loc.getX()), enc(loc.getY()), enc(loc.getZ())});
    com.github.retrooper.packetevents.protocol.world.Location peLoc =
        new com.github.retrooper.packetevents.protocol.world.Location(
            loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    send(
        player,
        new WrapperPlayServerSpawnEntity(
            eid, UUID.randomUUID(), EntityTypes.ARMOR_STAND, peLoc, loc.getYaw(), 0, null));
    List<EntityData<?>> meta = new ArrayList<>();
    meta.add(new EntityData<>(0, EntityDataTypes.BYTE, (byte) 32));
    meta.add(new EntityData<>(5, EntityDataTypes.BOOLEAN, true));
    meta.add(new EntityData<>(15, EntityDataTypes.BYTE, (byte) 1));
    send(player, new WrapperPlayServerEntityMetadata(eid, meta));
    send(player, new WrapperPlayServerCamera(eid));
    return eid;
  }

  public static void moveCamera(
      Player player, double x, double y, double z, float yaw, float pitch) {
    UUID uid = player.getUniqueId();
    Integer eid = cameraEntityIds.get(uid);
    if (eid != null) {
      long[] last = encodedPositions.get(uid);
      if (last != null) {
        long nx = enc(x);
        long ny = enc(y);
        long nz = enc(z);
        long dx = nx - last[0];
        long dy = ny - last[1];
        long dz = nz - last[2];
        if (Math.abs(dx) > 32760 || Math.abs(dy) > 32760 || Math.abs(dz) > 32760) {
          send(
              player,
              new WrapperPlayServerEntityTeleport(
                  eid,
                  new com.github.retrooper.packetevents.protocol.world.Location(
                      x, y, z, yaw, pitch),
                  false));
          send(player, new WrapperPlayServerEntityHeadLook(eid, yaw));
          encodedPositions.put(uid, new long[] {nx, ny, nz});
        } else {
          send(
              player,
              new WrapperPlayServerEntityRelativeMoveAndRotation(
                  eid, dx / 4096.0, dy / 4096.0, dz / 4096.0, yaw, pitch, false));
          send(player, new WrapperPlayServerEntityHeadLook(eid, yaw));
          encodedPositions.put(uid, new long[] {last[0] + dx, last[1] + dy, last[2] + dz});
        }
      }
    }
  }

  public static void stopCamera(Player player) {
    UUID uid = player.getUniqueId();
    send(player, new WrapperPlayServerCamera(player.getEntityId()));
    Integer eid = cameraEntityIds.remove(uid);
    encodedPositions.remove(uid);
    if (eid != null) {
      send(player, new WrapperPlayServerDestroyEntities(eid));
    }

    GameMode original = savedGameModes.remove(uid);
    if (original != null) {
      player.setGameMode(original);
    }
  }

  public static boolean isInCameraMode(Player player) {
    return cameraEntityIds.containsKey(player.getUniqueId());
  }

  public static int spawnMarker(Player viewer, Location loc, String label) {
    return spawnMarker(
        viewer,
        loc,
        label,
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWE3ZDJhN2ZiYjRkMzdiNGQ1M2ZlODc3NTcxMjhlNWVmNjZlYzIzZDdmZjRmZTk5NDQ1NDZkYmM4Y2U3NzcifX19");
  }

  public static int spawnMarker(Player viewer, Location loc, String label, String base64Texture) {
    World w = loc.getWorld();
    if (w == null) {
      return -1;
    } else {
      ArmorStand stand =
          w.spawn(
              loc,
              ArmorStand.class,
              as -> {
                as.setSmall(true);
                as.setGravity(false);
                as.setVisible(false);
                as.setMarker(false);
                as.setCustomNameVisible(true);
                as.customName(Component.text(label, NamedTextColor.GOLD));
                as.setInvulnerable(true);
                as.setCanPickupItems(false);
                as.setSilent(true);
                as.setPersistent(false);
                as.getEquipment().setHelmet(createSkull(base64Texture));
              });
      stand.addScoreboardTag("cookiecamera_marker");
      stand.addScoreboardTag("cookiecamera_owner_" + viewer.getUniqueId());
      markerEntities.computeIfAbsent(viewer.getUniqueId(), k -> new ArrayList<>()).add(stand);
      return stand.getEntityId();
    }
  }

  @SuppressWarnings("deprecation")
  private static ItemStack createSkull(String base64Texture) {
    ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
    if (base64Texture == null || base64Texture.isEmpty()) {
      return skull;
    }

    try {
      SkullMeta meta = (SkullMeta) skull.getItemMeta();
      URL skinUrl = decodeSkinUrl(base64Texture);
      if (skinUrl != null) {
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "cookiecam");
        PlayerTextures textures = profile.getTextures();
        textures.setSkin(skinUrl);
        profile.setTextures(textures);
        meta.setOwnerProfile(profile);
        skull.setItemMeta(meta);
      }
    } catch (Exception exception) {
      Bukkit.getLogger().warning("[CookieCamera] Skull texture failed: " + exception.getMessage());
    }

    return skull;
  }

  private static URL decodeSkinUrl(String base64Texture) {
    try {
      String json = new String(Base64.getDecoder().decode(base64Texture), StandardCharsets.UTF_8);
      String marker = "\"url\":\"";
      int start = json.indexOf(marker);
      if (start < 0) {
        return null;
      }

      start += marker.length();
      int end = json.indexOf('"', start);
      if (end < 0) {
        return null;
      }

      return URI.create(json.substring(start, end)).toURL();
    } catch (Exception exception) {
      return null;
    }
  }

  public static void destroyMarkers(Player viewer) {
    List<ArmorStand> markers = markerEntities.remove(viewer.getUniqueId());
    if (markers != null) {
      for (ArmorStand as : markers) {
        if (!as.isDead()) {
          as.remove();
        }
      }
    }
  }

  public static List<ArmorStand> getMarkers(Player viewer) {
    return markerEntities.getOrDefault(viewer.getUniqueId(), Collections.emptyList());
  }

  public static void destroyAll(Player viewer) {
    if (isInCameraMode(viewer)) {
      stopCamera(viewer);
    }

    destroyMarkers(viewer);
  }

  public static void preloadChunks(Player viewer, World world, double x, double z, int radius) {
    int cx = (int) Math.floor(x) >> 4;
    int cz = (int) Math.floor(z) >> 4;

    for (int i = cx - radius; i <= cx + radius; i++) {
      for (int j = cz - radius; j <= cz + radius; j++) {
        if (!world.isChunkLoaded(i, j)) {
          world.getChunkAtAsync(i, j);
        }
      }
    }
  }
}

