package com.cookiecamera.manager;

import com.cookiecamera.CookieCamera;
import com.cookiecamera.math.CatmullRom;
import com.cookiecamera.model.CameraPoint;
import com.cookiecamera.model.EaseType;
import com.cookiecamera.model.Scene;
import com.cookiecamera.protocol.PacketHandler;
import com.cookiecamera.util.Scheduler;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EditSession {
  private static final MiniMessage MM = MiniMessage.miniMessage();
  private static final Map<UUID, EditSession> sessions = new ConcurrentHashMap<>();
  public static final String TOOL_ADD_POINT = "add-point";
  public static final String TOOL_TP_POINT = "tp-point";
  public static final String TOOL_PLAY = "play";
  public static final String TOOL_POINTS_LIST = "points-list";
  public static final String TOOL_REC_TOGGLE = "rec-toggle";
  public static final String TOOL_PATH_TOGGLE = "path-toggle";
  public static final String TOOL_EVENT_EDIT = "event-edit";
  public static final String TOOL_EXIT = "exit";
  private static final Map<String, Integer> DEFAULT_SLOTS =
      Map.of(
          "add-point",
          0,
          "tp-point",
          1,
          "play",
          2,
          "points-list",
          3,
          "rec-toggle",
          4,
          "path-toggle",
          5,
          "exit",
          8);
  private static final Map<String, Material> DEFAULT_MATERIALS =
      Map.of(
          "add-point",
          Material.STICK,
          "tp-point",
          Material.ENDER_EYE,
          "play",
          Material.GREEN_CANDLE,
          "points-list",
          Material.WRITABLE_BOOK,
          "rec-toggle",
          Material.REDSTONE,
          "path-toggle",
          Material.LANTERN,
          "exit",
          Material.BARRIER);
  private final Map<Integer, String> slotToTool = new HashMap<>();
  private final Player player;
  private final Scene scene;
  private final CookieCamera plugin;
  private ItemStack[] savedInventory;
  private Scheduler.TaskHandle particleTask;
  private Scheduler.TaskHandle actionBarTask;
  private boolean recording = false;
  private Scheduler.TaskHandle recordTask;
  private boolean pathVisible = true;
  private int lastTpIndex = -1;

  public EditSession(Player player, Scene scene, CookieCamera plugin) {
    this.player = player;
    this.scene = scene;
    this.plugin = plugin;
  }

  public static void startEditing(Player player, Scene scene, CookieCamera plugin) {
    EditSession existing = sessions.get(player.getUniqueId());
    if (existing != null) {
      existing.cleanup();
    }

    EditSession session = new EditSession(player, scene, plugin);
    sessions.put(player.getUniqueId(), session);
    session.saveInventory();
    session.giveTools();
    session.spawnMarkers();
    session.startVisuals();
    session.startActionBar();
  }

  public static EditSession getSession(Player player) {
    return sessions.get(player.getUniqueId());
  }

  public static void stopEditing(Player player) {
    EditSession session = sessions.remove(player.getUniqueId());
    if (session != null) {
      session.cleanup();
      session.restoreInventory();
    }
  }

  public Scene getScene() {
    return this.scene;
  }

  public Player getPlayer() {
    return this.player;
  }

  public boolean isRecording() {
    return this.recording;
  }

  public boolean isPathVisible() {
    return this.pathVisible;
  }

  public String getToolKeyForSlot(int slot) {
    return this.slotToTool.get(slot);
  }

  private void startActionBar() {
    this.actionBarTask =
        Scheduler.runEntityTimer(
            this.plugin,
            this.player,
            h -> {
              if (!this.player.isOnline()) {
                h.cancel();
              } else {
                LangManager lang = this.plugin.getLang();
                String key = this.recording ? "edit.actionbar-recording" : "edit.actionbar";
                Component msg =
                    lang.get(
                        key, "scene", this.scene.getId(), "points", this.scene.getPoints().size());
                this.player.sendActionBar(msg);
              }
            },
            10L,
            10L);
  }

  private void saveInventory() {
    this.savedInventory = this.player.getInventory().getContents().clone();

    for (int i = 0; i < this.savedInventory.length; i++) {
      if (this.savedInventory[i] != null) {
        this.savedInventory[i] = this.savedInventory[i].clone();
      }
    }
  }

  private void restoreInventory() {
    if (this.savedInventory != null) {
      this.player.getInventory().setContents(this.savedInventory);
      this.savedInventory = null;
    }
  }

  private void giveTools() {
    this.player.getInventory().clear();
    this.slotToTool.clear();
    ConfigurationSection toolsCfg = this.plugin.getConfig().getConfigurationSection("edit-tools");
    LangManager lang = this.plugin.getLang();
    String[] toolKeys =
        new String[] {
          "add-point", "tp-point", "play", "points-list", "rec-toggle", "path-toggle", "exit"
        };

    for (String key : toolKeys) {
      Material mat = DEFAULT_MATERIALS.getOrDefault(key, Material.STICK);
      int slot = DEFAULT_SLOTS.getOrDefault(key, 0);
      if (toolsCfg != null && toolsCfg.isConfigurationSection(key)) {
        ConfigurationSection sec = toolsCfg.getConfigurationSection(key);
        String matName = sec.getString("material");
        if (matName != null) {
          Material parsed = parseToolMaterial(matName);
          if (parsed != null) {
            mat = parsed;
          }
        }

        slot = sec.getInt("slot", slot);
      }

      String nameRaw = lang.raw("tool." + key);
      String loreRaw = lang.raw("tool." + key + "-lore");
      String markerRaw = lang.raw("tool.marker");
      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      meta.displayName(MM.deserialize(nameRaw).decoration(TextDecoration.ITALIC, false));
      meta.lore(
          List.of(
              Component.text(loreRaw, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
              Component.text(markerRaw, NamedTextColor.DARK_GRAY)
                  .decoration(TextDecoration.ITALIC, false)));
      item.setItemMeta(meta);
      this.player.getInventory().setItem(slot, item);
      this.slotToTool.put(slot, key);
    }
  }

  private static Material parseToolMaterial(String rawName) {
    if (rawName == null || rawName.isBlank()) {
      return null;
    }

    Material direct = Material.matchMaterial(rawName);
    if (direct != null) {
      return direct;
    }

    String normalized = rawName.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    return switch (normalized) {
      case "EYE_OF_ENDER" -> Material.ENDER_EYE;
      case "BOOK_AND_QUIL", "BOOK_AND_QUILL" -> Material.WRITABLE_BOOK;
      case "REDTONE_DUST", "REDSTONE_DUST" -> Material.REDSTONE;
      case "LARNTERN" -> Material.LANTERN;
      default -> Material.matchMaterial(normalized);
    };
  }

  public static boolean isEditTool(ItemStack item) {
    if (item != null && item.hasItemMeta()) {
      ItemMeta meta = item.getItemMeta();
      if (meta.lore() != null && meta.lore().size() >= 2) {
        Component lastLore = meta.lore().get(meta.lore().size() - 1);
        return lastLore.toString().contains("CookieCamera");
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  public void spawnMarkers() {
    this.destroyMarkers();
    if (this.scene.isShowMarkers()) {
      World w = this.player.getWorld();
      List<CameraPoint> points = this.scene.getPoints();

      for (int i = 0; i < points.size(); i++) {
        CameraPoint p = points.get(i);
        Location loc = p.toLocation(w);
        String label =
            "#"
                + i
                + " ("
                + String.format("%.0f", p.getX())
                + ","
                + String.format("%.0f", p.getY())
                + ","
                + String.format("%.0f", p.getZ())
                + ")";
        PacketHandler.spawnMarker(this.player, loc, label);
      }
    }
  }

  public void destroyMarkers() {
    PacketHandler.destroyMarkers(this.player);
  }

  public void refreshMarkers() {
    this.spawnMarkers();
  }

  public void startVisuals() {
    if (this.particleTask == null || this.particleTask.isCancelled()) {
      this.pathVisible = true;
      int interval = this.plugin.getConfig().getInt("particles.refresh-interval", 10);
      this.particleTask =
          Scheduler.runEntityTimer(
              this.plugin,
              this.player,
              h -> {
                if (!this.player.isOnline()) {
                  h.cancel();
                } else {
                  this.showParticles();
                }
              },
              interval,
              interval);
    }
  }

  public void stopVisuals() {
    this.pathVisible = false;
    if (this.particleTask != null && !this.particleTask.isCancelled()) {
      this.particleTask.cancel();
      this.particleTask = null;
    }
  }

  public void toggleVisuals() {
    if (this.pathVisible) {
      this.stopVisuals();
    } else {
      this.startVisuals();
    }
  }

  private void showParticles() {
    List<CameraPoint> points = this.scene.getPoints();
    if (points.size() < 2) {
      for (CameraPoint p : points) {
        this.player.spawnParticle(
            Particle.FLAME, p.toLocation(this.player.getWorld()), 3, 0.1, 0.1, 0.1, 0.0);
      }
    } else {
      World w = this.player.getWorld();
      int density = this.plugin.getConfig().getInt("particles.path-density", 20);

      for (int seg = 0; seg < points.size() - 1; seg++) {
        int i0 = Math.max(0, seg - 1);
        int i2 = Math.min(points.size() - 1, seg + 1);
        int i3 = Math.min(points.size() - 1, seg + 2);
        CameraPoint cp0 = points.get(i0);
        CameraPoint cp1 = points.get(seg);
        CameraPoint cp2 = points.get(i2);
        CameraPoint cp3 = points.get(i3);
        float segProgress = (float) seg / (points.size() - 1);

        for (int step = 0; step <= density; step++) {
          double t = (double) step / density;
          double[] pos =
              CatmullRom.interpolate3D(
                  new double[] {cp0.getX(), cp0.getY(), cp0.getZ()},
                  new double[] {cp1.getX(), cp1.getY(), cp1.getZ()},
                  new double[] {cp2.getX(), cp2.getY(), cp2.getZ()},
                  new double[] {cp3.getX(), cp3.getY(), cp3.getZ()},
                  t);
          Location particleLoc = new Location(w, pos[0], pos[1], pos[2]);
          float localProgress = segProgress + (float) t / (points.size() - 1);
          int r = (int) (85.0F + localProgress * 170.0F);
          int g = (int) (255.0F - localProgress * 85.0F);
          int b = (int) (255.0F - localProgress * 255.0F);
          DustOptions dust =
              new DustOptions(
                  Color.fromRGB(Math.min(255, r), Math.min(255, g), Math.max(0, b)), 0.8F);
          this.player.spawnParticle(Particle.DUST, particleLoc, 1, 0.0, 0.0, 0.0, 0.0, dust);
        }
      }

      for (CameraPoint p : points) {
        Location loc = p.toLocation(w);
        double rad = Math.toRadians(p.getYaw());
        double dx = -Math.sin(rad) * 0.5;
        double dz = Math.cos(rad) * 0.5;
        this.player.spawnParticle(
            Particle.FLAME, loc.clone().add(dx, 0.3, dz), 1, 0.0, 0.0, 0.0, 0.0);
      }
    }
  }

  public void startRecording() {
    if (!this.recording) {
      this.recording = true;
      int interval = this.plugin.getConfig().getInt("record-interval-ticks", 10);
      this.recordTask =
          Scheduler.runEntityTimer(
              this.plugin,
              this.player,
              h -> {
                if (this.player.isOnline() && this.recording) {
                  Location loc = this.player.getLocation();
                  this.scene.setWorld(loc.getWorld().getName());
                  CameraPoint point = new CameraPoint(loc, -1, -1.0F, EaseType.LINEAR);
                  this.scene.addPoint(point);
                  this.plugin.getSceneManager().saveScene(this.scene);
                  this.refreshMarkers();
                } else {
                  h.cancel();
                }
              },
              interval,
              interval);
    }
  }

  public void stopRecording() {
    this.recording = false;
    if (this.recordTask != null && !this.recordTask.isCancelled()) {
      this.recordTask.cancel();
    }
  }

  public void tpNextPoint() {
    List<CameraPoint> points = this.scene.getPoints();
    if (points.isEmpty()) {
      this.player.sendMessage(
          Component.text("[CookieCamera] ", NamedTextColor.DARK_AQUA)
              .append(this.plugin.getLang().get("point.no-points")));
    } else {
      this.lastTpIndex = (this.lastTpIndex + 1) % points.size();
      CameraPoint p = points.get(this.lastTpIndex);
      Scheduler.teleport(this.player, p.toLocation(this.player.getWorld()), null);
      this.player.sendActionBar(
          MM.deserialize(this.plugin.getLang().raw("point.tp", "idx", this.lastTpIndex)));
    }
  }

  private void cleanup() {
    this.stopVisuals();
    this.stopRecording();
    this.destroyMarkers();
    if (this.actionBarTask != null && !this.actionBarTask.isCancelled()) {
      this.actionBarTask.cancel();
      this.actionBarTask = null;
    }
  }
}

