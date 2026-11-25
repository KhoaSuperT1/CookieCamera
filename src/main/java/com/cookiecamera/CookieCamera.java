package com.cookiecamera;

import com.cookiecamera.command.CookieCommand;
import com.cookiecamera.command.CookieTabCompleter;
import com.cookiecamera.listener.EditItemListener;
import com.cookiecamera.listener.PlayerListener;
import com.cookiecamera.listener.TriggerListener;
import com.cookiecamera.manager.LangManager;
import com.cookiecamera.manager.PlaybackManager;
import com.cookiecamera.manager.SceneManager;
import com.cookiecamera.manager.TriggerManager;
import com.cookiecamera.protocol.MovementBlocker;
import com.cookiecamera.script.CookieScriptEngine;
import com.cookiecamera.util.Scheduler;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class CookieCamera extends JavaPlugin {
  private static final String PRIMARY_COMMAND = "cookiecamera";
  private static final String SECONDARY_COMMAND_ALIAS = "cc";
  private static CookieCamera instance;
  private SceneManager sceneManager;
  private PlaybackManager playbackManager;
  private TriggerManager triggerManager;
  private CookieScriptEngine scriptEngine;
  private LangManager langManager;
  private MovementBlocker movementBlocker;
  private boolean packetEventsAvailable = false;
  private CookieCommand cookieCommand;
  private CookieTabCompleter cookieTabCompleter;

  public void onEnable() {
    instance = this;
    this.saveDefaultConfig();
    this.sceneManager = new SceneManager(this);
    this.playbackManager = new PlaybackManager(this);
    this.triggerManager = new TriggerManager(this);
    this.scriptEngine = new CookieScriptEngine(this);
    this.langManager = new LangManager(this);
    this.langManager.load();
    this.sceneManager.loadAll();
    this.triggerManager.loadAll();
    this.scriptEngine.loadAll();
    this.cookieCommand = new CookieCommand(this);
    this.cookieTabCompleter = new CookieTabCompleter(this);
    this.registerCommands();
    this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    this.getServer().getPluginManager().registerEvents(new TriggerListener(this), this);
    this.getServer().getPluginManager().registerEvents(new EditItemListener(this), this);
    this.initPacketEvents();
    this.getLogger()
        .info(
            "CookieCamera enabled! Folia="
                + Scheduler.isFolia()
                + " PacketEvents="
                + this.packetEventsAvailable);
  }

  private void registerCommands() {
    if (this.tryPaperCommands()) {
      return;
    }

    this.registerLegacyCommands();
  }

  private boolean tryPaperCommands() {
    try {
      Class<?> basicCommandClass =
          Class.forName("io.papermc.paper.command.brigadier.BasicCommand");
      Class<?> commandSourceStackClass =
          Class.forName("io.papermc.paper.command.brigadier.CommandSourceStack");
      Method getSenderMethod = commandSourceStackClass.getMethod("getSender");
      Object basicCommand =
          Proxy.newProxyInstance(
              basicCommandClass.getClassLoader(),
              new Class<?>[] {basicCommandClass},
              (proxy, method, args) -> {
                switch (method.getName()) {
                  case "execute":
                    {
                      CommandSender sender = this.resolveCommandSender(args[0], getSenderMethod);
                      this.cookieCommand.onCommand(
                          sender, null, PRIMARY_COMMAND, (String[]) args[1]);
                      return null;
                    }
                  case "suggest":
                    {
                      CommandSender sender = this.resolveCommandSender(args[0], getSenderMethod);
                      if (!this.canUseCommands(sender)) {
                        return List.of();
                      }

                      List<String> result =
                          this.cookieTabCompleter.onTabComplete(
                              sender, null, PRIMARY_COMMAND, (String[]) args[1]);
                      return result != null ? result : List.of();
                    }
                  case "canUse":
                    return this.canUseCommands(this.resolveCommandSender(args[0], getSenderMethod));
                  case "permission":
                    return null;
                  default:
                    return method.getDefaultValue();
                }
              });
      Method registerCommandMethod =
          this.getClass()
              .getMethod(
                  "registerCommand",
                  String.class,
                  String.class,
                  Collection.class,
                  basicCommandClass);
      registerCommandMethod.invoke(
          this,
          PRIMARY_COMMAND,
          "CookieCamera main command",
          List.of(SECONDARY_COMMAND_ALIAS),
          basicCommand);
      this.getLogger().info("Registered commands via JavaPlugin#registerCommand.");
      return true;
    } catch (ClassNotFoundException | NoSuchMethodException ignored) {
      return false;
    } catch (Exception exception) {
      this.getLogger()
          .severe("Paper command registration failed: " + exception.getMessage());
      return false;
    }
  }

  private void registerLegacyCommands() {
    PluginCommand cmd = this.getCommand(PRIMARY_COMMAND);
    if (cmd != null) {
      cmd.setExecutor(this.cookieCommand);
      cmd.setTabCompleter(this.cookieTabCompleter);
      this.getLogger().info("Registered commands via plugin.yml.");
    } else {
      this.getLogger()
          .warning(
              "Command '" + PRIMARY_COMMAND + "' not found in plugin.yml! Commands will not work.");
    }
  }

  private void initPacketEvents() {
    try {
      if (PacketEvents.getAPI() != null) {
        this.movementBlocker = new MovementBlocker(this);
        PacketEvents.getAPI().getEventManager().registerListener(this.movementBlocker);
        this.packetEventsAvailable = true;
        this.getLogger().info("PacketEvents listener registered successfully.");
      } else {
        this.getLogger()
            .warning("PacketEvents API is null at enable time, trying delayed registration...");
        Scheduler.runGlobalDelayed(
            this,
            () -> {
              try {
                if (PacketEvents.getAPI() != null) {
                  this.movementBlocker = new MovementBlocker(this);
                  PacketEvents.getAPI().getEventManager().registerListener(this.movementBlocker);
                  this.packetEventsAvailable = true;
                  this.getLogger().info("PacketEvents listener registered (delayed).");
                } else {
                  this.getLogger()
                      .severe(
                          "PacketEvents API is still null after delay! Is packetevents-spigot"
                              + " installed?");
                }
              } catch (Exception exception) {
                this.getLogger()
                    .severe(
                        "Failed to register PacketEvents listener (delayed): "
                            + exception.getMessage());
              }
            },
            1L);
      }
    } catch (Exception exception) {
      this.getLogger().severe("Failed to initialize PacketEvents: " + exception.getMessage());
      exception.printStackTrace();
    }
  }

  public void onDisable() {
    if (this.playbackManager != null) {
      this.playbackManager.stopAll();
    }

    if (this.movementBlocker != null && this.packetEventsAvailable) {
      try {
        PacketEventsAPI<?> api = PacketEvents.getAPI();
        if (api != null) {
          api.getEventManager().unregisterListener(this.movementBlocker);
        }
      } catch (Exception exception) {
        this.getLogger()
            .warning("Failed to unregister PacketEvents listener: " + exception.getMessage());
      }
    }

    this.getLogger().info("CookieCamera disabled.");
  }

  public static CookieCamera getInstance() {
    return instance;
  }

  public SceneManager getSceneManager() {
    return this.sceneManager;
  }

  public PlaybackManager getPlaybackManager() {
    return this.playbackManager;
  }

  public TriggerManager getTriggerManager() {
    return this.triggerManager;
  }

  public CookieScriptEngine getScriptEngine() {
    return this.scriptEngine;
  }

  public LangManager getLang() {
    return this.langManager;
  }

  public boolean isFolia() {
    return Scheduler.isFolia();
  }

  public boolean isPacketEventsAvailable() {
    return this.packetEventsAvailable;
  }

  public void clearMovementState(Player player) {
    if (this.movementBlocker != null && player != null) {
      this.movementBlocker.clearPlayerState(player);
    }
  }

  public void clearAllMovementStates() {
    if (this.movementBlocker != null) {
      this.movementBlocker.clearAllStates();
    }
  }

  public boolean canUseCommands(CommandSender sender) {
    if (sender instanceof Player player) {
      return player.isOp();
    }

    return sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender;
  }

  public boolean canUsePlaybackExit(Player player) {
    return player != null && player.isOp();
  }

  public boolean shouldShowPlaybackBossBar(Player player) {
    if (player == null || !this.getConfig().getBoolean("show-progress-bar", true)) {
      return false;
    }

    return player.isOp() || this.getConfig().getBoolean("bossbar.member", true);
  }

  public boolean shouldSendActionBar(Player player) {
    return player != null
        && (player.isOp() || this.getConfig().getBoolean("actionbar.member", true));
  }

  public boolean shouldShowPlaybackScoreboard(Player player) {
    return player != null && (player.isOp() || this.getConfig().getBoolean("scoreboard", true));
  }

  private CommandSender resolveCommandSender(Object source, Method getSenderMethod)
      throws ReflectiveOperationException {
    if (source instanceof CommandSender sender) {
      return sender;
    }

    return (CommandSender) getSenderMethod.invoke(source);
  }
}

