package com.cookiecamera.protocol;

import com.cookiecamera.CookieCamera;
import com.cookiecamera.util.Scheduler;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction.Action;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public class MovementBlocker extends PacketListenerAbstract {
  private final CookieCamera plugin;
  private final Map<UUID, Boolean> previousShiftStates = new ConcurrentHashMap<>();

  public MovementBlocker(CookieCamera plugin) {
    super(PacketListenerPriority.HIGHEST);
    this.plugin = plugin;
  }

  public void clearPlayerState(Player player) {
    if (player != null) {
      this.previousShiftStates.remove(player.getUniqueId());
    }
  }

  public void clearAllStates() {
    this.previousShiftStates.clear();
  }

  public void onPacketReceive(PacketReceiveEvent event) {
    if (event.getPlayer() instanceof Player player) {
      if (this.plugin.getPlaybackManager().isPlaying(player)) {
        PacketTypeCommon type = event.getPacketType();
        if (type != Client.PLAYER_POSITION
            && type != Client.PLAYER_POSITION_AND_ROTATION
            && type != Client.PLAYER_ROTATION
            && type != Client.PLAYER_FLYING
            && type != Client.VEHICLE_MOVE) {
          if (type == Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction wrapper = new WrapperPlayClientEntityAction(event);
            if (this.plugin.getConfig().getBoolean("allow-skip", true)) {
              if (wrapper.getAction() == Action.START_SNEAKING) {
                Scheduler.runEntity(
                    this.plugin, player, () -> this.plugin.getPlaybackManager().handleExitTap(player));
              }
            }
          } else if (type == Client.PLAYER_INPUT) {
            User user = event.getUser();
            if (user != null
                && user.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2)
                && this.plugin.getConfig().getBoolean("allow-skip", true)) {
              WrapperPlayClientPlayerInput wrapper = new WrapperPlayClientPlayerInput(event);
              boolean shifting = wrapper.isShift();
              Boolean previous = this.previousShiftStates.put(player.getUniqueId(), shifting);
              if (shifting && !Boolean.TRUE.equals(previous)) {
                Scheduler.runEntity(
                    this.plugin, player, () -> this.plugin.getPlaybackManager().handleExitTap(player));
              }
            }
          }
        } else {
          event.setCancelled(true);
        }
      }
    }
  }
}

