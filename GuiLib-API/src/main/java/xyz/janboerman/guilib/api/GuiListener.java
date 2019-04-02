package xyz.janboerman.guilib.api;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * The listener that listens when GUIs are opened, clicked and closed. There should only ever be one instance registered.
 * <br> That means that, when you shade and relocate GuiLib into your plugin, you have to register it yourself in your onEnable.
 * <pre>
 *     <code>
 *     {@literal @}Override
 *     public void onEnable() {
 *         getServer().getPluginManager().registerEvents(GuiListener.getInstance(), this);
 *
 *         // more initialization stuff...
 *     }
 *     </code>
 * </pre>
 * If instead you decide to use GuiLib as a runtime dependency and put the jar in your plugins folder, GuiLib registers this listener itself.
 */
public class GuiListener implements Listener {

    private static final GuiListener INSTANCE = new GuiListener();

    private final WeakHashMap<Inventory, WeakReference<GuiInventoryHolder<?>>> guiInventories = new WeakHashMap<>();

    private GuiListener() {}

    /**
     * Gets the GuiListener.
     * @return the Gui listener singleton instance
     */
    public static GuiListener getInstance() {
        return INSTANCE;
    }


    // ===== registering stuff =====

    /**
     * Registers an inventory gui.
     *
     * @param holder the gui holder
     * @param inventory the inventory that holds the gui item stacks
     * @return true if the gui was registered successfully, otherwise false
     */
    public boolean registerGui(GuiInventoryHolder<?> holder, Inventory inventory) {
        return guiInventories.putIfAbsent(inventory, new WeakReference<>(holder)) == null;
    }

    /**
     * Substitute for {@link Inventory#getHolder()} for gui inventories.
     * @param inventory the inventory
     * @return the holder - or null if no holder was registered with the inventory.
     */
    public GuiInventoryHolder<?> getHolder(Inventory inventory){
        WeakReference<GuiInventoryHolder<?>> reference = guiInventories.get(inventory);
        if (reference == null) return null;

        return reference.get();
    }

    /**
     * Checks whether the Inventory is registered with the gui inventory holder.
     *
     * @param inventory the inventory that is maybe registered with the holder
     * @param holder the holder to check
     * @return whether the holder and inventory are registered
     */
    public boolean isGuiRegistered(GuiInventoryHolder<?> holder, Inventory inventory) {
        WeakReference<GuiInventoryHolder<?>> reference = guiInventories.get(inventory);
        if (reference == null) return false;

        return reference.get() == holder; //yes, reference equality!
    }


    // ===== event stuff =====

    private void onGuiInventoryEvent(InventoryEvent event, Consumer<GuiInventoryHolder> action) {
        if (event.getInventory() == null) return;

        GuiInventoryHolder<?> guiHolder;
        if (event.getInventory().getHolder() instanceof GuiInventoryHolder) {
            guiHolder = (GuiInventoryHolder) event.getInventory().getHolder();
        } else {
            guiHolder = getHolder(event.getInventory());
        }

        if (guiHolder != null && guiHolder.getPlugin().isEnabled()) {
            action.accept(guiHolder);
        }
    }

    /**
     * Delegates the InventoryOpenEvent to the {@link GuiInventoryHolder} if the top inventory is held by a Gui and the event is not cancelled.
     * @param event the InventoryOpenEvent
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        onGuiInventoryEvent(event, gui -> gui.onOpen(event));
    }

    /**
     * Delegates the InventoryClickEvent to the {@link GuiInventoryHolder} if the top inventory is held by a Gui and the event is not cancelled.
     * InventoryClickEvents are cancelled before they are passed to the Gui.
     * @param event the InventoryClickEvent
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        onGuiInventoryEvent(event, gui -> {
            event.setCancelled(true);
            gui.onClick(event);
        });
    }

    /**
     * Delegates the InventoryDragEvent to the {@link GuiInventoryHolder} if the top inventory is held by a Gui and the event is not cancelled.
     * InventoryDragEvents are cancelled before they are passed to the Gui.
     * @param event the InventoryDragEvent
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDragEvent(InventoryDragEvent event) {
        onGuiInventoryEvent(event, gui -> {
            event.setCancelled(true);
            gui.onDrag(event);
        });
    }

    /**
     * Delegates the InventoryCloseEvent to the {@link GuiInventoryHolder} if the top inventory is held by a Gui.
     * @param event InventoryCloseEvent
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        onGuiInventoryEvent(event, gui -> gui.onClose(event));
    }

}
