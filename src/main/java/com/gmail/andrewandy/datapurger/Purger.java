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

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.gmail.andrewandy.datapurger.DataPurger.config;
import static com.gmail.andrewandy.datapurger.DataPurger.directories;

public enum Purger {

    INSTANCE;

    public static final String BACKUP_FOLDER = "PURGER_BACKUP";

    public static final DataPurger purger = DataPurger.getPlugin(DataPurger.class);
    private boolean shouldMove;
    private BukkitTask previous;

    public void loadSettings() {
        final String action = config.getString("todo", "move");
        shouldMove = !action.equalsIgnoreCase("delete");
    }

    public void cancel() {
        if (previous != null) {
            previous.cancel();
        }
    }

    public boolean isRunning() {
        return previous != null && !previous.isCancelled();
    }

    public BukkitTask executePurge() {
        final long delay = Common.toTicks(config.getLong("delay", 0), TimeUnit.SECONDS);
        return executePurge(delay, true);
    }

    public BukkitTask executePurge(final long delay, final boolean async) {
        if (previous != null && !previous.isCancelled()) {
            return previous;
        }
        final int defaultDays = config.getInt("lastModified", -1);
        if (defaultDays < 1) {
            throw new IllegalStateException(
                "Invalid Config Detected! Invalid Value for lastModified: " + defaultDays);
        }
        final List<String> keys =
            directories.getKeys(false).stream().map(Integer::valueOf).sorted(Integer::compareTo)
                .map(String::valueOf).collect(Collectors.toList());
        final Collection<Runnable> runnables = new HashSet<>();
        for (final String key : keys) {
            final ConfigurationSection section = directories.getConfigurationSection(key);
            assert section != null;
            final String rawPath = section.getString("path", null);
            if (rawPath == null) {
                Common.log(Level.WARNING, "&eMissing path, key: " + key);
                continue;
            }
            final String filtered =
                rawPath.replace("/", File.separator).replace("\\", File.separator);
            final Path path =
                new File(Bukkit.getWorldContainer(), File.separator + filtered).toPath();
            if (!Files.exists(path)) {
                Common.log(Level.INFO, "Skipping deletion of " + path.toString());
                continue;
            }
            final File backupPath =
                new File(purger.getDataFolder() + File.separator + BACKUP_FOLDER, filtered);
            final Collection<String> extensions = section.getStringList("extensions");
            final Collection<String> exclusions = section.getStringList("excluded_names");
            final int maxDays =
                section.isSet("max_days_since") ? defaultDays : section.getInt("max_days_since");
            backupPath.mkdirs();
            runnables.add(
                () -> purge(path, (file) -> true, backupPath, extensions, exclusions, maxDays));
        }
        final Runnable task = () -> runnables.forEach(Runnable::run);
        if (async) {
            previous = Bukkit.getScheduler().runTaskLaterAsynchronously(purger, task, delay);
        } else {
            previous = Bukkit.getScheduler().runTaskLater(purger, task, delay);
        }
        return previous;
    }

    public void purge(final Path directory, final Predicate<File> predicate,
        final File backupFolder, final Collection<String> extensions,
        final Collection<String> excluded_names, final int maxDays) {
        Common.log(Level.INFO, "Beginning Purge Task for " + directory.toString());
        final Collection<File> files;
        try {
            ensureBackups(backupFolder);
            files = Files.walk(directory).map(Path::toFile).filter(predicate).filter((file -> {
                final String name = file.getName();
                final String[] split = name.split("\\.");
                if (!excluded_names.isEmpty() && excluded_names.contains(split[0])) {
                    return false;
                }
                if (extensions.isEmpty()) {
                    return exceedsTimeLimit(file, maxDays);
                }
                for (final String extension : extensions) {
                    if (name.endsWith(extension)) {
                        return exceedsTimeLimit(file, maxDays);
                    }
                }
                return false;
            })).collect(Collectors.toList());
        } catch (final IOException ex) {
            Common.log(Level.SEVERE, "Unable to survey files for deletion, see the error below.");
            ex.printStackTrace();
            return;
        }
        final int detectedFiles = files.size();
        int shouldDelete = 0, deleted = 0;
        double percentageComplete;
        Common.log(Level.INFO, "Detected files to delete: " + detectedFiles);
        for (final File file : files) {
            percentageComplete = shouldDelete / (double) detectedFiles * 100;
            if (Math.floor(percentageComplete) % 10 == 0) {
                Common.log(Level.INFO,
                    "Deleted Count:" + shouldDelete + " Percentage Complete: " + percentageComplete
                        + "%");
            }
            shouldDelete++;
            if (shouldMove) {
                try {
                    final Path moved = new File(backupFolder, file.getName()).toPath();
                    Files.copy(file.toPath(), moved,
                        StandardCopyOption.REPLACE_EXISTING); //Move files.
                } catch (final IOException ex) {
                    Common.log(Level.SEVERE, "&cFailed to move file! Message: " + ex.getMessage());
                    return; //Don't delete if failed to move.
                }
            }
            if (file.delete()) {
                deleted++;
            } else {
                Common.log(Level.WARNING, "&eFailed to delete file: " + file.getName());
            }
        }
        final int successPercentage = (int) Math.round(deleted / (double) shouldDelete * 100);
        Common.log(Level.INFO,
            "&aPurge Complete! Deleted/Moved " + deleted + "/" + shouldDelete + " Files! ~ ("
                + successPercentage + "%)");
    }

    public void ensureBackups(final File directory) throws IOException {
        final Collection<File> files = new ArrayList<>(
            Arrays.asList(directory.listFiles() == null ? new File[0] : directory.listFiles()));
        files
            .removeIf(file -> file.isDirectory() && BACKUP_FOLDER.equalsIgnoreCase(file.getName()));
        if (files.isEmpty()) {
            return;
        }
        Common.log(Level.INFO, "Files found inside backup folder, moving them to " + BACKUP_FOLDER);
        final Path folder =
            new File(directory, BACKUP_FOLDER + File.separator + "-" + getFormattedDate()).toPath();
        Files.deleteIfExists(folder);
        Files.createDirectories(folder);
        for (final File file : files) {
            Files.move(file.toPath(), folder, StandardCopyOption.ATOMIC_MOVE);
        }
    }

    public boolean exceedsTimeLimit(final File file, final int daysLastModifiedThreshold) {
        if (!file.exists()) {
            return false;
        }
        final long daysSince =
            (Instant.now().getEpochSecond() * 1000 - file.lastModified()) / 24 / 60 / 1000;
        return (daysSince > daysLastModifiedThreshold);
    }

    public String getFormattedDate() {
        return String.valueOf(Instant.now().getEpochSecond());
    }
}
