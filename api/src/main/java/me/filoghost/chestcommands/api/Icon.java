/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.chestcommands.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface Icon {

    boolean isBlank();

    ItemStack render(Player viewer);

    ClickResult onClick(MenuView menuView, Player clicker);

}