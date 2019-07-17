package org.savage.skyblock;

import com.sun.jna.Memory;
import jdk.nashorn.internal.runtime.arrays.ArrayIndex;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.savage.skyblock.island.Island;
import org.savage.skyblock.island.quests.Quest;
import org.savage.skyblock.island.warps.IslandWarp;
import org.savage.skyblock.island.MemoryPlayer;
import org.savage.skyblock.island.upgrades.Upgrade;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Utils {

    public void log(String message){
        Bukkit.getLogger().info("\n"+message+"\n");
    }

    public String color(String s){
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public String capitalizeFirstLetter(String original) {
        if (original == null || original.length() == 0) {
            return original;
        }
        original = original.toLowerCase();
        return original.substring(0, 1).toUpperCase() + original.substring(1);
    }

    public List<String> color(List<String> list){
        List<String> color = new ArrayList<>();
        for (String s : list){
            color.add(color(s));
        }
        return color;
    }

    public ItemStack createItem(String materialName, int data, String name, List<String> lore, int amount){
        Material material = null;
        if (Materials.requestXMaterial(materialName, (byte)data) != null && Materials.requestXMaterial(materialName, (byte)data).parseMaterial() != null){
            material = Materials.requestXMaterial(materialName, (byte)data).parseMaterial();
        }else{
            material = Material.valueOf(materialName.toUpperCase());
        }
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(color(name));
        meta.setLore(color(lore));
        itemStack.setAmount(amount);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public String getMessage(String key){
        return color(SkyBlock.getInstance().getFileManager().getMessages().getFileConfig().getString("messages." + key));
    }

    public List<String> getMessageList(String key) {
        return color(SkyBlock.getInstance().getFileManager().getMessages().getFileConfig().getStringList("messages." + key));
    }

    public int getSettingInt(String key){
        return SkyBlock.getInstance().getConfig().getInt("settings." + key);
    }
    public String getSettingString(String key) {
        return SkyBlock.getInstance().getConfig().getString("settings." + key);
    }
    public double getSettingDouble(String key){
        return SkyBlock.getInstance().getConfig().getDouble("settings." + key);
    }

    public boolean getSettingBool(String key) {
        return SkyBlock.getInstance().getConfig().getBoolean("settings." + key);
    }

    public static Location randomLocation(Location min, Location max) {
        Location range = new Location(min.getWorld(), Math.abs(max.getX() - min.getX()), min.getY(), Math.abs(max.getZ() - min.getZ()));
        return new Location(min.getWorld(), (Math.random() * range.getX()) + (min.getX() <= max.getX() ? min.getX() : max.getX()), range.getY(), (Math.random() * range.getZ()) + (min.getZ() <= max.getZ() ? min.getZ() : max.getZ()));
    }

    public Location generateIslandLocation(Location min, Location max){
        Location randomLocation = randomLocation(min, max);
        if (randomLocation != null){
            //check this location's distance from all preexisting islands's locations
            if (!Storage.islandList.isEmpty()) {
                for (Island island : Storage.islandList) {
                    Location location = island.getLocation();

                    double distance = randomLocation.distanceSquared(location);
                    if (distance >= 100) {
                        return randomLocation;
                    }
                }
            }else{
                return randomLocation;
            }
        }
        return generateIslandLocation(min, max);
    }

    public void savePlayers(){
        List<String> data = new ArrayList<>();
        for (MemoryPlayer memoryPlayer : Storage.memoryPlayerList){
            if (memoryPlayer == null) continue;

            UUID uuid = memoryPlayer.getUuid();
            String islandName = "";
            int resets = memoryPlayer.getResets();

            if (memoryPlayer.getIsland() != null){
                islandName = memoryPlayer.getIsland().getName();
            }

            int playTime = memoryPlayer.getPlayTime();
            int playerKills = memoryPlayer.getPlayerKills();
            int mobKills = memoryPlayer.getMobKills();
            int deaths = memoryPlayer.getDeaths();

            String blocksPlacedString = "";
            String blocksBrokenString = "";
            String completedQuests = "";
            double moneySpent = memoryPlayer.getMoneySpent();

            try{
                blocksPlacedString = serializeObject(memoryPlayer.getBlocksPlaced());
                blocksBrokenString = serializeObject(memoryPlayer.getBlocksBroke());
            }catch(IOException e){
                e.printStackTrace();
            }

            for (Quest quest : memoryPlayer.getCompletedQuests()){
                String type = quest.getQuestType().name().toUpperCase();
                int id = quest.getId();
                if (completedQuests.equalsIgnoreCase("")){
                    completedQuests = type+","+id;
                }else{
                    completedQuests = completedQuests+"-"+type+","+id;
                }
            }

            data.add(uuid.toString()+";"+islandName+";"+resets+";"+playTime+";"+blocksPlacedString+";"+blocksBrokenString+";"+completedQuests+";"+moneySpent+";"+playerKills+";"+mobKills+";"+deaths);
        }
        SkyBlock.getInstance().getFileManager().getPlayerData().getFileConfig().set("players", data);
        SkyBlock.getInstance().getFileManager().getPlayerData().saveFile();
    }

    public void loadPlayer(UUID pUUID){
        List<String> data = SkyBlock.getInstance().getFileManager().getPlayerData().getFileConfig().getStringList("players");
        MemoryPlayer memoryPlayer = null;
        if (!hasMemoryPlayer(pUUID)) {
            for (String playerData : data) {
                String[] l = playerData.split(";");
                //data.add(uuid.toString()+";"+islandName+";"+resets+";"+playTime+";"+blocksPlacedString+";"+blocksBrokenString);
                UUID uuid = UUID.fromString(l[0]);
                if (pUUID.equals(uuid)) {

                    String islandName = l[1];
                    int islandResets = 0;
                    int playTime = 0;

                    if (!l[2].equalsIgnoreCase("")) {
                        try {
                            islandResets = Integer.parseInt(l[2]);
                        } catch (NumberFormatException e) {
                        }
                    }
                    if (!l[3].equalsIgnoreCase("")) {
                        try {
                            playTime = Integer.parseInt(l[3]);
                        } catch (NumberFormatException e) {
                        }
                    }

                    HashMap<String, Integer> blocksPlacedMap = new HashMap<>();
                    HashMap<String, Integer> blockBrokenMap = new HashMap<>();

                    List<Quest> completedQuests = new ArrayList<>();

                    double moneySpent = 0;
                    int mobKills = 0;
                    int playerKills = 0;
                    int deaths = 0;

                    if (!l[4].equalsIgnoreCase("")){
                        try{
                            blocksPlacedMap = (HashMap<String, Integer>) deserializeObject(l[4]);
                        }catch(ClassNotFoundException | IOException e){ }
                    }
                    if (!l[5].equalsIgnoreCase("")){
                        try{
                            blockBrokenMap = (HashMap<String, Integer>) deserializeObject(l[5]);
                        }catch(ClassNotFoundException | IOException e){ }
                    }

                    try {
                        if (!l[6].equalsIgnoreCase("")) {
                            //completedQuests = completedQuests+"-"+type+","+id;
                            if (l[6].contains("-")) {
                                //multiple
                                String[] list = l[6].split("-");
                                for (String s : list) {
                                    String type = s.split(",")[0];
                                    int id = Integer.parseInt(s.split(",")[1]);
                                    //fetch the quest
                                    Quest quest = SkyBlock.getInstance().getQuests().getQuest(id, Quest.QuestType.valueOf(type));
                                    completedQuests.add(quest);
                                }
                            } else {
                                //single
                                String type = l[6].split(",")[0];
                                int id = Integer.parseInt(l[6].split(",")[1]);
                                Quest quest = SkyBlock.getInstance().getQuests().getQuest(id, Quest.QuestType.valueOf(type));
                                completedQuests.add(quest);
                            }
                        }

                        try {
                            moneySpent = Double.parseDouble(l[7]);

                            playerKills = Integer.parseInt(l[8]);
                            mobKills = Integer.parseInt(l[9]);
                            deaths = Integer.parseInt(l[10]);
                        }catch(NumberFormatException e){ }

                    }catch(ArrayIndexOutOfBoundsException e){ }

                    //create memory player
                    memoryPlayer = new MemoryPlayer(uuid);

                    memoryPlayer.setPlayerKills(playerKills);
                    memoryPlayer.setMobKills(mobKills);
                    memoryPlayer.setDeaths(deaths);

                    memoryPlayer.setResets(islandResets);
                    memoryPlayer.setPlayTime(playTime);

                    memoryPlayer.setBlocksBroke(blockBrokenMap);
                    memoryPlayer.setBlocksPlaced(blocksPlacedMap);

                    memoryPlayer.setCompletedQuests(completedQuests);

                    memoryPlayer.setMoneySpent(moneySpent);

                    if (!islandName.equalsIgnoreCase("")){
                        Island island = SkyBlock.getInstance().getIslandUtils().getIslandFromName(islandName);
                        if (island != null){
                            memoryPlayer.setIsland(island);
                        }
                    }
                    if (Bukkit.getPlayer(pUUID) != null){
                        memoryPlayer.setPlayer(Bukkit.getPlayer(pUUID));
                    }
                }
            }
            if (memoryPlayer == null){
                //couldn't find any data to load. we make new
                memoryPlayer = new MemoryPlayer(pUUID);
                Island island = SkyBlock.getInstance().getIslandUtils().getIsland(pUUID);
                if (island != null){
                    memoryPlayer.setIsland(island);
                }
                memoryPlayer.setResets(0);
            }
        }
    }

    public void loadIslands(){
        //onEnable
        //layout: ownerUUID;member1,member2;x;y;z;protectionRadius
        List<String> data = SkyBlock.getInstance().getFileManager().getData().getFileConfig().getStringList("data");

        for (String islandData : data){
            String[] l = islandData.split(";");
            UUID ownerUUID = UUID.fromString(l[0]);
            double x = Double.parseDouble(l[4]);
            double y = Double.parseDouble(l[5]);
            double z = Double.parseDouble(l[6]);
            int protectionRadius = Integer.parseInt(l[7]);
            String home = l[8];
            String biome = l[9];

            boolean memberPlace = Boolean.parseBoolean(l[10]);
            boolean memberBreak = Boolean.parseBoolean(l[11]);
            boolean memberInteract = Boolean.parseBoolean(l[12]);

            boolean officerPlace = Boolean.parseBoolean(l[13]);
            boolean officerBreak = Boolean.parseBoolean(l[14]);
            boolean officerInteract = Boolean.parseBoolean(l[15]);

            String name = l[16];
            HashMap<Upgrade, Integer> upgradesMap = new HashMap<>();

            Location homeLocation = null;

            if (deserializeLocation(home) != null && !home.equalsIgnoreCase("")){
                homeLocation = deserializeLocation(home);
            }


            String[] l2 = l[1].split(",");
            String[] l3 = l[2].split(",");
            String[] l4 = l[3].split(",");

            List<UUID> memberList = new ArrayList<>();
            List<UUID> officerList = new ArrayList<>();
            List<UUID> coOwnerList = new ArrayList<>();

            if (!Arrays.asList(l2).isEmpty()) {
                for (String s : l2) {
                    if (!s.equalsIgnoreCase("")) {
                        memberList.add(UUID.fromString(s));
                    }
                }
            }
            if (!Arrays.asList(l3).isEmpty()) {
                for (String s : l3) {
                    if (!s.equalsIgnoreCase("")) {
                        officerList.add(UUID.fromString(s));
                    }
                }
            }
            if (!Arrays.asList(l4).isEmpty()) {
                for (String s : l4) {
                    if (!s.equalsIgnoreCase("")) {
                        coOwnerList.add(UUID.fromString(s));
                    }
                }
            }

            try {
                String upgradeString = l[17];
                if (!upgradeString.equalsIgnoreCase("")) {
                    for (String upgrades : upgradeString.split(",")) {
                        int id = Integer.parseInt(upgrades.split("!")[0]);
                        int tier = Integer.parseInt(upgrades.split("!")[1]);
                        upgradesMap.put(Upgrade.Upgrades.getUpgrade(id), tier);
                    }
                }
            }catch(ArrayIndexOutOfBoundsException e){}

            double bankBalance = getDoublesFromString(l[18]);

            Island island = new Island("", x, y, z, ownerUUID, coOwnerList, officerList, memberList, protectionRadius, name, bankBalance);

            String bankData = l[19];
            if (!bankData.equalsIgnoreCase("")){
                //try to convert it
                island.createBank(bankData);
            }

            try {
                String islandWarpString = l[20];
                if (!islandWarpString.equalsIgnoreCase("")) {

                    if (islandWarpString.contains(":")) {
                        //has multiple island warps
                        String[] warpList = islandWarpString.split(":");
                        for (String warp : warpList) {
                            String warpName = warp.split("!")[0];
                            String warpLocation = warp.split("!")[1];

                            IslandWarp islandWarp = new IslandWarp(island, warpName, deserializeLocation(warpLocation));
                            island.addIslandWarp(islandWarp);
                        }
                    } else {
                        //only has 1 island warp
                        String warpName = islandWarpString.split("!")[0];
                        String warpLocation = islandWarpString.split("!")[1];
                        IslandWarp islandWarp = new IslandWarp(island, warpName, deserializeLocation(warpLocation));
                        island.addIslandWarp(islandWarp);
                    }
                }
            }catch(ArrayIndexOutOfBoundsException e){ }

            island.setUpgradeMap(upgradesMap);

            if (name.equalsIgnoreCase("")) {
                if (!SkyBlock.getInstance().getIslandUtils().isIslandName(getNameFromUUID(ownerUUID))){
                    island.setName(getNameFromUUID(ownerUUID));
                }else{
                    //taken
                    island.setName(getNameFromUUID(ownerUUID)+"-1");
                }
            }

            island.setPermissionMemberPlace(memberPlace);
            island.setPermissionMemberBreak(memberBreak);
            island.setPermissionMemberInteract(memberInteract);

            island.setPermissionOfficerPlace(officerPlace);
            island.setPermissionOfficerBreak(officerBreak);
            island.setPermissionOfficerInteract(officerInteract);

            if (homeLocation != null){
                island.setHome(homeLocation);
            }else{
                island.setHome(island.getLocation());
            }
            //island.setBiome(Biome.valueOf(biome));
        }
    }

    public void saveIslands(){
        //layout: ownerUUID;member1,member2;x,y,z;protectionRadius

        // SkyBlock.getInstance().getFileManager().d.set("data", new ArrayList<>());
        // SkyBlock.getInstance().getFileManager().dataFileCustom.saveFile();

        List<String> islandData = new ArrayList<>();

        for (Island island : Storage.islandList){
            UUID owner = island.getOwnerUUID();
            double x = island.getCenterX();
            double y = island.getCenterY();
            double z = island.getCenterZ();
            List<UUID> members = island.getMemberList();
            List<UUID> officers = island.getOfficerList();
            List<UUID> coowners = island.getCoownerList();
            int protectionRadius = island.getProtectionRadius();
            String home = serializeLocation(island.getHome());
            String biome = island.getBiome().name();
            String name = island.getName();
            HashMap<Upgrade, Integer> upgradesMap = island.getUpgrade_tier();
            double bankBalance = island.getBankBalance();
            String inventoryData = itemStackArrayToBase64(island.getBank().getContents());

            String upgradeString = "";
            String memberList = "";
            String officerList = "";
            String coOwnerList = "";

            String islandWarpsString = "";

            if (!island.getIslandWarps().isEmpty()){
                //not empty
                for (IslandWarp islandWarp : island.getIslandWarps()){
                    String warpName = islandWarp.getName();
                    String warpLocation = serializeLocation(islandWarp.getLocation());
                    if (islandWarpsString.equalsIgnoreCase("")){
                        //empty
                        islandWarpsString = warpName+"!"+warpLocation;
                    }else{
                        //is not empty
                        islandWarpsString = islandWarpsString + ":"+warpName+"!"+warpLocation;
                    }
                }
            }

            if (!members.isEmpty()){
                for (UUID uuid : members){
                    if (memberList.equalsIgnoreCase("")){
                        memberList = uuid.toString();
                    }else{
                        memberList = memberList+","+uuid.toString();
                    }
                }
            }
            if (!officers.isEmpty()){
                for (UUID uuid : officers){
                    if (officerList.equalsIgnoreCase("")){
                        officerList = uuid.toString();
                    }else{
                        officerList = officerList+","+uuid.toString();
                    }
                }
            }
            if (!coowners.isEmpty()){
                for (UUID uuid : coowners){
                    if (coOwnerList.equalsIgnoreCase("")){
                        coOwnerList = uuid.toString();
                    }else{
                        coOwnerList = coOwnerList+","+uuid.toString();
                    }
                }
            }

            for (Upgrade upgrade : upgradesMap.keySet()){
                int tier = upgradesMap.get(upgrade);
                int id = upgrade.getId();
                if (upgradeString.equalsIgnoreCase("")) {
                    //empty
                    upgradeString = id+"!"+tier;
                }else{
                    upgradeString = upgradeString+","+id+"!"+tier;
                }
            }

            islandData.add(owner.toString() + ";" + memberList + ";" + officerList + ";" + coOwnerList+ ";" + x + ";" + y + ";" + z + ";" +
                    protectionRadius + ";" + home + ";" + biome + ";" +
                    island.canMemberPlace() + ";" + island.canMemberBreak() + ";" + island.canMemberInteract() + ";" +
                    island.canOfficerPlace() + ";" + island.canOfficerBreak() + ";" + island.canOfficerInteract() + ";" + name+";"+upgradeString+";"+bankBalance+";"+inventoryData+";"+islandWarpsString);
        }

        SkyBlock.getInstance().getFileManager().getData().getFileConfig().set("data", islandData);
        SkyBlock.getInstance().getFileManager().getData().saveFile();
    }

    public double getBalance(UUID uuid){
        return SkyBlock.getEcon().getBalance(Bukkit.getOfflinePlayer(uuid));
    }

    public void takeMoney(UUID uuid, double amount){
        SkyBlock.getEcon().withdrawPlayer(Bukkit.getOfflinePlayer(uuid), amount);
    }

    public void addMoney(UUID uuid, double amount){
        SkyBlock.getEcon().depositPlayer(Bukkit.getOfflinePlayer(uuid), amount);
    }


    public List<Block> getBlocksBetweenPoints(Location l1, Location l2) {
        List<Block> blocks = new ArrayList<Block>();
        int topBlockX = (l1.getBlockX() < l2.getBlockX() ? l2.getBlockX() : l1.getBlockX());
        int bottomBlockX = (l1.getBlockX() > l2.getBlockX() ? l2.getBlockX() : l1.getBlockX());
        int topBlockY = (l1.getBlockY() < l2.getBlockY() ? l2.getBlockY() : l1.getBlockY());
        int bottomBlockY = (l1.getBlockY() > l2.getBlockY() ? l2.getBlockY() : l1.getBlockY());
        int topBlockZ = (l1.getBlockZ() < l2.getBlockZ() ? l2.getBlockZ() : l1.getBlockZ());
        int bottomBlockZ = (l1.getBlockZ() > l2.getBlockZ() ? l2.getBlockZ() : l1.getBlockZ());
        for (int x = bottomBlockX; x <= topBlockX; x++) {
            for (int y = bottomBlockY; y <= topBlockY; y++) {
                for (int z = bottomBlockZ; z <= topBlockZ; z++) {
                    Block block = l1.getWorld().getBlockAt(x, y, z);
                    blocks.add(block);
                }
            }
        }
        return blocks;
    }

    public List<Block> getNearbyBlocks(Location location, int radius) {
        List<Block> blocks = new ArrayList<Block>();
        for(int x = location.getBlockX() - radius; x <= location.getBlockX() + radius; x++) {
            for(int y = location.getBlockY() - radius; y <= location.getBlockY() + radius; y++) {
                for(int z = location.getBlockZ() - radius; z <= location.getBlockZ() + radius; z++) {
                    blocks.add(location.getWorld().getBlockAt(x, y, z));
                }
            }
        }
        return blocks;
    }

    public String getNameFromUUID(UUID uuid) {
        if (Bukkit.getPlayer(uuid) != null) {
            return Bukkit.getPlayer(uuid).getName();
        } else {
            return Bukkit.getOfflinePlayer(uuid).getName();
        }
    }

    public int getIntegersFromString(String string) {
        return Integer.parseInt(string.replaceAll("[\\D]", ""));
    }
    public double getDoublesFromString(String string) {
        return Double.parseDouble(string.replaceAll("[\\D]", ""));
    }

    public String stripIntegersFromString(String string) {
        return string.replaceAll("[0-9]", "");
    }

    public Location deserializeLocation(String locationString){
        try {
            String[] l = locationString.split(",");
            return new Location(Bukkit.getWorld(l[0]), Double.parseDouble(l[1]), Double.parseDouble(l[2]), Double.parseDouble(l[3]));
        }catch(ArrayIndexOutOfBoundsException e){
            return null;
        }
    }

    public String serializeLocation(Location location){
        return location.getWorld().getName()+","+location.getX()+","+location.getY()+","+location.getZ();
    }

    public boolean hasMemoryPlayer(UUID uuid){
        for (MemoryPlayer memoryPlayer : Storage.memoryPlayerList){
            if (memoryPlayer.getUuid().equals(uuid)){
                return true;
            }
        }
        return false;
    }

    public MemoryPlayer getMemoryPlayer(UUID uuid){
        for (MemoryPlayer memoryPlayer : Storage.memoryPlayerList){
            if (memoryPlayer.getUuid().equals(uuid)){
                return memoryPlayer;
            }
        }
        return null;
    }

    public boolean hasPermissionAtleast(UUID uuid, String permissionBase){ // uses the cache class
        if (Bukkit.getPlayer(uuid) != null && Bukkit.getPlayer(uuid).isOp()){
            return true;
        }
        if (hasMemoryPlayer(uuid)) {
            return getMemoryPlayer(uuid).hasPermission(permissionBase);
        }
        return false;
    }

    public String convertTime(int time) {
        String result = String.valueOf(Math.round((System.currentTimeMillis() / 1000L - time) / 36.0D) / 100.0D);
        return (result.length() == 3 ? result + "0" : result) + "/hrs ago";
    }

    public enum time{
        DAYS, HOURS, MINUTES
    }

    public double convertSeconds(int seconds, Enum typeTime){
        if (typeTime.equals(time.DAYS)){
            return TimeUnit.SECONDS.toDays(seconds);
        }
        if (typeTime.equals(time.HOURS)){
            return TimeUnit.SECONDS.toHours(seconds);
        }
        if (typeTime.equals(time.MINUTES)){
            return TimeUnit.SECONDS.toMinutes(seconds);
        }
        return 0;
    }

    public String formatNumber(final String s) {
        double amount = Double.parseDouble(s);
        if (amount > 0) {
            final DecimalFormat formatter = new DecimalFormat("#,###.00");
            final String number = formatter.format(amount);
            return number;
        }else{
            return amount+"";
        }
    }



    public String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            // Write the size of the inventory
            dataOutput.writeInt(items.length);

            // Save every element in the list
            for (int i = 0; i < items.length; i++) {
                dataOutput.writeObject(items[i]);
            }

            // Serialize that array
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    public ItemStack[] itemStackArrayFromBase64(String data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];

            // Read the serialized inventory
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            dataInput.close();
            return items;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }

    public String createBar(int barNum, int progress, int totalProgress){
        String yesBar = SkyBlock.getInstance().getFileManager().getQuestFile().getFileConfig().getString("placeholder.progressBar-Yes");
        String noBar = SkyBlock.getInstance().getFileManager().getQuestFile().getFileConfig().getString("placeholder.progressBar-No");

        if (progress > barNum){
            progress = barNum;
        }
        if (progress > totalProgress){
            progress = barNum;
        }

        String progressBar = IntStream.range(0, progress).mapToObj(i -> yesBar).collect(Collectors.joining("")); // adds goodBars that many times...
        progressBar = progressBar + IntStream.range(0, Math.subtractExact(barNum, progress)).mapToObj(i -> noBar).collect(Collectors.joining("")); // adds badBars that many times...
        return progressBar;
    }

    private void collectDigits(int num, List<Integer> digits) {
        if(num / 10 > 0) {
            collectDigits(num / 10, digits);
        }
        digits.add(num % 10);
    }

    public Integer[] getDigits(int num) {
        if (num < 0) { return new Integer[0]; }
        List<Integer> digits = new ArrayList<Integer>();
        collectDigits(num, digits);
        Collections.reverse(digits);
        return digits.toArray(new Integer[]{});
    }

    public String serializeObject(Serializable o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public Object deserializeObject(String s) throws IOException,
            ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(s);
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(data));
        Object o = ois.readObject();
        ois.close();
        return o;
    }


}