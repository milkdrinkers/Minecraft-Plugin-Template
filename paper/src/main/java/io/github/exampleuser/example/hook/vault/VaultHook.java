package io.github.exampleuser.example.hook.vault;

import io.github.exampleuser.example.AbstractExample;
import io.github.exampleuser.example.Example;
import io.github.exampleuser.example.hook.AbstractHook;
import io.github.exampleuser.example.hook.Hook;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A hook to interface with the <a href="https://github.com/MilkBowl/VaultAPI">Vault API</a>.
 */
public class VaultHook extends AbstractHook implements Listener {
    private @Nullable RegisteredServiceProvider<Economy> rspEconomy;
    private @Nullable RegisteredServiceProvider<Permission> rspPermissions;
    private @Nullable RegisteredServiceProvider<Chat> rspChat;

    /**
     * Instantiates a new Vault hook.
     *
     * @param plugin the plugin instance
     */
    public VaultHook(Example plugin) {
        super(plugin);
    }

    @Override
    public void onEnable(AbstractExample plugin) {
        if (!isHookLoaded()) return;

        setEconomy(getPlugin().getServer().getServicesManager().getRegistration(Economy.class));
        setPermissions(getPlugin().getServer().getServicesManager().getRegistration(Permission.class));
        setChat(getPlugin().getServer().getServicesManager().getRegistration(Chat.class));
    }

    @Override
    public void onDisable(AbstractExample plugin) {
        if (!isHookLoaded()) return;

        setEconomy(null);
        setPermissions(null);
        setChat(null);
    }

    /**
     * Check if Vault is present on the server.
     *
     * @return the boolean
     */
    @Override
    public boolean isHookLoaded() {
        return isPluginEnabled(Hook.Vault.getPluginName());
    }

    /**
     * Check if a vault economy plugin is loaded.
     *
     * @return boolean
     */
    public boolean isEconomyLoaded() {
        return rspEconomy != null && getEconomy() != null;
    }

    /**
     * Gets vault economy instance. Should only be used after {@link #isEconomyLoaded()}.
     *
     * @return vault instance
     */
    public Economy getEconomy() {
        if (rspEconomy == null)
            throw new NullPointerException("The plugin tried to use Vault without it being loaded. Use the VaultHook#isHookLoaded method before using vault methods.");
        return rspEconomy.getProvider();
    }

    /**
     * Sets the vault economy service provider.
     *
     * @param rsp The service provider providing {@link Economy}
     */
    @ApiStatus.Internal
    private void setEconomy(@Nullable RegisteredServiceProvider<Economy> rsp) {
        this.rspEconomy = rsp;
    }

    /**
     * Check if a vault permissions plugin is loaded.
     *
     * @return boolean
     */
    public boolean isPermissionsLoaded() {
        return rspPermissions != null && getPermissions() != null;
    }

    /**
     * Gets vault permissions instance. Should only be used after {@link #isPermissionsLoaded()}.
     *
     * @return vault instance
     */
    public Permission getPermissions() {
        if (rspPermissions == null)
            throw new NullPointerException("The plugin tried to use Vault without it being loaded. Use the VaultHook#isHookLoaded method before using vault methods.");
        return rspPermissions.getProvider();
    }

    /**
     * Sets the vault permissions service provider.
     *
     * @param rsp The service provider providing {@link Permission}
     */
    @ApiStatus.Internal
    private void setPermissions(@Nullable RegisteredServiceProvider<Permission> rsp) {
        this.rspPermissions = rsp;
    }

    /**
     * Check if a vault chat plugin is loaded.
     *
     * @return boolean
     */
    public boolean isChatLoaded() {
        return rspChat != null && getChat() != null;
    }

    /**
     * Gets vault Chat instance. Should only be used after {@link #isChatLoaded()}.
     *
     * @return vault instance
     */
    public Chat getChat() {
        if (rspChat == null)
            throw new NullPointerException("The plugin tried to use Vault without it being loaded. Use the VaultHook#isHookLoaded method before using vault methods.");
        return rspChat.getProvider();
    }

    /**
     * Sets the vault Chat service provider.
     *
     * @param rsp The service provider providing {@link Chat}
     */
    @ApiStatus.Internal
    private void setChat(@Nullable RegisteredServiceProvider<Chat> rsp) {
        this.rspChat = rsp;
    }

    /**
     * Update the Vault hooks RegisteredServiceProviders in {@link VaultHook}. <br>This ensures the Vault hook is lazily loaded and working properly, even on reloads.
     *
     * @param e event
     */
    @SuppressWarnings({"unchecked", "unused"})
    @EventHandler
    public void onServiceRegisterEvent(ServiceRegisterEvent e) {
        RegisteredServiceProvider<?> rsp = e.getProvider();
        Object rspProvider = rsp.getProvider();
        switch (rspProvider) {
            case Economy ignored -> setEconomy((RegisteredServiceProvider<Economy>) rsp);
            case Permission ignored -> setPermissions((RegisteredServiceProvider<Permission>) rsp);
            case Chat ignored -> setChat((RegisteredServiceProvider<Chat>) rsp);
            default -> {
            }
        }
    }
}