package com.cookiecamera.listener;

import com.cookiecamera.CookieCamera;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class TriggerListener implements Listener {
  private final CookieCamera plugin;
  private final Map<UUID, Long> lastRegionCheckAt = new ConcurrentHashMap<>();

  public TriggerListener(CookieCamera plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMove(PlayerMoveEvent event) {
    Location from = event.getFrom();
    Location to = event.getTo();
    if (to != null) {
      if (from.getBlockX() != to.getBlockX()
          || from.getBlockY() != to.getBlockY()
          || from.getBlockZ() != to.getBlockZ()) {
        Player player = event.getPlayer();
        if (!this.plugin.getPlaybackManager().isPlaying(player)) {
          int intervalTicks = this.plugin.getConfig().getInt("trigger-check-interval", 5);
          if (intervalTicks > 0) {
            long now = System.currentTimeMillis();
            long minDelayMillis = intervalTicks * 50L;
            Long lastCheck = this.lastRegionCheckAt.get(player.getUniqueId());
            if (lastCheck != null && now - lastCheck < minDelayMillis) {
              return;
            }

            this.lastRegionCheckAt.put(player.getUniqueId(), now);
          }

          this.plugin
              .getTriggerManager()
              .checkRegionTriggers(
                  player, to.getWorld().getName(), to.getX(), to.getY(), to.getZ());
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onInteract(PlayerInteractEvent event) {
    if (event.getAction().isRightClick()) {
      Block block = event.getClickedBlock();
      if (block != null) {
        Player player = event.getPlayer();
        if (!this.plugin.getPlaybackManager().isPlaying(player)) {
          this.plugin
              .getTriggerManager()
              .checkBlockTrigger(
                  player, block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onCommand(PlayerCommandPreprocessEvent event) {
    Player player = event.getPlayer();
    if (!this.plugin.getPlaybackManager().isPlaying(player)) {
      this.plugin.getTriggerManager().checkCommandTrigger(player, event.getMessage());
    }
  }
}

