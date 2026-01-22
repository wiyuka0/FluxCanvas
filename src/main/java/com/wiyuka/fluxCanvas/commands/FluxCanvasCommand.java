package com.wiyuka.fluxCanvas.commands;

import com.wiyuka.fluxCanvas.config.ConfigManager;
import com.wiyuka.fluxCanvas.renderer.FluxScreen;
import com.wiyuka.fluxCanvas.renderer.ScreenManager;
import com.wiyuka.fluxCanvas.tools.ScreenBuilder;
import com.wiyuka.fluxCanvas.tools.ScreenTools;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FluxCanvasCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只有玩家可以使用此命令！");
            return true;
        }

        if (!player.hasPermission("flux.admin")) {
            player.sendMessage("§c你没有权限执行此操作 (flux.admin)");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
            case "start":
                handleCreate(player);
                break;

            case "build":
            case "confirm":
                handleBuild(player);
                break;

            case "cancel":
                handleCancel(player);
                break;

            case "remove":
            case "delete":
                handleRemove(player);
                break;

            case "killall":
                handleKillAll(player);
                break;

            case "list":
                handleList(player);
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    // --- 子命令逻辑 ---

    private void handleCreate(Player player) {
        Block targetBlockPoint = player.getTargetBlockExact(10);
        BlockFace blockFace = player.getTargetBlockFace(10);
        if (targetBlockPoint == null || blockFace == null) {
            player.sendMessage("§c目标太远或无效！");
            return;
        }


        Block target = targetBlockPoint.getLocation().add(
                blockFace.getDirection().getX(),
                blockFace.getDirection().getY(),
                blockFace.getDirection().getZ()
        ).getBlock();


        ScreenBuilder.startBuilding(player, target.getLocation());
        player.sendMessage("§a[Flux] 起点已设定！");
        player.sendMessage("§7现在请看向对角线的终点方块，并输入 §f/flux build");
    }

    private void handleBuild(Player player) {
        if (!ScreenBuilder.isBuilding(player)) {
            player.sendMessage("§c你还没有设定起点！请先使用 /flux create");
            return;
        }

        Block target = player.getTargetBlockExact(10);
        if (target == null || target.getType().isAir()) {
            player.sendMessage("§c请对准一个方块作为屏幕的【右下角】终点！");
            return;
        }
        int maxCreate = ConfigManager.getCurrentConfig().playerMaxScreens;

        try {
            int playerCreated = ScreenManager.getPlayerCreated().get(player.getUniqueId().toString());
            if (playerCreated > maxCreate) player.sendMessage("§c你已经创建了超过" + maxCreate + "个屏幕！");
        }catch(NullPointerException e) {
            if(maxCreate == 0) player.sendMessage("§c你已经创建了超过" + maxCreate + "个屏幕！");
        }

        ScreenBuilder.buildScreen(player);
    }

    private void handleCancel(Player player) {
        if (ScreenBuilder.isBuilding(player)) {
            ScreenBuilder.cancelBuilding(player);
            player.sendMessage("§e[Flux] 已取消构建预览。");
        } else {
            player.sendMessage("§c当前没有正在进行的构建任务。");
        }
    }

    private void handleRemove(Player player) {
        FluxScreen targetScreen = ScreenTools.getScreenLookingAt(player, 10);

        if (targetScreen != null) {
            String id = targetScreen.getId();
            ScreenManager.removeScreen(id);
            player.sendMessage("§a[Flux] 成功销毁屏幕: " + ChatColor.YELLOW + id);
        } else {
            player.sendMessage("§c你没有注视任何 Flux 屏幕。");
        }
    }

    private void handleKillAll(Player player) {
        int count = ScreenManager.getAllScreens().size();
        ScreenManager.removeAll();
        player.sendMessage("§c[Flux] 已移除 " + count + " 个屏幕实例。");
    }

    private void handleList(Player player) {
        player.sendMessage("§e=== 当前加载的屏幕 ===");
        ScreenManager.getAllScreens().forEach((id, screen) -> {
            player.sendMessage("§7- ID: " + id + " Loc: " + screen.getOriginLocation().toVector());
        });
    }

    private void sendHelp(Player player) {
        player.sendMessage("§b§lFluxCanvas 帮助菜单");
        player.sendMessage("§7/flux create   - §f设定屏幕起点 (看着方块)");
        player.sendMessage("§7/flux build    - §f确认屏幕终点并生成 (看着方块)");
        player.sendMessage("§7/flux cancel   - §f取消预览");
        player.sendMessage("§7/flux remove   - §f销毁看着的屏幕");
        player.sendMessage("§7/flux killall  - §c清理全服屏幕");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("create", "build", "cancel", "remove", "killall", "list");
            return subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
