/*
 *     This file is part of DataPurger
 *     Simple program which purge's old data
 *     Copyright (C) 2020 andrewandy
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *
 */

package com.gmail.andrewandy.datapurger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PurgeCommand implements TabExecutor {

    private final List<String> subCommands = Arrays.asList("reload", "purge", "cancel");
    private final String noPermsMessage;

    public PurgeCommand() {
        noPermsMessage = DataPurger.config.getString("noPermsMessage", "&cInsufficient Permission");
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label,
        final String[] args) {
        if (args.length == 0) {
            if (!sender.hasPermission("datapurger.info")) {
                Common.tell(sender, noPermsMessage);
                return true;
            }
            Common.tell(sender,
                "&bYou are running DataPurger version: &a" + DataPurger.getPlugin(DataPurger.class)
                    .getDescription().getVersion());
            return true;
        }
        if (!sender.hasPermission("data.purger.info")) {
            Common.tell(sender, noPermsMessage);
        }
        switch (args[0].toLowerCase()) {
            case "purge":
                if (!sender.hasPermission("datapurger.purge")) {
                    Common.tell(sender, noPermsMessage);
                    return true;
                }
                Common.tell(sender, Purger.INSTANCE.isRunning() ?
                    "&cThere is already a purge task running!" :
                    "&ePurge task will now be executed!");
                Purger.INSTANCE.executePurge();
                break;
            case "reload":
                if (!sender.hasPermission("datapurger.reload")) {
                    Common.tell(sender, noPermsMessage);
                    return true;
                }
                DataPurger.getPlugin(DataPurger.class).loadConfig();
                Common.tell(sender, "&aConfiguration Reloaded!");
                break;
            case "cancel":
                if (!sender.hasPermission("datapurger.cancel")) {
                    Common.tell(sender, noPermsMessage);
                    return true;
                }
                if (!Purger.INSTANCE.isRunning()) {
                    Common.tell(sender, "&cNo purge task is currently running.");
                    return true;
                }
                Purger.INSTANCE.cancel();
                Common.tell(sender, "&bPurge task cancelled.");
                return true;
            default:
                Common
                    .tell(sender, "&bInvalid Sub Command, available sub-commands: purger | reload");
                return false;
        }
        return true;
    }

    @Override public List<String> onTabComplete(final CommandSender sender, final Command command,
        final String alias, final String[] args) {
        if (args.length == 1) {
            return subCommands.stream()
                .filter(string -> args[0].startsWith(string) || args[0].equalsIgnoreCase(string))
                .sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
