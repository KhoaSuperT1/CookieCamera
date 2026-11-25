package com.cookiecamera.manager;

import com.cookiecamera.CookieCamera;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;

public class LangManager {
  private static final MiniMessage MM = MiniMessage.miniMessage();
  private static final String FALLBACK_LANGUAGE = "en_US";
  private static final Set<String> SUPPORTED_LANGUAGES = Set.of("en_US", "vi_VN");
  private final CookieCamera plugin;
  private final Map<String, String> messages = new HashMap<>();

  public LangManager(CookieCamera plugin) {
    this.plugin = plugin;
  }

  public void load() {
    String configuredLanguage = this.plugin.getConfig().getString("language", FALLBACK_LANGUAGE);
    String language = this.resolveLanguage(configuredLanguage);
    File langDir = new File(this.plugin.getDataFolder(), "lang");
    if (!langDir.exists()) {
      langDir.mkdirs();
    }

    File oldLang = new File(this.plugin.getDataFolder(), "lang.yml");
    if (oldLang.exists()) {
      oldLang.renameTo(new File(this.plugin.getDataFolder(), "lang.yml.old"));
      this.plugin.getLogger().info("Migrated old lang.yml -> lang.yml.old");
    }

    for (String lang : SUPPORTED_LANGUAGES) {
      this.saveDefaultLang(lang);
    }

    this.messages.clear();
    this.loadFromJar(FALLBACK_LANGUAGE);
    if (!language.equals(FALLBACK_LANGUAGE)) {
      this.loadFromJar(language);
    }

    File langFile = new File(langDir, language + ".yml");
    if (!langFile.exists()) {
      this.plugin
          .getLogger()
          .warning("Language file '" + language + ".yml' not found on disk, using jar defaults.");
    } else {
      YamlConfiguration diskCfg = YamlConfiguration.loadConfiguration(langFile);
      String diskPointAdded = diskCfg.getString("point.added");
      this.plugin
          .getLogger()
          .info(
              "[Lang] Disk "
                  + language
                  + ".yml point.added="
                  + diskPointAdded
                  + ", keys="
                  + diskCfg.getKeys(true).size());
      this.loadFromConfig(diskCfg);
    }

    this.plugin
        .getLogger()
        .info(
            "Loaded "
                + this.messages.size()
                + " lang entries (language="
                + language
                + "), final point.added="
                + this.messages.get("point.added"));
  }

  private String resolveLanguage(String language) {
    if (language == null || language.isBlank()) {
      return FALLBACK_LANGUAGE;
    }

    if (SUPPORTED_LANGUAGES.contains(language)) {
      return language;
    }

    this.plugin
        .getLogger()
        .warning(
            "Unsupported language '"
                + language
                + "', falling back to "
                + FALLBACK_LANGUAGE
                + ".");
    return FALLBACK_LANGUAGE;
  }

  private void loadFromJar(String lang) {
    String resourcePath = "lang/" + lang + ".yml";
    InputStream in = this.plugin.getResource(resourcePath);
    if (in == null) {
      this.plugin.getLogger().warning("Jar resource not found: " + resourcePath);
    } else {
      try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(reader);
        int before = this.messages.size();
        this.loadFromConfig(cfg);
        int after = this.messages.size();
        this.plugin
            .getLogger()
            .info(
                "[Lang] Loaded jar "
                    + lang
                    + ": "
                    + cfg.getKeys(true).size()
                    + " keys total, messages map "
                    + before
                    + " -> "
                    + after
                    + ", point.added="
                    + this.messages.get("point.added"));
      } catch (Exception exception) {
        this.plugin
            .getLogger()
            .warning("Failed to load jar lang/" + lang + ".yml: " + exception.getMessage());
      }
    }
  }

  private void loadFromConfig(YamlConfiguration cfg) {
    for (String key : cfg.getKeys(true)) {
      if (cfg.isString(key)) {
        this.messages.put(key, cfg.getString(key));
      }
    }
  }

  private void saveDefaultLang(String lang) {
    File target = new File(this.plugin.getDataFolder(), "lang/" + lang + ".yml");
    if (!target.exists()) {
      String resourcePath = "lang/" + lang + ".yml";
      if (this.plugin.getResource(resourcePath) != null) {
        this.plugin.saveResource(resourcePath, false);
      }
    }
  }

  public String raw(String key, Object... replacements) {
    String msg = this.messages.getOrDefault(key, key);

    for (int i = 0; i + 1 < replacements.length; i += 2) {
      msg = msg.replace("{" + replacements[i] + "}", String.valueOf(replacements[i + 1]));
    }

    return msg;
  }

  public Component get(String key, Object... replacements) {
    return MM.deserialize(this.raw(key, replacements));
  }
}

