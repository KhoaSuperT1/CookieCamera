package com.cookiecamera.command;

import com.cookiecamera.CookieCamera;
import com.cookiecamera.manager.EditSession;
import com.cookiecamera.model.EaseType;
import com.cookiecamera.model.Scene;
import com.cookiecamera.model.SmoothMode;
import com.cookiecamera.model.Trigger;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CookieTabCompleter implements TabCompleter {
  private static final List<String> SUBCOMMANDS =
      List.of(
          "help", "new", "del", "list", "edit", "exit", "add", "insert", "rm", "clear", "set",
          "info", "points", "pt", "rec", "setting", "play", "stop", "pause", "path", "smooth",
          "reverse", "copy", "tp", "export", "import", "trigger", "action", "script", "reload");
  private static final List<String> SET_PARAMS =
      List.of("duration", "dur", "fov", "yaw", "pitch", "x", "y", "z", "ease", "pos");
  private static final List<String> SCENE_SETTINGS =
      List.of("speed", "markers", "name", "mode", "tension", "smoothing");
  private static final List<String> TRIGGER_SUBS =
      List.of("add", "del", "delete", "list", "set", "info", "setregion", "setblock", "fire");
  private static final List<String> TRIGGER_PARAMS =
      List.of("type", "scene", "command", "world", "hand", "inv", "perm", "cooldown", "onetime");
  private final CookieCamera plugin;

  public CookieTabCompleter(CookieCamera plugin) {
    this.plugin = plugin;
  }

  @Nullable
  public List<String> onTabComplete(
      @NotNull CommandSender sender,
      @Nullable Command command,
      @NotNull String label,
      @NotNull String[] args) {
    if (!this.plugin.canUseCommands(sender)) {
      return List.of();
    }

    if (args.length == 0) {
      return this.filter(SUBCOMMANDS, "");
    } else if (args.length == 1) {
      return this.filter(SUBCOMMANDS, args[0]);
    } else {
      String sub = args[0].toLowerCase();
      List<String> sceneIds = new ArrayList<>(this.plugin.getSceneManager().getSceneIds());

      return switch (sub) {
        case "new" -> List.of();
        case "del", "edit", "smooth", "info", "export", "reverse" ->
            args.length == 2 ? this.filter(sceneIds, args[1]) : List.of();
        case "copy" -> args.length == 2 ? this.filter(sceneIds, args[1]) : List.of();
        case "tp", "insert", "rm", "pt" -> args.length == 2 ? this.pointIndices(sender) : List.of();
        case "clear" -> args.length == 2 ? this.filter(sceneIds, args[1]) : List.of();
        case "points" -> args.length == 2 ? this.filter(sceneIds, args[1]) : List.of();
        case "play" -> {
          if (args.length == 2) {
            yield this.filter(sceneIds, args[1]);
          } else if (args.length == 3) {
            List<String> options = new ArrayList<>();
            options.add("all");
            this.plugin.getServer().getOnlinePlayers().forEach(p -> options.add(p.getName()));
            yield this.filter(options, args[2]);
          } else {
            yield List.of();
          }
        }
        case "stop" -> {
          if (args.length == 2) {
            List<String> options = new ArrayList<>();
            options.add("all");
            this.plugin.getServer().getOnlinePlayers().forEach(p -> options.add(p.getName()));
            yield this.filter(options, args[1]);
          } else {
            yield List.of();
          }
        }
        case "rec" -> args.length == 2 ? this.filter(List.of("start", "stop"), args[1]) : List.of();
        case "setting" -> {
          if (args.length == 2) {
            yield this.filter(SCENE_SETTINGS, args[1]);
          } else {
            if (args.length == 3) {
              String settingParam = args[1].toLowerCase();
              if (settingParam.equals("markers")) {
                yield this.filter(List.of("on", "off"), args[2]);
              }

              if (settingParam.equals("mode")) {
                yield this.filter(
                    Arrays.stream(SmoothMode.values())
                        .map(m -> m.name().toLowerCase())
                        .collect(Collectors.toList()),
                    args[2]);
              }
            }

            yield List.of();
          }
        }
        case "set" ->
            args.length == 2
                ? this.pointIndices(sender)
                : (args.length == 3
                    ? this.filter(SET_PARAMS, args[2])
                    : (args.length == 4 && args[2].equalsIgnoreCase("ease")
                        ? this.filter(
                            Arrays.stream(EaseType.values())
                                .map(e -> e.name().toLowerCase())
                                .collect(Collectors.toList()),
                            args[3])
                        : List.of()));
        case "path" -> args.length == 2 ? this.filter(List.of("on", "off"), args[1]) : List.of();
        case "trigger" -> {
          if (args.length == 2) {
            yield this.filter(TRIGGER_SUBS, args[1]);
          } else {
            String trigSub = args[1].toLowerCase();
            if (args.length != 3
                || !trigSub.equals("delete")
                    && !trigSub.equals("del")
                    && !trigSub.equals("set")
                    && !trigSub.equals("info")
                    && !trigSub.equals("setregion")
                    && !trigSub.equals("setblock")
                    && !trigSub.equals("fire")) {
              if (args.length == 4 && trigSub.equals("set")) {
                yield this.filter(TRIGGER_PARAMS, args[3]);
              } else {
                if (args.length == 5 && trigSub.equals("set")) {
                  String param = args[3].toLowerCase();
                  if (param.equals("type")) {
                    yield this.filter(
                        List.of("REGION_ENTER", "RIGHT_CLICK_BLOCK", "COMMAND"), args[4]);
                  }

                  if (param.equals("scene")) {
                    yield this.filter(sceneIds, args[4]);
                  }

                  if (param.equals("onetime")) {
                    yield this.filter(List.of("true", "false"), args[4]);
                  }
                }

                yield List.of();
              }
            } else {
              yield this.filter(
                  this.plugin.getTriggerManager().getAllTriggers().stream()
                      .map(Trigger::getId)
                      .collect(Collectors.toList()),
                  args[2]);
            }
          }
        }
        case "script" -> {
          if (args.length == 2) {
            List<String> names = new ArrayList<>();
            names.addAll(this.plugin.getScriptEngine().getSceneBlocks().keySet());
            names.addAll(this.plugin.getScriptEngine().getChainBlocks().keySet());
            yield this.filter(names, args[1]);
          } else {
            yield List.of();
          }
        }
        case "action" ->
            args.length == 2
                ? this.pointIndices(sender)
                : (args.length == 3
                    ? this.filter(List.of("add", "rm", "remove", "list"), args[2])
                    : (args.length == 4 && args[2].equalsIgnoreCase("add")
                        ? this.filter(
                            List.of(
                                "title", "actionbar", "sound", "cmd", "playercmd", "script", "msg"),
                            args[3])
                        : (args.length != 4
                                || !args[2].equalsIgnoreCase("rm")
                                    && !args[2].equalsIgnoreCase("remove")
                            ? List.of()
                            : this.actionIndices(sender, args))));
        case "import" -> {
          if (args.length == 2) {
            List<String> files = new ArrayList<>();
            File importDir = new File(this.plugin.getDataFolder(), "imports");
            if (importDir.exists()) {
              File[] arr = importDir.listFiles();
              if (arr != null) {
                for (File f : arr) {
                  files.add(f.getName());
                }
              }
            }

            File exportDir = new File(this.plugin.getDataFolder(), "exports");
            if (exportDir.exists()) {
              File[] arr = exportDir.listFiles();
              if (arr != null) {
                for (File f : arr) {
                  files.add(f.getName());
                }
              }
            }

            yield this.filter(files, args[1]);
          } else {
            yield List.of();
          }
        }
        default -> List.of();
      };
    }
  }

  private List<String> pointIndices(CommandSender sender) {
    if (!(sender instanceof Player player)) {
      return List.of();
    } else {
      EditSession session = EditSession.getSession(player);
      if (session == null) {
        return List.of();
      } else {
        int size = session.getScene().getPoints().size();
        List<String> indices = new ArrayList<>();

        for (int i = 0; i < size; i++) {
          indices.add(String.valueOf(i));
        }

        return indices;
      }
    }
  }

  private List<String> actionIndices(CommandSender sender, String[] args) {
    if (sender instanceof Player player) {
      EditSession session = EditSession.getSession(player);
      if (session == null) {
        return List.of();
      } else {
        try {
          int ptIdx = Integer.parseInt(args[1]);
          Scene scene = session.getScene();
          if (ptIdx >= 0 && ptIdx < scene.getPoints().size()) {
            int actionCount = scene.getPoints().get(ptIdx).getActions().size();
            List<String> indices = new ArrayList<>();

            for (int i = 0; i < actionCount; i++) {
              indices.add(String.valueOf(i));
            }

            return indices;
          } else {
            return List.of();
          }
        } catch (NumberFormatException ignored) {
          return List.of();
        }
      }
    } else {
      return List.of();
    }
  }

  private List<String> filter(List<String> options, String input) {
    String lower = input.toLowerCase();
    return options.stream()
        .filter(s -> s.toLowerCase().startsWith(lower))
        .collect(Collectors.toList());
  }
}

