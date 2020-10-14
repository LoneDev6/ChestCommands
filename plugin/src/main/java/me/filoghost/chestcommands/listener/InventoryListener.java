/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.chestcommands.listener;

import java.util.Map;
import java.util.WeakHashMap;
import me.filoghost.chestcommands.ChestCommands;
import me.filoghost.chestcommands.api.ClickResult;
import me.filoghost.chestcommands.api.Icon;
import me.filoghost.chestcommands.config.Settings;
import me.filoghost.chestcommands.inventory.DefaultMenuView;
import me.filoghost.chestcommands.menu.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;

public class InventoryListener implements Listener {

    private final MenuManager menuManager;
    private final Map<Player, Long> antiClickSpam;
    private static final Map<Player, Boolean> playerClosedMenuPressingIcon = new WeakHashMap<>();

    public InventoryListener(MenuManager menuManager) {
        this.menuManager = menuManager;
        this.antiClickSpam = new WeakHashMap<>();
    }

    public static boolean canPlayerClose_AutoOpenMenu(Player player)
    {
        return playerClosedMenuPressingIcon.containsKey(player);
    }

    public static void setCanPlayerClose_AutoOpenMenu(Player player, boolean value)
    {
        if(value)
            playerClosedMenuPressingIcon.put(player, true);
        else
            playerClosedMenuPressingIcon.remove(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.hasItem() && event.getAction() != Action.PHYSICAL) {
            menuManager.openMenuByItem(event.getPlayer(), event.getItem(), event.getAction());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onEarlyInventoryClick(InventoryClickEvent event) {
        if (MenuManager.isMenuInventory(event.getInventory())) {
            // Cancel the event as early as possible
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onLateInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        DefaultMenuView menuView = MenuManager.getOpenMenuView(inventory);
        if (menuView == null) {
            return;
        }

        // Cancel the event again just in case a plugin un-cancels it
        event.setCancelled(true);

        int slot = event.getRawSlot();
        Player clicker = (Player) event.getWhoClicked();
        Icon icon = menuView.getIcon(slot);
        if (icon == null) {
            return;
        }

        Long cooldownUntil = antiClickSpam.get(clicker);
        long now = System.currentTimeMillis();
        int minDelay = Settings.anti_click_spam_delay;

        if (minDelay > 0) {
            if (cooldownUntil != null && cooldownUntil > now) {
                return;
            } else {
                antiClickSpam.put(clicker, now + minDelay);
            }
        }

        // Only handle the click AFTER the event has finished
        Bukkit.getScheduler().runTask(ChestCommands.getPluginInstance(), () -> {
            ClickResult result = icon.onClick(menuView, clicker);

            if (result == ClickResult.CLOSE) {
                clicker.closeInventory();
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        DefaultMenuView menuView = MenuManager.getOpenMenuView(inventory);
        if (menuView == null) {
            return;
        }

        if(!menuView.getMenu().isAutoReopen())
            return;

        if(canPlayerClose_AutoOpenMenu((Player) event.getPlayer()))
        {
            setCanPlayerClose_AutoOpenMenu((Player) event.getPlayer(), false);
            return;
        }

        // Only handle the click AFTER the event has finished
        Bukkit.getScheduler().runTaskLater(ChestCommands.getPluginInstance(), () -> {
            menuView.getMenu().open((Player) event.getPlayer());
        }, 1L);
    }

}
