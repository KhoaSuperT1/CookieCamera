package com.cookiecamera.manager;

import com.cookiecamera.CookieCamera;
import com.cookiecamera.math.CatmullRom;
import com.cookiecamera.model.CameraPoint;
import com.cookiecamera.model.EaseType;
import com.cookiecamera.model.Scene;
import com.cookiecamera.model.SmoothMode;
import com.cookiecamera.protocol.PacketHandler;
import com.cookiecamera.util.Scheduler;
import com.cookiecamera.util.TabScoreboardCompat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class PlaybackManager {
  private static final MiniMessage MM = MiniMessage.miniMessage();
  private final CookieCamera plugin;
  private final Map<UUID, PlaybackManager.PlaybackSession> activeSessions =
      new ConcurrentHashMap<>();
  private final Map<UUID, Scheduler.TaskHandle> pendingExitTapTasks = new ConcurrentHashMap<>();
  private final Map<UUID, Long> lastSneakSignalTimes = new ConcurrentHashMap<>();
  private static final long DOUBLE_SNEAK_EXIT_TICKS = 20L;
  private static final long SNEAK_SIGNAL_DEBOUNCE_MILLIS = 150L;

  public PlaybackManager(CookieCamera plugin) {
    this.plugin = plugin;
  }

  public void handleExitTap(Player player) {
    if (!this.isPlaying(player) || !this.plugin.canUsePlaybackExit(player)) {
      return;
    }

    UUID uuid = player.getUniqueId();
    long now = System.currentTimeMillis();
    Long lastSignalTime = this.lastSneakSignalTimes.get(uuid);
    if (lastSignalTime != null && now - lastSignalTime < SNEAK_SIGNAL_DEBOUNCE_MILLIS) {
      return;
    }

    this.lastSneakSignalTimes.put(uuid, now);
    Scheduler.TaskHandle pendingTapTask = this.pendingExitTapTasks.remove(uuid);
    if (pendingTapTask != null) {
      if (!pendingTapTask.isCancelled()) {
        pendingTapTask.cancel();
      }

      this.lastSneakSignalTimes.remove(uuid);
      this.stop(player);
      return;
    }

    if (this.plugin.shouldSendActionBar(player)) {
      player.sendActionBar(MM.deserialize(this.plugin.getLang().raw("play.exit-hint")));
    }
    Scheduler.TaskHandle timeoutTask =
        Scheduler.runEntityDelayed(
            this.plugin,
            player,
            () -> {
              this.pendingExitTapTasks.remove(uuid);
              this.lastSneakSignalTimes.remove(uuid);
            },
            DOUBLE_SNEAK_EXIT_TICKS);
    this.pendingExitTapTasks.put(uuid, timeoutTask);
  }

  private void clearPendingExitTap(Player player) {
    UUID uuid = player.getUniqueId();
    Scheduler.TaskHandle pendingTapTask = this.pendingExitTapTasks.remove(uuid);
    if (pendingTapTask != null && !pendingTapTask.isCancelled()) {
      pendingTapTask.cancel();
    }

    this.lastSneakSignalTimes.remove(uuid);
    this.plugin.clearMovementState(player);
  }

  public boolean play(Player player, Scene scene) {
    if (this.activeSessions.containsKey(player.getUniqueId())) {
      return false;
    } else if (scene.getPoints().size() < 2) {
      return false;
    } else {
      World world = player.getWorld();
      if (scene.getWorld() != null) {
        World sw = this.plugin.getServer().getWorld(scene.getWorld());
        if (sw != null) {
          world = sw;
        }
      }

      PlaybackManager.PlaybackSession session =
          new PlaybackManager.PlaybackSession(player, scene, world);
      this.activeSessions.put(player.getUniqueId(), session);
      session.start();
      return true;
    }
  }

  public void stop(Player p) {
    PlaybackManager.PlaybackSession s = this.activeSessions.remove(p.getUniqueId());
    this.clearPendingExitTap(p);
    if (s != null) {
      s.stop();
    }
  }

  public void stopAll() {
    this.activeSessions.values().forEach(PlaybackManager.PlaybackSession::stop);
    this.activeSessions.clear();
    this.pendingExitTapTasks
        .values()
        .forEach(
            task -> {
              if (task != null && !task.isCancelled()) {
                task.cancel();
              }
            });
    this.pendingExitTapTasks.clear();
    this.lastSneakSignalTimes.clear();
    this.plugin.clearAllMovementStates();
  }

  public boolean isPlaying(Player p) {
    return this.activeSessions.containsKey(p.getUniqueId());
  }

  public PlaybackManager.PlaybackSession getSession(Player p) {
    return this.activeSessions.get(p.getUniqueId());
  }

  public void pause(Player p) {
    PlaybackManager.PlaybackSession s = this.activeSessions.get(p.getUniqueId());
    if (s != null) {
      s.setPaused(true);
    }
  }

  public void resume(Player p) {
    PlaybackManager.PlaybackSession s = this.activeSessions.get(p.getUniqueId());
    if (s != null) {
      s.setPaused(false);
    }
  }

  public class PlaybackSession {
    private final Player player;
    private final Scene scene;
    private final World world;
    private final Location originalLocation;
    private Scheduler.TaskHandle task;
    private int currentTick = 0;
    private boolean stopped = false;
    private boolean paused = false;
    private BossBar bossBar;
    private final int[] segStartTicks;
    private final int totalTicks;
    private int lastCX = Integer.MIN_VALUE;
    private int lastCZ = Integer.MIN_VALUE;
    private int lastFiredSeg = -1;
    private double lpX;
    private double lpY;
    private double lpZ;
    private float lpYaw;
    private float lpPitch;
    private boolean lpInit = false;
    private final int pathTicks;
    private boolean inBuffer = false;
    private Scoreboard originalScoreboard;
    private Scoreboard hiddenScoreboard;
    private TabScoreboardCompat.State tabScoreboardState = TabScoreboardCompat.State.inactive();
    private static final double EYE_HEIGHT = 1.62;

    public PlaybackSession(Player player, Scene scene, World world) {
      this.player = player;
      this.scene = scene;
      this.world = world;
      this.originalLocation = player.getLocation().clone();
      List<CameraPoint> pts = scene.getPoints();
      this.segStartTicks = new int[pts.size()];
      int acc = 0;

      for (int i = 1; i < pts.size(); i++) {
        this.segStartTicks[i] = acc;
        acc += scene.getEffectiveDuration(i);
      }

      this.pathTicks = acc;
      int bufferTicks = PlaybackManager.this.plugin.getConfig().getInt("end-buffer-ticks", 30);
      this.totalTicks = acc + bufferTicks;
    }

    public void start() {
      Location startLoc = this.scene.getPoints().get(0).toLocation(this.world);
      Location cameraStart = startLoc.clone().add(0.0, EYE_HEIGHT, 0.0);
      PacketHandler.preloadChunks(this.player, this.world, startLoc.getX(), startLoc.getZ(), 3);
      if (PlaybackManager.this.plugin.shouldShowPlaybackBossBar(this.player)) {
        this.bossBar =
            BossBar.bossBar(
                PlaybackManager.MM.deserialize(
                    PlaybackManager.this
                        .plugin
                        .getLang()
                        .raw(
                            "play.bossbar",
                            "icon",
                            "▶",
                            "scene",
                            this.scene.getDisplayName(),
                            "percent",
                            0,
                            "exit_hint",
                            this.getBossBarExitHint())),
                0.0F,
                Color.BLUE,
                Overlay.PROGRESS);
        this.player.showBossBar(this.bossBar);
      }
      Scheduler.teleportAsync(
          this.player,
          startLoc,
          ok -> {
            if (ok && !this.stopped && this.player.isOnline()) {
              Scheduler.runEntity(
                  PlaybackManager.this.plugin,
                  this.player,
                  () -> {
                    if (!this.stopped && this.player.isOnline()) {
                      this.prepareScoreboard();
                      PacketHandler.startCamera(this.player, cameraStart);
                      CameraPoint firstPt = this.scene.getPoints().get(0);
                      if (!firstPt.getActions().isEmpty()) {
                        PointActionExecutor.execute(
                            PlaybackManager.this.plugin, this.player, this.scene, 0, firstPt);
                      }

                      this.lastFiredSeg = -1;
                      Scheduler.runEntityDelayed(
                          PlaybackManager.this.plugin,
                          this.player,
                          () -> {
                            if (!this.stopped && this.player.isOnline()) {
                              this.task =
                                  Scheduler.runEntityTimer(
                                      PlaybackManager.this.plugin,
                                      this.player,
                                      h -> this.tick(),
                                      1L,
                                      1L);
                            }
                          },
                          3L);
                    }
                  });
            }
          });
    }

    private void tick() {
      if (this.stopped || !this.player.isOnline()) {
        this.stop();
        return;
      }

      if (this.currentTick >= this.totalTicks) {
        this.stop();
        return;
      }

      if (this.paused) {
        return;
      }

      if (!this.tabScoreboardState.handled() && this.currentTick % 20 == 0) {
        this.ensureTabScoreboardHidden();
      }

      List<CameraPoint> points = this.scene.getPoints();
      SmoothMode smoothMode = this.scene.getSmoothMode();
      double smoothingFactor = this.scene.getSmoothing();

      if (this.currentTick >= this.pathTicks) {
        this.inBuffer = true;
        if (this.currentTick % 4 == 0) {
          this.updateBar();
        }

        this.currentTick++;
        return;
      }

      int segmentIndex = 0;
      for (int i = 1; i < points.size(); i++) {
        if (this.currentTick >= this.segStartTicks[i]) {
          segmentIndex = i - 1;
        }
      }

      this.firePointActions(points, segmentIndex);

      int segmentStartTick = this.segStartTicks[segmentIndex + 1];
      int segmentDuration = this.scene.getEffectiveDuration(segmentIndex + 1);
      double rawProgress =
          segmentDuration > 0
              ? (double) (this.currentTick - segmentStartTick) / segmentDuration
              : 1.0;
      rawProgress = Math.max(0.0, Math.min(1.0, rawProgress));

      EaseType ease = points.get(segmentIndex + 1).getEaseType();
      if (ease == null) {
        ease = EaseType.LINEAR;
      }

      double easedProgress = ease.apply(rawProgress);
      CameraPoint previousPoint = points.get(Math.max(0, segmentIndex - 1));
      CameraPoint startPoint = points.get(segmentIndex);
      CameraPoint endPoint = points.get(Math.min(points.size() - 1, segmentIndex + 1));
      CameraPoint nextPoint = points.get(Math.min(points.size() - 1, segmentIndex + 2));

      double[] position =
          this.interpolatePosition(
              previousPoint, startPoint, endPoint, nextPoint, smoothMode, ease, easedProgress);
      float[] rotation =
          this.interpolateRotation(
              previousPoint, startPoint, endPoint, nextPoint, smoothMode, ease, easedProgress);

      if (smoothingFactor > 0.0) {
        this.applySmoothing(position, rotation[0], rotation[1], smoothingFactor);
        rotation[0] = this.lpYaw;
        rotation[1] = this.lpPitch;
      }

      PacketHandler.moveCamera(
          this.player,
          position[0],
          position[1] + EYE_HEIGHT,
          position[2],
          rotation[0],
          rotation[1]);
      this.preloadChunksIfNeeded(position);

      if (this.currentTick % 4 == 0) {
        this.updateBar();
      }

      this.currentTick++;
    }

    private void updateBar() {
      if (this.bossBar != null) {
        double p = this.getProgress();
        this.bossBar.progress(Math.min(1.0F, (float) p));
        this.bossBar.color(
            this.paused
                ? Color.WHITE
                : (p < 0.33 ? Color.BLUE : (p < 0.66 ? Color.GREEN : Color.YELLOW)));
        String icon = this.paused ? "⏸" : "▶";
        this.bossBar.name(
            PlaybackManager.MM.deserialize(
                PlaybackManager.this
                    .plugin
                    .getLang()
                    .raw(
                        "play.bossbar",
                        "icon",
                        icon,
                        "scene",
                        this.scene.getDisplayName(),
                        "percent",
                        (int) (p * 100.0),
                        "exit_hint",
                        this.getBossBarExitHint())));
      }
    }

    public void stop() {
      if (!this.stopped) {
        this.stopped = true;
        if (this.task != null && !this.task.isCancelled()) {
          this.task.cancel();
        }

        PlaybackManager.this.activeSessions.remove(this.player.getUniqueId());
        PlaybackManager.this.clearPendingExitTap(this.player);
        if (this.bossBar != null) {
          this.player.hideBossBar(this.bossBar);
          this.bossBar = null;
        }
        this.restoreScoreboard();

        if (this.player.isOnline()) {
          try {
            PacketHandler.stopCamera(this.player);
          } catch (Exception exception) {
            PlaybackManager.this
                .plugin
                .getLogger()
                .warning("[Camera] Cleanup: " + exception.getMessage());
          }

          int restoreDelay =
              PlaybackManager.this.plugin.getConfig().getInt("restore-delay-ticks", 5);
          Runnable restoreTask =
              () -> {
                if (!this.player.isOnline()) {
                  return;
                }

                Scheduler.teleport(this.player, this.originalLocation, null);
                if (PlaybackManager.this.plugin.shouldSendActionBar(this.player)) {
                  this.player.sendActionBar(
                      PlaybackManager.MM.deserialize(
                          PlaybackManager.this.plugin.getLang().raw("play.finished")));
                }
              };
          if (!PlaybackManager.this.plugin.isEnabled()) {
            if (!Scheduler.isFolia()) {
              restoreTask.run();
            }
          } else if (restoreDelay > 0) {
            Scheduler.runEntityDelayed(
                PlaybackManager.this.plugin, this.player, restoreTask, restoreDelay);
          } else {
            Scheduler.runEntity(PlaybackManager.this.plugin, this.player, restoreTask);
          }
        }
      }
    }

    public Scene getScene() {
      return this.scene;
    }

    public int getCurrentTick() {
      return this.currentTick;
    }

    public int getTotalTicks() {
      return this.totalTicks;
    }

    public double getProgress() {
      return this.totalTicks > 0 ? (double) this.currentTick / this.totalTicks : 0.0;
    }

    public boolean isPaused() {
      return this.paused;
    }

    public void setPaused(boolean p) {
      this.paused = p;
    }

    public void seekTo(int tick) {
      this.currentTick = Math.max(0, Math.min(this.totalTicks - 1, tick));
    }

    public void skip(int ticks) {
      this.seekTo(this.currentTick + ticks);
    }

    private void firePointActions(List<CameraPoint> points, int segmentIndex) {
      if (segmentIndex == this.lastFiredSeg) {
        return;
      }

      this.lastFiredSeg = segmentIndex;
      int targetPointIndex = segmentIndex + 1;
      if (targetPointIndex >= points.size()) {
        return;
      }

      CameraPoint targetPoint = points.get(targetPointIndex);
      if (!targetPoint.getActions().isEmpty()) {
        PointActionExecutor.execute(
            PlaybackManager.this.plugin, this.player, this.scene, targetPointIndex, targetPoint);
      }
    }

    private double[] interpolatePosition(
        CameraPoint previousPoint,
        CameraPoint startPoint,
        CameraPoint endPoint,
        CameraPoint nextPoint,
        SmoothMode smoothMode,
        EaseType ease,
        double progress) {
      double[] p0 = new double[] {previousPoint.getX(), previousPoint.getY(), previousPoint.getZ()};
      double[] p1 = new double[] {startPoint.getX(), startPoint.getY(), startPoint.getZ()};
      double[] p2 = new double[] {endPoint.getX(), endPoint.getY(), endPoint.getZ()};
      double[] p3 = new double[] {nextPoint.getX(), nextPoint.getY(), nextPoint.getZ()};

      if (ease == EaseType.HOLD) {
        return p1.clone();
      }

      return switch (smoothMode) {
        case BSPLINE -> CatmullRom.bspline3D(p0, p1, p2, p3, progress);
        case HERMITE -> CatmullRom.hermite3D(p0, p1, p2, p3, progress, this.scene.getTension());
        case LINEAR ->
            new double[] {
              CatmullRom.lerp(p1[0], p2[0], progress),
              CatmullRom.lerp(p1[1], p2[1], progress),
              CatmullRom.lerp(p1[2], p2[2], progress)
            };
        default -> CatmullRom.interpolate3D(p0, p1, p2, p3, progress);
      };
    }

    private float[] interpolateRotation(
        CameraPoint previousPoint,
        CameraPoint startPoint,
        CameraPoint endPoint,
        CameraPoint nextPoint,
        SmoothMode smoothMode,
        EaseType ease,
        double progress) {
      float yaw;
      float pitch;
      if (ease == EaseType.HOLD) {
        yaw = startPoint.getYaw();
        pitch = startPoint.getPitch();
      } else if (smoothMode == SmoothMode.LINEAR) {
        yaw = CatmullRom.lerpAngle(startPoint.getYaw(), endPoint.getYaw(), progress);
        pitch = (float) CatmullRom.lerp(startPoint.getPitch(), endPoint.getPitch(), progress);
      } else {
        yaw =
            CatmullRom.splineAngle(
                previousPoint.getYaw(),
                startPoint.getYaw(),
                endPoint.getYaw(),
                nextPoint.getYaw(),
                progress);
        pitch =
            (float)
                CatmullRom.interpolate(
                    previousPoint.getPitch(),
                    startPoint.getPitch(),
                    endPoint.getPitch(),
                    nextPoint.getPitch(),
                    progress);
        pitch = Math.max(-90.0F, Math.min(90.0F, pitch));
      }

      return new float[] {yaw, pitch};
    }

    private void applySmoothing(double[] position, float yaw, float pitch, double smoothingFactor) {
      if (!this.lpInit) {
        this.lpX = position[0];
        this.lpY = position[1];
        this.lpZ = position[2];
        this.lpYaw = yaw;
        this.lpPitch = pitch;
        this.lpInit = true;
      } else {
        this.lpX = this.lpX * smoothingFactor + position[0] * (1.0 - smoothingFactor);
        this.lpY = this.lpY * smoothingFactor + position[1] * (1.0 - smoothingFactor);
        this.lpZ = this.lpZ * smoothingFactor + position[2] * (1.0 - smoothingFactor);
        this.lpYaw = CatmullRom.lerpAngle(this.lpYaw, yaw, 1.0 - smoothingFactor);
        this.lpPitch = (float) (this.lpPitch * smoothingFactor + pitch * (1.0 - smoothingFactor));
      }

      position[0] = this.lpX;
      position[1] = this.lpY;
      position[2] = this.lpZ;
    }

    private void preloadChunksIfNeeded(double[] position) {
      if (this.currentTick % 20 != 0) {
        return;
      }

      int chunkX = (int) Math.floor(position[0]) >> 4;
      int chunkZ = (int) Math.floor(position[2]) >> 4;
      if (chunkX == this.lastCX && chunkZ == this.lastCZ) {
        return;
      }

      this.lastCX = chunkX;
      this.lastCZ = chunkZ;
      PacketHandler.preloadChunks(this.player, this.world, position[0], position[2], 2);
    }

    private void prepareScoreboard() {
      if (PlaybackManager.this.plugin.shouldShowPlaybackScoreboard(this.player)) {
        return;
      }

      ScoreboardManager scoreboardManager =
          PlaybackManager.this.plugin.getServer().getScoreboardManager();
      if (scoreboardManager == null) {
        return;
      }

      this.originalScoreboard = this.player.getScoreboard();
      this.hiddenScoreboard = scoreboardManager.getNewScoreboard();
      this.player.setScoreboard(this.hiddenScoreboard);
      this.ensureTabScoreboardHidden();
    }

    private void restoreScoreboard() {
      if (this.tabScoreboardState.handled()) {
        TabScoreboardCompat.restoreScoreboard(
            PlaybackManager.this.plugin, this.player, this.tabScoreboardState);
        this.tabScoreboardState = TabScoreboardCompat.State.inactive();
      }

      if (this.hiddenScoreboard == null || !this.player.isOnline()) {
        this.hiddenScoreboard = null;
        this.originalScoreboard = null;
        return;
      }

      if (this.player.getScoreboard() == this.hiddenScoreboard && this.originalScoreboard != null) {
        this.player.setScoreboard(this.originalScoreboard);
      }

      this.hiddenScoreboard = null;
      this.originalScoreboard = null;
    }

    private void ensureTabScoreboardHidden() {
      if (PlaybackManager.this.plugin.shouldShowPlaybackScoreboard(this.player)
          || this.tabScoreboardState.handled()) {
        return;
      }

      this.tabScoreboardState =
          TabScoreboardCompat.hideScoreboard(PlaybackManager.this.plugin, this.player);
    }

    private String getBossBarExitHint() {
      return PlaybackManager.this.plugin.canUsePlaybackExit(this.player)
          ? PlaybackManager.this.plugin.getLang().raw("play.bossbar-exit-hint")
          : "";
    }
  }
}

