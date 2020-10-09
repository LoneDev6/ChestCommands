/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.chestcommands.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Callback to handle a player clicking on an icon.
 *
 * @since 1
 */
@FunctionalInterface
public interface ClickHandler {

    /**
     * Called when a player clicks on an icon associated with this handler.
     *
     * @param menuView the menu view associated with the player that clicked an icon
     * @param clicker  the player that clicked an icon
     * @return the result of this interaction (if the menu should be closed or kept open)
     * @since 1
     */
    @NotNull ClickResult onClick(@NotNull MenuView menuView, @NotNull Player clicker);

}
