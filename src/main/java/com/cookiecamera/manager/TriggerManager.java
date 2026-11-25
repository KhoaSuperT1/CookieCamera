package com.cookiecamera.manager;

import com.cookiecamera.CookieCamera;
import com.cookiecamera.model.Scene;
import com.cookiecamera.model.Trigger;
import com.cookiecamera.util.Scheduler;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TriggerManager {
  private final CookieCamera plugin;
  private final File triggerFile;
  private final Map<String, Trigger> triggers = new ConcurrentHashMap<>();
  private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
  private final Set<String> oneTimeFired = ConcurrentHashMap.newKeySet();
  private final Map<UUID, Set<String>> activeRegionTriggers = new ConcurrentHashMap<>();

  public TriggerManager(CookieCamera plugin) {
    this.plugin = plugin;
    this.triggerFile = new File(plugin.getDataFolder(), "triggers.yml");
  }

  public void loadAll() {
    this.triggers.clear();
    this.cooldowns.clear();
    this.activeRegionTriggers.clear();
    if (this.triggerFile.exists()) {
      YamlConfiguration cfg = YamlConfiguration.loadConfiguration(this.triggerFile);

      for (String key : cfg.getKeys(false)) {
        ConfigurationSection sec = cfg.getConfigurationSection(key);
        if (sec != null) {
          Trigger t = new Trigger(key);
          Trigger.TriggerType type =
              Trigger.TriggerType.fromString(sec.getString("type", "REGION_ENTER"));
          if (type == null) {
            this.plugin
                .getLogger()
                .warning(
                    "Trigger '"
                        + key
                        + "' has invalid type '"
                        + sec.getString("type")
                        + "', defaulting to REGION_ENTER.");
            type = Trigger.TriggerType.REGION_ENTER;
          }

          t.setType(type);
          t.setSceneId(sec.getString("scene"));
          t.setCommandPattern(sec.getString("command"));
          t.setWorld(sec.getString("world"));
          if (t.getType() == Trigger.TriggerType.REGION_ENTER) {
            t.setRegion(
                sec.getDouble("x1"),
                sec.getDouble("y1"),
                sec.getDouble("z1"),
                sec.getDouble("x2"),
                sec.getDouble("y2"),
                sec.getDouble("z2"));
          } else if (t.getType() == Trigger.TriggerType.RIGHT_CLICK_BLOCK) {
            t.setBlock(sec.getInt("blockX"), sec.getInt("blockY"), sec.getInt("blockZ"));
          }

          t.setRequiredItemInHand(sec.getString("requiredItemInHand"));
          t.setRequiredItemInInv(sec.getString("requiredItemInInv"));
          t.setRequiredPermission(sec.getString("requiredPermission"));
          t.setCooldownTicks(sec.getInt("cooldownTicks", 0));
          t.setOneTime(sec.getBoolean("oneTime", false));
          t.getCommands().addAll(sec.getStringList("commands"));
          this.triggers.put(key.toLowerCase(), t);
        }
      }

      this.plugin.getLogger().info("Loaded " + this.triggers.size() + " trigger(s).");
    }
  }

  public void saveAll() {
    YamlConfiguration cfg = new YamlConfiguration();

    for (Trigger t : this.triggers.values()) {
      ConfigurationSection sec = cfg.createSection(t.getId());
      sec.set("type", t.getType().name());
      sec.set("scene", t.getSceneId());
      sec.set("command", t.getCommandPattern());
      sec.set("world", t.getWorld());
      if (t.getType() == Trigger.TriggerType.REGION_ENTER) {
        sec.set("x1", t.getX1());
        sec.set("y1", t.getY1());
        sec.set("z1", t.getZ1());
        sec.set("x2", t.getX2());
        sec.set("y2", t.getY2());
        sec.set("z2", t.getZ2());
      } else if (t.getType() == Trigger.TriggerType.RIGHT_CLICK_BLOCK) {
        sec.set("blockX", t.getBlockX());
        sec.set("blockY", t.getBlockY());
        sec.set("blockZ", t.getBlockZ());
      }

      sec.set("requiredItemInHand", t.getRequiredItemInHand());
      sec.set("requiredItemInInv", t.getRequiredItemInInv());
      sec.set("requiredPermission", t.getRequiredPermission());
      sec.set("cooldownTicks", t.getCooldownTicks());
      sec.set("oneTime", t.isOneTime());
      sec.set("commands", t.getCommands());
    }

    try {
      cfg.save(this.triggerFile);
    } catch (IOException exception) {
      this.plugin.getLogger().log(Level.SEVERE, "Failed to save triggers", exception);
    }
  }

  public Trigger createTrigger(String id) {
    Trigger t = new Trigger(id);
    this.triggers.put(id.toLowerCase(), t);
    this.saveAll();
    return t;
  }

  public boolean deleteTrigger(String id) {
    Trigger removed = this.triggers.remove(id.toLowerCase());
    if (removed != null) {
      this.saveAll();
      return true;
    } else {
      return false;
    }
  }

  public Trigger getTrigger(String id) {
    return this.triggers.get(id.toLowerCase());
  }

  public Collection<Trigger> getAllTriggers() {
    return Collections.unmodifiableCollection(this.triggers.values());
  }

  public void registerFromScript(Trigger trigger) {
    this.triggers.put(trigger.getId().toLowerCase(), trigger);
  }

  public void checkRegionTriggers(Player player, String world, double x, double y, double z) {
    Set<String> previouslyInside =
        this.activeRegionTriggers.computeIfAbsent(
            player.getUniqueId(), uuid -> ConcurrentHashMap.newKeySet());
    Set<String> currentlyInside = new HashSet<>();

    for (Trigger t : this.triggers.values()) {
      if (t.getType() == Trigger.TriggerType.REGION_ENTER) {
        String triggerKey = t.getId().toLowerCase(Locale.ROOT);
        if (t.isInRegion(world, x, y, z)) {
          currentlyInside.add(triggerKey);
          if (!previouslyInside.contains(triggerKey)) {
            this.fireTrigger(player, t);
          }
        }
      }
    }

    previouslyInside.clear();
    previouslyInside.addAll(currentlyInside);
  }

  public void checkBlockTrigger(Player player, String world, int bx, int by, int bz) {
    for (Trigger t : this.triggers.values()) {
      if (t.getType() == Trigger.TriggerType.RIGHT_CLICK_BLOCK && t.isBlock(world, bx, by, bz)) {
        this.fireTrigger(player, t);
      }
    }
  }

  public void checkCommandTrigger(Player player, String commandLine) {
    for (Trigger t : this.triggers.values()) {
      if (t.getType() == Trigger.TriggerType.COMMAND && t.matchesCommand(commandLine)) {
        this.fireTrigger(player, t);
      }
    }
  }

  public boolean fireTrigger(Player player, Trigger trigger) {
    if (trigger.getRequiredPermission() != null
        && !player.hasPermission(trigger.getRequiredPermission())) {
      return false;
    } else {
      String oneTimeKey = player.getUniqueId() + ":" + trigger.getId();
      if (trigger.isOneTime() && this.oneTimeFired.contains(oneTimeKey)) {
        return false;
      } else {
        long now = System.currentTimeMillis();
        if (trigger.getCooldownTicks() > 0) {
          Map<String, Long> playerCooldowns =
              this.cooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
          Long lastFired = playerCooldowns.get(trigger.getId());
          if (lastFired != null && now - lastFired < trigger.getCooldownTicks() * 50L) {
            return false;
          }
        }

        if (trigger.getRequiredItemInHand() != null) {
          ItemStack hand = player.getInventory().getItemInMainHand();
          Material required = Material.matchMaterial(trigger.getRequiredItemInHand());
          if (required == null || hand.getType() != required) {
            return false;
          }
        }

        if (trigger.getRequiredItemInInv() != null) {
          Material required = Material.matchMaterial(trigger.getRequiredItemInInv());
          if (required == null || !player.getInventory().contains(required)) {
            return false;
          }
        }

        boolean executed = false;
        if (trigger.getSceneId() != null) {
          Scene scene = this.plugin.getSceneManager().getScene(trigger.getSceneId());
          if (scene != null && scene.getPoints().size() >= 2) {
            executed = this.plugin.getPlaybackManager().play(player, scene) || executed;
          }
        }

        for (String cmd : trigger.getCommands()) {
          if (cmd == null || cmd.isBlank()) {
            continue;
          }

          String parsed = cmd.replace("{player}", player.getName());
          Scheduler.runGlobal(
              this.plugin,
              () ->
                  this.plugin
                      .getServer()
                      .dispatchCommand(this.plugin.getServer().getConsoleSender(), parsed));
          executed = true;
        }

        if (!executed) {
          return false;
        }

        if (trigger.getCooldownTicks() > 0) {
          this.cooldowns
              .computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
              .put(trigger.getId(), now);
        }

        if (trigger.isOneTime()) {
          this.oneTimeFired.add(oneTimeKey);
        }

        return true;
      }
    }
  }

  public void clearPlayerState(Player player) {
    UUID uuid = player.getUniqueId();
    this.cooldowns.remove(uuid);
    this.activeRegionTriggers.remove(uuid);
  }
}

