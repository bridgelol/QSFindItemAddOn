package io.myzticbean.finditemaddon.QuickShopHandler;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.QuickShopAPI;
import com.ghostchu.quickshop.api.command.CommandContainer;
import com.ghostchu.quickshop.api.obj.QUser;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.permission.BuiltInShopPermission;
import io.myzticbean.finditemaddon.Commands.QSSubCommands.FindItemCmdHikariImpl;
import io.myzticbean.finditemaddon.FindItemAddOn;
import io.myzticbean.finditemaddon.Models.FoundShopItemModel;
import io.myzticbean.finditemaddon.Models.ShopSearchActivityModel;
import io.myzticbean.finditemaddon.Utils.Defaults.PlayerPerms;
import io.myzticbean.finditemaddon.Utils.JsonStorageUtils.HiddenShopStorageUtil;
import io.myzticbean.finditemaddon.Utils.LoggerUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Implementation of QSApi for Reremake
 *
 * @author ronsane
 */
public class QSHikariAPIHandler implements QSApi<QuickShop, Shop> {

    private QuickShopAPI api;

    public QSHikariAPIHandler() {
        api = QuickShopAPI.getInstance();
    }

    public List<FoundShopItemModel> findItemBasedOnTypeFromAllShops(ItemStack item, boolean toBuy, Player searchingPlayer) {
        List<FoundShopItemModel> shopsFoundList = new ArrayList<>();
        List<Shop> allShops;
        if (FindItemAddOn.getConfigProvider().SEARCH_LOADED_SHOPS_ONLY) {
            allShops = new ArrayList<>(api.getShopManager().getLoadedShops());
        } else {
            allShops = api.getShopManager().getAllShops();
        }
        LoggerUtils.logDebugInfo("Total shops on server: " + allShops.size());
        allShops.forEach((shop_i) -> {
            // check for quickshop hikari internal per-shop based search permission
            if (shop_i.playerAuthorize(searchingPlayer.getUniqueId(), BuiltInShopPermission.SEARCH)) {
                // check for blacklisted worlds
                if (!FindItemAddOn.getConfigProvider().getBlacklistedWorlds().contains(shop_i.getLocation().getWorld())
                        && shop_i.getItem().getType().equals(item.getType())
                        && (toBuy ? shop_i.getRemainingStock() != 0 : shop_i.getRemainingSpace() != 0)
                        && (toBuy ? shop_i.isSelling() : shop_i.isBuying())) {
                    // check for shop if hidden
                    if (!HiddenShopStorageUtil.isShopHidden(shop_i)) {
                        shopsFoundList.add(new FoundShopItemModel(
                                shop_i.getPrice(),
                                (toBuy ? shop_i.getRemainingStock() : shop_i.getRemainingSpace()),
                                shop_i.getOwner().getUniqueIdOptional().orElse(new UUID(0, 0)),
                                shop_i.getLocation(),
                                shop_i.getItem()
                        ));
                    }
                }
            }
        });
        if (!shopsFoundList.isEmpty()) {
            int sortingMethod = 2;
            try {
                sortingMethod = FindItemAddOn.getConfigProvider().SHOP_SORTING_METHOD;
            } catch (Exception e) {
                LoggerUtils.logError("Invalid value in config.yml : 'shop-sorting-method'");
                LoggerUtils.logError("Defaulting to sorting by prices method");
            }
            return QSApi.sortShops(sortingMethod, shopsFoundList);
        }
        return shopsFoundList;
    }

    public List<FoundShopItemModel> findItemBasedOnDisplayNameFromAllShops(String displayName, boolean toBuy, Player searchingPlayer) {
        List<FoundShopItemModel> shopsFoundList = new ArrayList<>();
        List<Shop> allShops;
        if (FindItemAddOn.getConfigProvider().SEARCH_LOADED_SHOPS_ONLY) {
            allShops = new ArrayList<>(api.getShopManager().getLoadedShops());
        } else {
            allShops = api.getShopManager().getAllShops();
        }
        LoggerUtils.logDebugInfo("Total shops on server: " + allShops.size());
        for (Shop shop_i : allShops) {
            // check for quickshop hikari internal per-shop based search permission
            if (shop_i.playerAuthorize(searchingPlayer.getUniqueId(), BuiltInShopPermission.SEARCH)) {
                // check for blacklisted worlds
                if (!FindItemAddOn.getConfigProvider().getBlacklistedWorlds().contains(shop_i.getLocation().getWorld())) {
                    // match the item based on query
                    if (shop_i.getItem().hasItemMeta()) {
                        if (Objects.requireNonNull(shop_i.getItem().getItemMeta()).hasDisplayName()) {
                            if (shop_i.getItem().getItemMeta().getDisplayName().toLowerCase().contains(displayName.toLowerCase())
                                    && (toBuy ? shop_i.getRemainingStock() != 0 : shop_i.getRemainingSpace() != 0)
                                    && (toBuy ? shop_i.isSelling() : shop_i.isBuying())) {
                                // check for shop if hidden
                                if (!HiddenShopStorageUtil.isShopHidden(shop_i)) {
                                    shopsFoundList.add(new FoundShopItemModel(
                                            shop_i.getPrice(),
                                            (toBuy ? shop_i.getRemainingStock() : shop_i.getRemainingSpace()),
                                            shop_i.getOwner().getUniqueIdOptional().orElse(new UUID(0, 0)),
                                            shop_i.getLocation(),
                                            shop_i.getItem()
                                    ));
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!shopsFoundList.isEmpty()) {
            int sortingMethod = 2;
            try {
                sortingMethod = FindItemAddOn.getConfigProvider().SHOP_SORTING_METHOD;
            } catch (Exception e) {
                LoggerUtils.logError("Invalid value in config.yml : 'shop-sorting-method'");
                LoggerUtils.logError("Defaulting to sorting by prices method");
            }
            return QSApi.sortShops(sortingMethod, shopsFoundList);
        }
        return shopsFoundList;
    }

    public List<FoundShopItemModel> fetchAllItemsFromAllShops(boolean toBuy, Player searchingPlayer) {
        List<FoundShopItemModel> shopsFoundList = new ArrayList<>();
        List<Shop> allShops;
        if (FindItemAddOn.getConfigProvider().SEARCH_LOADED_SHOPS_ONLY) {
            allShops = new ArrayList<>(api.getShopManager().getLoadedShops());
        } else {
            allShops = api.getShopManager().getAllShops();
        }
        LoggerUtils.logDebugInfo("Total shops on server: " + allShops.size());
        allShops.forEach((shop_i) -> {
            // check for quickshop hikari internal per-shop based search permission
            if (shop_i.playerAuthorize(searchingPlayer.getUniqueId(), BuiltInShopPermission.SEARCH)) {
                // check for blacklisted worlds
                if (!FindItemAddOn.getConfigProvider().getBlacklistedWorlds().contains(shop_i.getLocation().getWorld())
                        && (toBuy ? shop_i.getRemainingStock() != 0 : shop_i.getRemainingSpace() != 0)
                        && (toBuy ? shop_i.isSelling() : shop_i.isBuying())) {
                    // check for shop if hidden
                    if (!HiddenShopStorageUtil.isShopHidden(shop_i)) {
                        shopsFoundList.add(new FoundShopItemModel(
                                shop_i.getPrice(),
                                (toBuy ? shop_i.getRemainingStock() : shop_i.getRemainingSpace()),
                                shop_i.getOwner().getUniqueIdOptional().orElse(new UUID(0, 0)),
                                shop_i.getLocation(),
                                shop_i.getItem()
                        ));
                    }
                }
            }
        });
        if (!shopsFoundList.isEmpty()) {
            int sortingMethod = 1;
            return QSApi.sortShops(sortingMethod, shopsFoundList);
        }
        return shopsFoundList;
    }

    public Material getShopSignMaterial() {
        return com.ghostchu.quickshop.util.Util.getSignMaterial();
    }

    public Shop findShopAtLocation(Block block) {
        Location loc = new Location(block.getWorld(), block.getX(), block.getY(), block.getZ());
        return api.getShopManager().getShop(loc);
    }

    public boolean isShopOwnerCommandRunner(Player player, Shop shop) {
        return shop.getOwner().toString().equalsIgnoreCase(player.getUniqueId().toString());
    }

    @Override
    public List<Shop> getAllShops() {
        return api.getShopManager().getAllShops();
    }

    @Override
    public List<ShopSearchActivityModel> syncShopsListForStorage(List<ShopSearchActivityModel> globalShopsList) {
        // copy all shops from shops list in API to a temp globalShopsList
        // now check shops from temp globalShopsList in current globalShopsList and pull playerVisit data
        List<ShopSearchActivityModel> tempGlobalShopsList = new ArrayList<>();
        for (Shop shop_i : getAllShops()) {
            Location shopLoc = shop_i.getLocation();
            tempGlobalShopsList.add(new ShopSearchActivityModel(
                    shopLoc.getWorld().getName(),
                    shopLoc.getX(),
                    shopLoc.getY(),
                    shopLoc.getZ(),
                    shopLoc.getPitch(),
                    shopLoc.getYaw(),
                    shop_i.getOwner().toString(),
                    new ArrayList<>(),
                    false
            ));
        }

        for (ShopSearchActivityModel shop_temp : tempGlobalShopsList) {
            ShopSearchActivityModel tempShopToRemove = null;
            for (ShopSearchActivityModel shop_global : globalShopsList) {
                if (shop_temp.getWorldName().equalsIgnoreCase(shop_global.getWorldName())
                        && shop_temp.getX() == shop_global.getX()
                        && shop_temp.getY() == shop_global.getY()
                        && shop_temp.getZ() == shop_global.getZ()
                        && shop_temp.getShopOwnerUUID().equalsIgnoreCase(shop_global.getShopOwnerUUID())
                ) {
                    shop_temp.setPlayerVisitList(shop_global.getPlayerVisitList());
                    shop_temp.setHiddenFromSearch(shop_global.isHiddenFromSearch());
                    tempShopToRemove = shop_global;
                    break;
                }
            }
            if (tempShopToRemove != null)
                globalShopsList.remove(tempShopToRemove);
        }
        return tempGlobalShopsList;
    }

    /**
     * Register finditem sub-command for /qs
     * Unregister /qs find
     */
    @Override
    public void registerSubCommand() {
        LoggerUtils.logInfo("Unregistered find sub-command for /qs");
        for (CommandContainer cmdContainer : api.getCommandManager().getRegisteredCommands()) {
            if (cmdContainer.getPrefix().equalsIgnoreCase("find")) {
                api.getCommandManager().unregisterCmd(cmdContainer);
                break;
            }
        }
        LoggerUtils.logInfo("Registered finditem sub-command for /qs");
        /*
            final TextComponent textComponent = Component.text("Search for items from all shops using an interactive GUI");
            final Function<String, Component> func = x -> Component.text("Search for items from all shops using an interactive GUI");
         */
        api.getCommandManager().registerCmd(
                CommandContainer.builder()
                        .prefix("finditem")
                        .permission(PlayerPerms.FINDITEM_USE.value())
                        .hidden(false)
                        .description(locale -> Component.text("Search for items from all shops using an interactive GUI"))
                        .executor(new FindItemCmdHikariImpl())
                        .build());
    }

    private UUID convertQUserToUUID(QUser qUser) {
        Optional<UUID> uuid = qUser.getUniqueIdOptional();
        if (uuid.isPresent()) {
            return uuid.get();
        }
        String username = qUser.getUsernameOptional().orElse("Unknown");
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }
}
