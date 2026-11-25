package com.cookiecamera.script;

import com.cookiecamera.CookieCamera;
import com.cookiecamera.manager.PointActionExecutor;
import com.cookiecamera.model.Scene;
import com.cookiecamera.model.Trigger;
import com.cookiecamera.util.Scheduler;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.entity.Player;

public class CookieScriptEngine {
  private static final MiniMessage MM = MiniMessage.miniMessage();
  private final CookieCamera plugin;
  private final File scriptsDir;
  private final Map<String, CookieScript> scripts = new ConcurrentHashMap<>();
  private final Map<String, CookieScript.AcBlock> sceneBlocks = new ConcurrentHashMap<>();
  private final Map<String, CookieScript.AcBlock> chainBlocks = new ConcurrentHashMap<>();
  private static final String EXAMPLE_BASIC =
      "# ═══════════════════════════════════════════════════\n"
          + "# CookieCamera 脚本示例 - 基础用法\n"
          + "# 文件位置: plugins/CookieCamera/scripts/example.ac\n"
          + "# 执行: /cookiecamera script intro\n"
          + "# ═══════════════════════════════════════════════════\n"
          + "#\n"
          + "# ── 格式支持 ──\n"
          + "#   MiniMessage: <gradient:#ff0000:#00ff00>渐变</gradient> <bold>粗体</bold>\n"
          + "#   旧版颜色码: &6金色 &l粗体 &a绿色\n"
          + "#\n"
          + "# ── 占位符 ──\n"
          + "#   {player}  — 玩家名\n"
          + "#   {scene}   — 场景ID\n"
          + "#   {point}   — 当前点位序号\n"
          + "#   {x} {y} {z} {yaw} {pitch} — 坐标\n"
          + "#   {world}   — 世界名\n"
          + "#\n"
          + "# ── 指令类型 ──\n"
          + "#   play <场景ID>              — 播放摄像机场景\n"
          + "#   wait <额外tick>            — 等待当前场景播放完毕(+额外tick)\n"
          + "#   delay <tick>               — 延迟指定tick (20tick=1秒)\n"
          + "#   title \"标题\" \"副标题\"       — 显示大标题\n"
          + "#   actionbar \"文字\"           — 显示动作栏文字\n"
          + "#   sound <音效> <音量> <音调>  — 播放音效\n"
          + "#   cmd <指令>                 — 以控制台身份执行指令\n"
          + "#   playercmd <指令>           — 以玩家身份执行指令\n"
          + "#   msg \"消息\"                 — 发送聊天消息给玩家\n"
          + "#   chain <链ID>               — 执行另一个脚本链\n\n"
          + "# ── 基础场景播放 ──\n"
          + "@scene intro\n"
          + "  title \"<gradient:#FFD700:#FF6347>欢迎来到服务器!</gradient>\" \"<gray>享受这段旅程</gray>\"\n"
          + "  sound entity.experience_orb.pickup 1.0 1.2\n"
          + "  delay 40\n"
          + "  play my_scene\n"
          + "  wait 0\n"
          + "  actionbar \"<green>✓ 过场动画结束!</green>\"\n"
          + "  msg \"<gold>[CookieCamera]</gold> <gray>感谢观看!</gray>\"\n";
  private static final String EXAMPLE_ADVANCED =
      "# ═══════════════════════════════════════════════════\n"
          + "# CookieCamera 脚本示例 - 高级用法\n"
          + "# ═══════════════════════════════════════════════════\n\n"
          + "# ── 多场景链接 ──\n"
          + "@chain epic_tour\n"
          + "  title \"<gold><bold>史诗之旅</bold></gold>\" \"<gray>第一幕: 起源</gray>\"\n"
          + "  sound entity.ender_dragon.growl 0.5 1.0\n"
          + "  delay 40\n"
          + "  play scene_part1\n"
          + "  wait 20\n"
          + "  title \"<gold><bold>第二幕</bold></gold>\" \"<gray>征途</gray>\"\n"
          + "  sound block.beacon.activate 1.0 1.0\n"
          + "  play scene_part2\n"
          + "  wait 20\n"
          + "  title \"<gold><bold>终章</bold></gold>\" \"<gray>归来</gray>\"\n"
          + "  play scene_part3\n"
          + "  wait 0\n"
          + "  msg \"<gradient:#FFD700:#FF6347>旅程结束! 感谢 {player} 的观看!</gradient>\"\n"
          + "  cmd give {player} diamond 1\n\n"
          + "# ── 带指令奖励的场景 ──\n"
          + "@scene reward_scene\n"
          + "  title \"<green>恭喜!</green>\" \"<gray>{player} 完成了挑战</gray>\"\n"
          + "  play reward_camera\n"
          + "  wait 0\n"
          + "  cmd give {player} diamond 5\n"
          + "  cmd say {player} 完成了过场动画挑战!\n"
          + "  playercmd spawn\n"
          + "  sound entity.player.levelup 1.0 1.0\n"
          + "  msg \"<gold>你获得了 5 颗钻石!</gold>\"\n\n"
          + "# ── 倒计时效果 ──\n"
          + "@chain countdown\n"
          + "  title \"<red><bold>3</bold></red>\" \"\"\n"
          + "  sound block.note_block.pling 1.0 0.5\n"
          + "  delay 20\n"
          + "  title \"<yellow><bold>2</bold></yellow>\" \"\"\n"
          + "  sound block.note_block.pling 1.0 0.8\n"
          + "  delay 20\n"
          + "  title \"<green><bold>1</bold></green>\" \"\"\n"
          + "  sound block.note_block.pling 1.0 1.0\n"
          + "  delay 20\n"
          + "  title \"<gold><bold>开始!</bold></gold>\" \"\"\n"
          + "  sound entity.firework_rocket.blast 1.0 1.0\n"
          + "  delay 10\n"
          + "  play race_scene\n\n"
          + "# ── NPC对话效果 ──\n"
          + "@chain npc_dialog\n"
          + "  actionbar \"<yellow>村长正在说话...</yellow>\"\n"
          + "  msg \"<gold>[村长]</gold> <white>欢迎来到我们的村庄, {player}!</white>\"\n"
          + "  delay 60\n"
          + "  msg \"<gold>[村长]</gold> <white>让我带你参观一下...</white>\"\n"
          + "  delay 40\n"
          + "  play village_tour\n"
          + "  wait 0\n"
          + "  msg \"<gold>[村长]</gold> <white>希望你喜欢这里!</white>\"\n"
          + "  sound entity.villager.yes 1.0 1.0\n";
  private static final String EXAMPLE_TRIGGERS =
      "# ═══════════════════════════════════════════════════\n"
          + "# CookieCamera 脚本示例 - 触发器定义\n"
          + "# 触发器在脚本加载时自动注册\n"
          + "# ═══════════════════════════════════════════════════\n\n"
          + "# ── 区域进入触发 ──\n"
          + "@trigger lobby_enter\n"
          + "  type REGION_ENTER\n"
          + "  scene lobby_intro\n"
          + "  world world\n"
          + "  region 0 60 0 20 80 20\n"
          + "  cooldown 6000\n\n"
          + "# ── 右键方块触发 ──\n"
          + "@trigger secret_button\n"
          + "  type RIGHT_CLICK_BLOCK\n"
          + "  scene secret_room\n"
          + "  world world\n"
          + "  block 100 65 200\n"
          + "  condition perm cookiecamera.secret\n"
          + "  cooldown 1200\n\n"
          + "# ── VIP专属触发 ──\n"
          + "@trigger vip_entrance\n"
          + "  type REGION_ENTER\n"
          + "  scene vip_tour\n"
          + "  world world\n"
          + "  region -10 60 -10 10 80 10\n"
          + "  condition perm cookiecamera.vip\n"
          + "  condition hand DIAMOND\n"
          + "  onetime true\n"
          + "  cmd say {player} 触发了VIP过场动画!\n";
  private static final String EXAMPLE_SERVER_TOUR =
      "# ═══════════════════════════════════════════════════\n"
          + "# 服务器导览 - 新玩家入服自动播放\n"
          + "# 适用: 新手引导、服务器宣传\n"
          + "# 执行: /cookiecamera script spawn_tour\n"
          + "# ═══════════════════════════════════════════════════\n\n"
          + "@chain spawn_tour\n"
          + "  title \"<gradient:#00BFFF:#7B68EE>欢迎来到 MyServer!</gradient>\""
          + " \"<gray>让我们带你看看这里</gray>\"\n"
          + "  sound entity.experience_orb.pickup 1.0 1.0\n"
          + "  delay 60\n\n"
          + "  # 第一站: 出生点广场\n"
          + "  actionbar \"<aqua>▸ 第一站: 出生点广场</aqua>\"\n"
          + "  play spawn_overview\n"
          + "  wait 10\n\n"
          + "  # 第二站: 商店区\n"
          + "  title \"<gold>商店区</gold>\" \"<gray>在这里购买你需要的物品</gray>\"\n"
          + "  sound block.note_block.harp 1.0 1.2\n"
          + "  play shop_area\n"
          + "  wait 10\n\n"
          + "  # 第三站: 竞技场\n"
          + "  title \"<red>竞技场</red>\" \"<gray>与其他玩家一决高下</gray>\"\n"
          + "  sound entity.ender_dragon.flap 0.5 1.0\n"
          + "  play arena_overview\n"
          + "  wait 10\n\n"
          + "  # 第四站: 资源世界传送门\n"
          + "  title \"<green>资源世界</green>\" \"<gray>无限资源等你探索</gray>\"\n"
          + "  sound block.portal.ambient 0.5 1.0\n"
          + "  play resource_portal\n"
          + "  wait 20\n\n"
          + "  # 结束\n"
          + "  title \"<gradient:#FFD700:#FF6347>导览结束!</gradient>\" \"<gray>祝你游戏愉快,"
          + " {player}!</gray>\"\n"
          + "  sound entity.player.levelup 1.0 1.0\n"
          + "  msg \"\"\n"
          + "  msg \"<gold>═══════════════════════════════</gold>\"\n"
          + "  msg \"<aqua>  导览已结束! 以下是常用指令:</aqua>\"\n"
          + "  msg \"<gray>  /spawn</gray> <dark_gray>- 回到出生点</dark_gray>\"\n"
          + "  msg \"<gray>  /shop</gray> <dark_gray>- 打开商店</dark_gray>\"\n"
          + "  msg \"<gray>  /tpa <玩家></gray> <dark_gray>- 传送请求</dark_gray>\"\n"
          + "  msg \"<gold>═══════════════════════════════</gold>\"\n"
          + "  cmd give {player} bread 16\n\n"
          + "# ── 新玩家自动触发 ──\n"
          + "@trigger new_player_tour\n"
          + "  type REGION_ENTER\n"
          + "  scene spawn_overview\n"
          + "  world world\n"
          + "  region -5 60 -5 5 80 5\n"
          + "  onetime true\n"
          + "  cooldown 0\n";
  private static final String EXAMPLE_BOSS_CINEMATIC =
      "# ═══════════════════════════════════════════════════\n"
          + "# Boss战过场动画 - 战斗前的史诗演出\n"
          + "# 适用: RPG服务器、副本入口、Boss战前\n"
          + "# 执行: /cookiecamera script boss_intro\n"
          + "# ═══════════════════════════════════════════════════\n\n"
          + "@chain boss_intro\n"
          + "  # 黑屏效果\n"
          + "  title \"<black><bold>. . .</bold></black>\" \"\"\n"
          + "  delay 40\n\n"
          + "  # 镜头拉远展示Boss巢穴\n"
          + "  title \"\" \"<dark_red><italic>一股不祥的气息弥漫开来...</italic></dark_red>\"\n"
          + "  sound ambient.cave 1.0 0.5\n"
          + "  play boss_lair_overview\n"
          + "  wait 10\n\n"
          + "  # Boss登场\n"
          + "  title \"<dark_red><bold>⚔ 暗影领主 ⚔</bold></dark_red>\" \"<gray>深渊的统治者</gray>\"\n"
          + "  sound entity.wither.spawn 1.0 0.8\n"
          + "  delay 20\n"
          + "  play boss_closeup\n"
          + "  wait 10\n\n"
          + "  # Boss台词\n"
          + "  msg \"\"\n"
          + "  msg \"<dark_red>[暗影领主]</dark_red> <gray>愚蠢的凡人... {player}...</gray>\"\n"
          + "  sound entity.ender_dragon.growl 0.8 0.6\n"
          + "  delay 60\n"
          + "  msg \"<dark_red>[暗影领主]</dark_red> <gray>你以为你能阻止我吗?</gray>\"\n"
          + "  delay 60\n"
          + "  msg \"<dark_red>[暗影领主]</dark_red> <red><bold>来吧, 让我看看你的实力!</bold></red>\"\n"
          + "  sound entity.ender_dragon.growl 1.0 1.0\n"
          + "  delay 20\n\n"
          + "  # 战斗开始\n"
          + "  chain countdown_boss\n\n"
          + "@chain countdown_boss\n"
          + "  title \"<red><bold>战斗即将开始</bold></red>\" \"\"\n"
          + "  sound block.anvil.land 1.0 0.5\n"
          + "  delay 30\n"
          + "  title \"<yellow><bold>准备战斗!</bold></yellow>\" \"\"\n"
          + "  sound block.anvil.land 1.0 0.8\n"
          + "  delay 20\n"
          + "  title \"<green><bold>⚔ 开战! ⚔</bold></green>\" \"\"\n"
          + "  sound entity.ender_dragon.flap 1.0 1.2\n"
          + "  cmd effect give {player} strength 60 1\n"
          + "  cmd effect give {player} resistance 10 0\n"
          + "  msg \"<gold>[系统]</gold> <gray>你获得了力量增益! 击败暗影领主!</gray>\"\n";
  private static final String EXAMPLE_BUILDING_SHOWCASE =
      "# ═══════════════════════════════════════════════════\n"
          + "# 建筑展示 - 展示服务器建筑/玩家作品\n"
          + "# 适用: 建筑服、作品展示、延时摄影效果\n"
          + "# 执行: /cookiecamera script building_show\n"
          + "# ═══════════════════════════════════════════════════\n\n"
          + "@chain building_show\n"
          + "  title \"<gradient:#FFD700:#FFA500>✦ 建筑展示 ✦</gradient>\" \"<gray>本周最佳作品</gray>\"\n"
          + "  sound block.note_block.chime 1.0 1.0\n"
          + "  delay 60\n\n"
          + "  # 作品1: 城堡\n"
          + "  title \"<gold>中世纪城堡</gold>\" \"<gray>建造者: Steve</gray>\"\n"
          + "  sound block.note_block.harp 0.8 1.0\n"
          + "  play castle_tour\n"
          + "  wait 20\n"
          + "  msg \"<yellow>⭐</yellow> <gray>这座城堡耗时 3 周建造, 使用了超过 50000 个方块!</gray>\"\n"
          + "  delay 60\n\n"
          + "  # 作品2: 现代别墅\n"
          + "  title \"<aqua>现代别墅</aqua>\" \"<gray>建造者: Alex</gray>\"\n"
          + "  sound block.note_block.harp 0.8 1.2\n"
          + "  play villa_tour\n"
          + "  wait 20\n"
          + "  msg \"<yellow>⭐</yellow> <gray>极简主义设计, 内含红石自动化系统!</gray>\"\n"
          + "  delay 60\n\n"
          + "  # 作品3: 像素画\n"
          + "  title \"<light_purple>巨型像素画</light_purple>\" \"<gray>建造者: Notch</gray>\"\n"
          + "  sound block.note_block.harp 0.8 1.4\n"
          + "  play pixel_art_tour\n"
          + "  wait 20\n\n"
          + "  # 投票提示\n"
          + "  title \"<green>为你喜欢的作品投票!</green>\" \"<gray>/vote <作品编号></gray>\"\n"
          + "  sound entity.experience_orb.pickup 1.0 1.0\n"
          + "  msg \"\"\n"
          + "  msg \"<gold>════════ 投票 ════════</gold>\"\n"
          + "  msg \"<yellow>1.</yellow> <gray>中世纪城堡</gray> <dark_gray>- /vote 1</dark_gray>\"\n"
          + "  msg \"<yellow>2.</yellow> <gray>现代别墅</gray> <dark_gray>- /vote 2</dark_gray>\"\n"
          + "  msg \"<yellow>3.</yellow> <gray>巨型像素画</gray> <dark_gray>- /vote 3</dark_gray>\"\n"
          + "  msg \"<gold>═════════════════════</gold>\"\n";
  private static final String EXAMPLE_SEASONAL_EVENT =
      "# ═══════════════════════════════════════════════════\n"
          + "# 节日活动 - 季节性/节日过场动画\n"
          + "# 适用: 春节、圣诞、万圣节、周年庆等\n"
          + "# 执行: /cookiecamera script spring_festival\n"
          + "# ═══════════════════════════════════════════════════\n\n"
          + "@chain spring_festival\n"
          + "  # 烟花开场\n"
          + "  sound entity.firework_rocket.launch 1.0 1.0\n"
          + "  delay 10\n"
          + "  sound entity.firework_rocket.blast 1.0 0.8\n"
          + "  title \"<red><bold>\ud83e\udde7 新春快乐 \ud83e\udde7</bold></red>\""
          + " \"<gold>龙年大吉!</gold>\"\n"
          + "  delay 20\n"
          + "  play fireworks_scene\n"
          + "  wait 10\n\n"
          + "  # 活动介绍\n"
          + "  title \"<gold>✦ 新春活动 ✦</gold>\" \"<gray>限时开启中!</gray>\"\n"
          + "  sound block.note_block.bell 1.0 1.0\n"
          + "  play event_area_tour\n"
          + "  wait 20\n\n"
          + "  # 活动内容\n"
          + "  msg \"\"\n"
          + "  msg \"<red>\ud83e\udde7═══════ 新春活动 ═══════\ud83e\udde7</red>\"\n"
          + "  msg \"\"\n"
          + "  msg \"<gold>  \ud83c\udf81 每日签到</gold> <gray>- 连续签到7天获得限定称号</gray>\"\n"
          + "  msg \"<gold>  \ud83c\udf86 烟花大会</gold> <gray>- 每晚8点广场集合</gray>\"\n"
          + "  msg \"<gold>  \ud83c\udfee 灯谜竞猜</gold> <gray>- /riddle 参与答题赢奖品</gray>\"\n"
          + "  msg \"<gold>  \ud83d\udc09 年兽讨伐</gold> <gray>- 击败年兽获得红包</gray>\"\n"
          + "  msg \"\"\n"
          + "  msg \"<red>\ud83e\udde7═══════════════════════\ud83e\udde7</red>\"\n"
          + "  sound entity.player.levelup 1.0 1.2\n"
          + "  cmd give {player} paper{CustomModelData:1001} 1\n\n"
          + "@chain halloween_event\n"
          + "  sound entity.ghast.scream 0.5 0.5\n"
          + "  title \"<dark_purple><bold>\ud83c\udf83 万圣夜 \ud83c\udf83</bold></dark_purple>\""
          + " \"<gray>不给糖就捣蛋!</gray>\"\n"
          + "  delay 40\n"
          + "  play haunted_house\n"
          + "  wait 10\n"
          + "  title \"<gold>Trick or Treat!</gold>\" \"<gray>收集南瓜灯兑换奖励</gray>\"\n"
          + "  sound entity.witch.ambient 0.8 1.0\n"
          + "  msg \"<dark_purple>[万圣使者]</dark_purple> <gray>在地图各处寻找隐藏的南瓜灯吧, {player}!</gray>\"\n"
          + "  cmd give {player} carved_pumpkin 1\n";
  private static final String EXAMPLE_TUTORIAL_GUIDE =
      "# ═══════════════════════════════════════════════════\n"
          + "# 新手教程 - 分步骤引导新玩家\n"
          + "# 适用: 新手教程、功能引导、规则说明\n"
          + "# 执行: /cookiecamera script tutorial\n"
          + "# ═══════════════════════════════════════════════════\n\n"
          + "@chain tutorial\n"
          + "  title \"<aqua><bold>\ud83d\udcd6 新手教程</bold></aqua>\" \"<gray>跟着镜头学习基础操作</gray>\"\n"
          + "  sound entity.experience_orb.pickup 1.0 1.0\n"
          + "  delay 60\n\n"
          + "  # 第1课: 基础操作\n"
          + "  title \"<yellow>第1课</yellow>\" \"<gray>移动与交互</gray>\"\n"
          + "  sound block.note_block.pling 1.0 1.0\n"
          + "  play tutorial_movement\n"
          + "  wait 10\n"
          + "  msg \"<aqua>[教程]</aqua> <gray>使用 WASD 移动, 空格跳跃, Shift 潜行</gray>\"\n"
          + "  msg \"<aqua>[教程]</aqua> <gray>左键攻击/破坏, 右键放置/使用</gray>\"\n"
          + "  delay 80\n\n"
          + "  # 第2课: 领地保护\n"
          + "  title \"<yellow>第2课</yellow>\" \"<gray>保护你的家园</gray>\"\n"
          + "  sound block.note_block.pling 1.0 1.2\n"
          + "  play tutorial_claim\n"
          + "  wait 10\n"
          + "  msg \"<aqua>[教程]</aqua> <gray>使用金铲子圈地: 左键选点1, 右键选点2</gray>\"\n"
          + "  msg \"<aqua>[教程]</aqua> <gray>指令: /claim 查看领地信息</gray>\"\n"
          + "  delay 80\n\n"
          + "  # 第3课: 经济系统\n"
          + "  title \"<yellow>第3课</yellow>\" \"<gray>赚取金币</gray>\"\n"
          + "  sound block.note_block.pling 1.0 1.4\n"
          + "  play tutorial_economy\n"
          + "  wait 10\n"
          + "  msg \"<aqua>[教程]</aqua> <gray>出售物品: /sell hand (出售手持物品)</gray>\"\n"
          + "  msg \"<aqua>[教程]</aqua> <gray>查看余额: /bal</gray>\"\n"
          + "  msg \"<aqua>[教程]</aqua> <gray>转账: /pay <玩家> <金额></gray>\"\n"
          + "  delay 80\n\n"
          + "  # 第4课: 传送系统\n"
          + "  title \"<yellow>第4课</yellow>\" \"<gray>快速移动</gray>\"\n"
          + "  sound block.note_block.pling 1.0 1.6\n"
          + "  play tutorial_teleport\n"
          + "  wait 10\n"
          + "  msg \"<aqua>[教程]</aqua> <gray>/home set - 设置家</gray>\"\n"
          + "  msg \"<aqua>[教程]</aqua> <gray>/home - 回家</gray>\"\n"
          + "  msg \"<aqua>[教程]</aqua> <gray>/tpa <玩家> - 传送请求</gray>\"\n"
          + "  delay 60\n\n"
          + "  # 完成\n"
          + "  title \"<green><bold>✓ 教程完成!</bold></green>\" \"<gray>你已经准备好开始冒险了!</gray>\"\n"
          + "  sound entity.player.levelup 1.0 1.0\n"
          + "  cmd give {player} iron_sword 1\n"
          + "  cmd give {player} iron_pickaxe 1\n"
          + "  cmd give {player} bread 32\n"
          + "  cmd give {player} golden_shovel 1\n"
          + "  msg \"<gold>[系统]</gold> <green>恭喜完成新手教程! 获得了新手礼包!</green>\"\n";
  private static final String EXAMPLE_PVP_ARENA =
      "# ═══════════════════════════════════════════════════\n"
          + "# PvP竞技场 - 战斗开始前的热血演出\n"
          + "# 适用: PvP服、竞技场、小游戏开场\n"
          + "# 执行: /cookiecamera script arena_intro\n"
          + "# ═══════════════════════════════════════════════════\n\n"
          + "@chain arena_intro\n"
          + "  # 竞技场全景\n"
          + "  title \"<gradient:#FF4500:#FF6347>⚔ 竞技场 ⚔</gradient>\" \"<gray>荣耀之战即将开始</gray>\"\n"
          + "  sound entity.ender_dragon.flap 0.8 1.0\n"
          + "  play arena_panorama\n"
          + "  wait 10\n\n"
          + "  # 规则说明\n"
          + "  title \"<yellow>竞技规则</yellow>\" \"<gray>最后存活者获胜</gray>\"\n"
          + "  sound block.note_block.pling 1.0 1.0\n"
          + "  delay 20\n"
          + "  msg \"\"\n"
          + "  msg \"<gold>⚔═══════ 竞技规则 ═══════⚔</gold>\"\n"
          + "  msg \"<gray>  ▸ 禁止使用末影珍珠</gray>\"\n"
          + "  msg \"<gray>  ▸ 禁止搭高超过10格</gray>\"\n"
          + "  msg \"<gray>  ▸ 最后存活者获胜</gray>\"\n"
          + "  msg \"<gray>  ▸ 时间限制: 5分钟</gray>\"\n"
          + "  msg \"<gold>⚔═══════════════════════⚔</gold>\"\n"
          + "  delay 60\n\n"
          + "  # 倒计时\n"
          + "  play arena_closeup\n"
          + "  wait 0\n"
          + "  title \"<red><bold>3</bold></red>\" \"<gray>准备战斗</gray>\"\n"
          + "  sound block.anvil.land 1.0 0.5\n"
          + "  delay 20\n"
          + "  title \"<yellow><bold>2</bold></yellow>\" \"<gray>准备战斗</gray>\"\n"
          + "  sound block.anvil.land 1.0 0.8\n"
          + "  delay 20\n"
          + "  title \"<green><bold>1</bold></green>\" \"<gray>准备战斗</gray>\"\n"
          + "  sound block.anvil.land 1.0 1.0\n"
          + "  delay 20\n"
          + "  title \"<gold><bold>⚔ 开战! ⚔</bold></gold>\" \"\"\n"
          + "  sound entity.ender_dragon.growl 1.0 1.2\n"
          + "  cmd effect give {player} speed 300 0\n"
          + "  cmd effect give {player} resistance 5 2\n";
  private static final String EXAMPLE_DUNGEON_ENTRANCE =
      "# ═══════════════════════════════════════════════════\n"
          + "# 副本入口 - 进入副本前的氛围渲染\n"
          + "# 适用: RPG副本、地牢探险、冒险地图\n"
          + "# 执行: /cookiecamera script dungeon_enter\n"
          + "# ═══════════════════════════════════════════════════\n\n"
          + "@chain dungeon_enter\n"
          + "  # 入口特写\n"
          + "  sound ambient.cave 1.0 0.5\n"
          + "  title \"\" \"<dark_gray><italic>前方传来阵阵寒意...</italic></dark_gray>\"\n"
          + "  delay 40\n"
          + "  play dungeon_gate\n"
          + "  wait 10\n\n"
          + "  # 副本信息\n"
          + "  title \"<dark_purple><bold>\ud83c\udff0 暗影地牢</bold></dark_purple>\" \"<gray>难度:"
          + " ★★★☆☆</gray>\"\n"
          + "  sound block.portal.ambient 0.5 0.8\n"
          + "  delay 60\n\n"
          + "  msg \"\"\n"
          + "  msg \"<dark_purple>\ud83c\udff0═══════ 副本信息 ═══════\ud83c\udff0</dark_purple>\"\n"
          + "  msg \"<gray>  名称: </gray><dark_purple>暗影地牢</dark_purple>\"\n"
          + "  msg \"<gray>  难度: </gray><yellow>★★★</yellow><dark_gray>☆☆</dark_gray>\"\n"
          + "  msg \"<gray>  推荐等级: </gray><green>Lv.30+</green>\"\n"
          + "  msg \"<gray>  推荐人数: </gray><aqua>2-4人</aqua>\"\n"
          + "  msg \"<gray>  Boss: </gray><red>暗影骑士</red>\"\n"
          + "  msg \"<gray>  掉落: </gray><gold>暗影之剑, 暗影护甲碎片</gold>\"\n"
          + "  msg \"<dark_purple>\ud83c\udff0═══════════════════════\ud83c\udff0</dark_purple>\"\n"
          + "  delay 40\n\n"
          + "  # 进入副本\n"
          + "  play dungeon_corridor\n"
          + "  wait 0\n"
          + "  title \"<green>进入副本</green>\" \"<gray>祝你好运, {player}!</gray>\"\n"
          + "  sound block.iron_door.open 1.0 0.8\n"
          + "  cmd effect give {player} night_vision 600 0\n\n"
          + "# ── 副本入口触发器 ──\n"
          + "@trigger dungeon_gate_trigger\n"
          + "  type RIGHT_CLICK_BLOCK\n"
          + "  scene dungeon_gate\n"
          + "  world world\n"
          + "  block 200 40 300\n"
          + "  condition perm cookiecamera.dungeon\n"
          + "  cooldown 2400\n"
          + "  cmd say {player} 进入了暗影地牢!\n";
  private static final String EXAMPLE_SHOP_TOUR =
      "# ═══════════════════════════════════════════════════\n"
          + "# 商店导览 - 展示服务器商店/市场\n"
          + "# 适用: 商店系统、拍卖行、玩家市场\n"
          + "# 执行: /cookiecamera script shop_tour\n"
          + "# ═══════════════════════════════════════════════════\n\n"
          + "@chain shop_tour\n"
          + "  title \"<gold><bold>\ud83c\udfea 商业街</bold></gold>\" \"<gray>应有尽有的购物天堂</gray>\"\n"
          + "  sound block.note_block.chime 1.0 1.2\n"
          + "  delay 40\n\n"
          + "  # 系统商店\n"
          + "  play system_shop\n"
          + "  wait 0\n"
          + "  title \"<green>系统商店</green>\" \"<gray>基础物资在这里购买</gray>\"\n"
          + "  msg \"<gold>[导购]</gold> <gray>使用 /shop 打开商店菜单</gray>\"\n"
          + "  delay 60\n\n"
          + "  # 玩家商店区\n"
          + "  play player_shops\n"
          + "  wait 0\n"
          + "  title \"<aqua>玩家商店区</aqua>\" \"<gray>玩家自由开店</gray>\"\n"
          + "  msg \"<gold>[导购]</gold> <gray>使用 /ps create 创建你自己的商店!</gray>\"\n"
          + "  delay 60\n\n"
          + "  # 拍卖行\n"
          + "  play auction_house\n"
          + "  wait 0\n"
          + "  title \"<light_purple>拍卖行</light_purple>\" \"<gray>竞拍稀有物品</gray>\"\n"
          + "  msg \"<gold>[导购]</gold> <gray>使用 /ah 浏览拍卖物品</gray>\"\n"
          + "  delay 40\n\n"
          + "  msg \"\"\n"
          + "  msg \"<gold>\ud83c\udfea═══════ 商业指南 ═══════\ud83c\udfea</gold>\"\n"
          + "  msg \"<gray>  /shop</gray> <dark_gray>- 系统商店</dark_gray>\"\n"
          + "  msg \"<gray>  /ps create</gray> <dark_gray>- 创建玩家商店</dark_gray>\"\n"
          + "  msg \"<gray>  /ah</gray> <dark_gray>- 拍卖行</dark_gray>\"\n"
          + "  msg \"<gray>  /trade <玩家></gray> <dark_gray>- 安全交易</dark_gray>\"\n"
          + "  msg \"<gold>\ud83c\udfea═══════════════════════\ud83c\udfea</gold>\"\n";
  private static final String EXAMPLE_ACHIEVEMENT =
      "# ═══════════════════════════════════════════════════\n"
          + "# 成就庆祝 - 玩家达成成就时的特效\n"
          + "# 适用: 成就系统、里程碑、排行榜\n"
          + "# 执行: /cookiecamera script achievement_first_kill\n"
          + "# ═══════════════════════════════════════════════════\n\n"
          + "# ── 首次击杀Boss ──\n"
          + "@scene achievement_first_kill\n"
          + "  sound entity.player.levelup 1.0 1.0\n"
          + "  title \"<gradient:#FFD700:#FFA500><bold>\ud83c\udfc6 成就达成!</bold></gradient>\""
          + " \"<gray>首次击杀Boss</gray>\"\n"
          + "  delay 20\n"
          + "  play achievement_camera\n"
          + "  wait 0\n"
          + "  sound entity.firework_rocket.blast 1.0 1.0\n"
          + "  msg \"\"\n"
          + "  msg \"<gold>\ud83c\udfc6═══════════════════════\ud83c\udfc6</gold>\"\n"
          + "  msg \"<gold>  成就达成: <yellow>屠龙勇士</yellow></gold>\"\n"
          + "  msg \"<gray>  首次击杀末影龙!</gray>\"\n"
          + "  msg \"<gray>  奖励: <green>1000金币 + 限定称号</green></gray>\"\n"
          + "  msg \"<gold>\ud83c\udfc6═══════════════════════\ud83c\udfc6</gold>\"\n"
          + "  cmd eco give {player} 1000\n"
          + "  cmd say {player} 达成了成就: 屠龙勇士!\n\n"
          + "# ── 建造大师 ──\n"
          + "@scene achievement_builder\n"
          + "  sound entity.player.levelup 1.0 1.2\n"
          + "  title \"<gradient:#87CEEB:#4169E1><bold>\ud83c\udfd7 建造大师!</bold></gradient>\""
          + " \"<gray>放置超过100000个方块</gray>\"\n"
          + "  delay 20\n"
          + "  play builder_showcase\n"
          + "  wait 0\n"
          + "  msg \"<aqua>[成就]</aqua> <gray>你已经放置了超过 100000 个方块! 真正的建造大师!</gray>\"\n"
          + "  cmd give {player} diamond_block 5\n\n"
          + "# ── 探险家 ──\n"
          + "@chain achievement_explorer\n"
          + "  sound entity.player.levelup 1.0 0.8\n"
          + "  title \"<gradient:#32CD32:#228B22><bold>\ud83d\uddfa 探险家!</bold></gradient>\""
          + " \"<gray>探索所有生物群系</gray>\"\n"
          + "  play world_montage\n"
          + "  wait 0\n"
          + "  msg \"<green>[成就]</green> <gray>你已经探索了所有生物群系! 世界尽在脚下!</gray>\"\n"
          + "  cmd give {player} elytra 1\n"
          + "  cmd say {player} 达成了成就: 探险家! 获得了鞘翅!\n";
  private static final String EXAMPLE_STORYLINE =
      "# ═══════════════════════════════════════════════════\n"
          + "# 剧情任务 - 多幕式故事线过场动画\n"
          + "# 适用: RPG任务、主线剧情、支线故事\n"
          + "# 执行: /cookiecamera script quest_chapter1\n"
          + "# ═══════════════════════════════════════════════════\n\n"
          + "# ── 第一章: 神秘来信 ──\n"
          + "@chain quest_chapter1\n"
          + "  title \"<dark_purple><bold>第一章</bold></dark_purple>\" \"<gray>神秘来信</gray>\"\n"
          + "  sound ambient.cave 0.8 0.5\n"
          + "  delay 60\n\n"
          + "  # 场景: 玩家在村庄\n"
          + "  play village_morning\n"
          + "  wait 0\n"
          + "  msg \"<gold>[旁白]</gold> <gray>清晨, 阳光洒在宁静的村庄上...</gray>\"\n"
          + "  delay 60\n"
          + "  msg \"<gold>[旁白]</gold> <gray>一封神秘的信件出现在 {player} 的门前。</gray>\"\n"
          + "  delay 60\n\n"
          + "  # NPC对话\n"
          + "  msg \"<green>[村长]</green> <white>{player}, 你来得正好!</white>\"\n"
          + "  sound entity.villager.ambient 1.0 1.0\n"
          + "  delay 40\n"
          + "  msg \"<green>[村长]</green> <white>北方的森林里出现了奇怪的光芒...</white>\"\n"
          + "  delay 60\n"
          + "  msg \"<green>[村长]</green> <white>你能去调查一下吗?</white>\"\n"
          + "  delay 40\n\n"
          + "  # 接受任务\n"
          + "  title \"<green>任务接受</green>\" \"<gray>前往北方森林调查</gray>\"\n"
          + "  sound entity.experience_orb.pickup 1.0 1.2\n"
          + "  msg \"\"\n"
          + "  msg \"<gold>═══════ 新任务 ═══════</gold>\"\n"
          + "  msg \"<yellow>  \ud83d\udcdc 神秘来信</yellow>\"\n"
          + "  msg \"<gray>  前往北方森林调查神秘光芒</gray>\"\n"
          + "  msg \"<gray>  奖励: 500金币, 经验x2</gray>\"\n"
          + "  msg \"<gold>═══════════════════════</gold>\"\n\n"
          + "  # 指引镜头\n"
          + "  play forest_direction\n"
          + "  wait 0\n"
          + "  actionbar \"<yellow>▸ 跟随指引前往北方森林</yellow>\"\n"
          + "  cmd effect give {player} glowing 60 0\n\n"
          + "# ── 第二章: 森林深处 ──\n"
          + "@chain quest_chapter2\n"
          + "  title \"<dark_purple><bold>第二章</bold></dark_purple>\" \"<gray>森林深处</gray>\"\n"
          + "  sound ambient.cave 1.0 0.3\n"
          + "  delay 60\n\n"
          + "  play dark_forest_enter\n"
          + "  wait 0\n"
          + "  msg \"<gold>[旁白]</gold> <gray>森林越来越密, 光线逐渐暗淡...</gray>\"\n"
          + "  delay 60\n\n"
          + "  sound entity.enderman.stare 0.5 0.5\n"
          + "  msg \"<gold>[旁白]</gold> <dark_purple>一个神秘的身影出现在前方!</dark_purple>\"\n"
          + "  delay 40\n\n"
          + "  play mysterious_figure\n"
          + "  wait 0\n"
          + "  msg \"<dark_purple>[???]</dark_purple> <gray>你终于来了, {player}...</gray>\"\n"
          + "  delay 60\n"
          + "  msg \"<dark_purple>[???]</dark_purple> <gray>我等你很久了。</gray>\"\n"
          + "  delay 60\n"
          + "  msg \"<dark_purple>[???]</dark_purple> <gray>这个世界的命运... 就掌握在你手中。</gray>\"\n"
          + "  sound entity.enderman.teleport 1.0 0.8\n"
          + "  delay 20\n\n"
          + "  title \"<dark_purple><bold>未完待续...</bold></dark_purple>\" \"<gray>第三章即将开启</gray>\"\n"
          + "  sound block.portal.ambient 0.5 1.0\n\n"
          + "# ── 剧情触发器 ──\n"
          + "@trigger quest_forest_enter\n"
          + "  type REGION_ENTER\n"
          + "  scene dark_forest_enter\n"
          + "  world world\n"
          + "  region 100 60 100 200 100 200\n"
          + "  condition perm cookiecamera.quest.chapter2\n"
          + "  onetime true\n"
          + "  cmd say {player} 进入了神秘森林...\n";

  public CookieScriptEngine(CookieCamera plugin) {
    this.plugin = plugin;
    this.scriptsDir = new File(plugin.getDataFolder(), "scripts");
    if (!this.scriptsDir.exists()) {
      this.scriptsDir.mkdirs();
      this.createExampleScript();
    }
  }

  public void loadAll() {
    this.scripts.clear();
    this.sceneBlocks.clear();
    this.chainBlocks.clear();

    try {
      for (CookieScript script : CookieScriptParser.parseAll(this.scriptsDir)) {
        this.scripts.put(script.getName().toLowerCase(), script);

        for (CookieScript.AcBlock block : script.getBlocks()) {
          switch (block.getType()) {
            case SCENE:
              this.sceneBlocks.put(block.getId().toLowerCase(), block);
              break;
            case CHAIN:
              this.chainBlocks.put(block.getId().toLowerCase(), block);
              break;
            case TRIGGER:
              this.registerTriggerFromScript(block);
          }
        }
      }

      this.plugin
          .getLogger()
          .info(
              "Loaded "
                  + this.scripts.size()
                  + " AC script(s) ("
                  + this.sceneBlocks.size()
                  + " scenes, "
                  + this.chainBlocks.size()
                  + " chains).");
    } catch (IOException exception) {
      this.plugin.getLogger().log(Level.SEVERE, "Failed to load AC scripts", exception);
    }
  }

  public void execute(Player player, String blockId) {
    String key = blockId.toLowerCase();
    CookieScript.AcBlock block = this.sceneBlocks.get(key);
    if (block == null) {
      block = this.chainBlocks.get(key);
    }

    if (block == null) {
      player.sendMessage(
          MM.deserialize(
              "<red>[CookieCamera] "
                  + this.plugin.getLang().raw("script.not-found", "id", blockId)
                  + "</red>"));
    } else {
      this.executeBlock(player, block, 0);
    }
  }

  private void executeBlock(Player player, CookieScript.AcBlock block, int startIndex) {
    List<CookieScript.AcInstruction> instructions = block.getInstructions();
    int i = startIndex;

    while (i < instructions.size()) {
      CookieScript.AcInstruction inst = instructions.get(i);
      int nextIndex = i + 1;
      switch (inst.getCommand()) {
        case "play":
          String sceneId = inst.getArg(0);
          if (sceneId != null) {
            Scene scene = this.plugin.getSceneManager().getScene(sceneId);
            if (scene != null && scene.getPoints().size() >= 2) {
              this.plugin.getPlaybackManager().play(player, scene);
            }
          }
          break;
        case "wait":
          int extraTicks = 0;
          if (inst.getArg(0) != null) {
            try {
              extraTicks = Integer.parseInt(inst.getArg(0));
            } catch (NumberFormatException ignored) {
              // Keep the default value when the script uses an invalid integer.
            }
          }

          this.scheduleAfterPlayback(
              player, extraTicks, () -> this.executeBlock(player, block, nextIndex));
          return;
        case "delay":
          int ticks = 20;
          if (inst.getArg(0) != null) {
            try {
              ticks = Integer.parseInt(inst.getArg(0));
            } catch (NumberFormatException ignored) {
              // Keep the default value when the script uses an invalid integer.
            }
          }

          Scheduler.runEntityDelayed(
              this.plugin, player, () -> this.executeBlock(player, block, nextIndex), ticks);
          return;
        case "title":
          String titleText = inst.getArg(0) != null ? inst.getArg(0) : "";
          String subtitleText = inst.getArgs().length > 1 ? inst.getArg(1) : "";
          Component title = this.parseText(titleText);
          Component subtitle = this.parseText(subtitleText);
          player.showTitle(
              Title.title(
                  title,
                  subtitle,
                  Times.times(
                      Duration.ofMillis(500L), Duration.ofSeconds(3L), Duration.ofMillis(500L))));
          break;
        case "actionbar":
          {
            String text = inst.getArgJoined(0);
            if (this.plugin.shouldSendActionBar(player)) {
              player.sendActionBar(this.parseText(text));
            }
            break;
          }
        case "sound":
          String soundName = inst.getArg(0);
          float volume = 1.0F;
          float pitch = 1.0F;
          if (inst.getArgs().length > 1) {
            try {
              volume = Float.parseFloat(inst.getArg(1));
            } catch (NumberFormatException ignored) {
              // Keep the default value when the script uses an invalid number.
            }
          }

          if (inst.getArgs().length > 2) {
            try {
              pitch = Float.parseFloat(inst.getArg(2));
            } catch (NumberFormatException ignored) {
              // Keep the default value when the script uses an invalid number.
            }
          }

          if (soundName != null) {
            PointActionExecutor.playSound(player, soundName, volume, pitch);
          }
          break;
        case "cmd":
          {
            String cmd = inst.getArgJoined(0).replace("{player}", player.getName());
            Scheduler.runGlobal(
                this.plugin,
                () ->
                    this.plugin
                        .getServer()
                        .dispatchCommand(this.plugin.getServer().getConsoleSender(), cmd));
            break;
          }
        case "playercmd":
          {
            String cmd = inst.getArgJoined(0).replace("{player}", player.getName());
            player.performCommand(cmd);
            break;
          }
        case "msg":
          {
            String text = inst.getArgJoined(0).replace("{player}", player.getName());
            player.sendMessage(this.parseText(text));
            break;
          }
        case "chain":
          String chainId = inst.getArg(0);
          if (chainId != null) {
            CookieScript.AcBlock chainBlock = this.chainBlocks.get(chainId.toLowerCase());
            if (chainBlock != null) {
              this.executeBlock(player, chainBlock, 0);
            }
          }
          break;
        default:
          this.plugin.getLogger().warning("Unknown AC instruction: " + inst.getCommand());
      }

      i = nextIndex;
    }
  }

  private void scheduleAfterPlayback(Player player, int extraTicks, Runnable then) {
    Scheduler.runEntityTimer(
        this.plugin,
        player,
        h -> {
          if (!this.plugin.getPlaybackManager().isPlaying(player)) {
            h.cancel();
            if (extraTicks > 0) {
              Scheduler.runEntityDelayed(this.plugin, player, then, extraTicks);
            } else {
              then.run();
            }
          }
        },
        5L,
        5L);
  }

  private void registerTriggerFromScript(CookieScript.AcBlock block) {
    Trigger trigger = new Trigger(block.getId());

    for (CookieScript.AcInstruction inst : block.getInstructions()) {
      switch (inst.getCommand()) {
        case "type":
          Trigger.TriggerType type = Trigger.TriggerType.fromString(inst.getArg(0));
          if (type == null) {
            this.plugin
                .getLogger()
                .warning(
                    "AC trigger '"
                        + block.getId()
                        + "' has invalid type '"
                        + inst.getArg(0)
                        + "', keeping default REGION_ENTER.");
          } else {
            trigger.setType(type);
          }
          break;
        case "scene":
          trigger.setSceneId(inst.getArg(0));
          break;
        case "command":
          trigger.setCommandPattern(inst.getArgJoined(0));
          break;
        case "world":
          trigger.setWorld(inst.getArg(0));
          break;
        case "region":
          if (inst.getArgs().length >= 6) {
            try {
              trigger.setRegion(
                  Double.parseDouble(inst.getArg(0)),
                  Double.parseDouble(inst.getArg(1)),
                  Double.parseDouble(inst.getArg(2)),
                  Double.parseDouble(inst.getArg(3)),
                  Double.parseDouble(inst.getArg(4)),
                  Double.parseDouble(inst.getArg(5)));
            } catch (NumberFormatException exception) {
              this.plugin
                  .getLogger()
                  .warning(
                      "AC trigger '"
                          + block.getId()
                          + "' has invalid region coordinates: "
                          + inst.getArgJoined(0));
            }
          }
          break;
        case "block":
          if (inst.getArgs().length >= 3) {
            try {
              trigger.setBlock(
                  Integer.parseInt(inst.getArg(0)),
                  Integer.parseInt(inst.getArg(1)),
                  Integer.parseInt(inst.getArg(2)));
            } catch (NumberFormatException exception) {
              this.plugin
                  .getLogger()
                  .warning(
                      "AC trigger '"
                          + block.getId()
                          + "' has invalid block coordinates: "
                          + inst.getArgJoined(0));
            }
          }
          break;
        case "condition":
          String condType = inst.getArg(0);
          String condValue = inst.getArg(1);
          if (condType != null && condValue != null) {
            switch (condType.toLowerCase()) {
              case "hand":
                trigger.setRequiredItemInHand(condValue);
                break;
              case "inv":
              case "inventory":
                trigger.setRequiredItemInInv(condValue);
                break;
              case "perm":
              case "permission":
                trigger.setRequiredPermission(condValue);
            }
          }
          break;
        case "cooldown":
          try {
            trigger.setCooldownTicks(Integer.parseInt(inst.getArg(0)));
          } catch (NumberFormatException ignored) {
            this.plugin
                .getLogger()
                .warning(
                    "AC trigger '"
                        + block.getId()
                        + "' has invalid cooldown value: "
                        + inst.getArg(0));
          }
          break;
        case "onetime":
          trigger.setOneTime("true".equalsIgnoreCase(inst.getArg(0)));
          break;
        case "cmd":
          trigger.getCommands().add(inst.getArgJoined(0));
      }
    }

    this.plugin.getTriggerManager().registerFromScript(trigger);
  }

  private Component parseText(String text) {
    return text.contains("<") && text.contains(">")
        ? MM.deserialize(text)
        : LegacyComponentSerializer.legacyAmpersand().deserialize(text);
  }

  private void createExampleScript() {
    this.createExampleFile(
        "example.ac",
        "# ═══════════════════════════════════════════════════\n"
            + "# CookieCamera 脚本示例 - 基础用法\n"
            + "# 文件位置: plugins/CookieCamera/scripts/example.ac\n"
            + "# 执行: /cookiecamera script intro\n"
            + "# ═══════════════════════════════════════════════════\n"
            + "#\n"
            + "# ── 格式支持 ──\n"
            + "#   MiniMessage: <gradient:#ff0000:#00ff00>渐变</gradient> <bold>粗体</bold>\n"
            + "#   旧版颜色码: &6金色 &l粗体 &a绿色\n"
            + "#\n"
            + "# ── 占位符 ──\n"
            + "#   {player}  — 玩家名\n"
            + "#   {scene}   — 场景ID\n"
            + "#   {point}   — 当前点位序号\n"
            + "#   {x} {y} {z} {yaw} {pitch} — 坐标\n"
            + "#   {world}   — 世界名\n"
            + "#\n"
            + "# ── 指令类型 ──\n"
            + "#   play <场景ID>              — 播放摄像机场景\n"
            + "#   wait <额外tick>            — 等待当前场景播放完毕(+额外tick)\n"
            + "#   delay <tick>               — 延迟指定tick (20tick=1秒)\n"
            + "#   title \"标题\" \"副标题\"       — 显示大标题\n"
            + "#   actionbar \"文字\"           — 显示动作栏文字\n"
            + "#   sound <音效> <音量> <音调>  — 播放音效\n"
            + "#   cmd <指令>                 — 以控制台身份执行指令\n"
            + "#   playercmd <指令>           — 以玩家身份执行指令\n"
            + "#   msg \"消息\"                 — 发送聊天消息给玩家\n"
            + "#   chain <链ID>               — 执行另一个脚本链\n\n"
            + "# ── 基础场景播放 ──\n"
            + "@scene intro\n"
            + "  title \"<gradient:#FFD700:#FF6347>欢迎来到服务器!</gradient>\" \"<gray>享受这段旅程</gray>\"\n"
            + "  sound entity.experience_orb.pickup 1.0 1.2\n"
            + "  delay 40\n"
            + "  play my_scene\n"
            + "  wait 0\n"
            + "  actionbar \"<green>✓ 过场动画结束!</green>\"\n"
            + "  msg \"<gold>[CookieCamera]</gold> <gray>感谢观看!</gray>\"\n");
    this.createExampleFile(
        "advanced.ac",
        "# ═══════════════════════════════════════════════════\n"
            + "# CookieCamera 脚本示例 - 高级用法\n"
            + "# ═══════════════════════════════════════════════════\n\n"
            + "# ── 多场景链接 ──\n"
            + "@chain epic_tour\n"
            + "  title \"<gold><bold>史诗之旅</bold></gold>\" \"<gray>第一幕: 起源</gray>\"\n"
            + "  sound entity.ender_dragon.growl 0.5 1.0\n"
            + "  delay 40\n"
            + "  play scene_part1\n"
            + "  wait 20\n"
            + "  title \"<gold><bold>第二幕</bold></gold>\" \"<gray>征途</gray>\"\n"
            + "  sound block.beacon.activate 1.0 1.0\n"
            + "  play scene_part2\n"
            + "  wait 20\n"
            + "  title \"<gold><bold>终章</bold></gold>\" \"<gray>归来</gray>\"\n"
            + "  play scene_part3\n"
            + "  wait 0\n"
            + "  msg \"<gradient:#FFD700:#FF6347>旅程结束! 感谢 {player} 的观看!</gradient>\"\n"
            + "  cmd give {player} diamond 1\n\n"
            + "# ── 带指令奖励的场景 ──\n"
            + "@scene reward_scene\n"
            + "  title \"<green>恭喜!</green>\" \"<gray>{player} 完成了挑战</gray>\"\n"
            + "  play reward_camera\n"
            + "  wait 0\n"
            + "  cmd give {player} diamond 5\n"
            + "  cmd say {player} 完成了过场动画挑战!\n"
            + "  playercmd spawn\n"
            + "  sound entity.player.levelup 1.0 1.0\n"
            + "  msg \"<gold>你获得了 5 颗钻石!</gold>\"\n\n"
            + "# ── 倒计时效果 ──\n"
            + "@chain countdown\n"
            + "  title \"<red><bold>3</bold></red>\" \"\"\n"
            + "  sound block.note_block.pling 1.0 0.5\n"
            + "  delay 20\n"
            + "  title \"<yellow><bold>2</bold></yellow>\" \"\"\n"
            + "  sound block.note_block.pling 1.0 0.8\n"
            + "  delay 20\n"
            + "  title \"<green><bold>1</bold></green>\" \"\"\n"
            + "  sound block.note_block.pling 1.0 1.0\n"
            + "  delay 20\n"
            + "  title \"<gold><bold>开始!</bold></gold>\" \"\"\n"
            + "  sound entity.firework_rocket.blast 1.0 1.0\n"
            + "  delay 10\n"
            + "  play race_scene\n\n"
            + "# ── NPC对话效果 ──\n"
            + "@chain npc_dialog\n"
            + "  actionbar \"<yellow>村长正在说话...</yellow>\"\n"
            + "  msg \"<gold>[村长]</gold> <white>欢迎来到我们的村庄, {player}!</white>\"\n"
            + "  delay 60\n"
            + "  msg \"<gold>[村长]</gold> <white>让我带你参观一下...</white>\"\n"
            + "  delay 40\n"
            + "  play village_tour\n"
            + "  wait 0\n"
            + "  msg \"<gold>[村长]</gold> <white>希望你喜欢这里!</white>\"\n"
            + "  sound entity.villager.yes 1.0 1.0\n");
    this.createExampleFile(
        "triggers.ac",
        "# ═══════════════════════════════════════════════════\n"
            + "# CookieCamera 脚本示例 - 触发器定义\n"
            + "# 触发器在脚本加载时自动注册\n"
            + "# ═══════════════════════════════════════════════════\n\n"
            + "# ── 区域进入触发 ──\n"
            + "@trigger lobby_enter\n"
            + "  type REGION_ENTER\n"
            + "  scene lobby_intro\n"
            + "  world world\n"
            + "  region 0 60 0 20 80 20\n"
            + "  cooldown 6000\n\n"
            + "# ── 右键方块触发 ──\n"
            + "@trigger secret_button\n"
            + "  type RIGHT_CLICK_BLOCK\n"
            + "  scene secret_room\n"
            + "  world world\n"
            + "  block 100 65 200\n"
            + "  condition perm cookiecamera.secret\n"
            + "  cooldown 1200\n\n"
            + "# ── VIP专属触发 ──\n"
            + "@trigger vip_entrance\n"
            + "  type REGION_ENTER\n"
            + "  scene vip_tour\n"
            + "  world world\n"
            + "  region -10 60 -10 10 80 10\n"
            + "  condition perm cookiecamera.vip\n"
            + "  condition hand DIAMOND\n"
            + "  onetime true\n"
            + "  cmd say {player} 触发了VIP过场动画!\n");
    this.createExampleFile(
        "server_tour.ac",
        "# ═══════════════════════════════════════════════════\n"
            + "# 服务器导览 - 新玩家入服自动播放\n"
            + "# 适用: 新手引导、服务器宣传\n"
            + "# 执行: /cookiecamera script spawn_tour\n"
            + "# ═══════════════════════════════════════════════════\n\n"
            + "@chain spawn_tour\n"
            + "  title \"<gradient:#00BFFF:#7B68EE>欢迎来到 MyServer!</gradient>\""
            + " \"<gray>让我们带你看看这里</gray>\"\n"
            + "  sound entity.experience_orb.pickup 1.0 1.0\n"
            + "  delay 60\n\n"
            + "  # 第一站: 出生点广场\n"
            + "  actionbar \"<aqua>▸ 第一站: 出生点广场</aqua>\"\n"
            + "  play spawn_overview\n"
            + "  wait 10\n\n"
            + "  # 第二站: 商店区\n"
            + "  title \"<gold>商店区</gold>\" \"<gray>在这里购买你需要的物品</gray>\"\n"
            + "  sound block.note_block.harp 1.0 1.2\n"
            + "  play shop_area\n"
            + "  wait 10\n\n"
            + "  # 第三站: 竞技场\n"
            + "  title \"<red>竞技场</red>\" \"<gray>与其他玩家一决高下</gray>\"\n"
            + "  sound entity.ender_dragon.flap 0.5 1.0\n"
            + "  play arena_overview\n"
            + "  wait 10\n\n"
            + "  # 第四站: 资源世界传送门\n"
            + "  title \"<green>资源世界</green>\" \"<gray>无限资源等你探索</gray>\"\n"
            + "  sound block.portal.ambient 0.5 1.0\n"
            + "  play resource_portal\n"
            + "  wait 20\n\n"
            + "  # 结束\n"
            + "  title \"<gradient:#FFD700:#FF6347>导览结束!</gradient>\" \"<gray>祝你游戏愉快,"
            + " {player}!</gray>\"\n"
            + "  sound entity.player.levelup 1.0 1.0\n"
            + "  msg \"\"\n"
            + "  msg \"<gold>═══════════════════════════════</gold>\"\n"
            + "  msg \"<aqua>  导览已结束! 以下是常用指令:</aqua>\"\n"
            + "  msg \"<gray>  /spawn</gray> <dark_gray>- 回到出生点</dark_gray>\"\n"
            + "  msg \"<gray>  /shop</gray> <dark_gray>- 打开商店</dark_gray>\"\n"
            + "  msg \"<gray>  /tpa <玩家></gray> <dark_gray>- 传送请求</dark_gray>\"\n"
            + "  msg \"<gold>═══════════════════════════════</gold>\"\n"
            + "  cmd give {player} bread 16\n\n"
            + "# ── 新玩家自动触发 ──\n"
            + "@trigger new_player_tour\n"
            + "  type REGION_ENTER\n"
            + "  scene spawn_overview\n"
            + "  world world\n"
            + "  region -5 60 -5 5 80 5\n"
            + "  onetime true\n"
            + "  cooldown 0\n");
    this.createExampleFile(
        "boss_cinematic.ac",
        "# ═══════════════════════════════════════════════════\n"
            + "# Boss战过场动画 - 战斗前的史诗演出\n"
            + "# 适用: RPG服务器、副本入口、Boss战前\n"
            + "# 执行: /cookiecamera script boss_intro\n"
            + "# ═══════════════════════════════════════════════════\n\n"
            + "@chain boss_intro\n"
            + "  # 黑屏效果\n"
            + "  title \"<black><bold>. . .</bold></black>\" \"\"\n"
            + "  delay 40\n\n"
            + "  # 镜头拉远展示Boss巢穴\n"
            + "  title \"\" \"<dark_red><italic>一股不祥的气息弥漫开来...</italic></dark_red>\"\n"
            + "  sound ambient.cave 1.0 0.5\n"
            + "  play boss_lair_overview\n"
            + "  wait 10\n\n"
            + "  # Boss登场\n"
            + "  title \"<dark_red><bold>⚔ 暗影领主 ⚔</bold></dark_red>\" \"<gray>深渊的统治者</gray>\"\n"
            + "  sound entity.wither.spawn 1.0 0.8\n"
            + "  delay 20\n"
            + "  play boss_closeup\n"
            + "  wait 10\n\n"
            + "  # Boss台词\n"
            + "  msg \"\"\n"
            + "  msg \"<dark_red>[暗影领主]</dark_red> <gray>愚蠢的凡人... {player}...</gray>\"\n"
            + "  sound entity.ender_dragon.growl 0.8 0.6\n"
            + "  delay 60\n"
            + "  msg \"<dark_red>[暗影领主]</dark_red> <gray>你以为你能阻止我吗?</gray>\"\n"
            + "  delay 60\n"
            + "  msg \"<dark_red>[暗影领主]</dark_red> <red><bold>来吧, 让我看看你的实力!</bold></red>\"\n"
            + "  sound entity.ender_dragon.growl 1.0 1.0\n"
            + "  delay 20\n\n"
            + "  # 战斗开始\n"
            + "  chain countdown_boss\n\n"
            + "@chain countdown_boss\n"
            + "  title \"<red><bold>战斗即将开始</bold></red>\" \"\"\n"
            + "  sound block.anvil.land 1.0 0.5\n"
            + "  delay 30\n"
            + "  title \"<yellow><bold>准备战斗!</bold></yellow>\" \"\"\n"
            + "  sound block.anvil.land 1.0 0.8\n"
            + "  delay 20\n"
            + "  title \"<green><bold>⚔ 开战! ⚔</bold></green>\" \"\"\n"
            + "  sound entity.ender_dragon.flap 1.0 1.2\n"
            + "  cmd effect give {player} strength 60 1\n"
            + "  cmd effect give {player} resistance 10 0\n"
            + "  msg \"<gold>[系统]</gold> <gray>你获得了力量增益! 击败暗影领主!</gray>\"\n");
    this.createExampleFile(
        "building_showcase.ac",
        "# ═══════════════════════════════════════════════════\n"
            + "# 建筑展示 - 展示服务器建筑/玩家作品\n"
            + "# 适用: 建筑服、作品展示、延时摄影效果\n"
            + "# 执行: /cookiecamera script building_show\n"
            + "# ═══════════════════════════════════════════════════\n\n"
            + "@chain building_show\n"
            + "  title \"<gradient:#FFD700:#FFA500>✦ 建筑展示 ✦</gradient>\" \"<gray>本周最佳作品</gray>\"\n"
            + "  sound block.note_block.chime 1.0 1.0\n"
            + "  delay 60\n\n"
            + "  # 作品1: 城堡\n"
            + "  title \"<gold>中世纪城堡</gold>\" \"<gray>建造者: Steve</gray>\"\n"
            + "  sound block.note_block.harp 0.8 1.0\n"
            + "  play castle_tour\n"
            + "  wait 20\n"
            + "  msg \"<yellow>⭐</yellow> <gray>这座城堡耗时 3 周建造, 使用了超过 50000 个方块!</gray>\"\n"
            + "  delay 60\n\n"
            + "  # 作品2: 现代别墅\n"
            + "  title \"<aqua>现代别墅</aqua>\" \"<gray>建造者: Alex</gray>\"\n"
            + "  sound block.note_block.harp 0.8 1.2\n"
            + "  play villa_tour\n"
            + "  wait 20\n"
            + "  msg \"<yellow>⭐</yellow> <gray>极简主义设计, 内含红石自动化系统!</gray>\"\n"
            + "  delay 60\n\n"
            + "  # 作品3: 像素画\n"
            + "  title \"<light_purple>巨型像素画</light_purple>\" \"<gray>建造者: Notch</gray>\"\n"
            + "  sound block.note_block.harp 0.8 1.4\n"
            + "  play pixel_art_tour\n"
            + "  wait 20\n\n"
            + "  # 投票提示\n"
            + "  title \"<green>为你喜欢的作品投票!</green>\" \"<gray>/vote <作品编号></gray>\"\n"
            + "  sound entity.experience_orb.pickup 1.0 1.0\n"
            + "  msg \"\"\n"
            + "  msg \"<gold>════════ 投票 ════════</gold>\"\n"
            + "  msg \"<yellow>1.</yellow> <gray>中世纪城堡</gray> <dark_gray>- /vote 1</dark_gray>\"\n"
            + "  msg \"<yellow>2.</yellow> <gray>现代别墅</gray> <dark_gray>- /vote 2</dark_gray>\"\n"
            + "  msg \"<yellow>3.</yellow> <gray>巨型像素画</gray> <dark_gray>- /vote 3</dark_gray>\"\n"
            + "  msg \"<gold>═════════════════════</gold>\"\n");
    this.createExampleFile(
        "seasonal_event.ac",
        "# ═══════════════════════════════════════════════════\n"
            + "# 节日活动 - 季节性/节日过场动画\n"
            + "# 适用: 春节、圣诞、万圣节、周年庆等\n"
            + "# 执行: /cookiecamera script spring_festival\n"
            + "# ═══════════════════════════════════════════════════\n\n"
            + "@chain spring_festival\n"
            + "  # 烟花开场\n"
            + "  sound entity.firework_rocket.launch 1.0 1.0\n"
            + "  delay 10\n"
            + "  sound entity.firework_rocket.blast 1.0 0.8\n"
            + "  title \"<red><bold>\ud83e\udde7 新春快乐 \ud83e\udde7</bold></red>\""
            + " \"<gold>龙年大吉!</gold>\"\n"
            + "  delay 20\n"
            + "  play fireworks_scene\n"
            + "  wait 10\n\n"
            + "  # 活动介绍\n"
            + "  title \"<gold>✦ 新春活动 ✦</gold>\" \"<gray>限时开启中!</gray>\"\n"
            + "  sound block.note_block.bell 1.0 1.0\n"
            + "  play event_area_tour\n"
            + "  wait 20\n\n"
            + "  # 活动内容\n"
            + "  msg \"\"\n"
            + "  msg \"<red>\ud83e\udde7═══════ 新春活动 ═══════\ud83e\udde7</red>\"\n"
            + "  msg \"\"\n"
            + "  msg \"<gold>  \ud83c\udf81 每日签到</gold> <gray>- 连续签到7天获得限定称号</gray>\"\n"
            + "  msg \"<gold>  \ud83c\udf86 烟花大会</gold> <gray>- 每晚8点广场集合</gray>\"\n"
            + "  msg \"<gold>  \ud83c\udfee 灯谜竞猜</gold> <gray>- /riddle 参与答题赢奖品</gray>\"\n"
            + "  msg \"<gold>  \ud83d\udc09 年兽讨伐</gold> <gray>- 击败年兽获得红包</gray>\"\n"
            + "  msg \"\"\n"
            + "  msg \"<red>\ud83e\udde7═══════════════════════\ud83e\udde7</red>\"\n"
            + "  sound entity.player.levelup 1.0 1.2\n"
            + "  cmd give {player} paper{CustomModelData:1001} 1\n\n"
            + "@chain halloween_event\n"
            + "  sound entity.ghast.scream 0.5 0.5\n"
            + "  title \"<dark_purple><bold>\ud83c\udf83 万圣夜 \ud83c\udf83</bold></dark_purple>\""
            + " \"<gray>不给糖就捣蛋!</gray>\"\n"
            + "  delay 40\n"
            + "  play haunted_house\n"
            + "  wait 10\n"
            + "  title \"<gold>Trick or Treat!</gold>\" \"<gray>收集南瓜灯兑换奖励</gray>\"\n"
            + "  sound entity.witch.ambient 0.8 1.0\n"
            + "  msg \"<dark_purple>[万圣使者]</dark_purple> <gray>在地图各处寻找隐藏的南瓜灯吧, {player}!</gray>\"\n"
            + "  cmd give {player} carved_pumpkin 1\n");
    this.createExampleFile(
        "tutorial_guide.ac",
        "# ═══════════════════════════════════════════════════\n"
            + "# 新手教程 - 分步骤引导新玩家\n"
            + "# 适用: 新手教程、功能引导、规则说明\n"
            + "# 执行: /cookiecamera script tutorial\n"
            + "# ═══════════════════════════════════════════════════\n\n"
            + "@chain tutorial\n"
            + "  title \"<aqua><bold>\ud83d\udcd6 新手教程</bold></aqua>\""
            + " \"<gray>跟着镜头学习基础操作</gray>\"\n"
            + "  sound entity.experience_orb.pickup 1.0 1.0\n"
            + "  delay 60\n\n"
            + "  # 第1课: 基础操作\n"
            + "  title \"<yellow>第1课</yellow>\" \"<gray>移动与交互</gray>\"\n"
            + "  sound block.note_block.pling 1.0 1.0\n"
            + "  play tutorial_movement\n"
            + "  wait 10\n"
            + "  msg \"<aqua>[教程]</aqua> <gray>使用 WASD 移动, 空格跳跃, Shift 潜行</gray>\"\n"
            + "  msg \"<aqua>[教程]</aqua> <gray>左键攻击/破坏, 右键放置/使用</gray>\"\n"
            + "  delay 80\n\n"
            + "  # 第2课: 领地保护\n"
            + "  title \"<yellow>第2课</yellow>\" \"<gray>保护你的家园</gray>\"\n"
            + "  sound block.note_block.pling 1.0 1.2\n"
            + "  play tutorial_claim\n"
            + "  wait 10\n"
            + "  msg \"<aqua>[教程]</aqua> <gray>使用金铲子圈地: 左键选点1, 右键选点2</gray>\"\n"
            + "  msg \"<aqua>[教程]</aqua> <gray>指令: /claim 查看领地信息</gray>\"\n"
            + "  delay 80\n\n"
            + "  # 第3课: 经济系统\n"
            + "  title \"<yellow>第3课</yellow>\" \"<gray>赚取金币</gray>\"\n"
            + "  sound block.note_block.pling 1.0 1.4\n"
            + "  play tutorial_economy\n"
            + "  wait 10\n"
            + "  msg \"<aqua>[教程]</aqua> <gray>出售物品: /sell hand (出售手持物品)</gray>\"\n"
            + "  msg \"<aqua>[教程]</aqua> <gray>查看余额: /bal</gray>\"\n"
            + "  msg \"<aqua>[教程]</aqua> <gray>转账: /pay <玩家> <金额></gray>\"\n"
            + "  delay 80\n\n"
            + "  # 第4课: 传送系统\n"
            + "  title \"<yellow>第4课</yellow>\" \"<gray>快速移动</gray>\"\n"
            + "  sound block.note_block.pling 1.0 1.6\n"
            + "  play tutorial_teleport\n"
            + "  wait 10\n"
            + "  msg \"<aqua>[教程]</aqua> <gray>/home set - 设置家</gray>\"\n"
            + "  msg \"<aqua>[教程]</aqua> <gray>/home - 回家</gray>\"\n"
            + "  msg \"<aqua>[教程]</aqua> <gray>/tpa <玩家> - 传送请求</gray>\"\n"
            + "  delay 60\n\n"
            + "  # 完成\n"
            + "  title \"<green><bold>✓ 教程完成!</bold></green>\" \"<gray>你已经准备好开始冒险了!</gray>\"\n"
            + "  sound entity.player.levelup 1.0 1.0\n"
            + "  cmd give {player} iron_sword 1\n"
            + "  cmd give {player} iron_pickaxe 1\n"
            + "  cmd give {player} bread 32\n"
            + "  cmd give {player} golden_shovel 1\n"
            + "  msg \"<gold>[系统]</gold> <green>恭喜完成新手教程! 获得了新手礼包!</green>\"\n");
    this.createExampleFile(
        "pvp_arena.ac",
        "# ═══════════════════════════════════════════════════\n"
            + "# PvP竞技场 - 战斗开始前的热血演出\n"
            + "# 适用: PvP服、竞技场、小游戏开场\n"
            + "# 执行: /cookiecamera script arena_intro\n"
            + "# ═══════════════════════════════════════════════════\n\n"
            + "@chain arena_intro\n"
            + "  # 竞技场全景\n"
            + "  title \"<gradient:#FF4500:#FF6347>⚔ 竞技场 ⚔</gradient>\" \"<gray>荣耀之战即将开始</gray>\"\n"
            + "  sound entity.ender_dragon.flap 0.8 1.0\n"
            + "  play arena_panorama\n"
            + "  wait 10\n\n"
            + "  # 规则说明\n"
            + "  title \"<yellow>竞技规则</yellow>\" \"<gray>最后存活者获胜</gray>\"\n"
            + "  sound block.note_block.pling 1.0 1.0\n"
            + "  delay 20\n"
            + "  msg \"\"\n"
            + "  msg \"<gold>⚔═══════ 竞技规则 ═══════⚔</gold>\"\n"
            + "  msg \"<gray>  ▸ 禁止使用末影珍珠</gray>\"\n"
            + "  msg \"<gray>  ▸ 禁止搭高超过10格</gray>\"\n"
            + "  msg \"<gray>  ▸ 最后存活者获胜</gray>\"\n"
            + "  msg \"<gray>  ▸ 时间限制: 5分钟</gray>\"\n"
            + "  msg \"<gold>⚔═══════════════════════⚔</gold>\"\n"
            + "  delay 60\n\n"
            + "  # 倒计时\n"
            + "  play arena_closeup\n"
            + "  wait 0\n"
            + "  title \"<red><bold>3</bold></red>\" \"<gray>准备战斗</gray>\"\n"
            + "  sound block.anvil.land 1.0 0.5\n"
            + "  delay 20\n"
            + "  title \"<yellow><bold>2</bold></yellow>\" \"<gray>准备战斗</gray>\"\n"
            + "  sound block.anvil.land 1.0 0.8\n"
            + "  delay 20\n"
            + "  title \"<green><bold>1</bold></green>\" \"<gray>准备战斗</gray>\"\n"
            + "  sound block.anvil.land 1.0 1.0\n"
            + "  delay 20\n"
            + "  title \"<gold><bold>⚔ 开战! ⚔</bold></gold>\" \"\"\n"
            + "  sound entity.ender_dragon.growl 1.0 1.2\n"
            + "  cmd effect give {player} speed 300 0\n"
            + "  cmd effect give {player} resistance 5 2\n");
    this.createExampleFile(
        "dungeon_entrance.ac",
        "# ═══════════════════════════════════════════════════\n"
            + "# 副本入口 - 进入副本前的氛围渲染\n"
            + "# 适用: RPG副本、地牢探险、冒险地图\n"
            + "# 执行: /cookiecamera script dungeon_enter\n"
            + "# ═══════════════════════════════════════════════════\n\n"
            + "@chain dungeon_enter\n"
            + "  # 入口特写\n"
            + "  sound ambient.cave 1.0 0.5\n"
            + "  title \"\" \"<dark_gray><italic>前方传来阵阵寒意...</italic></dark_gray>\"\n"
            + "  delay 40\n"
            + "  play dungeon_gate\n"
            + "  wait 10\n\n"
            + "  # 副本信息\n"
            + "  title \"<dark_purple><bold>\ud83c\udff0 暗影地牢</bold></dark_purple>\" \"<gray>难度:"
            + " ★★★☆☆</gray>\"\n"
            + "  sound block.portal.ambient 0.5 0.8\n"
            + "  delay 60\n\n"
            + "  msg \"\"\n"
            + "  msg \"<dark_purple>\ud83c\udff0═══════ 副本信息 ═══════\ud83c\udff0</dark_purple>\"\n"
            + "  msg \"<gray>  名称: </gray><dark_purple>暗影地牢</dark_purple>\"\n"
            + "  msg \"<gray>  难度: </gray><yellow>★★★</yellow><dark_gray>☆☆</dark_gray>\"\n"
            + "  msg \"<gray>  推荐等级: </gray><green>Lv.30+</green>\"\n"
            + "  msg \"<gray>  推荐人数: </gray><aqua>2-4人</aqua>\"\n"
            + "  msg \"<gray>  Boss: </gray><red>暗影骑士</red>\"\n"
            + "  msg \"<gray>  掉落: </gray><gold>暗影之剑, 暗影护甲碎片</gold>\"\n"
            + "  msg"
            + " \"<dark_purple>\ud83c\udff0═══════════════════════\ud83c\udff0</dark_purple>\"\n"
            + "  delay 40\n\n"
            + "  # 进入副本\n"
            + "  play dungeon_corridor\n"
            + "  wait 0\n"
            + "  title \"<green>进入副本</green>\" \"<gray>祝你好运, {player}!</gray>\"\n"
            + "  sound block.iron_door.open 1.0 0.8\n"
            + "  cmd effect give {player} night_vision 600 0\n\n"
            + "# ── 副本入口触发器 ──\n"
            + "@trigger dungeon_gate_trigger\n"
            + "  type RIGHT_CLICK_BLOCK\n"
            + "  scene dungeon_gate\n"
            + "  world world\n"
            + "  block 200 40 300\n"
            + "  condition perm cookiecamera.dungeon\n"
            + "  cooldown 2400\n"
            + "  cmd say {player} 进入了暗影地牢!\n");
    this.createExampleFile(
        "shop_tour.ac",
        "# ═══════════════════════════════════════════════════\n"
            + "# 商店导览 - 展示服务器商店/市场\n"
            + "# 适用: 商店系统、拍卖行、玩家市场\n"
            + "# 执行: /cookiecamera script shop_tour\n"
            + "# ═══════════════════════════════════════════════════\n\n"
            + "@chain shop_tour\n"
            + "  title \"<gold><bold>\ud83c\udfea 商业街</bold></gold>\" \"<gray>应有尽有的购物天堂</gray>\"\n"
            + "  sound block.note_block.chime 1.0 1.2\n"
            + "  delay 40\n\n"
            + "  # 系统商店\n"
            + "  play system_shop\n"
            + "  wait 0\n"
            + "  title \"<green>系统商店</green>\" \"<gray>基础物资在这里购买</gray>\"\n"
            + "  msg \"<gold>[导购]</gold> <gray>使用 /shop 打开商店菜单</gray>\"\n"
            + "  delay 60\n\n"
            + "  # 玩家商店区\n"
            + "  play player_shops\n"
            + "  wait 0\n"
            + "  title \"<aqua>玩家商店区</aqua>\" \"<gray>玩家自由开店</gray>\"\n"
            + "  msg \"<gold>[导购]</gold> <gray>使用 /ps create 创建你自己的商店!</gray>\"\n"
            + "  delay 60\n\n"
            + "  # 拍卖行\n"
            + "  play auction_house\n"
            + "  wait 0\n"
            + "  title \"<light_purple>拍卖行</light_purple>\" \"<gray>竞拍稀有物品</gray>\"\n"
            + "  msg \"<gold>[导购]</gold> <gray>使用 /ah 浏览拍卖物品</gray>\"\n"
            + "  delay 40\n\n"
            + "  msg \"\"\n"
            + "  msg \"<gold>\ud83c\udfea═══════ 商业指南 ═══════\ud83c\udfea</gold>\"\n"
            + "  msg \"<gray>  /shop</gray> <dark_gray>- 系统商店</dark_gray>\"\n"
            + "  msg \"<gray>  /ps create</gray> <dark_gray>- 创建玩家商店</dark_gray>\"\n"
            + "  msg \"<gray>  /ah</gray> <dark_gray>- 拍卖行</dark_gray>\"\n"
            + "  msg \"<gray>  /trade <玩家></gray> <dark_gray>- 安全交易</dark_gray>\"\n"
            + "  msg \"<gold>\ud83c\udfea═══════════════════════\ud83c\udfea</gold>\"\n");
    this.createExampleFile(
        "achievement.ac",
        "# ═══════════════════════════════════════════════════\n"
            + "# 成就庆祝 - 玩家达成成就时的特效\n"
            + "# 适用: 成就系统、里程碑、排行榜\n"
            + "# 执行: /cookiecamera script achievement_first_kill\n"
            + "# ═══════════════════════════════════════════════════\n\n"
            + "# ── 首次击杀Boss ──\n"
            + "@scene achievement_first_kill\n"
            + "  sound entity.player.levelup 1.0 1.0\n"
            + "  title \"<gradient:#FFD700:#FFA500><bold>\ud83c\udfc6 成就达成!</bold></gradient>\""
            + " \"<gray>首次击杀Boss</gray>\"\n"
            + "  delay 20\n"
            + "  play achievement_camera\n"
            + "  wait 0\n"
            + "  sound entity.firework_rocket.blast 1.0 1.0\n"
            + "  msg \"\"\n"
            + "  msg \"<gold>\ud83c\udfc6═══════════════════════\ud83c\udfc6</gold>\"\n"
            + "  msg \"<gold>  成就达成: <yellow>屠龙勇士</yellow></gold>\"\n"
            + "  msg \"<gray>  首次击杀末影龙!</gray>\"\n"
            + "  msg \"<gray>  奖励: <green>1000金币 + 限定称号</green></gray>\"\n"
            + "  msg \"<gold>\ud83c\udfc6═══════════════════════\ud83c\udfc6</gold>\"\n"
            + "  cmd eco give {player} 1000\n"
            + "  cmd say {player} 达成了成就: 屠龙勇士!\n\n"
            + "# ── 建造大师 ──\n"
            + "@scene achievement_builder\n"
            + "  sound entity.player.levelup 1.0 1.2\n"
            + "  title \"<gradient:#87CEEB:#4169E1><bold>\ud83c\udfd7 建造大师!</bold></gradient>\""
            + " \"<gray>放置超过100000个方块</gray>\"\n"
            + "  delay 20\n"
            + "  play builder_showcase\n"
            + "  wait 0\n"
            + "  msg \"<aqua>[成就]</aqua> <gray>你已经放置了超过 100000 个方块! 真正的建造大师!</gray>\"\n"
            + "  cmd give {player} diamond_block 5\n\n"
            + "# ── 探险家 ──\n"
            + "@chain achievement_explorer\n"
            + "  sound entity.player.levelup 1.0 0.8\n"
            + "  title \"<gradient:#32CD32:#228B22><bold>\ud83d\uddfa 探险家!</bold></gradient>\""
            + " \"<gray>探索所有生物群系</gray>\"\n"
            + "  play world_montage\n"
            + "  wait 0\n"
            + "  msg \"<green>[成就]</green> <gray>你已经探索了所有生物群系! 世界尽在脚下!</gray>\"\n"
            + "  cmd give {player} elytra 1\n"
            + "  cmd say {player} 达成了成就: 探险家! 获得了鞘翅!\n");
    this.createExampleFile(
        "storyline.ac",
        "# ═══════════════════════════════════════════════════\n"
            + "# 剧情任务 - 多幕式故事线过场动画\n"
            + "# 适用: RPG任务、主线剧情、支线故事\n"
            + "# 执行: /cookiecamera script quest_chapter1\n"
            + "# ═══════════════════════════════════════════════════\n\n"
            + "# ── 第一章: 神秘来信 ──\n"
            + "@chain quest_chapter1\n"
            + "  title \"<dark_purple><bold>第一章</bold></dark_purple>\" \"<gray>神秘来信</gray>\"\n"
            + "  sound ambient.cave 0.8 0.5\n"
            + "  delay 60\n\n"
            + "  # 场景: 玩家在村庄\n"
            + "  play village_morning\n"
            + "  wait 0\n"
            + "  msg \"<gold>[旁白]</gold> <gray>清晨, 阳光洒在宁静的村庄上...</gray>\"\n"
            + "  delay 60\n"
            + "  msg \"<gold>[旁白]</gold> <gray>一封神秘的信件出现在 {player} 的门前。</gray>\"\n"
            + "  delay 60\n\n"
            + "  # NPC对话\n"
            + "  msg \"<green>[村长]</green> <white>{player}, 你来得正好!</white>\"\n"
            + "  sound entity.villager.ambient 1.0 1.0\n"
            + "  delay 40\n"
            + "  msg \"<green>[村长]</green> <white>北方的森林里出现了奇怪的光芒...</white>\"\n"
            + "  delay 60\n"
            + "  msg \"<green>[村长]</green> <white>你能去调查一下吗?</white>\"\n"
            + "  delay 40\n\n"
            + "  # 接受任务\n"
            + "  title \"<green>任务接受</green>\" \"<gray>前往北方森林调查</gray>\"\n"
            + "  sound entity.experience_orb.pickup 1.0 1.2\n"
            + "  msg \"\"\n"
            + "  msg \"<gold>═══════ 新任务 ═══════</gold>\"\n"
            + "  msg \"<yellow>  \ud83d\udcdc 神秘来信</yellow>\"\n"
            + "  msg \"<gray>  前往北方森林调查神秘光芒</gray>\"\n"
            + "  msg \"<gray>  奖励: 500金币, 经验x2</gray>\"\n"
            + "  msg \"<gold>═══════════════════════</gold>\"\n\n"
            + "  # 指引镜头\n"
            + "  play forest_direction\n"
            + "  wait 0\n"
            + "  actionbar \"<yellow>▸ 跟随指引前往北方森林</yellow>\"\n"
            + "  cmd effect give {player} glowing 60 0\n\n"
            + "# ── 第二章: 森林深处 ──\n"
            + "@chain quest_chapter2\n"
            + "  title \"<dark_purple><bold>第二章</bold></dark_purple>\" \"<gray>森林深处</gray>\"\n"
            + "  sound ambient.cave 1.0 0.3\n"
            + "  delay 60\n\n"
            + "  play dark_forest_enter\n"
            + "  wait 0\n"
            + "  msg \"<gold>[旁白]</gold> <gray>森林越来越密, 光线逐渐暗淡...</gray>\"\n"
            + "  delay 60\n\n"
            + "  sound entity.enderman.stare 0.5 0.5\n"
            + "  msg \"<gold>[旁白]</gold> <dark_purple>一个神秘的身影出现在前方!</dark_purple>\"\n"
            + "  delay 40\n\n"
            + "  play mysterious_figure\n"
            + "  wait 0\n"
            + "  msg \"<dark_purple>[???]</dark_purple> <gray>你终于来了, {player}...</gray>\"\n"
            + "  delay 60\n"
            + "  msg \"<dark_purple>[???]</dark_purple> <gray>我等你很久了。</gray>\"\n"
            + "  delay 60\n"
            + "  msg \"<dark_purple>[???]</dark_purple> <gray>这个世界的命运... 就掌握在你手中。</gray>\"\n"
            + "  sound entity.enderman.teleport 1.0 0.8\n"
            + "  delay 20\n\n"
            + "  title \"<dark_purple><bold>未完待续...</bold></dark_purple>\""
            + " \"<gray>第三章即将开启</gray>\"\n"
            + "  sound block.portal.ambient 0.5 1.0\n\n"
            + "# ── 剧情触发器 ──\n"
            + "@trigger quest_forest_enter\n"
            + "  type REGION_ENTER\n"
            + "  scene dark_forest_enter\n"
            + "  world world\n"
            + "  region 100 60 100 200 100 200\n"
            + "  condition perm cookiecamera.quest.chapter2\n"
            + "  onetime true\n"
            + "  cmd say {player} 进入了神秘森林...\n");
  }

  private void createExampleFile(String name, String content) {
    File file = new File(this.scriptsDir, name);
    if (!file.exists()) {
      try (FileWriter writer = new FileWriter(file)) {
        writer.write(content);
      } catch (IOException exception) {
        this.plugin
            .getLogger()
            .warning("Failed to create example script " + name + ": " + exception.getMessage());
      }
    }
  }

  public Map<String, CookieScript> getScripts() {
    return this.scripts;
  }

  public Map<String, CookieScript.AcBlock> getSceneBlocks() {
    return this.sceneBlocks;
  }

  public Map<String, CookieScript.AcBlock> getChainBlocks() {
    return this.chainBlocks;
  }

  public File getScriptsDir() {
    return this.scriptsDir;
  }
}


