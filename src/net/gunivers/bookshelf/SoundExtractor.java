package net.gunivers.bookshelf;

import static net.gunivers.bookshelf.Extractor.writeJsonToFile;
import com.google.gson.JsonObject;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.world.level.block.SoundType;

public class SoundExtractor {

    public static void generateBlockSounds(String path, String fileName) {
        JsonObject blockSounds = extractBlocksSounds();
        writeJsonToFile(path + fileName + ".json", blockSounds, true);
        writeJsonToFile(path + fileName + ".min.json", blockSounds, false);
    }

    private static JsonObject extractBlocksSounds() {
        JsonObject blocksJson = new JsonObject();
        for (Field blockField : Blocks.class.getFields()) {
            try {
                Block block = (Block) blockField.get(null);
                String blockID = block.toString().substring(6, block.toString().length() - 1);
                blocksJson.add(blockID, extractBlockSounds(block));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return blocksJson;
    }

    private static JsonObject extractBlockSounds(Block block) {
        JsonObject blockJson = new JsonObject();
        SoundType sound = block.defaultBlockState().getSoundType();
        blockJson.addProperty("break", sound.getBreakSound().getLocation().toString());
        blockJson.addProperty("hit", sound.getHitSound().getLocation().toString());
        blockJson.addProperty("fall", sound.getFallSound().getLocation().toString());
        blockJson.addProperty("place", sound.getPlaceSound().getLocation().toString());
        blockJson.addProperty("step", sound.getStepSound().getLocation().toString());
        return blockJson;
    }

}