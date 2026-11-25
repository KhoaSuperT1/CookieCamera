package com.cookiecamera.manager;

import com.cookiecamera.CookieCamera;
import com.cookiecamera.model.CameraPoint;
import com.cookiecamera.model.Scene;
import com.cookiecamera.model.SmoothMode;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;

public class SceneManager {
  private final CookieCamera plugin;
  private final File scenesDir;
  private final Map<String, Scene> scenes = new ConcurrentHashMap<>();

  public SceneManager(CookieCamera plugin) {
    this.plugin = plugin;
    this.scenesDir = new File(plugin.getDataFolder(), "scenes");
    if (!this.scenesDir.exists()) {
      this.scenesDir.mkdirs();
    }
  }

  public void loadAll() {
    this.scenes.clear();
    File[] files = this.scenesDir.listFiles((dir, name) -> name.endsWith(".yml"));
    if (files != null) {
      for (File file : files) {
        try {
          Scene scene = this.loadScene(file);
          if (scene != null) {
            this.scenes.put(scene.getId().toLowerCase(), scene);
          }
        } catch (Exception exception) {
          this.plugin
              .getLogger()
              .log(Level.WARNING, "Failed to load scene: " + file.getName(), exception);
        }
      }

      this.plugin.getLogger().info("Loaded " + this.scenes.size() + " scene(s).");
    }
  }

  private Scene loadScene(File file) {
    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
    String id = cfg.getString("id");
    if (id == null) {
      return null;
    } else {
      Scene scene = new Scene(id);
      scene.setDisplayName(cfg.getString("displayName", id));
      scene.setDefaultSpeedTicks(cfg.getInt("defaultSpeedTicks", 40));
      scene.setShowMarkers(cfg.getBoolean("showMarkers", true));
      scene.setWorld(cfg.getString("world", "world"));
      scene.setSmoothMode(SmoothMode.fromString(cfg.getString("smoothMode", "CATMULL_ROM")));
      scene.setTension(cfg.getDouble("tension", 0.0));
      scene.setSmoothing(cfg.getDouble("smoothing", 0.0));

      for (Map<?, ?> raw : cfg.getMapList("points")) {
        CameraPoint point = CameraPoint.deserialize(castConfigMap(raw));
        scene.addPoint(point);
      }

      return scene;
    }
  }

  public void saveScene(Scene scene) {
    File file = new File(this.scenesDir, scene.getId().toLowerCase() + ".yml");
    YamlConfiguration cfg = new YamlConfiguration();
    cfg.set("id", scene.getId());
    cfg.set("displayName", scene.getDisplayName());
    cfg.set("defaultSpeedTicks", scene.getDefaultSpeedTicks());
    cfg.set("showMarkers", scene.isShowMarkers());
    cfg.set("world", scene.getWorld());
    cfg.set("smoothMode", scene.getSmoothMode().name());
    cfg.set("tension", scene.getTension());
    cfg.set("smoothing", scene.getSmoothing());
    List<Map<String, Object>> pointsList = new ArrayList<>();

    for (CameraPoint p : scene.getPoints()) {
      pointsList.add(p.serialize());
    }

    cfg.set("points", pointsList);

    try {
      cfg.save(file);
    } catch (IOException exception) {
      this.plugin
          .getLogger()
          .log(Level.SEVERE, "Failed to save scene: " + scene.getId(), exception);
    }
  }

  public Scene createScene(String id) {
    Scene scene = new Scene(id);
    this.scenes.put(id.toLowerCase(), scene);
    this.saveScene(scene);
    return scene;
  }

  public boolean deleteScene(String id) {
    Scene removed = this.scenes.remove(id.toLowerCase());
    if (removed != null) {
      File file = new File(this.scenesDir, id.toLowerCase() + ".yml");
      file.delete();
      return true;
    } else {
      return false;
    }
  }

  public Scene getScene(String id) {
    return this.scenes.get(id.toLowerCase());
  }

  public Collection<Scene> getAllScenes() {
    return Collections.unmodifiableCollection(this.scenes.values());
  }

  public Set<String> getSceneIds() {
    return Collections.unmodifiableSet(this.scenes.keySet());
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> castConfigMap(Map<?, ?> raw) {
    return (Map<String, Object>) raw;
  }
}

