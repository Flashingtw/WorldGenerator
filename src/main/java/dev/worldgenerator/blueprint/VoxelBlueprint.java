package dev.worldgenerator.blueprint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Deep module for authored structures. Its interface loads one textual blueprint
 * and returns fully transformed edits; parsing, anchoring, mirroring, state
 * rotation, validation, and sparse air handling remain inside the implementation.
 */
public final class VoxelBlueprint {
    private final String id;
    private final int anchorX;
    private final int anchorY;
    private final int anchorZ;
    private final int width;
    private final int height;
    private final int depth;
    private final List<LocalBlock> blocks;

    private VoxelBlueprint(
            String id, int anchorX, int anchorY, int anchorZ,
            int width, int height, int depth, List<LocalBlock> blocks) {
        this.id = id;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.blocks = List.copyOf(blocks);
    }

    public static VoxelBlueprint read(Reader source) throws IOException {
        BufferedReader reader = source instanceof BufferedReader buffered
                ? buffered : new BufferedReader(source);
        String id = null;
        int[] anchor = null;
        Map<Character, BlueprintState> palette = new HashMap<>();
        List<Layer> layers = new ArrayList<>();
        List<String> rows = null;
        Integer layerY = null;

        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            if (trimmed.startsWith("id=")) {
                id = trimmed.substring(3).strip();
            } else if (trimmed.startsWith("anchor=")) {
                anchor = coordinates(trimmed.substring(7), lineNumber);
            } else if (trimmed.startsWith("palette.")) {
                int equals = trimmed.indexOf('=');
                if (equals != 9 || trimmed.length() < 11) {
                    throw format(lineNumber, "palette lines use palette.X=material;facing;part;connections;open");
                }
                char symbol = trimmed.charAt(8);
                if (symbol == '.') throw format(lineNumber, "'.' is reserved for empty space");
                BlueprintState previous = palette.put(symbol, state(trimmed.substring(equals + 1), lineNumber));
                if (previous != null) throw format(lineNumber, "duplicate palette symbol " + symbol);
            } else if (trimmed.startsWith("layer=")) {
                if (rows != null) layers.add(new Layer(layerY, List.copyOf(rows)));
                try {
                    layerY = Integer.parseInt(trimmed.substring(6).strip());
                } catch (NumberFormatException exception) {
                    throw format(lineNumber, "layer height must be an integer");
                }
                rows = new ArrayList<>();
            } else if (rows != null) {
                rows.add(trimmed);
            } else {
                throw format(lineNumber, "unknown header or row outside a layer");
            }
        }
        if (rows != null) layers.add(new Layer(layerY, List.copyOf(rows)));
        if (id == null || id.isBlank()) throw format(lineNumber, "missing id");
        if (!id.matches("[a-z0-9_-]+")) throw format(lineNumber, "id must use a-z, 0-9, _ or -");
        if (anchor == null) throw format(lineNumber, "missing anchor");
        if (palette.isEmpty()) throw format(lineNumber, "empty palette");
        if (layers.isEmpty()) throw format(lineNumber, "no layers");

        int depth = layers.getFirst().rows().size();
        int width = depth == 0 ? 0 : layers.getFirst().rows().getFirst().length();
        int maximumY = -1;
        List<LocalBlock> blocks = new ArrayList<>();
        for (Layer layer : layers) {
            if (layer.y() < 0) throw format(lineNumber, "layer height cannot be negative");
            if (layer.rows().size() != depth) throw format(lineNumber, "all layers must have equal depth");
            maximumY = Math.max(maximumY, layer.y());
            for (int z = 0; z < depth; z++) {
                String row = layer.rows().get(z);
                if (row.length() != width) throw format(lineNumber, "all rows must have equal width");
                for (int x = 0; x < width; x++) {
                    char symbol = row.charAt(x);
                    if (symbol == '.') continue;
                    BlueprintState blockState = palette.get(symbol);
                    if (blockState == null) throw format(lineNumber, "undefined palette symbol " + symbol);
                    blocks.add(new LocalBlock(x, layer.y(), z, blockState));
                }
            }
        }
        if (width == 0 || depth == 0) throw format(lineNumber, "layers cannot be empty");
        if (anchor[0] < 0 || anchor[0] >= width || anchor[1] < 0 || anchor[1] > maximumY
                || anchor[2] < 0 || anchor[2] >= depth) {
            throw format(lineNumber, "anchor lies outside the blueprint");
        }
        return new VoxelBlueprint(
                id, anchor[0], anchor[1], anchor[2], width, maximumY + 1, depth, blocks);
    }

    public String id() {
        return id;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int depth() {
        return depth;
    }

    public List<BlueprintEdit> place(BlueprintPlacement placement) {
        List<BlueprintEdit> result = new ArrayList<>(blocks.size());
        for (LocalBlock block : blocks) {
            int dx = block.x() - anchorX;
            int dz = block.z() - anchorZ;
            if (placement.mirrored()) dx = -dx;
            int rotatedX;
            int rotatedZ;
            switch (placement.quarterTurns()) {
                case 1 -> {
                    rotatedX = -dz;
                    rotatedZ = dx;
                }
                case 2 -> {
                    rotatedX = -dx;
                    rotatedZ = -dz;
                }
                case 3 -> {
                    rotatedX = dz;
                    rotatedZ = -dx;
                }
                default -> {
                    rotatedX = dx;
                    rotatedZ = dz;
                }
            }
            result.add(new BlueprintEdit(
                    placement.anchorX() + rotatedX,
                    placement.anchorY() + block.y() - anchorY,
                    placement.anchorZ() + rotatedZ,
                    block.state().transform(placement.quarterTurns(), placement.mirrored())));
        }
        return List.copyOf(result);
    }

    private static BlueprintState state(String input, int line) throws IOException {
        String[] fields = input.split(";", -1);
        if (fields.length < 1 || fields.length > 5) throw format(line, "invalid palette state");
        String material = fields[0].strip();
        BlueprintFacing facing = fields.length > 1 && !fields[1].isBlank()
                ? enumValue(BlueprintFacing.class, fields[1], line) : BlueprintFacing.NORTH;
        BlueprintPart part = fields.length > 2 && !fields[2].isBlank()
                ? enumValue(BlueprintPart.class, fields[2], line) : BlueprintPart.FULL;
        int connections = fields.length > 3 ? connections(fields[3], line) : 0;
        boolean open = fields.length > 4 && Boolean.parseBoolean(fields[4].strip());
        return new BlueprintState(material, facing, part, connections, open);
    }

    private static int connections(String input, int line) throws IOException {
        if (input.isBlank() || input.equalsIgnoreCase("none")) return 0;
        int result = 0;
        for (String value : input.split(",")) {
            result |= switch (value.strip().toLowerCase(Locale.ROOT)) {
                case "north" -> BlueprintState.NORTH;
                case "east" -> BlueprintState.EAST;
                case "south" -> BlueprintState.SOUTH;
                case "west" -> BlueprintState.WEST;
                default -> throw format(line, "unknown connection " + value);
            };
        }
        return result;
    }

    private static int[] coordinates(String input, int line) throws IOException {
        String[] values = input.split(",", -1);
        if (values.length != 3) throw format(line, "anchor must contain x,y,z");
        try {
            return new int[] {
                    Integer.parseInt(values[0].strip()),
                    Integer.parseInt(values[1].strip()),
                    Integer.parseInt(values[2].strip())};
        } catch (NumberFormatException exception) {
            throw format(line, "anchor coordinates must be integers");
        }
    }

    private static <T extends Enum<T>> T enumValue(
            Class<T> type, String input, int line) throws IOException {
        try {
            return Enum.valueOf(type, input.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw format(line, "invalid " + type.getSimpleName() + " value " + input);
        }
    }

    private static IOException format(int line, String message) {
        return new IOException("Blueprint format error near line " + line + ": " + message);
    }

    private record LocalBlock(int x, int y, int z, BlueprintState state) {
    }

    private record Layer(int y, List<String> rows) {
    }
}
