package com.cookiecamera.util;

import com.cookiecamera.CookieCamera;
import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class TabScoreboardCompat {
  private static final String TAB_PLUGIN_NAME = "TAB";
  private static final String TAB_API_CLASS = "me.neznamy.tab.api.TabAPI";
  private static final String TAB_PLAYER_CLASS = "me.neznamy.tab.api.TabPlayer";

  private TabScoreboardCompat() {}

  public static State hideScoreboard(CookieCamera plugin, Player player) {
    return setScoreboardVisible(plugin, player, false);
  }

  public static void restoreScoreboard(CookieCamera plugin, Player player, State state) {
    if (state == null || !state.handled()) {
      return;
    }

    setScoreboardVisible(plugin, player, state.previousVisible());
  }

  private static State setScoreboardVisible(CookieCamera plugin, Player player, boolean visible) {
    if (plugin == null || player == null) {
      return State.inactive();
    }

    try {
      Plugin tabPlugin = plugin.getServer().getPluginManager().getPlugin(TAB_PLUGIN_NAME);
      if (tabPlugin == null || !tabPlugin.isEnabled()) {
        return State.inactive();
      }

      ClassLoader loader = tabPlugin.getClass().getClassLoader();
      Class<?> tabApiClass = Class.forName(TAB_API_CLASS, true, loader);
      Class<?> tabPlayerClass = Class.forName(TAB_PLAYER_CLASS, true, loader);
      Object api = tabApiClass.getMethod("getInstance").invoke(null);
      if (api == null) {
        return State.inactive();
      }

      Object scoreboardManager = api.getClass().getMethod("getScoreboardManager").invoke(api);
      if (scoreboardManager == null) {
        return State.inactive();
      }

      Object tabPlayer = tabApiClass.getMethod("getPlayer", UUID.class).invoke(api, player.getUniqueId());
      if (tabPlayer == null) {
        return State.inactive();
      }

      Method hasScoreboardVisible =
          scoreboardManager.getClass().getMethod("hasScoreboardVisible", tabPlayerClass);
      Method setScoreboardVisible =
          scoreboardManager
              .getClass()
              .getMethod("setScoreboardVisible", tabPlayerClass, boolean.class, boolean.class);
      boolean previousVisible = (boolean) hasScoreboardVisible.invoke(scoreboardManager, tabPlayer);
      setScoreboardVisible.invoke(scoreboardManager, tabPlayer, visible, false);
      return new State(true, previousVisible);
    } catch (Exception exception) {
      return State.inactive();
    }
  }

  public record State(boolean handled, boolean previousVisible) {
    private static final State INACTIVE = new State(false, true);

    public static State inactive() {
      return INACTIVE;
    }
  }
}
