package com.cookiecamera.listener;

import com.cookiecamera.CookieCamera;
import com.cookiecamera.manager.EditSession;
import com.cookiecamera.protocol.PacketHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
  private final CookieCamera plugin;

  public PlayerListener(CookieCamera plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();

    try {
      if (this.plugin.getPlaybackManager().isPlaying(player)) {
        this.plugin.getPlaybackManager().stop(player);
      }
    } catch (Exception exception) {
      this.plugin
          .getLogger()
          .warning(
              "Failed to stop playback during quit for "
                  + player.getName()
                  + ": "
                  + exception.getMessage());
    }

    EditSession.stopEditing(player);
    this.plugin.getTriggerManager().clearPlayerState(player);
    this.plugin.clearMovementState(player);

    try {
      PacketHandler.destroyAll(player);
    } catch (Exception exception) {
      this.plugin
          .getLogger()
          .warning(
              "Failed to destroy camera state during quit for "
                  + player.getName()
                  + ": "
                  + exception.getMessage());
    }
  }
}

