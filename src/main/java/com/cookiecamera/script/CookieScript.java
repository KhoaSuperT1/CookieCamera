package com.cookiecamera.script;

import java.util.ArrayList;
import java.util.List;

public class CookieScript {
  private final String name;
  private final List<CookieScript.AcBlock> blocks = new ArrayList<>();

  public CookieScript(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public List<CookieScript.AcBlock> getBlocks() {
    return this.blocks;
  }

  public void addBlock(CookieScript.AcBlock block) {
    this.blocks.add(block);
  }

  public static class AcBlock {
    private final CookieScript.AcBlock.BlockType type;
    private final String id;
    private final List<CookieScript.AcInstruction> instructions = new ArrayList<>();

    public AcBlock(CookieScript.AcBlock.BlockType type, String id) {
      this.type = type;
      this.id = id;
    }

    public CookieScript.AcBlock.BlockType getType() {
      return this.type;
    }

    public String getId() {
      return this.id;
    }

    public List<CookieScript.AcInstruction> getInstructions() {
      return this.instructions;
    }

    public void addInstruction(CookieScript.AcInstruction instruction) {
      this.instructions.add(instruction);
    }

    public static enum BlockType {
      SCENE,
      TRIGGER,
      CHAIN;
    }
  }

  public static class AcInstruction {
    private final String command;
    private final String[] args;

    public AcInstruction(String command, String[] args) {
      this.command = command;
      this.args = args;
    }

    public String getCommand() {
      return this.command;
    }

    public String[] getArgs() {
      return this.args;
    }

    public String getArg(int index) {
      return index < this.args.length ? this.args[index] : null;
    }

    public String getArgJoined(int fromIndex) {
      if (fromIndex >= this.args.length) {
        return "";
      } else {
        StringBuilder sb = new StringBuilder();

        for (int i = fromIndex; i < this.args.length; i++) {
          if (i > fromIndex) {
            sb.append(" ");
          }

          sb.append(this.args[i]);
        }

        return sb.toString();
      }
    }
  }
}

