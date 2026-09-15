package net.civmc.nameapi.velocity.renamer;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.GameProfile;
import net.civmc.nameapi.NameAPI;
import net.civmc.nameapi.velocity.NameApiVelocityPlugin;

public class PlayerRenamer {

    private final NameApiVelocityPlugin plugin;
    private final ProxyServer server;

    private final NameAPI nameAPI;

    public PlayerRenamer(NameApiVelocityPlugin plugin, ProxyServer server, NameAPI nameAPI) {
        this.plugin = plugin;
        this.server = server;
        this.nameAPI = nameAPI;
    }

    @Subscribe
    public void on(GameProfileRequestEvent requestEvent) {
        GameProfile profile = requestEvent.getGameProfile();
        nameAPI.addPlayer(profile.getName(), profile.getId());

        String name = nameAPI.getCurrentName(profile.getId());
        if (name == null) {
            throw new IllegalStateException("Unknown name for " + profile.getName());
        }

        requestEvent.setGameProfile(requestEvent.getGameProfile().withName(name));
    }

    public void start() {
        server.getEventManager().register(plugin, this);
        server.getCommandManager().register(server.getCommandManager().metaBuilder("changeplayername").aliases("nlcpn").plugin(plugin).build(),
            new ChangePlayerNameCommand(server, nameAPI));
    }
}
