package com.cookiecamera.command;

import com.cookiecamera.CookieCamera;
import com.cookiecamera.manager.EditSession;
import com.cookiecamera.manager.LangManager;
import com.cookiecamera.manager.PlaybackManager;
import com.cookiecamera.model.CameraPoint;
import com.cookiecamera.model.EaseType;
import com.cookiecamera.model.PointAction;
import com.cookiecamera.model.Scene;
import com.cookiecamera.model.SmoothMode;
import com.cookiecamera.model.Trigger;
import com.cookiecamera.util.Scheduler;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CookieCommand implements CommandExecutor {
  private static final int PAGE_SIZE = 8;
  private final CookieCamera plugin;

  public CookieCommand(CookieCamera plugin) {
    this.plugin = plugin;
  }

  private LangManager lang() {
    return this.plugin.getLang();
  }

  private Component ok(String key, Object... args) {
    return Component.text("[CookieCamera] ", NamedTextColor.DARK_AQUA)
        .append(this.lang().get(key, args));
  }

  private Component err(String key, Object... args) {
    return Component.text("[CookieCamera] ", NamedTextColor.DARK_AQUA)
        .append(this.lang().get(key, args));
  }

  public boolean onCommand(
      @NotNull CommandSender sender,
      @Nullable Command command,
      @NotNull String label,
      @NotNull String[] args) {
    if (!this.plugin.canUseCommands(sender)) {
      sender.sendMessage(this.err("no-permission"));
      return true;
    }

    if (args.length == 0) {
      this.sendHelp(sender, 1);
      return true;
    } else {
      String sub = args[0].toLowerCase();

      return switch (sub) {
        case "help" -> {
          this.sendHelp(sender, this.argInt(args, 1, 1));
          yield true;
        }
        case "new" -> this.handleAddScene(sender, args);
        case "del" -> this.handleDeleteScene(sender, args);
        case "list" -> this.handleListScenes(sender, this.argInt(args, 1, 1));
        case "edit" -> this.handleSwitchScene(sender, args);
        case "exit" -> this.handleExit(sender);
        case "add" -> this.handleAddPoint(sender);
        case "insert" -> this.handleInsertPoint(sender, args);
        case "rm" -> this.handleRemovePoint(sender, args);
        case "clear" -> this.handleClearPoints(sender, args);
        case "set" -> this.handleSetPoint(sender, args);
        case "info" -> this.handleInfo(sender, args, this.argInt(args, 2, 1));
        case "points" -> this.handlePoints(sender, args, this.argInt(args, 2, 1));
        case "pt" -> this.handlePointDetail(sender, args);
        case "rec" -> this.handleRecord(sender, args);
        case "setting" -> this.handleSceneSetting(sender, args);
        case "play" -> this.handlePlay(sender, args);
        case "stop" -> this.handleStop(sender, args);
        case "pause" -> this.handlePause(sender);
        case "path" -> this.handleShowPath(sender, args);
        case "smooth" -> this.handleSmooth(sender, args);
        case "trigger" -> this.handleTrigger(sender, args);
        case "script" -> this.handleRunScript(sender, args);
        case "export" -> this.handleExport(sender, args);
        case "import" -> this.handleImport(sender, args);
        case "copy" -> this.handleCopy(sender, args);
        case "tp" -> this.handleTpToPoint(sender, args);
        case "reverse" -> this.handleReverse(sender, args);
        case "action" -> this.handleAction(sender, args);
        case "reload" -> this.handleReload(sender);
        default -> {
          this.sendHelp(sender, 1);
          yield true;
        }
      };
    }
  }

  private void sendHelp(CommandSender sender, int page) {
    String[][] cmds =
        new String[][] {
          {"/cookiecamera new", "<id>", this.lang().raw("help.new")},
          {"/cookiecamera del", "<id>", this.lang().raw("help.del")},
          {"/cookiecamera list", "", this.lang().raw("help.list")},
          {"/cookiecamera copy", "<src> <dst>", this.lang().raw("help.copy")},
          {"/cookiecamera edit", "<id>", this.lang().raw("help.edit")},
          {"/cookiecamera exit", "", this.lang().raw("help.exit")},
          {"/cookiecamera add", "", this.lang().raw("help.add")},
          {"/cookiecamera insert", "<idx>", this.lang().raw("help.insert")},
          {"/cookiecamera rm", "<idx>", this.lang().raw("help.rm")},
          {"/cookiecamera clear", "[id]", this.lang().raw("help.clear")},
          {"/cookiecamera set", "<idx> <param> [val]", this.lang().raw("help.set")},
          {"/cookiecamera points", "[id] [page]", this.lang().raw("help.points")},
          {"/cookiecamera pt", "<idx>", this.lang().raw("help.pt")},
          {"/cookiecamera info", "[id]", this.lang().raw("help.info")},
          {"/cookiecamera rec", "start|stop", this.lang().raw("help.rec")},
          {"/cookiecamera setting", "<param> <val>", this.lang().raw("help.setting")},
          {"/cookiecamera play", "<id> [player|all]", this.lang().raw("help.play")},
          {"/cookiecamera stop", "[player|all]", this.lang().raw("help.stop")},
          {"/cookiecamera pause", "", this.lang().raw("help.pause")},
          {"/cookiecamera path", "[on|off]", this.lang().raw("help.path")},
          {"/cookiecamera smooth", "<id>", this.lang().raw("help.smooth")},
          {"/cookiecamera reverse", "<id>", this.lang().raw("help.reverse")},
          {"/cookiecamera tp", "<idx>", this.lang().raw("help.tp")},
          {"/cookiecamera export", "<id>", this.lang().raw("help.export")},
          {"/cookiecamera import", "<file>", this.lang().raw("help.import")},
          {"/cookiecamera trigger", "<sub>", this.lang().raw("help.trigger")},
          {"/cookiecamera action", "<idx> <add|rm|list> ...", this.lang().raw("help.action")},
          {"/cookiecamera script", "<name>", this.lang().raw("help.script")},
          {"/cookiecamera reload", "", this.lang().raw("help.reload")}
        };
    int totalPages = (cmds.length + 8 - 1) / 8;
    page = Math.max(1, Math.min(page, totalPages));
    sender.sendMessage(
        Component.text(
                "CookieCamera " + this.lang().raw("help.title"),
                NamedTextColor.GOLD,
                new TextDecoration[] {TextDecoration.BOLD})
            .append(
                Component.text(" (" + page + "/" + totalPages + ")", NamedTextColor.GRAY)
                    .decoration(TextDecoration.BOLD, false)));
    int start = (page - 1) * 8;
    int end = Math.min(start + 8, cmds.length);

    for (int i = start; i < end; i++) {
      String cmd = cmds[i][0];
      String params = cmds[i][1];
      String desc = cmds[i][2];
      Component line =
          Component.text(cmd, NamedTextColor.GOLD)
              .clickEvent(ClickEvent.suggestCommand(cmd + " "))
              .hoverEvent(
                  HoverEvent.showText(
                      Component.text(this.lang().raw("help.click-hint"), NamedTextColor.GRAY)));
      if (!params.isEmpty()) {
        line = line.append(Component.text(" " + params, NamedTextColor.WHITE));
      }

      line =
          line.append(Component.text(" - ", NamedTextColor.GRAY))
              .append(Component.text(desc, NamedTextColor.YELLOW));
      sender.sendMessage(line);
    }

    this.sendPageBar(sender, page, totalPages, "/cookiecamera help");
  }

  private void sendPageBar(CommandSender sender, int page, int totalPages, String cmdPrefix) {
    Component bar = Component.text("  ----", NamedTextColor.GRAY);
    if (page > 1) {
      bar =
          bar.append(
              Component.text("<< ", NamedTextColor.GRAY)
                  .append(
                      Component.text(this.lang().raw("page.prev"), NamedTextColor.GOLD)
                          .clickEvent(ClickEvent.runCommand(cmdPrefix + " " + (page - 1)))
                          .hoverEvent(
                              HoverEvent.showText(
                                  Component.text(this.lang().raw("page.goto", "page", page - 1)))))
                  .append(Component.text(" ", NamedTextColor.GRAY)));
    } else {
      bar =
          bar.append(
              Component.text("<< " + this.lang().raw("page.prev") + " ", NamedTextColor.DARK_GRAY));
    }

    bar =
        bar.append(Component.text(page, NamedTextColor.GOLD))
            .append(Component.text("/", NamedTextColor.GRAY))
            .append(Component.text(totalPages, NamedTextColor.GOLD));
    if (page < totalPages) {
      bar =
          bar.append(Component.text(" ", NamedTextColor.GRAY))
              .append(
                  Component.text(this.lang().raw("page.next"), NamedTextColor.GOLD)
                      .clickEvent(ClickEvent.runCommand(cmdPrefix + " " + (page + 1)))
                      .hoverEvent(
                          HoverEvent.showText(
                              Component.text(this.lang().raw("page.goto", "page", page + 1)))))
              .append(Component.text(" >>", NamedTextColor.GRAY));
    } else {
      bar =
          bar.append(
              Component.text(" " + this.lang().raw("page.next") + " >>", NamedTextColor.DARK_GRAY));
    }

    bar = bar.append(Component.text("----", NamedTextColor.GRAY));
    sender.sendMessage(bar);
  }

  private boolean handleAddScene(CommandSender sender, String[] args) {
    if (!this.checkPerm(sender, "cookiecamera.admin")) {
      return true;
    } else if (args.length < 2) {
      sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera new <id>"));
      return true;
    } else {
      String id = args[1];
      if (this.plugin.getSceneManager().getScene(id) != null) {
        sender.sendMessage(this.err("scene.already-exists", "id", id));
        return true;
      } else {
        this.plugin.getSceneManager().createScene(id);
        sender.sendMessage(this.ok("scene.created", "id", id));
        sender.sendMessage(
            Component.text("  ", NamedTextColor.GRAY)
                .append(
                    Component.text(
                            "[" + this.lang().raw("ui.enter-edit") + "]",
                            NamedTextColor.AQUA,
                            new TextDecoration[] {TextDecoration.BOLD})
                        .clickEvent(ClickEvent.runCommand("/cookiecamera edit " + id))
                        .hoverEvent(
                            HoverEvent.showText(
                                Component.text(this.lang().raw("ui.click-enter-edit"))))));
        return true;
      }
    }
  }

  private boolean handleDeleteScene(CommandSender sender, String[] args) {
    if (!this.checkPerm(sender, "cookiecamera.admin")) {
      return true;
    } else if (args.length < 2) {
      sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera del <id>"));
      return true;
    } else {
      if (this.plugin.getSceneManager().deleteScene(args[1])) {
        sender.sendMessage(this.ok("scene.deleted", "id", args[1]));
      } else {
        sender.sendMessage(this.err("scene.not-found"));
      }

      return true;
    }
  }

  private boolean handleListScenes(CommandSender sender, int page) {
    List<Scene> scenes = this.plugin.getSceneManager().getAllScenes().stream().toList();
    if (scenes.isEmpty()) {
      sender.sendMessage(this.ok("scene.no-scenes"));
      return true;
    } else {
      int totalPages = (scenes.size() + 8 - 1) / 8;
      page = Math.max(1, Math.min(page, totalPages));
      sender.sendMessage(Component.text(""));
      sender.sendMessage(
          Component.text(
              "  " + this.lang().raw("ui.scene-list"),
              NamedTextColor.GOLD,
              new TextDecoration[] {TextDecoration.BOLD}));
      sender.sendMessage(Component.text(""));
      int start = (page - 1) * 8;
      int end = Math.min(start + 8, scenes.size());

      for (int i = start; i < end; i++) {
        Scene s = scenes.get(i);
        double sec = s.getTotalDurationTicks() / 20.0;
        sender.sendMessage(
            Component.text("  ", NamedTextColor.GRAY)
                .append(
                    Component.text(s.getId(), NamedTextColor.YELLOW)
                        .clickEvent(ClickEvent.runCommand("/cookiecamera info " + s.getId()))
                        .hoverEvent(
                            HoverEvent.showText(
                                Component.text(this.lang().raw("ui.click-detail")))))
                .append(
                    Component.text(
                        " " + s.getPoints().size() + "pt " + String.format("%.1fs", sec),
                        NamedTextColor.GRAY))
                .append(
                    Component.text(" [▶]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/cookiecamera play " + s.getId()))
                        .hoverEvent(
                            HoverEvent.showText(Component.text(this.lang().raw("ui.play")))))
                .append(
                    Component.text(" [✎]", NamedTextColor.AQUA)
                        .clickEvent(ClickEvent.runCommand("/cookiecamera edit " + s.getId()))
                        .hoverEvent(
                            HoverEvent.showText(Component.text(this.lang().raw("ui.edit")))))
                .append(
                    Component.text(" [✕]", NamedTextColor.RED)
                        .clickEvent(ClickEvent.suggestCommand("/cookiecamera del " + s.getId()))
                        .hoverEvent(
                            HoverEvent.showText(Component.text(this.lang().raw("ui.delete"))))));
      }

      sender.sendMessage(Component.text(""));
      this.sendPageBar(sender, page, totalPages, "/cookiecamera list");
      return true;
    }
  }

  private boolean handleSwitchScene(CommandSender sender, String[] args) {
    if (!this.checkPerm(sender, "cookiecamera.admin")) {
      return true;
    } else if (sender instanceof Player player) {
      if (args.length < 2) {
        sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera edit <id>"));
        return true;
      } else {
        Scene scene = this.plugin.getSceneManager().getScene(args[1]);
        if (scene == null) {
          sender.sendMessage(this.err("scene.not-found"));
          return true;
        } else {
          EditSession.startEditing(player, scene, this.plugin);
          sender.sendMessage(this.ok("edit.enter", "id", scene.getId()));
          sender.sendMessage(
              Component.text("  " + this.lang().raw("ui.tools-hint"), NamedTextColor.GRAY));
          return true;
        }
      }
    } else {
      sender.sendMessage(this.err("player-only"));
      return true;
    }
  }

  private boolean handleExit(CommandSender sender) {
    if (sender instanceof Player player) {
      EditSession session = EditSession.getSession(player);
      if (session == null) {
        sender.sendMessage(this.err("edit.not-editing"));
        return true;
      } else {
        EditSession.stopEditing(player);
        sender.sendMessage(this.ok("edit.exit"));
        return true;
      }
    } else {
      return true;
    }
  }

  private boolean handleAddPoint(CommandSender sender) {
    if (sender instanceof Player player) {
      EditSession session = this.requireEdit(player);
      if (session == null) {
        return true;
      } else {
        Location loc = player.getLocation();
        Scene scene = session.getScene();
        scene.setWorld(loc.getWorld().getName());
        scene.addPoint(new CameraPoint(loc, -1, -1.0F, EaseType.LINEAR));
        this.plugin.getSceneManager().saveScene(scene);
        session.refreshMarkers();
        int idx = scene.getPoints().size() - 1;
        sender.sendMessage(this.ok("point.added", "idx", idx, "pos", this.fmtPos(loc)));
        sender.sendMessage(
            Component.text("  ", NamedTextColor.GRAY)
                .append(
                    Component.text("[" + this.lang().raw("ui.detail") + "]", NamedTextColor.AQUA)
                        .clickEvent(ClickEvent.runCommand("/cookiecamera pt " + idx))));
        return true;
      }
    } else {
      return true;
    }
  }

  private boolean handleInsertPoint(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      return true;
    } else {
      EditSession session = this.requireEdit(player);
      if (session == null) {
        return true;
      } else if (args.length < 2) {
        sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera insert <idx>"));
        return true;
      } else {
        try {
          int idx = Integer.parseInt(args[1]);
          Scene scene = session.getScene();
          if (idx < 0 || idx > scene.getPoints().size()) {
            sender.sendMessage(this.err("index-out-of-range"));
            return true;
          }

          Location loc = player.getLocation();
          scene.setWorld(loc.getWorld().getName());
          scene.getPoints().add(idx, new CameraPoint(loc, -1, -1.0F, EaseType.LINEAR));
          this.plugin.getSceneManager().saveScene(scene);
          session.refreshMarkers();
          sender.sendMessage(this.ok("point.inserted", "idx", idx));
        } catch (NumberFormatException ignored) {
          sender.sendMessage(this.err("invalid-index"));
        }

        return true;
      }
    }
  }

  private boolean handleRemovePoint(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      return true;
    } else {
      EditSession session = this.requireEdit(player);
      if (session == null) {
        return true;
      } else if (args.length < 2) {
        sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera rm <idx>"));
        return true;
      } else {
        try {
          int idx = Integer.parseInt(args[1]);
          Scene scene = session.getScene();
          if (idx < 0 || idx >= scene.getPoints().size()) {
            sender.sendMessage(this.err("index-out-of-range"));
            return true;
          }

          scene.removePoint(idx);
          this.plugin.getSceneManager().saveScene(scene);
          session.refreshMarkers();
          sender.sendMessage(
              this.ok("point.removed", "idx", idx, "count", scene.getPoints().size()));
        } catch (NumberFormatException ignored) {
          sender.sendMessage(this.err("invalid-index"));
        }

        return true;
      }
    }
  }

  private boolean handleClearPoints(CommandSender sender, String[] args) {
    if (!this.checkPerm(sender, "cookiecamera.admin")) {
      return true;
    } else {
      Scene scene;
      if (args.length >= 2) {
        scene = this.plugin.getSceneManager().getScene(args[1]);
      } else {
        if (!(sender instanceof Player player)) {
          sender.sendMessage(this.err("player-only"));
          return true;
        }

        EditSession session = this.requireEdit(player);
        if (session == null) {
          return true;
        }

        scene = session.getScene();
      }

      if (scene == null) {
        sender.sendMessage(this.err("scene.not-found"));
        return true;
      } else {
        int count = scene.getPoints().size();
        scene.clearPoints();
        this.plugin.getSceneManager().saveScene(scene);
        if (sender instanceof Player player) {
          EditSession session = EditSession.getSession(player);
          if (session != null && session.getScene().getId().equals(scene.getId())) {
            session.refreshMarkers();
          }
        }

        sender.sendMessage(this.ok("point.cleared", "count", count));
        return true;
      }
    }
  }

  private boolean handleSetPoint(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      return true;
    } else {
      EditSession session = this.requireEdit(player);
      if (session == null) {
        return true;
      } else if (args.length < 3) {
        sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera set <idx> <param> [val]"));
        return true;
      } else {
        try {
          int idx = Integer.parseInt(args[1]);
          Scene scene = session.getScene();
          if (idx < 0 || idx >= scene.getPoints().size()) {
            sender.sendMessage(this.err("index-out-of-range"));
            return true;
          }

          CameraPoint point = scene.getPoints().get(idx);
          String param = args[2].toLowerCase();
          String value = args.length >= 4 ? args[3] : null;
          switch (param) {
            case "duration":
            case "dur":
              point.setDurationTicks(value != null ? Integer.parseInt(value) : -1);
              this.plugin.getSceneManager().saveScene(scene);
              session.refreshMarkers();
              this.handlePointDetail(sender, new String[] {"pt", String.valueOf(idx)});
              return true;
            case "fov":
              point.setFov(value != null ? Float.parseFloat(value) : -1.0F);
              this.plugin.getSceneManager().saveScene(scene);
              session.refreshMarkers();
              this.handlePointDetail(sender, new String[] {"pt", String.valueOf(idx)});
              return true;
            case "yaw":
              point.setYaw(value != null ? Float.parseFloat(value) : player.getLocation().getYaw());
              break;
            case "pitch":
              point.setPitch(
                  value != null ? Float.parseFloat(value) : player.getLocation().getPitch());
              break;
            case "x":
              point.setX(value != null ? Double.parseDouble(value) : player.getLocation().getX());
              break;
            case "y":
              point.setY(value != null ? Double.parseDouble(value) : player.getLocation().getY());
              break;
            case "z":
              point.setZ(value != null ? Double.parseDouble(value) : player.getLocation().getZ());
              break;
            case "ease":
              if (value == null) {
                sender.sendMessage(
                    Component.text(
                        "  LINEAR, SMOOTH, EASE_IN, EASE_OUT, EASE_IN_OUT, HOLD",
                        NamedTextColor.GRAY));
                return true;
              }

              EaseType easeType = EaseType.fromString(value);
              if (easeType == null) {
                sender.sendMessage(
                    Component.text(
                        "[CookieCamera] Invalid ease type: " + value, NamedTextColor.RED));
                sender.sendMessage(
                    Component.text(
                        "  LINEAR, SMOOTH, EASE_IN, EASE_OUT, EASE_IN_OUT, HOLD",
                        NamedTextColor.GRAY));
                return true;
              }

              point.setEaseType(easeType);
              this.plugin.getSceneManager().saveScene(scene);
              session.refreshMarkers();
              this.handlePointDetail(sender, new String[] {"pt", String.valueOf(idx)});
              return true;
            case "pos":
              Location loc = player.getLocation();
              point.setX(loc.getX());
              point.setY(loc.getY());
              point.setZ(loc.getZ());
              point.setYaw(loc.getYaw());
              point.setPitch(loc.getPitch());
              this.plugin.getSceneManager().saveScene(scene);
              session.refreshMarkers();
              this.handlePointDetail(sender, new String[] {"pt", String.valueOf(idx)});
              return true;
            default:
              sender.sendMessage(this.err("setting.unknown", "param", param));
          }

          this.plugin.getSceneManager().saveScene(scene);
          session.refreshMarkers();
        } catch (NumberFormatException ignored) {
          sender.sendMessage(this.err("invalid-number"));
        } catch (IllegalArgumentException ignored) {
          sender.sendMessage(this.err("invalid-number"));
        }

        return true;
      }
    }
  }

  private boolean handlePoints(CommandSender sender, String[] args, int page) {
    Scene scene = this.resolveScene(sender, args, 1);
    if (scene == null) {
      return true;
    } else {
      List<CameraPoint> points = scene.getPoints();
      if (points.isEmpty()) {
        sender.sendMessage(this.ok("point.no-points"));
        return true;
      } else {
        int totalPages = (points.size() + 8 - 1) / 8;
        page = Math.max(1, Math.min(page, totalPages));
        sender.sendMessage(Component.text(""));
        sender.sendMessage(
            Component.text(
                "  "
                    + this.lang().raw("ui.points-list")
                    + ": "
                    + scene.getId()
                    + " ("
                    + points.size()
                    + ")",
                NamedTextColor.GOLD,
                new TextDecoration[] {TextDecoration.BOLD}));
        sender.sendMessage(Component.text(""));
        int start = (page - 1) * 8;
        int end = Math.min(start + 8, points.size());

        for (int i = start; i < end; i++) {
          CameraPoint p = points.get(i);
          int dur = scene.getEffectiveDuration(i);
          String easeShort =
              p.getEaseType().name().substring(0, Math.min(3, p.getEaseType().name().length()));
          sender.sendMessage(
              Component.text("  #" + i, NamedTextColor.YELLOW)
                  .clickEvent(ClickEvent.runCommand("/cookiecamera pt " + i))
                  .hoverEvent(
                      HoverEvent.showText(Component.text(this.lang().raw("ui.click-detail"))))
                  .append(
                      Component.text(
                          " " + String.format("%.0f,%.0f,%.0f", p.getX(), p.getY(), p.getZ()),
                          NamedTextColor.WHITE))
                  .append(
                      Component.text(" " + String.format("%.1fs", dur / 20.0), NamedTextColor.GRAY))
                  .append(Component.text(" " + easeShort, NamedTextColor.DARK_AQUA))
                  .append(
                      Component.text(" [✎]", NamedTextColor.AQUA)
                          .clickEvent(ClickEvent.runCommand("/cookiecamera pt " + i)))
                  .append(
                      Component.text(" [TP]", NamedTextColor.GREEN)
                          .clickEvent(ClickEvent.runCommand("/cookiecamera tp " + i)))
                  .append(
                      Component.text(" [✕]", NamedTextColor.RED)
                          .clickEvent(ClickEvent.suggestCommand("/cookiecamera rm " + i))));
        }

        sender.sendMessage(Component.text(""));
        this.sendPageBar(sender, page, totalPages, "/cookiecamera points " + scene.getId());
        return true;
      }
    }
  }

  private boolean handlePointDetail(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      return true;
    } else {
      EditSession session = this.requireEdit(player);
      if (session == null) {
        return true;
      } else if (args.length < 2) {
        sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera pt <idx>"));
        return true;
      } else {
        try {
          int idx = Integer.parseInt(args[1]);
          Scene scene = session.getScene();
          if (idx < 0 || idx >= scene.getPoints().size()) {
            sender.sendMessage(this.err("index-out-of-range"));
            return true;
          }

          CameraPoint p = scene.getPoints().get(idx);
          int dur = scene.getEffectiveDuration(idx);
          double sec = dur / 20.0;
          float fov = p.getFov();
          String fovText = fov < 0.0F ? this.lang().raw("ui.default") : String.valueOf((int) fov);
          int fovInt = fov < 0.0F ? 70 : (int) fov;
          sender.sendMessage(Component.text(""));
          sender.sendMessage(
              Component.text(
                      "  ● " + this.lang().raw("ui.point") + " #" + idx,
                      NamedTextColor.GOLD,
                      new TextDecoration[] {TextDecoration.BOLD})
                  .append(
                      Component.text(
                              "  "
                                  + String.format("%.1f, %.1f, %.1f", p.getX(), p.getY(), p.getZ()),
                              NamedTextColor.GRAY)
                          .decoration(TextDecoration.BOLD, false)));
          sender.sendMessage(
              Component.text("  " + this.lang().raw("ui.pos-dir") + " ", NamedTextColor.GRAY)
                  .append(
                      Component.text(
                          String.format("%.1f°/%.1f°", p.getYaw(), p.getPitch()),
                          NamedTextColor.WHITE))
                  .append(
                      Component.text(
                              " [" + this.lang().raw("ui.set-current") + "]", NamedTextColor.AQUA)
                          .clickEvent(ClickEvent.runCommand("/cookiecamera set " + idx + " pos"))
                          .hoverEvent(
                              HoverEvent.showText(
                                  Component.text(this.lang().raw("ui.set-current-hint"))))));
          sender.sendMessage(
              Component.text("  " + this.lang().raw("ui.duration") + " ", NamedTextColor.GRAY)
                  .append(
                      Component.text("<", NamedTextColor.RED)
                          .clickEvent(
                              ClickEvent.runCommand(
                                  "/cookiecamera set " + idx + " dur " + Math.max(1, dur - 20))))
                  .append(
                      Component.text("<", NamedTextColor.GOLD)
                          .clickEvent(
                              ClickEvent.runCommand(
                                  "/cookiecamera set " + idx + " dur " + Math.max(1, dur - 5))))
                  .append(
                      Component.text("<", NamedTextColor.YELLOW)
                          .clickEvent(
                              ClickEvent.runCommand(
                                  "/cookiecamera set " + idx + " dur " + Math.max(1, dur - 1))))
                  .append(
                      Component.text(
                              " " + dur + "t=" + String.format("%.1fs", sec) + " ",
                              NamedTextColor.WHITE)
                          .clickEvent(ClickEvent.suggestCommand("/cookiecamera set " + idx + " dur ")))
                  .append(
                      Component.text(">", NamedTextColor.YELLOW)
                          .clickEvent(
                              ClickEvent.runCommand("/cookiecamera set " + idx + " dur " + (dur + 1))))
                  .append(
                      Component.text(">", NamedTextColor.GOLD)
                          .clickEvent(
                              ClickEvent.runCommand("/cookiecamera set " + idx + " dur " + (dur + 5))))
                  .append(
                      Component.text(">", NamedTextColor.RED)
                          .clickEvent(
                              ClickEvent.runCommand("/cookiecamera set " + idx + " dur " + (dur + 20)))));
          sender.sendMessage(
              Component.text("  FOV ", NamedTextColor.GRAY)
                  .append(
                      Component.text("<", NamedTextColor.RED)
                          .clickEvent(
                              ClickEvent.runCommand(
                                  "/cookiecamera set " + idx + " fov " + Math.max(10, fovInt - 10))))
                  .append(
                      Component.text("<", NamedTextColor.GOLD)
                          .clickEvent(
                              ClickEvent.runCommand(
                                  "/cookiecamera set " + idx + " fov " + Math.max(10, fovInt - 5))))
                  .append(
                      Component.text("<", NamedTextColor.YELLOW)
                          .clickEvent(
                              ClickEvent.runCommand(
                                  "/cookiecamera set " + idx + " fov " + Math.max(10, fovInt - 1))))
                  .append(
                      Component.text(" " + fovText + " ", NamedTextColor.WHITE)
                          .clickEvent(ClickEvent.suggestCommand("/cookiecamera set " + idx + " fov ")))
                  .append(
                      Component.text(">", NamedTextColor.YELLOW)
                          .clickEvent(
                              ClickEvent.runCommand(
                                  "/cookiecamera set " + idx + " fov " + Math.min(160, fovInt + 1))))
                  .append(
                      Component.text(">", NamedTextColor.GOLD)
                          .clickEvent(
                              ClickEvent.runCommand(
                                  "/cookiecamera set " + idx + " fov " + Math.min(160, fovInt + 5))))
                  .append(
                      Component.text(">", NamedTextColor.RED)
                          .clickEvent(
                              ClickEvent.runCommand(
                                  "/cookiecamera set " + idx + " fov " + Math.min(160, fovInt + 10))))
                  .append(
                      Component.text(
                              " [" + this.lang().raw("ui.reset") + "]", NamedTextColor.DARK_GRAY)
                          .clickEvent(ClickEvent.runCommand("/cookiecamera set " + idx + " fov -1"))));
          sender.sendMessage(this.easeSelector(idx, p.getEaseType()));
          List<PointAction> actions = p.getActions();
          sender.sendMessage(
              Component.text("  " + this.lang().raw("ui.events") + " ", NamedTextColor.GRAY)
                  .append(Component.text("[" + actions.size() + "]", NamedTextColor.WHITE))
                  .append(
                      Component.text(" [+title]", NamedTextColor.GOLD)
                          .clickEvent(
                              ClickEvent.suggestCommand(
                                  "/cookiecamera action " + idx + " add title \"\" \"\"")))
                  .append(
                      Component.text(" [+cmd]", NamedTextColor.AQUA)
                          .clickEvent(
                              ClickEvent.suggestCommand(
                                  "/cookiecamera action " + idx + " add cmd say {player}")))
                  .append(
                      Component.text(" [+sound]", NamedTextColor.GREEN)
                          .clickEvent(
                              ClickEvent.suggestCommand(
                                  "/cookiecamera action "
                                      + idx
                                      + " add sound entity.experience_orb.pickup 1.0 1.0")))
                  .append(
                      Component.text(" [+...]", NamedTextColor.YELLOW)
                          .clickEvent(ClickEvent.suggestCommand("/cookiecamera action " + idx + " add "))));

          for (int ai = 0; ai < actions.size(); ai++) {
            PointAction a = actions.get(ai);
            sender.sendMessage(
                Component.text("    " + ai + ". ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(a.toShortString(), NamedTextColor.WHITE))
                    .append(
                        Component.text(" [✕]", NamedTextColor.RED)
                            .clickEvent(
                                ClickEvent.runCommand("/cookiecamera action " + idx + " rm " + ai))));
          }

          sender.sendMessage(Component.text(""));
          Component nav = Component.text("  ");
          if (idx > 0) {
            nav =
                nav.append(
                    Component.text("◀", NamedTextColor.YELLOW)
                        .clickEvent(ClickEvent.runCommand("/cookiecamera pt " + (idx - 1))));
          }

          nav =
              nav.append(
                      Component.text(" [TP]", NamedTextColor.GREEN)
                          .clickEvent(ClickEvent.runCommand("/cookiecamera tp " + idx)))
                  .append(
                      Component.text(" [" + this.lang().raw("ui.delete") + "]", NamedTextColor.RED)
                          .clickEvent(ClickEvent.suggestCommand("/cookiecamera rm " + idx)))
                  .append(
                      Component.text(" [" + this.lang().raw("ui.list") + "]", NamedTextColor.AQUA)
                          .clickEvent(ClickEvent.runCommand("/cookiecamera points")));
          if (idx < scene.getPoints().size() - 1) {
            nav =
                nav.append(
                    Component.text(" ▶", NamedTextColor.YELLOW)
                        .clickEvent(ClickEvent.runCommand("/cookiecamera pt " + (idx + 1))));
          }

          sender.sendMessage(nav);
        } catch (NumberFormatException ignored) {
          sender.sendMessage(this.err("invalid-index"));
        }

        return true;
      }
    }
  }

  private Component easeSelector(int idx, EaseType current) {
    Component line = Component.text("  " + this.lang().raw("ui.ease") + ": ", NamedTextColor.GRAY);

    for (EaseType e : EaseType.values()) {
      boolean active = e == current;
      NamedTextColor color = active ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY;
      String display = active ? "[" + e.name() + "]" : e.name();
      line =
          line.append(
                  Component.text(display, color)
                      .clickEvent(
                          ClickEvent.runCommand(
                              "/cookiecamera set " + idx + " ease " + e.name().toLowerCase())))
              .append(Component.text(" ", NamedTextColor.GRAY));
    }

    return line;
  }

  private boolean handleInfo(CommandSender sender, String[] args, int page) {
    Scene scene = this.resolveScene(sender, args, 1);
    if (scene == null) {
      return true;
    } else {
      sender.sendMessage(Component.text(""));
      sender.sendMessage(
          Component.text(
              "  " + this.lang().raw("ui.scene") + ": " + scene.getId(),
              NamedTextColor.GOLD,
              new TextDecoration[] {TextDecoration.BOLD}));
      sender.sendMessage(
          Component.text("  " + this.lang().raw("ui.name") + ": ", NamedTextColor.GRAY)
              .append(Component.text(scene.getDisplayName(), NamedTextColor.WHITE)));
      sender.sendMessage(
          Component.text("  " + this.lang().raw("ui.world") + ": ", NamedTextColor.GRAY)
              .append(Component.text(String.valueOf(scene.getWorld()), NamedTextColor.WHITE)));
      sender.sendMessage(
          Component.text("  " + this.lang().raw("ui.speed") + ": ", NamedTextColor.GRAY)
              .append(
                  Component.text(
                      scene.getDefaultSpeedTicks()
                          + "t ("
                          + String.format("%.1fs", scene.getDefaultSpeedTicks() / 20.0)
                          + ")",
                      NamedTextColor.WHITE)));
      sender.sendMessage(
          Component.text("  " + this.lang().raw("ui.total-dur") + ": ", NamedTextColor.GRAY)
              .append(
                  Component.text(
                      String.format("%.1fs", scene.getTotalDurationTicks() / 20.0),
                      NamedTextColor.WHITE)));
      sender.sendMessage(
          Component.text("  " + this.lang().raw("ui.point-count") + ": ", NamedTextColor.GRAY)
              .append(
                  Component.text(String.valueOf(scene.getPoints().size()), NamedTextColor.WHITE)));
      sender.sendMessage(
          Component.text("  " + this.lang().raw("ui.interp-mode") + ": ", NamedTextColor.GRAY)
              .append(Component.text(scene.getSmoothMode().name(), NamedTextColor.WHITE)));
      sender.sendMessage(Component.text(""));
      sender.sendMessage(
          Component.text("  ", NamedTextColor.GRAY)
              .append(
                  Component.text("[" + this.lang().raw("ui.view-points") + "]", NamedTextColor.AQUA)
                      .clickEvent(ClickEvent.runCommand("/cookiecamera points " + scene.getId())))
              .append(Component.text(" ", NamedTextColor.GRAY))
              .append(
                  Component.text("[" + this.lang().raw("ui.play") + "]", NamedTextColor.GREEN)
                      .clickEvent(ClickEvent.runCommand("/cookiecamera play " + scene.getId())))
              .append(Component.text(" ", NamedTextColor.GRAY))
              .append(
                  Component.text("[" + this.lang().raw("ui.edit") + "]", NamedTextColor.YELLOW)
                      .clickEvent(ClickEvent.runCommand("/cookiecamera edit " + scene.getId()))));
      return true;
    }
  }

  private boolean handleRecord(CommandSender sender, String[] args) {
    if (sender instanceof Player player) {
      EditSession session = this.requireEdit(player);
      if (session == null) {
        return true;
      } else if (args.length < 2) {
        sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera rec start|stop"));
        return true;
      } else {
        String action = args[1].toLowerCase();
        if (action.equals("start")) {
          if (session.isRecording()) {
            sender.sendMessage(this.ok("rec.already"));
            return true;
          }

          session.startRecording();
          sender.sendMessage(this.ok("rec.start"));
        } else if (action.equals("stop")) {
          if (!session.isRecording()) {
            sender.sendMessage(this.ok("rec.not-recording"));
            return true;
          }

          session.stopRecording();
          sender.sendMessage(this.ok("rec.stop", "count", session.getScene().getPoints().size()));
        } else {
          sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera rec start|stop"));
        }

        return true;
      }
    } else {
      return true;
    }
  }

  private boolean handleSceneSetting(CommandSender sender, String[] args) {
    if (sender instanceof Player player) {
      EditSession session = this.requireEdit(player);
      if (session == null) {
        return true;
      } else if (args.length < 3) {
        sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera setting <param> <val>"));
        sender.sendMessage(this.lang().get("setting.hint"));
        return true;
      } else {
        Scene scene = session.getScene();
        String param = args[1].toLowerCase();
        String value = args[2];
        switch (param) {
          case "speed":
            try {
              int t = Integer.parseInt(value);
              scene.setDefaultSpeedTicks(t);
              sender.sendMessage(
                  this.ok("setting.speed", "value", t, "sec", String.format("%.1f", t / 20.0)));
            } catch (NumberFormatException ignored) {
              sender.sendMessage(this.err("invalid-number"));
            }
            break;
          case "name":
            StringBuilder sb = new StringBuilder(value);

            for (int i = 3; i < args.length; i++) {
              sb.append(" ").append(args[i]);
            }

            scene.setDisplayName(sb.toString());
            sender.sendMessage(this.ok("setting.name", "value", scene.getDisplayName()));
            break;
          case "markers":
            boolean on = value.equalsIgnoreCase("on") || value.equalsIgnoreCase("true");
            scene.setShowMarkers(on);
            session.refreshMarkers();
            sender.sendMessage(this.ok("setting.markers", "value", on ? "ON" : "OFF"));
            break;
          case "mode":
            SmoothMode mode = SmoothMode.fromString(value);
            scene.setSmoothMode(mode);
            sender.sendMessage(this.ok("setting.mode", "value", mode.name()));
            sender.sendMessage(this.lang().get("setting.mode-hint"));
            break;
          case "tension":
            try {
              double t = Double.parseDouble(value);
              scene.setTension(t);
              sender.sendMessage(this.ok("setting.tension", "value", String.format("%.2f", t)));
            } catch (NumberFormatException ignored) {
              sender.sendMessage(this.err("invalid-number"));
            }
            break;
          case "smoothing":
            try {
              double s = Double.parseDouble(value);
              scene.setSmoothing(s);
              sender.sendMessage(this.ok("setting.smoothing", "value", String.format("%.2f", s)));
            } catch (NumberFormatException ignored) {
              sender.sendMessage(this.err("invalid-number"));
            }
            break;
          default:
            sender.sendMessage(this.err("setting.unknown", "param", param));
            sender.sendMessage(this.lang().get("setting.hint"));
        }

        this.plugin.getSceneManager().saveScene(scene);
        return true;
      }
    } else {
      return true;
    }
  }

  private boolean handlePlay(CommandSender sender, String[] args) {
    if (!this.checkPerm(sender, "cookiecamera.play")) {
      return true;
    } else if (args.length < 2) {
      sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera play <id> [player|all]"));
      return true;
    } else {
      Scene scene = this.plugin.getSceneManager().getScene(args[1]);
      if (scene == null) {
        sender.sendMessage(this.err("scene.not-found"));
        return true;
      } else if (scene.getPoints().size() < 2) {
        sender.sendMessage(this.err("point.need-2"));
        return true;
      } else {
        if (args.length >= 3) {
          if (!this.checkPerm(sender, "cookiecamera.play.other")) {
            return true;
          }

          String target = args[2];
          if (target.equalsIgnoreCase("all")) {
            int count = 0;

            for (Player p : Bukkit.getOnlinePlayers()) {
              if (this.plugin.getPlaybackManager().play(p, scene)) {
                count++;
              }
            }

            sender.sendMessage(this.ok("play.for-all", "id", scene.getId(), "count", count));
          } else {
            Player tp = Bukkit.getPlayerExact(target);
            if (tp == null) {
              sender.sendMessage(this.err("ui.player-offline"));
              return true;
            }

            if (this.plugin.getPlaybackManager().play(tp, scene)) {
              sender.sendMessage(
                  this.ok("play.for-player", "id", scene.getId(), "name", tp.getName()));
            } else {
              sender.sendMessage(this.err("play.already"));
            }
          }
        } else {
          if (!(sender instanceof Player player)) {
            sender.sendMessage(this.err("player-only"));
            return true;
          }

          if (this.plugin.getPlaybackManager().play(player, scene)) {
            sender.sendMessage(this.ok("play.start", "id", scene.getId()));
          } else {
            sender.sendMessage(this.err("play.already"));
          }
        }

        return true;
      }
    }
  }

  private boolean handleStop(CommandSender sender, String[] args) {
    if (args.length >= 2) {
      if (sender instanceof Player && !this.checkPerm(sender, "cookiecamera.play.other")) {
        return true;
      }

      String target = args[1];
      if (target.equalsIgnoreCase("all")) {
        this.plugin.getPlaybackManager().stopAll();
        sender.sendMessage(this.ok("play.stopped-all"));
      } else {
        Player tp = Bukkit.getPlayerExact(target);
        if (tp == null) {
          sender.sendMessage(this.err("ui.player-offline"));
          return true;
        }

        this.plugin.getPlaybackManager().stop(tp);
        sender.sendMessage(this.ok("play.stopped-player", "name", tp.getName()));
      }
    } else {
      if (!(sender instanceof Player player)) {
        sender.sendMessage(this.err("player-only"));
        return true;
      }

      if (!this.checkPerm(sender, "cookiecamera.play")) {
        return true;
      }

      this.plugin.getPlaybackManager().stop(player);
      sender.sendMessage(this.ok("play.stopped"));
    }

    return true;
  }

  private boolean handlePause(CommandSender sender) {
    if (sender instanceof Player player) {
      if (!this.checkPerm(sender, "cookiecamera.play")) {
        return true;
      }

      PlaybackManager.PlaybackSession session = this.plugin.getPlaybackManager().getSession(player);
      if (session == null) {
        sender.sendMessage(this.err("play.not-playing"));
        return true;
      } else {
        if (session.isPaused()) {
          this.plugin.getPlaybackManager().resume(player);
          sender.sendMessage(this.ok("play.resumed"));
        } else {
          this.plugin.getPlaybackManager().pause(player);
          sender.sendMessage(this.ok("play.paused"));
        }

        return true;
      }
    } else {
      return true;
    }
  }

  private boolean handleShowPath(CommandSender sender, String[] args) {
    if (sender instanceof Player player) {
      EditSession session = this.requireEdit(player);
      if (session == null) {
        return true;
      } else {
        if (args.length >= 2) {
          boolean on = args[1].equalsIgnoreCase("on");
          if (on) {
            session.startVisuals();
            sender.sendMessage(this.ok("path.on"));
          } else {
            session.stopVisuals();
            sender.sendMessage(this.ok("path.off"));
          }
        } else {
          session.stopVisuals();
          session.startVisuals();
          sender.sendMessage(this.ok("path.refresh"));
        }

        return true;
      }
    } else {
      return true;
    }
  }

  private boolean handleSmooth(CommandSender sender, String[] args) {
    if (!this.checkPerm(sender, "cookiecamera.admin")) {
      return true;
    } else {
      Scene scene = this.resolveScene(sender, args, 1);
      if (scene == null) {
        return true;
      } else {
        List<CameraPoint> points = scene.getPoints();
        if (points.size() < 3) {
          sender.sendMessage(this.err("smooth.need-3"));
          return true;
        } else {
          ArrayList<CameraPoint> smoothed = new ArrayList<>();

          for (int i = 0; i < points.size() - 1; i++) {
            CameraPoint a = points.get(i);
            CameraPoint b = points.get(i + 1);
            smoothed.add(a);
            int mdur = Math.max(a.getDurationTicks(), b.getDurationTicks()) / 2;
            smoothed.add(
                new CameraPoint(
                    (a.getX() + b.getX()) / 2.0,
                    (a.getY() + b.getY()) / 2.0,
                    (a.getZ() + b.getZ()) / 2.0,
                    (a.getYaw() + b.getYaw()) / 2.0F,
                    (a.getPitch() + b.getPitch()) / 2.0F,
                    mdur,
                    -1.0F,
                    EaseType.SMOOTH));
          }

          smoothed.add(points.get(points.size() - 1));
          scene.clearPoints();

          for (CameraPoint p : smoothed) {
            scene.addPoint(p);
          }

          this.plugin.getSceneManager().saveScene(scene);
          if (sender instanceof Player player) {
            EditSession s = EditSession.getSession(player);
            if (s != null) {
              s.refreshMarkers();
            }
          }

          sender.sendMessage(
              this.ok("smooth.done", "before", points.size(), "after", smoothed.size()));
          return true;
        }
      }
    }
  }

  private boolean handleReverse(CommandSender sender, String[] args) {
    if (!this.checkPerm(sender, "cookiecamera.admin")) {
      return true;
    } else {
      Scene scene = this.resolveScene(sender, args, 1);
      if (scene == null) {
        return true;
      } else {
        Collections.reverse(scene.getPoints());
        this.plugin.getSceneManager().saveScene(scene);
        if (sender instanceof Player player) {
          EditSession s = EditSession.getSession(player);
          if (s != null) {
            s.refreshMarkers();
          }
        }

        sender.sendMessage(this.ok("scene.reversed", "id", scene.getId()));
        return true;
      }
    }
  }

  private boolean handleCopy(CommandSender sender, String[] args) {
    if (!this.checkPerm(sender, "cookiecamera.admin")) {
      return true;
    } else if (args.length < 3) {
      sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera copy <src> <dst>"));
      return true;
    } else {
      Scene src = this.plugin.getSceneManager().getScene(args[1]);
      if (src == null) {
        sender.sendMessage(this.err("scene.not-found"));
        return true;
      } else if (this.plugin.getSceneManager().getScene(args[2]) != null) {
        sender.sendMessage(this.err("scene.already-exists", "id", args[2]));
        return true;
      } else {
        Scene copy = this.plugin.getSceneManager().createScene(args[2]);
        copy.setDisplayName(src.getDisplayName());
        copy.setDefaultSpeedTicks(src.getDefaultSpeedTicks());
        copy.setShowMarkers(src.isShowMarkers());
        copy.setWorld(src.getWorld());

        for (CameraPoint p : src.getPoints()) {
          copy.addPoint(
              new CameraPoint(
                  p.getX(),
                  p.getY(),
                  p.getZ(),
                  p.getYaw(),
                  p.getPitch(),
                  p.getDurationTicks(),
                  p.getFov(),
                  p.getEaseType()));
        }

        this.plugin.getSceneManager().saveScene(copy);
        sender.sendMessage(this.ok("scene.copied", "src", src.getId(), "dst", copy.getId()));
        return true;
      }
    }
  }

  private boolean handleTpToPoint(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      return true;
    } else {
      EditSession session = this.requireEdit(player);
      if (session == null) {
        return true;
      } else if (args.length < 2) {
        sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera tp <idx>"));
        return true;
      } else {
        try {
          int idx = Integer.parseInt(args[1]);
          Scene scene = session.getScene();
          if (idx < 0 || idx >= scene.getPoints().size()) {
            sender.sendMessage(this.err("index-out-of-range"));
            return true;
          }

          Scheduler.teleport(player, scene.getPoints().get(idx).toLocation(player.getWorld()), null);
          sender.sendMessage(this.ok("point.tp", "idx", idx));
        } catch (NumberFormatException ignored) {
          sender.sendMessage(this.err("invalid-index"));
        }

        return true;
      }
    }
  }

  private boolean handleExport(CommandSender sender, String[] args) {
    if (!this.checkPerm(sender, "cookiecamera.admin")) {
      return true;
    } else if (args.length < 2) {
      sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera export <id>"));
      return true;
    } else {
      Scene scene = this.plugin.getSceneManager().getScene(args[1]);
      if (scene == null) {
        sender.sendMessage(this.err("scene.not-found"));
        return true;
      } else {
        try {
          File dir = new File(this.plugin.getDataFolder(), "exports");
          if (!dir.exists()) {
            dir.mkdirs();
          }

          File file = new File(dir, scene.getId() + ".yml");
          YamlConfiguration cfg = new YamlConfiguration();
          cfg.set("id", scene.getId());
          cfg.set("displayName", scene.getDisplayName());
          cfg.set("defaultSpeedTicks", scene.getDefaultSpeedTicks());
          cfg.set("world", scene.getWorld());
          cfg.set("smoothMode", scene.getSmoothMode().name());
          cfg.set("tension", scene.getTension());
          cfg.set("smoothing", scene.getSmoothing());
          ArrayList<Map<String, Object>> pts = new ArrayList<>();

          for (CameraPoint p : scene.getPoints()) {
            pts.add(p.serialize());
          }

          cfg.set("points", pts);
          cfg.save(file);
          sender.sendMessage(this.ok("export.done", "id", scene.getId()));
        } catch (IOException exception) {
          sender.sendMessage(this.err("export.fail", "error", exception.getMessage()));
        }

        return true;
      }
    }
  }

  private boolean handleImport(CommandSender sender, String[] args) {
    if (!this.checkPerm(sender, "cookiecamera.admin")) {
      return true;
    } else if (args.length < 2) {
      sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera import <file>"));
      return true;
    } else {
      String fileName = args[1].endsWith(".yml") ? args[1] : args[1] + ".yml";
      File file = new File(this.plugin.getDataFolder(), "imports/" + fileName);
      if (!file.exists()) {
        file = new File(this.plugin.getDataFolder(), "exports/" + fileName);
      }

      if (!file.exists()) {
        sender.sendMessage(this.err("import.not-found"));
        return true;
      } else {
        try {
          YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
          String id = cfg.getString("id");
          if (id == null) {
            sender.sendMessage(this.err("import.invalid"));
            return true;
          }

          Scene scene = this.plugin.getSceneManager().createScene(id);
          scene.setDisplayName(cfg.getString("displayName", id));
          scene.setDefaultSpeedTicks(cfg.getInt("defaultSpeedTicks", 40));
          scene.setWorld(cfg.getString("world", "world"));
          scene.setSmoothMode(SmoothMode.fromString(cfg.getString("smoothMode", "CATMULL_ROM")));
          scene.setTension(cfg.getDouble("tension", 0.0));
          scene.setSmoothing(cfg.getDouble("smoothing", 0.0));

          for (Map<?, ?> raw : cfg.getMapList("points")) {
            scene.addPoint(CameraPoint.deserialize(castConfigMap(raw)));
          }

          this.plugin.getSceneManager().saveScene(scene);
          sender.sendMessage(this.ok("import.done", "id", id, "count", scene.getPoints().size()));
        } catch (Exception exception) {
          sender.sendMessage(this.err("import.invalid"));
        }

        return true;
      }
    }
  }

  private boolean handleTrigger(CommandSender sender, String[] args) {
    if (!this.checkPerm(sender, "cookiecamera.admin")) {
      return true;
    } else if (args.length < 2) {
      sender.sendMessage(
          this.err("invalid-usage", "cmd", "/cookiecamera trigger <add|del|list|set|info|fire>"));
      return true;
    } else {
      String sub = args[1].toLowerCase();
      switch (sub) {
        case "add":
          if (args.length < 3) {
            sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera trigger add <id>"));
            return true;
          }

          String id = args[2];
          if (this.plugin.getTriggerManager().getTrigger(id) != null) {
            sender.sendMessage(this.err("trigger.already-exists", "id", id));
            return true;
          }

          this.plugin.getTriggerManager().createTrigger(id);
          sender.sendMessage(this.ok("trigger.created", "id", id));
          break;
        case "del":
        case "delete":
          if (args.length < 3) {
            sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera trigger del <id>"));
            return true;
          }

          if (this.plugin.getTriggerManager().deleteTrigger(args[2])) {
            sender.sendMessage(this.ok("trigger.deleted", "id", args[2]));
          } else {
            sender.sendMessage(this.err("trigger.not-found"));
          }
          break;
        case "list":
          Collection<Trigger> triggers = this.plugin.getTriggerManager().getAllTriggers();
          if (triggers.isEmpty()) {
            sender.sendMessage(this.ok("trigger.no-triggers"));
            return true;
          }

          sender.sendMessage(
              Component.text(
                  "  " + this.lang().raw("trigger.list-title"),
                  NamedTextColor.GOLD,
                  new TextDecoration[] {TextDecoration.BOLD}));

          for (Trigger trigger : triggers) {
            sender.sendMessage(
                Component.text("  " + trigger.getId(), NamedTextColor.YELLOW)
                    .append(Component.text(" " + trigger.getType().name(), NamedTextColor.GRAY))
                    .append(
                        Component.text(
                            " -> " + this.describeTriggerTarget(trigger), NamedTextColor.WHITE)));
          }
          break;
        case "info":
          if (args.length < 3) {
            sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera trigger info <id>"));
            return true;
          }

          Trigger infoTrigger = this.plugin.getTriggerManager().getTrigger(args[2]);
          if (infoTrigger == null) {
            sender.sendMessage(this.err("trigger.not-found"));
            return true;
          }

          this.sendTriggerInfo(sender, infoTrigger);
          break;
        case "set":
          if (args.length < 5) {
            sender.sendMessage(
                this.err("invalid-usage", "cmd", "/cookiecamera trigger set <id> <param> <value>"));
            return true;
          }

          Trigger trigger = this.plugin.getTriggerManager().getTrigger(args[2]);
          if (trigger == null) {
            sender.sendMessage(this.err("trigger.not-found"));
            return true;
          }

          String param = args[3].toLowerCase();
          String value = param.equals("command") ? this.joinArgs(args, 4) : args[4];
          switch (param) {
            case "type":
              Trigger.TriggerType triggerType = Trigger.TriggerType.fromString(value);
              if (triggerType == null) {
                sender.sendMessage(
                    Component.text(
                        "[CookieCamera] Invalid trigger type: " + value, NamedTextColor.RED));
                sender.sendMessage(
                    Component.text(
                        "  REGION_ENTER, RIGHT_CLICK_BLOCK, COMMAND", NamedTextColor.GRAY));
                return true;
              }

              trigger.setType(triggerType);
              break;
            case "scene":
              trigger.setSceneId(value);
              break;
            case "command":
              trigger.setCommandPattern(value);
              break;
            case "world":
              trigger.setWorld(value);
              break;
            case "cooldown":
              try {
                trigger.setCooldownTicks(Integer.parseInt(value));
                break;
              } catch (NumberFormatException ignored) {
                sender.sendMessage(this.err("invalid-number"));
                return true;
              }
            case "onetime":
              trigger.setOneTime(value.equalsIgnoreCase("true"));
              break;
            case "perm":
              trigger.setRequiredPermission(value);
              break;
            case "hand":
              trigger.setRequiredItemInHand(value);
              break;
            case "inv":
              trigger.setRequiredItemInInv(value);
              break;
            default:
              sender.sendMessage(this.err("setting.unknown", "param", param));
              return true;
          }

          this.plugin.getTriggerManager().saveAll();
          sender.sendMessage(
              this.ok("trigger.updated", "id", trigger.getId(), "param", param, "value", value));
          break;
        case "setregion":
          if (!(sender instanceof Player player)) {
            sender.sendMessage(this.err("player-only"));
            return true;
          }

          if (args.length < 3) {
            sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera trigger setregion <id>"));
            return true;
          }

          Trigger regionTrigger = this.plugin.getTriggerManager().getTrigger(args[2]);
          if (regionTrigger == null) {
            sender.sendMessage(this.err("trigger.not-found"));
            return true;
          }

          Location loc = player.getLocation();
          regionTrigger.setType(Trigger.TriggerType.REGION_ENTER);
          regionTrigger.setWorld(loc.getWorld().getName());
          regionTrigger.setRegion(
              loc.getX() - 5.0,
              loc.getY() - 3.0,
              loc.getZ() - 5.0,
              loc.getX() + 5.0,
              loc.getY() + 3.0,
              loc.getZ() + 5.0);
          this.plugin.getTriggerManager().saveAll();
          sender.sendMessage(this.ok("trigger.region-set", "id", regionTrigger.getId()));
          break;
        case "setblock":
          if (!(sender instanceof Player player)) {
            sender.sendMessage(this.err("player-only"));
            return true;
          }

          if (args.length < 3) {
            sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera trigger setblock <id>"));
            return true;
          }

          Trigger blockTrigger = this.plugin.getTriggerManager().getTrigger(args[2]);
          if (blockTrigger == null) {
            sender.sendMessage(this.err("trigger.not-found"));
            return true;
          }

          Block block = player.getTargetBlockExact(5);
          if (block == null) {
            sender.sendMessage(this.err("trigger.no-block"));
            return true;
          }

          blockTrigger.setType(Trigger.TriggerType.RIGHT_CLICK_BLOCK);
          blockTrigger.setWorld(block.getWorld().getName());
          blockTrigger.setBlock(block.getX(), block.getY(), block.getZ());
          this.plugin.getTriggerManager().saveAll();
          sender.sendMessage(
              this.ok(
                  "trigger.block-set",
                  "id",
                  blockTrigger.getId(),
                  "pos",
                  block.getX() + "," + block.getY() + "," + block.getZ()));
          break;
        case "fire":
          if (!(sender instanceof Player player)) {
            sender.sendMessage(this.err("player-only"));
            return true;
          }

          if (args.length < 3) {
            sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera trigger fire <id>"));
            return true;
          }

          Trigger fireTrigger = this.plugin.getTriggerManager().getTrigger(args[2]);
          if (fireTrigger == null) {
            sender.sendMessage(this.err("trigger.not-found"));
            return true;
          }

          this.plugin.getTriggerManager().fireTrigger(player, fireTrigger);
          sender.sendMessage(this.ok("trigger.fired", "id", fireTrigger.getId()));
          break;
        default:
          sender.sendMessage(
              this.err("invalid-usage", "cmd", "/cookiecamera trigger <add|del|list|set|fire>"));
      }

      return true;
    }
  }

  private boolean handleRunScript(CommandSender sender, String[] args) {
    if (!this.checkPerm(sender, "cookiecamera.play")) {
      return true;
    } else if (sender instanceof Player player) {
      if (args.length < 2) {
        sender.sendMessage(this.err("invalid-usage", "cmd", "/cookiecamera script <name>"));
        return true;
      } else {
        this.plugin.getScriptEngine().execute(player, args[1]);
        return true;
      }
    } else {
      sender.sendMessage(this.err("player-only"));
      return true;
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> castConfigMap(Map<?, ?> raw) {
    return (Map<String, Object>) raw;
  }

  private boolean handleReload(CommandSender sender) {
    if (!this.checkPerm(sender, "cookiecamera.admin")) {
      return true;
    } else {
      this.plugin.reloadConfig();
      this.plugin.getLang().load();
      this.plugin.getSceneManager().loadAll();
      this.plugin.getTriggerManager().loadAll();
      this.plugin.getScriptEngine().loadAll();
      sender.sendMessage(this.ok("reload.done"));
      return true;
    }
  }

  private boolean handleAction(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      return true;
    } else {
      EditSession session = this.requireEdit(player);
      if (session == null) {
        return true;
      } else if (args.length < 3) {
        sender.sendMessage(
            this.err("invalid-usage", "cmd", "/cookiecamera action <idx> <add|rm|list> ..."));
        return true;
      } else {
        try {
          int idx = Integer.parseInt(args[1]);
          Scene scene = session.getScene();
          if (idx < 0 || idx >= scene.getPoints().size()) {
            sender.sendMessage(this.err("index-out-of-range"));
            return true;
          }

          CameraPoint point = scene.getPoints().get(idx);
          String sub = args[2].toLowerCase();
          switch (sub) {
            case "add":
              if (args.length < 5) {
                sender.sendMessage(
                    this.err("invalid-usage", "cmd", "/cookiecamera action <idx> add <type> <value...>"));
                return true;
              }

              String typeName = args[3].toUpperCase();

              PointAction.ActionType actionType;
              try {
                actionType = PointAction.ActionType.valueOf(typeName);
              } catch (IllegalArgumentException ignored) {
                sender.sendMessage(this.err("action.unknown-type", "type", typeName));
                return true;
              }

              String[] quoted = this.parseQuotedArgs(args, 4);
              String val = quoted.length > 0 ? quoted[0] : "";
              String extra = quoted.length > 1 ? quoted[1] : null;
              String extra2 = quoted.length > 2 ? quoted[2] : null;
              point.getActions().add(new PointAction(actionType, val, extra, extra2));
              this.plugin.getSceneManager().saveScene(scene);
              sender.sendMessage(this.ok("action.added", "type", typeName, "idx", idx));
              this.handlePointDetail(sender, new String[] {"pt", String.valueOf(idx)});
              break;
            case "rm":
            case "remove":
              if (args.length < 4) {
                sender.sendMessage(
                    this.err("invalid-usage", "cmd", "/cookiecamera action <idx> rm <actionIdx>"));
                return true;
              }

              int ai = Integer.parseInt(args[3]);
              if (ai >= 0 && ai < point.getActions().size()) {
                point.getActions().remove(ai);
                this.plugin.getSceneManager().saveScene(scene);
                sender.sendMessage(this.ok("action.removed", "idx", ai));
                this.handlePointDetail(sender, new String[] {"pt", String.valueOf(idx)});
                break;
              }

              sender.sendMessage(this.err("index-out-of-range"));
              return true;
            case "list":
              List<PointAction> actions = point.getActions();
              if (actions.isEmpty()) {
                sender.sendMessage(this.ok("action.no-actions"));
                return true;
              }

              sender.sendMessage(
                  Component.text(
                      "  " + this.lang().raw("action.list-title", "idx", idx),
                      NamedTextColor.GOLD));

              for (int i = 0; i < actions.size(); i++) {
                sender.sendMessage(
                    Component.text("  " + i + ". ", NamedTextColor.DARK_GRAY)
                        .append(
                            Component.text(actions.get(i).toShortString(), NamedTextColor.WHITE)));
              }
              break;
            default:
              sender.sendMessage(
                  this.err("invalid-usage", "cmd", "/cookiecamera action <idx> <add|rm|list>"));
          }
        } catch (NumberFormatException ignored) {
          sender.sendMessage(this.err("invalid-index"));
        }

        return true;
      }
    }
  }

  private String joinArgs(String[] args, int from) {
    StringBuilder sb = new StringBuilder();

    for (int i = from; i < args.length; i++) {
      if (i > from) {
        sb.append(" ");
      }

      sb.append(args[i]);
    }

    return sb.toString();
  }

  private String[] parseQuotedArgs(String[] args, int from) {
    String joined = this.joinArgs(args, from);
    ArrayList<String> result = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuote = false;

    for (int i = 0; i < joined.length(); i++) {
      char c = joined.charAt(i);
      if (c == '"') {
        inQuote = !inQuote;
      } else if (c != ' ' || inQuote) {
        current.append(c);
      } else if (current.length() > 0) {
        result.add(current.toString());
        current.setLength(0);
      }
    }

    if (current.length() > 0) {
      result.add(current.toString());
    }

    return result.toArray(new String[0]);
  }

  private EditSession requireEdit(Player player) {
    EditSession session = EditSession.getSession(player);
    if (session == null) {
      player.sendMessage(this.err("edit.not-editing"));
    }

    return session;
  }

  private boolean checkPerm(CommandSender sender, String perm) {
    if (!sender.hasPermission(perm)) {
      sender.sendMessage(this.err("no-permission"));
      return false;
    } else {
      return true;
    }
  }

  private Scene resolveScene(CommandSender sender, String[] args, int argIdx) {
    if (args.length > argIdx) {
      Scene scene = this.plugin.getSceneManager().getScene(args[argIdx]);
      if (scene == null) {
        sender.sendMessage(this.err("scene.not-found"));
      }

      return scene;
    } else if (sender instanceof Player player) {
      EditSession session = this.requireEdit(player);
      return session != null ? session.getScene() : null;
    } else {
      sender.sendMessage(this.err("player-only"));
      return null;
    }
  }

  private String fmtPos(Location loc) {
    return String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
  }

  private int argInt(String[] args, int idx, int def) {
    if (args.length <= idx) {
      return def;
    } else {
      try {
        return Integer.parseInt(args[idx]);
      } catch (NumberFormatException ignored) {
        return def;
      }
    }
  }

  private String describeTriggerTarget(Trigger trigger) {
    List<String> targets = new ArrayList<>();
    if (trigger.getSceneId() != null && !trigger.getSceneId().isBlank()) {
      targets.add("scene=" + trigger.getSceneId());
    }

    if (trigger.getCommandPattern() != null && !trigger.getCommandPattern().isBlank()) {
      targets.add("command=/" + trigger.getCommandPattern());
    }

    return targets.isEmpty() ? "-" : String.join(", ", targets);
  }

  private void sendTriggerInfo(CommandSender sender, Trigger trigger) {
    sender.sendMessage(Component.text(""));
    sender.sendMessage(
        Component.text(
            "  Trigger " + trigger.getId(),
            NamedTextColor.GOLD,
            new TextDecoration[] {TextDecoration.BOLD}));
    sender.sendMessage(
        Component.text("  Type: ", NamedTextColor.GRAY)
            .append(Component.text(trigger.getType().name(), NamedTextColor.WHITE)));
    sender.sendMessage(
        Component.text("  Target: ", NamedTextColor.GRAY)
            .append(Component.text(this.describeTriggerTarget(trigger), NamedTextColor.WHITE)));
    if (trigger.getWorld() != null && !trigger.getWorld().isBlank()) {
      sender.sendMessage(
          Component.text("  World: ", NamedTextColor.GRAY)
              .append(Component.text(trigger.getWorld(), NamedTextColor.WHITE)));
    }

    switch (trigger.getType()) {
      case REGION_ENTER:
        sender.sendMessage(
            Component.text("  Region: ", NamedTextColor.GRAY)
                .append(
                    Component.text(
                        String.format(
                            "%.1f %.1f %.1f -> %.1f %.1f %.1f",
                            trigger.getX1(),
                            trigger.getY1(),
                            trigger.getZ1(),
                            trigger.getX2(),
                            trigger.getY2(),
                            trigger.getZ2()),
                        NamedTextColor.WHITE)));
        break;
      case RIGHT_CLICK_BLOCK:
        sender.sendMessage(
            Component.text("  Block: ", NamedTextColor.GRAY)
                .append(
                    Component.text(
                        trigger.getBlockX()
                            + ", "
                            + trigger.getBlockY()
                            + ", "
                            + trigger.getBlockZ(),
                        NamedTextColor.WHITE)));
        break;
      case COMMAND:
        if (trigger.getCommandPattern() != null && !trigger.getCommandPattern().isBlank()) {
          sender.sendMessage(
              Component.text("  Match: ", NamedTextColor.GRAY)
                  .append(Component.text("/" + trigger.getCommandPattern(), NamedTextColor.WHITE)));
        }
        break;
      default:
        break;
    }

    sender.sendMessage(
        Component.text("  Cooldown: ", NamedTextColor.GRAY)
            .append(Component.text(trigger.getCooldownTicks() + "t", NamedTextColor.WHITE)));
    sender.sendMessage(
        Component.text("  One-time: ", NamedTextColor.GRAY)
            .append(Component.text(String.valueOf(trigger.isOneTime()), NamedTextColor.WHITE)));
    if (trigger.getRequiredPermission() != null && !trigger.getRequiredPermission().isBlank()) {
      sender.sendMessage(
          Component.text("  Permission: ", NamedTextColor.GRAY)
              .append(Component.text(trigger.getRequiredPermission(), NamedTextColor.WHITE)));
    }

    if (trigger.getRequiredItemInHand() != null && !trigger.getRequiredItemInHand().isBlank()) {
      sender.sendMessage(
          Component.text("  Hand item: ", NamedTextColor.GRAY)
              .append(Component.text(trigger.getRequiredItemInHand(), NamedTextColor.WHITE)));
    }

    if (trigger.getRequiredItemInInv() != null && !trigger.getRequiredItemInInv().isBlank()) {
      sender.sendMessage(
          Component.text("  Inventory item: ", NamedTextColor.GRAY)
              .append(Component.text(trigger.getRequiredItemInInv(), NamedTextColor.WHITE)));
    }

    if (!trigger.getCommands().isEmpty()) {
      sender.sendMessage(
          Component.text("  Console commands:", NamedTextColor.GRAY)
              .append(Component.text(" " + trigger.getCommands().size(), NamedTextColor.WHITE)));
      for (String command : trigger.getCommands()) {
        sender.sendMessage(Component.text("    - " + command, NamedTextColor.WHITE));
      }
    }
  }
}


