package com.cookiecamera.manager;

import com.cookiecamera.CookieCamera;
import com.cookiecamera.model.CameraPoint;
import com.cookiecamera.model.PointAction;
import com.cookiecamera.model.Scene;
import com.cookiecamera.util.Scheduler;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class PointActionExecutor {
  private static final MiniMessage MM = MiniMessage.miniMessage();

  private PointActionExecutor() {}

  public static void execute(
      CookieCamera plugin, Player player, Scene scene, int pointIndex, CameraPoint point) {
    List<PointAction> actions = point.getActions();
    if (actions != null && !actions.isEmpty()) {
      for (PointAction action : actions) {
        try {
          executeOne(plugin, player, scene, pointIndex, point, action);
        } catch (Exception exception) {
          plugin
              .getLogger()
              .warning(
                  "[PointAction] Error executing "
                      + action.getType()
                      + " on point #"
                      + pointIndex
                      + ": "
                      + exception.getMessage());
        }
      }
    }
  }

  private static void executeOne(
      CookieCamera plugin,
      Player player,
      Scene scene,
      int pointIndex,
      CameraPoint point,
      PointAction action) {
    String val = replacePlaceholders(action.getValue(), player, scene, pointIndex, point);
    String ext =
        action.getExtra() != null
            ? replacePlaceholders(action.getExtra(), player, scene, pointIndex, point)
            : null;
    String ext2 =
        action.getExtra2() != null
            ? replacePlaceholders(action.getExtra2(), player, scene, pointIndex, point)
            : null;
    switch (action.getType()) {
      case TITLE:
        Component title = parseText(val);
        Component subtitle = ext != null ? parseText(ext) : Component.empty();
        int fadeIn = 10;
        int stay = 70;
        int fadeOut = 20;
        if (ext2 != null) {
          String[] parts = ext2.split(":");

          try {
            if (parts.length >= 1) {
              fadeIn = Integer.parseInt(parts[0]);
            }

            if (parts.length >= 2) {
              stay = Integer.parseInt(parts[1]);
            }

            if (parts.length >= 3) {
              fadeOut = Integer.parseInt(parts[2]);
            }
          } catch (NumberFormatException ignored) {
            // Keep default title timings when the override is malformed.
          }
        }

        player.showTitle(
            Title.title(
                title,
                subtitle,
                Times.times(
                    Duration.ofMillis(fadeIn * 50L),
                    Duration.ofMillis(stay * 50L),
                    Duration.ofMillis(fadeOut * 50L))));
        break;
      case ACTIONBAR:
        if (plugin.shouldSendActionBar(player)) {
          player.sendActionBar(parseText(val));
        }
        break;
      case SOUND:
        float volume = 1.0F;
        float pitch = 1.0F;
        if (ext != null) {
          try {
            volume = Float.parseFloat(ext);
          } catch (NumberFormatException ignored) {
            // Keep default volume when the value is malformed.
          }
        }

        if (ext2 != null) {
          try {
            pitch = Float.parseFloat(ext2);
          } catch (NumberFormatException ignored) {
            // Keep default pitch when the value is malformed.
          }
        }

        playSound(player, val, volume, pitch);
        break;
      case CMD:
        Scheduler.runGlobal(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), val));
        break;
      case PLAYERCMD:
        player.performCommand(val);
        break;
      case SCRIPT:
        plugin.getScriptEngine().execute(player, val);
        break;
      case MSG:
        player.sendMessage(parseText(val));
    }
  }

  public static String replacePlaceholders(
      String text, Player player, Scene scene, int pointIndex, CameraPoint point) {
    return text == null
        ? ""
        : text.replace("{player}", player.getName())
            .replace("{scene}", scene != null ? scene.getId() : "")
            .replace("{point}", String.valueOf(pointIndex))
            .replace("{x}", String.format("%.2f", point.getX()))
            .replace("{y}", String.format("%.2f", point.getY()))
            .replace("{z}", String.format("%.2f", point.getZ()))
            .replace("{yaw}", String.format("%.1f", point.getYaw()))
            .replace("{pitch}", String.format("%.1f", point.getPitch()))
            .replace(
                "{world}", scene != null && scene.getWorld() != null ? scene.getWorld() : "world");
  }

  private static Component parseText(String text) {
    return text.contains("<") && text.contains(">")
        ? MM.deserialize(text)
        : LegacyComponentSerializer.legacyAmpersand().deserialize(text);
  }

  public static void playSound(Player player, String soundName, float volume, float pitch) {
    if (player == null || soundName == null || soundName.isBlank()) {
      return;
    }

    String normalized = soundName.trim().toLowerCase(Locale.ROOT);
    NamespacedKey key = NamespacedKey.fromString(normalized);
    Sound sound = key != null ? Registry.SOUNDS.get(key) : null;
    if (sound != null) {
      player.playSound(player.getLocation(), sound, volume, pitch);
    } else {
      player.playSound(player.getLocation(), normalized, volume, pitch);
    }
  }
}

