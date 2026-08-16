package dev.worldgenerator.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

class CommandSuggestionsTest {
    @Test
    void seedSlotDoesNotContainWorldSizes() {
        List<String> suggestions = CommandSuggestions.createArgument(2);
        assertEquals(List.of("random"), suggestions);
        assertFalse(suggestions.stream().anyMatch(value -> value.contains("x")));
    }

    @Test
    void sizeSlotContainsWorldSizes() {
        assertEquals(
                List.of("5000x5000", "10000x10000", "unlimited"),
                CommandSuggestions.createArgument(3));
    }

    @Test
    void previewSuggestionsAndRotationParserAreDeterministic() {
        assertEquals(List.of("clear", "rebuild", "rotation_lab"),
                CommandSuggestions.matchingPrefix(
                        List.of("rotation_lab", "rebuild", "clear"), ""));
        assertEquals(3, PreviewCommandHandler.parseRotation("270"));
        assertEquals(List.of("rotation_lab"),
                CommandSuggestions.matchingPrefix(List.of("rotation_lab", "rebuild"), "rot"));
    }
}
