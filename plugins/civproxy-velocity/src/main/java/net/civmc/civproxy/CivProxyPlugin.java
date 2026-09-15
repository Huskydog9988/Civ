package net.civmc.civproxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import net.civmc.zorweth.velocity.ZorwethVelocityPlugin;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

@Plugin(id = "civproxy", name = "CivProxy", version = "1.0.0", authors = {"Okx"}, dependencies = {
    @Dependency(id = "ajqueue"),
    @Dependency(id = "luckperms"),
    @Dependency(id = "zorweth")
})
public class CivProxyPlugin {

    private final ProxyServer server;
    private final Logger logger;

    private CommentedConfigurationNode config;

    @Inject
    public CivProxyPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;

        loadConfig(dataDirectory);
    }

    public Logger getLogger() {
        return logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        new PlayerCount(this, server).start();

        final Optional<ZorwethVelocityPlugin> zorweth = server.getPluginManager().getPlugin("zorweth")
            .flatMap(PluginContainer::getInstance)
            .map(ZorwethVelocityPlugin.class::cast);
        if (zorweth.isPresent()) {
            if (server.getPluginManager().isLoaded("ajqueue")) {
                new QueueListener(this, server, zorweth.get()).start();
            }
        } else {
            this.logger.error("Zorweth is required for route management, but its plugin instance was not available");
        }
    }

    /**
     * Loads the config from disk, and creates it if necessary
     */
    private void loadConfig(Path dataDirectory) {
        try {
            // ensure data directory exists
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }
        } catch (IOException e) {
            logger.error("Could not create data directory: {}", dataDirectory, e);
            return;
        }

        // create config file if it doesn't exist
        Path configFile = dataDirectory.resolve("config.yml");
        if (!Files.exists(configFile)) {
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile);
                    logger.info("Default configuration file created.");
                } else {
                    logger.error("Default configuration file is missing in resources!");
                    return;
                }
            } catch (IOException e) {
                logger.error("Could not create default configuration file: {}", configFile, e);
            }
        }

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(configFile).build();
        try {
            config = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Could not load configuration file: " + configFile, e);
        }
    }
}
