package io.github.djxy.permissionManager.promotion;

import com.google.common.base.Preconditions;
import io.github.djxy.permissionManager.exceptions.PromotionNameExistException;
import io.github.djxy.permissionManager.logger.Logger;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.yaml.snakeyaml.DumperOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Samuel on 2016-08-17.
 */
public class Promotions {

    private static final Logger LOGGER = new Logger(Promotions.class);

    public static final Promotions instance = new Promotions();

    private final ConcurrentHashMap<String,Promotion> promotions = new ConcurrentHashMap<>();
    private Path directory;

    private Promotions(){
    }

    public Path getDirectory() {
        return directory;
    }

    public void setDirectory(Path directory) {
        Preconditions.checkNotNull(directory);

        this.directory = directory;
    }

    public synchronized void setPromotionName(Promotion promotion, String newName) throws PromotionNameExistException {
        Preconditions.checkNotNull(newName);
        Preconditions.checkNotNull(promotion);

        if(promotions.containsKey(newName))
            throw new PromotionNameExistException(newName+" already exist.");

        promotions.remove(promotion.getName());
        promotions.put(newName, promotion);

        promotion.setName(newName);
    }

    public synchronized Promotion createPromotion(String name) throws PromotionNameExistException {
        Preconditions.checkNotNull(name);

        if(promotions.containsKey(name))
            throw new PromotionNameExistException(name+" already exist.");

        Promotion promotion = new Promotion(name);

        promotions.put(name, promotion);

        return promotion;
    }

    public Promotion getPromotion(String name){
        Preconditions.checkNotNull(name);

        return promotions.get(name);
    }

    public void deletePromotion(String name){
        Preconditions.checkNotNull(name);

        promotions.remove(name);
    }

    public void load(){
        File files[] = this.directory.toFile().listFiles();

        if(files == null)
            return;

        for(File file : files)
            if(file.getName().contains("."))
                load(file.getName().substring(0, file.getName().lastIndexOf(".")));
    }

    public synchronized  boolean load(String name){
        Preconditions.checkNotNull(name);

        directory.toFile().mkdirs();

        File file = directory.resolve(name+".yml").toFile();

        if(!file.exists())
            return false;

        Promotion promotion;

        LOGGER.info("Promotion " + name + " loading started.");

        if(promotions.containsKey(name))
            promotion = promotions.get(name);
        else{
            try{
                promotion = createPromotion(name);
            }catch (Exception e){
                LOGGER.error("Promotion " + name + " loading failed.");
                e.printStackTrace();
                return false;
            }
        }

        try{
            ConfigurationLoader loader = YAMLConfigurationLoader.builder().setIndent(4).setFlowStyle(DumperOptions.FlowStyle.BLOCK).setDefaultOptions(ConfigurationOptions.defaults()).setFile(file).build();
            ConfigurationNode node = loader.load();

            promotion.deserialize(node);
        } catch (Exception e) {
            LOGGER.error("Promotion " + name + " loading failed.");
            e.printStackTrace();
            return false;
        }

        LOGGER.info("Promotion " + name + " loaded.");
        return true;
    }

    public void save(){
        Enumeration<String> names = promotions.keys();

        while(names.hasMoreElements()) {
            try {
                save(names.nextElement());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean save(String name) throws IOException {
        Preconditions.checkNotNull(name);

        directory.toFile().mkdirs();

        File file = directory.resolve(name+".yml").toFile();

        if(!promotions.containsKey(name))
            return false;

        ConfigurationLoader loader = YAMLConfigurationLoader.builder().setIndent(4).setFlowStyle(DumperOptions.FlowStyle.BLOCK).setDefaultOptions(ConfigurationOptions.defaults()).setFile(file).build();
        ConfigurationNode node = loader.createEmptyNode();

        promotions.get(name).serialize(node);

        loader.save(node);

        LOGGER.info("Promotion " + name + " saved.");
        return true;
    }

}
