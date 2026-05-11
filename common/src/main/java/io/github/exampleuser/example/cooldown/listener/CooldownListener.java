package io.github.exampleuser.example.cooldown.listener;

import io.github.exampleuser.example.AbstractExample;
import io.github.exampleuser.example.cooldown.Cooldowns;
import io.github.exampleuser.example.database.Queries;
import io.github.milkdrinkers.threadutil.Scheduler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@SuppressWarnings({"unused", "FieldCanBeLocal", "CodeBlock2Expr"})
class CooldownListener implements Listener {
    private final AbstractExample plugin;

    public CooldownListener(AbstractExample plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Scheduler.async(() -> {
                Queries.Cooldown.load(e.getPlayer()).forEach((cooldownType, instant) -> {
                    Cooldowns.set(e.getPlayer(), cooldownType, instant);
                });
            })
            .execute();

    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Scheduler.async(() -> {
                Queries.Cooldown.save(e.getPlayer());
                Cooldowns.removeAll(e.getPlayer());
            })
            .execute();
    }
}
