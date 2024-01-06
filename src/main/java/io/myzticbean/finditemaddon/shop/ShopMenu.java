package io.myzticbean.finditemaddon.shop;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.ScrollType;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.ScrollingGui;
import io.myzticbean.finditemaddon.ConfigUtil.ConfigSetup;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ShopMenu {
    private final Gui gui;

    public ShopMenu() {
        final FileConfiguration config = ConfigSetup.get();

        this.gui = Gui.gui().title(text("Chest Shops", null)).rows(config.getInt("shop-gui.rows", 3)).disableAllInteractions().create();

        final List<Category> categories = !config.isConfigurationSection("shop-gui.categories") ? Collections.emptyList() : Objects.requireNonNull(config.getConfigurationSection("shop-gui.categories")).getKeys(false).stream().map(key -> {
            final String prefix = "shop-gui.categories." + key + ".";
            return Category.create(
                    config.getString(prefix + "display"),
                    config.getStringList(prefix + "lore"),
                    Material.valueOf(config.getString(prefix + "display-item", "GRASS_BLOCK").toUpperCase()),
                    config.getInt(prefix + "slot", 11),
                    config.getStringList(prefix + "items").stream().map(String::toUpperCase)
                            .map(material -> {
                                try {
                                    return Material.valueOf(material);
                                } catch (IllegalArgumentException e) {
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull)
                            .toArray(Material[]::new)
            );
        }).toList();

        gui.getFiller().fill(FILLER);
        categories.forEach(category -> gui.setItem(category.getButtonSlot(), category.getButton().asGuiItem(event -> {
            event.getWhoClicked().closeInventory();
            category.open((Player) event.getWhoClicked());
        })));
    }

    public void open(HumanEntity player) {
        gui.open(player);
    }

    private static Component text(@NotNull String content, @Nullable TextColor color) {
        Component component = Component.text(content).decoration(TextDecoration.ITALIC, false);

        if (color != null) {
            component = component.color(color);
        }

        return component;
    }

    private static Component deserialize(@NotNull String content) {
        return MiniMessage.miniMessage().deserialize(content).decoration(TextDecoration.ITALIC, false);
    }


    private static final GuiItem FILLER =
            ItemBuilder.from(
                    Material.valueOf(ConfigSetup.get()
                            .getString("shop-gui.filler-item", "GRAY_STAINED_GLASS_PANE")
                            .toUpperCase()
                    )
            ).name(text(" ", null))
            .asGuiItem();

    private static class Category {
        @Getter
        private final ItemBuilder button;
        @Getter
        private final int buttonSlot;
        private final Consumer<Player> gui;
        private Category(ItemBuilder button, int buttonSlot, Consumer<Player> gui) {
            this.button = button;
            this.buttonSlot = buttonSlot;
            this.gui = gui;
        }

        public static Category create(String display, List<String> lore, Material displayType, int slot, Material... materials) {
            Component displayComponent = deserialize(display);

            return new Category(
                    ItemBuilder.from(displayType)
                            .name(displayComponent)
                            .lore(lore.stream().map(ShopMenu::deserialize).toList()),
                    slot,
                    player -> {
                        final ScrollingGui gui = Gui.scrolling().title(displayComponent.color(NamedTextColor.DARK_GRAY))
                                .rows(4)
                                .pageSize(32)
                                .scrollType(ScrollType.VERTICAL)
                                .disableAllInteractions()
                                .create();

                        final FileConfiguration config = ConfigSetup.get();

                        gui.setItem(1, 9, ItemBuilder.from(Material.valueOf(config.getString("shop-gui.category-gui.buttons.scroll-up.item", "RED_STAINED_GLASS_PANE").toUpperCase()))
                                .name(deserialize(config.getString("shop-gui.category-gui.buttons.scroll-up.display", "")))
                                .lore(config.getStringList("shop-gui.category-gui.buttons.scroll-up.lore").stream().map(ShopMenu::deserialize).toList())
                                .asGuiItem(event -> gui.previous()));

                        gui.setItem(2, 9, FILLER);
                        gui.setItem(3, 9, FILLER);

                        gui.setItem(4, 9, ItemBuilder.from(Material.valueOf(config.getString("shop-gui.category-gui.buttons.scroll-down.item", "GREEN_STAINED_GLASS_PANE").toUpperCase()))
                                .name(deserialize(config.getString("shop-gui.category-gui.buttons.scroll-down.display", "")))
                                .lore(config.getStringList("shop-gui.category-gui.buttons.scroll-down.lore").stream().map(ShopMenu::deserialize).toList())
                                .asGuiItem(event -> gui.next()));

                        for (Material material : materials) {
                            gui.addItem(ItemBuilder.from(material).name(text(WordUtils.capitalizeFully(material.name().toLowerCase().replaceAll("_", " ")), NamedTextColor.GREEN)).lore(text("Click to view shops...", NamedTextColor.GRAY)).asGuiItem(event -> {
                                event.getWhoClicked().closeInventory();
                                new ShopModeGui(material).open(event.getWhoClicked());
                            }));
                        }

                        gui.open(player);
                    }
            );
        }

        public void open(Player player) {
            gui.accept(player);
        }
    }

    private static final class ShopModeGui {

        private final Gui gui;

        public ShopModeGui(Material material) {
            final FileConfiguration config = ConfigSetup.get();
            this.gui = Gui.gui().title(text(config.getString("shop-gui.mode-gui.title", "Shop Mode"), null))
                    .rows(config.getInt("shop-gui.mode-gui.rows", 1))
                    .disableAllInteractions()
                    .create();

            gui.getFiller().fill(FILLER);

            ConfigurationSection section = config.getConfigurationSection("shop-gui.mode-gui.buttons.buy");

            if (section != null) {
                updateButton(section, true, material);
            }

            section = config.getConfigurationSection("shop-gui.mode-gui.buttons.sell");

            if (section != null) {
                updateButton(section, false, material);
            }
        }

        private void updateButton(ConfigurationSection section, boolean buy, Material material) {
            gui.setItem(section.getInt("slot"),
                    ItemBuilder.from(Material.valueOf(section.getString("item", "BEDROCK").toUpperCase()))
                    .name(deserialize(section.getString("display", "")))
                    .lore(section.getStringList("lore").stream().map(ShopMenu::deserialize).toList())
                    .asGuiItem(event -> {
                        event.getWhoClicked().closeInventory();

                        Player player = (Player) event.getWhoClicked();

                        if (buy) {
                            player.performCommand("finditem TO_BUY " + material.name());
                        } else {
                            player.performCommand("finditem TO_SELL " + material.name());
                        }
                    }));
        }

        public void open(HumanEntity player) {
            gui.open(player);
        }
    }
}
