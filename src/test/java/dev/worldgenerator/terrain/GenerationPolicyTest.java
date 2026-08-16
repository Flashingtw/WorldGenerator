package dev.worldgenerator.terrain;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class GenerationPolicyTest {
    @Test
    void barrenPolicyDisablesTreeSnowDecorationAndInitialMobs() {
        assertFalse(GenerationPolicy.BARREN.vanillaDecorations());
        assertFalse(GenerationPolicy.BARREN.initialMobs());
    }
}
