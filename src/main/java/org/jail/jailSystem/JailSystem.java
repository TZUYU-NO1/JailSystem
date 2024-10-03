package org.jail.jailSystem;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class JailSystem extends JavaPlugin {

    private Location jailLocation;      // /jail 用の座標
    private Location jailBanLocation;   // /jail-ban 用の座標

    @Override
    public void onEnable() {
        getLogger().info("JailSystem has been enabled!");

        // config.ymlをロードして座標を設定
        saveDefaultConfig();
        loadLocationsFromConfig();

        // コマンド "jail" と "jail-ban" を登録
        this.getCommand("jail").setExecutor(new JailCommand());
        this.getCommand("jail-ban").setExecutor(new JailBanCommand());
    }

    @Override
    public void onDisable() {
        getLogger().info("JailSystem has been disabled!");
    }

    private void loadLocationsFromConfig() {
        // jail 用の座標を config.yml から読み込む
        String jailWorld = getConfig().getString("jail-location.world");
        double jailX = getConfig().getDouble("jail-location.x");
        double jailY = getConfig().getDouble("jail-location.y");
        double jailZ = getConfig().getDouble("jail-location.z");
        float jailYaw = (float) getConfig().getDouble("jail-location.yaw");
        float jailPitch = (float) getConfig().getDouble("jail-location.pitch");
        jailLocation = new Location(Bukkit.getWorld(jailWorld), jailX, jailY, jailZ, jailYaw, jailPitch);

        // jail-ban 用の座標を config.yml から読み込む
        String jailBanWorld = getConfig().getString("jail-ban-location.world");
        double jailBanX = getConfig().getDouble("jail-ban-location.x");
        double jailBanY = getConfig().getDouble("jail-ban-location.y");
        double jailBanZ = getConfig().getDouble("jail-ban-location.z");
        float jailBanYaw = (float) getConfig().getDouble("jail-ban-location.yaw");
        float jailBanPitch = (float) getConfig().getDouble("jail-ban-location.pitch");
        jailBanLocation = new Location(Bukkit.getWorld(jailBanWorld), jailBanX, jailBanY, jailBanZ, jailBanYaw, jailBanPitch);
    }

    // UUIDを直接取得し、LuckPermsに追加するメソッド
    private void jailPlayer(CommandSender sender, String playerName, Location teleportLocation) {
        Player targetPlayer = Bukkit.getPlayerExact(playerName);

        if (targetPlayer != null && targetPlayer.isOnline()) {
            String uuid = targetPlayer.getUniqueId().toString();

            // LuckPermsコマンドでUUIDを使ってグループに追加および既存のdefaultグループを削除
            Bukkit.getScheduler().runTask(this, () -> {
                // defaultグループを削除
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + uuid + " parent remove default");

                // jailグループに追加
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + uuid + " parent add jail");

                // 全体メッセージを送信
                Bukkit.broadcastMessage(ChatColor.YELLOW + "【お知らせ】" + playerName + "が投獄されました。");

                // プレイヤーをテレポート
                targetPlayer.teleport(teleportLocation);
                targetPlayer.sendMessage(ChatColor.RED + "あなたは刑務所に収監されました。");

                // 12時間後に jail グループから解除し、defaultグループに戻す
                scheduleJailRelease(playerName, uuid, 12 * 60 * 60 * 20);
            });
        } else {
            sender.sendMessage(ChatColor.RED + "Player not found or is not online.");
        }
    }

    // "/jail <player>" コマンドの実装
    public class JailCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length == 1) {
                String playerName = args[0];
                jailPlayer(sender, playerName, jailLocation);
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /jail <player>");
            }
            return true;
        }
    }

    // "/jail-ban <player>" コマンドの実装
    public class JailBanCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length == 1) {
                String playerName = args[0];
                jailPlayer(sender, playerName, jailBanLocation);
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /jail-ban <player>");
            }
            return true;
        }
    }

    // 12時間後に jail グループからプレイヤーを解除し、defaultグループに追加するスケジュール
    private void scheduleJailRelease(String playerName, String uuid, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            // jail グループからプレイヤーを解除する
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + uuid + " parent remove jail");

            // defaultグループに追加
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + uuid + " parent add default");

            Bukkit.broadcastMessage(ChatColor.YELLOW + "【お知らせ】" + playerName + " が釈放されました。");
        }, delayTicks);
    }
}
