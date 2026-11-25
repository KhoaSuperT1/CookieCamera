package com.cookiecamera.util;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class Scheduler {
  private static final boolean FOLIA;

  public static boolean isFolia() {
    return FOLIA;
  }

  public static void runEntity(Plugin plugin, Entity entity, Runnable task) {
    if (FOLIA) {
      entity.getScheduler().run(plugin, t -> task.run(), null);
    } else {
      Bukkit.getScheduler().runTask(plugin, task);
    }
  }

  public static Scheduler.TaskHandle runEntityDelayed(
      Plugin plugin, Entity entity, Runnable task, long delayTicks) {
    if (FOLIA) {
      Object foliaTask =
          entity.getScheduler().runDelayed(plugin, t -> task.run(), null, delayTicks);
      return wrap(foliaTask);
    } else {
      BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
      return wrap(bukkitTask);
    }
  }

  public static Scheduler.TaskHandle runEntityTimer(
      Plugin plugin,
      Entity entity,
      Consumer<Scheduler.TaskHandle> task,
      long delayTicks,
      long periodTicks) {
    if (FOLIA) {
      AtomicReference<Scheduler.TaskHandle> ref = new AtomicReference<>();
      Object foliaTask =
          entity
              .getScheduler()
              .runAtFixedRate(
                  plugin,
                  t -> {
                    Scheduler.TaskHandle h = ref.get();
                    if (h == null) {
                      h = wrap(t);
                    }

                    task.accept(h);
                  },
                  null,
                  delayTicks,
                  periodTicks);
      Scheduler.TaskHandle handle = wrap(foliaTask);
      ref.set(handle);
      return handle;
    } else {
      AtomicReference<Scheduler.TaskHandle> ref = new AtomicReference<>();
      BukkitTask bukkitTask =
          Bukkit.getScheduler()
              .runTaskTimer(plugin, () -> task.accept(ref.get()), delayTicks, periodTicks);
      Scheduler.TaskHandle handle = wrap(bukkitTask);
      ref.set(handle);
      return handle;
    }
  }

  public static void runGlobal(Plugin plugin, Runnable task) {
    if (FOLIA) {
      Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run());
    } else {
      Bukkit.getScheduler().runTask(plugin, task);
    }
  }

  public static Scheduler.TaskHandle runGlobalDelayed(
      Plugin plugin, Runnable task, long delayTicks) {
    if (FOLIA) {
      Object foliaTask =
          Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), delayTicks);
      return wrap(foliaTask);
    } else {
      BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
      return wrap(bukkitTask);
    }
  }

  public static void teleport(Entity entity, Location location, Runnable onComplete) {
    teleportAsync(
        entity,
        location,
        ok -> {
          if (ok && onComplete != null) {
            onComplete.run();
          }
        });
  }

  public static void teleportAsync(Entity entity, Location location, Consumer<Boolean> callback) {
    try {
      entity
          .teleportAsync(location)
          .thenAccept(
              ok -> {
                if (callback != null) {
                  callback.accept(ok);
                }
              });
    } catch (NoSuchMethodError | Exception ignored) {
      boolean ok = entity.teleport(location);
      if (callback != null) {
        callback.accept(ok);
      }
    }
  }

  private static Scheduler.TaskHandle wrap(final Object foliaTask) {
    return new Scheduler.TaskHandle() {
      @Override
      public void cancel() {
        if (foliaTask != null) {
          try {
            foliaTask.getClass().getMethod("cancel").invoke(foliaTask);
          } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to cancel Folia task", exception);
          }
        }
      }

      @Override
      public boolean isCancelled() {
        if (foliaTask == null) {
          return true;
        }

        try {
          return (boolean) foliaTask.getClass().getMethod("isCancelled").invoke(foliaTask);
        } catch (ReflectiveOperationException exception) {
          throw new IllegalStateException("Failed to inspect Folia task state", exception);
        }
      }
    };
  }

  private static Scheduler.TaskHandle wrap(final BukkitTask bukkitTask) {
    return new Scheduler.TaskHandle() {
      @Override
      public void cancel() {
        if (bukkitTask != null) {
          bukkitTask.cancel();
        }
      }

      @Override
      public boolean isCancelled() {
        return bukkitTask == null || bukkitTask.isCancelled();
      }
    };
  }

  private Scheduler() {}

  static {
    boolean folia;
    try {
      Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
      folia = true;
    } catch (ClassNotFoundException ignored) {
      folia = false;
    }

    FOLIA = folia;
  }

  public interface TaskHandle {
    void cancel();

    boolean isCancelled();
  }
}

