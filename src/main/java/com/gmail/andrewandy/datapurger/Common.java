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

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Common {

    public static String logPrefix;

    public static String colorise(final String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    public static String[] colorise(final String... strings) {
        if (strings == null) {
            return new String[0];
        }
        for (int index = 0; index < strings.length; index++) {
            strings[index] = colorise(strings[index]);
        }
        return strings;
    }

    public static void tell(final CommandSender target, final String... messages) {
        target.sendMessage(colorise(messages));
    }

    public static long toTicks(final long duration, final TimeUnit unit) {
        return unit.toMillis(duration) / 50;
    }

    public static long fromTicks(final long ticks, final TimeUnit to) {
        return TimeUnit.MILLISECONDS.convert(ticks * 50, to);
    }

    public static String capitalise(final String string) {
        if (string.length() <= 1) {
            return string.toUpperCase();
        }
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    public static void log(final Level level, final String... messages) {
        final Logger logger = DataPurger.getPlugin(DataPurger.class).getLogger();
        for (final String s : messages) {
            logger.log(level, colorise(logPrefix + " " + s));
        }
    }

}
