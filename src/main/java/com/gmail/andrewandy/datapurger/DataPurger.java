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

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public final class DataPurger extends JavaPlugin {

    public static YamlConfiguration config, directories;

    public static String when;

    @Override public void onEnable() {
        // Plugin startup logic
        Common.logPrefix = "&b[Data Purger]&r";
        Common.log(Level.INFO, "Loading Configuration...");
        loadConfig();
        Purger.INSTANCE.loadSettings();
        Common.log(Level.INFO, "Setup Complete!");
        if (when.equalsIgnoreCase("startup")) {
            Purger.INSTANCE.executePurge(config.getLong("delay", 0), true);
        }
        getCommand("datapurger").setExecutor(new PurgeCommand());
    }

    public void loadConfig() {
        getDataFolder().mkdir();
        final File rawConfig = new File(getDataFolder(), "config.yml"), rawDirectories =
            new File(getDataFolder(), "directories.yml");
        config = rawConfig.isFile() ?
            YamlConfiguration.loadConfiguration(rawConfig) :
            YamlConfiguration.loadConfiguration(this.getTextResource("config.yml"));
        directories = rawDirectories.isFile() ?
            YamlConfiguration.loadConfiguration(rawDirectories) :
            YamlConfiguration.loadConfiguration(this.getTextResource("directories.yml"));
        try {
            config.save(rawConfig);
            directories.save(rawDirectories);
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
        when = config.getString("when", "command");
    }

    @Override public void onDisable() {
        if (when.equalsIgnoreCase("shutdown")) {
            Purger.INSTANCE.executePurge(0, false); //Do a sync purge.
        }
        Common.log(Level.INFO, "&aGoodbye! Data purger is now disabled.");
        // Plugin shutdown logic
    }
}
