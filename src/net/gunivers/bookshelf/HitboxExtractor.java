package net.gunivers.bookshelf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.data.Main;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.phys.Vec3;

public class HitboxExtractor {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java HitboxExtractor <minecraft_version>");
            return;
        }

        try {
            Main.main(new String[] { "--validate" });
        } catch (Exception e) {
            System.out.println("Main call of the Minecraft client's init failed.");
            e.printStackTrace();
            return;
        }

        JsonObject blockShapes = extractBlockRegistryShapes();
        writeJsonToFile("generated/" + args[0] + "/blocks/shapes.json", blockShapes, true);
        writeJsonToFile("generated/" + args[0] + "/blocks/shapes.min.json", blockShapes, false);
    }

    /**
     * Extracts block shapes from the Minecraft Blocks registry.
     */
    public static JsonObject extractBlockRegistryShapes() {
        JsonObject blocksJson = new JsonObject();

        for (Field blockField : Blocks.class.getFields()) {
            try {
                Block block = (Block) blockField.get(null);
                String blockID = block.toString().substring(6, block.toString().length() - 1);
                blocksJson.add(blockID, extractBlockShapes(block));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return blocksJson;
    }

    /**
     * Extracts shapes from a Minecraft Block.
     */
    public static JsonArray extractBlockShapes(Block block) {
        JsonArray states = new JsonArray();

        block.getStateDefinition().getPossibleStates().forEach(state -> {

            JsonObject properties = new JsonObject();
            for (Map.Entry<Property<?>, Comparable<?>> entry : state.getValues().entrySet())
                properties.addProperty(entry.getKey().getName(), String.valueOf(entry.getValue()).toLowerCase());

            Vec3 offset = state.getOffset(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).reverse();
            VoxelShape shape = new VoxelShape(state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).move(offset.x, offset.y, offset.z));

            JsonObject stateJson = new JsonObject();
            stateJson.add("properties", properties);
            stateJson.add("shape", shape.optimize().toJson());
            states.add(stateJson);
        });

        return states;
    }

    /**
     * Writes a JsonObject to a JSON file.
     */
    public static void writeJsonToFile(String fileName, JsonObject data, boolean prettyPrint) {
        Gson gson = prettyPrint ? new GsonBuilder().setPrettyPrinting().create() : new Gson();

        try (FileWriter writer = new FileWriter(fileName)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
