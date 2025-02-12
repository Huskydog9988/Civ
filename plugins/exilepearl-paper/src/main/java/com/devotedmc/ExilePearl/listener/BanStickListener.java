package com.devotedmc.ExilePearl.listener;

import com.devotedmc.ExilePearl.ExilePearl;
import com.devotedmc.ExilePearl.ExilePearlApi;
import com.devotedmc.ExilePearl.event.PlayerPearledEvent;
import com.programmerdan.minecraft.banstick.data.BSPlayer;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;

public class BanStickListener extends RuleListener {

    public BanStickListener(ExilePearlApi pearlApi) {
        super(pearlApi);
    }

    /**
     * Prevents alts from logging in if the limit of pearled accounts is reached
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLoginEvent(AsyncPlayerPreLoginEvent e) {
        if (Bukkit.getOfflinePlayer(e.getUniqueId()).isOp()) {
            return; // Allow login if the player is an operator
        }

        if (pearlApi.getPearl(e.getUniqueId()) != null) {
            // dont lock out pearled account
            return;
        }
        if (pearlApi.getExiledAlts(e.getUniqueId(), false) >= config.maxAltsPearled()) {
            if (pearlApi.getPrimaryPearl(e.getUniqueId()).getFreedOffline()) {
                //Player is not actually pearled, but technically awaiting pearl logon.
                //therefore we simply return.
                return;
            }
            e.setLoginResult(Result.KICK_OTHER);
            e.setKickMessage(config.altBanMessage());
        }
    }

    /**
     * Kicks online alts if the limit of pearled accounts is reached
     *
     * @param e
     */
    @EventHandler
    public void playerPearl(PlayerPearledEvent e) {
        UUID uuid = e.getPearl().getPlayerId();
        if (pearlApi.getExiledAlts(uuid, false) < config.maxAltsPearled()) {
            return;
        }
        BSPlayer player = BSPlayer.byUUID(uuid);
        for (BSPlayer alt : player.getTransitiveSharedPlayers(true)) {
            if (Bukkit.getOfflinePlayer(alt.getUUID()).isOp()) {
                continue; // Don't kick ops
            }

            ExilePearl altPearl = pearlApi.getPearl(alt.getUUID());
            if (altPearl == null && !alt.getUUID().equals(uuid)) {
                Player p = Bukkit.getPlayer(alt.getUUID());
                if (p != null) {
                    p.kickPlayer(config.altBanMessage());
                }
            }
        }
    }

}
