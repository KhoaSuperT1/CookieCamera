package com.cookiecamera.script;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class CookieScriptParser {
  private CookieScriptParser() {}

  public static CookieScript parse(File file) throws IOException {
    String name = file.getName().replace(".ac", "");
    CookieScript script = new CookieScript(name);

    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      CookieScript.AcBlock currentBlock = null;

      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (!line.isEmpty() && !line.startsWith("#")) {
          if (line.startsWith("@")) {
            String[] parts = line.substring(1).split("\\s+", 2);
            String blockType = parts[0].toLowerCase();
            String blockId = parts.length > 1 ? parts[1] : "unnamed";

            CookieScript.AcBlock.BlockType type =
                switch (blockType) {
                  case "scene" -> CookieScript.AcBlock.BlockType.SCENE;
                  case "trigger" -> CookieScript.AcBlock.BlockType.TRIGGER;
                  case "chain" -> CookieScript.AcBlock.BlockType.CHAIN;
                  default -> null;
                };
            if (type != null) {
              currentBlock = new CookieScript.AcBlock(type, blockId);
              script.addBlock(currentBlock);
            }
          } else if (currentBlock != null) {
            List<String> tokens = tokenize(line);
            if (!tokens.isEmpty()) {
              String cmd = tokens.get(0).toLowerCase();
              String[] args = tokens.subList(1, tokens.size()).toArray(new String[0]);
              currentBlock.addInstruction(new CookieScript.AcInstruction(cmd, args));
            }
          }
        }
      }
    }

    return script;
  }

  private static List<String> tokenize(String line) {
    List<String> tokens = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuote = false;

    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '"') {
        inQuote = !inQuote;
      } else if (c != ' ' || inQuote) {
        current.append(c);
      } else if (!current.isEmpty()) {
        tokens.add(current.toString());
        current.setLength(0);
      }
    }

    if (!current.isEmpty()) {
      tokens.add(current.toString());
    }

    return tokens;
  }

  public static List<CookieScript> parseAll(File scriptsDir) throws IOException {
    List<CookieScript> scripts = new ArrayList<>();
    if (scriptsDir.exists() && scriptsDir.isDirectory()) {
      File[] files = scriptsDir.listFiles((dir, name) -> name.endsWith(".ac"));
      if (files == null) {
        return scripts;
      } else {
        for (File file : files) {
          scripts.add(parse(file));
        }

        return scripts;
      }
    } else {
      return scripts;
    }
  }
}

