package io.github.exampleuser.example.hook.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.exampleuser.example.AbstractExample;
import io.github.exampleuser.example.Example;
import io.github.exampleuser.example.hook.AbstractHook;
import io.github.exampleuser.example.hook.Hook;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;

/**
 * A hook that enables API for PacketEvents.
 */
public class PacketEventsHook extends AbstractHook {
    /**
     * Instantiates a new PacketEvents hook.
     *
     * @param plugin the plugin instance
     */
    public PacketEventsHook(Example plugin) {
        super(plugin);
    }

    @Override
    public void onLoad(AbstractExample plugin) {
        if (!isPluginPresent(Hook.PacketEvents.getPluginName()))
            return;

        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(getPlugin()));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable(AbstractExample plugin) {
        if (!isPluginEnabled(Hook.PacketEvents.getPluginName()))
            return;

        PacketEvents.getAPI().init();
    }

    @Override
    public void onDisable(AbstractExample plugin) {
        if (!isPluginEnabled(Hook.PacketEvents.getPluginName()))
            return;

        PacketEvents.getAPI().terminate();
    }

    @Override
    public boolean isHookLoaded() {
        return isPluginPresent(Hook.PacketEvents.getPluginName()) && PacketEvents.getAPI().isLoaded();
    }
}
