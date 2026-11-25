package com.cookiecamera.listener;

import com.cookiecamera.CookieCamera;
import com.cookiecamera.manager.EditSession;
import com.cookiecamera.model.CameraPoint;
import com.cookiecamera.model.EaseType;
import com.cookiecamera.model.Scene;
import com.cookiecamera.protocol.PacketHandler;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class EditItemListener implements Listener {
  private final CookieCamera plugin;

  public EditItemListener(CookieCamera plugin) {
    this.plugin = plugin;
  }

  private Component prefix() {
    return Component.text("[CookieCamera] ", NamedTextColor.DARK_AQUA);
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    EditSession session = EditSession.getSession(player);
    if (session != null) {
      if (event.getAction() == Action.RIGHT_CLICK_AIR
          || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
        int slot = player.getInventory().getHeldItemSlot();
        String toolKey = session.getToolKeyForSlot(slot);
        if (toolKey != null) {
          event.setCancelled(true);
          switch (toolKey) {
            case "add-point":
              {
                Location loc = player.getLocation();
                Scene scene = session.getScene();
                scene.setWorld(loc.getWorld().getName());
                scene.addPoint(new CameraPoint(loc, -1, -1.0F, EaseType.LINEAR));
                this.plugin.getSceneManager().saveScene(scene);
                session.refreshMarkers();
                int idx = scene.getPoints().size() - 1;
                String pos = String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
                player.sendMessage(
                    this.prefix()
                        .append(this.plugin.getLang().get("point.added", "idx", idx, "pos", pos)));
                break;
              }
            case "tp-point":
              session.tpNextPoint();
              break;
            case "play":
              {
                Scene scene = session.getScene();
                if (scene.getPoints().size() < 2) {
                  player.sendMessage(
                      this.prefix().append(this.plugin.getLang().get("point.need-2")));
                  return;
                }

                if (this.plugin.getPlaybackManager().isPlaying(player)) {
                  this.plugin.getPlaybackManager().stop(player);
                } else {
                  this.plugin.getPlaybackManager().play(player, scene);
                }
                break;
              }
            case "points-list":
              player.performCommand("cookiecamera points");
              break;
            case "rec-toggle":
              if (session.isRecording()) {
                session.stopRecording();
                player.sendMessage(
                    this.prefix()
                        .append(
                            this.plugin
                                .getLang()
                                .get("rec.stop", "count", session.getScene().getPoints().size())));
              } else {
                session.startRecording();
                player.sendMessage(this.prefix().append(this.plugin.getLang().get("rec.start")));
              }
              break;
            case "path-toggle":
              session.toggleVisuals();
              player.sendMessage(
                  this.prefix()
                      .append(
                          this.plugin
                              .getLang()
                              .get(
                                  "path.toggle", "value", session.isPathVisible() ? "ON" : "OFF")));
              break;
            case "exit":
              EditSession.stopEditing(player);
              player.sendMessage(this.prefix().append(this.plugin.getLang().get("edit.exit")));
          }
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onInteractEntity(PlayerInteractAtEntityEvent event) {
    if (event.getRightClicked() instanceof ArmorStand stand) {
      if (stand.getScoreboardTags().contains("cookiecamera_marker")) {
        Player player = event.getPlayer();
        EditSession session = EditSession.getSession(player);
        if (session != null) {
          if (stand.getScoreboardTags().contains("cookiecamera_owner_" + player.getUniqueId())) {
            event.setCancelled(true);
            List<ArmorStand> markers = PacketHandler.getMarkers(player);
            int idx = markers.indexOf(stand);
            if (idx >= 0 && idx < session.getScene().getPoints().size()) {
              player.performCommand("cookiecamera pt " + idx);
            }
          }
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onSneak(PlayerToggleSneakEvent event) {
    Player player = event.getPlayer();
    if (event.isSneaking() && this.plugin.getPlaybackManager().isPlaying(player)) {
      this.plugin.getPlaybackManager().handleExitTap(player);
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onDrop(PlayerDropItemEvent event) {
    Player player = event.getPlayer();
    if (this.plugin.getPlaybackManager().isPlaying(player)) {
      event.setCancelled(true);
    } else {
      if (EditSession.getSession(player) != null) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onInventoryClick(InventoryClickEvent event) {
    if (event.getWhoClicked() instanceof Player player) {
      if (EditSession.getSession(player) != null) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onSwapHand(PlayerSwapHandItemsEvent event) {
    Player player = event.getPlayer();
    if (this.plugin.getPlaybackManager().isPlaying(player)) {
      event.setCancelled(true);
    } else {
      if (EditSession.getSession(player) != null) {
        event.setCancelled(true);
      }
    }
  }
}


