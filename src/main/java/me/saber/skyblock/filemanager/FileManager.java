package me.saber.skyblock.filemanager;

import me.saber.skyblock.Main;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

public class FileManager {

    public CustomFile dataFileCustom = new CustomFile(new File(Main.getInstance().getDataFolder()+"/data.yml"));
    public CustomFile levelWorth = new CustomFile(new File(Main.getInstance().getDataFolder()+"/level-worth.yml"));

    public FileConfiguration d = dataFileCustom.getFileConfig();

    public void setup(){

        dataFileCustom.setup(false);
        levelWorth.setup(true);

    }
}