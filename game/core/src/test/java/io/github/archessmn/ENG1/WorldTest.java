// Test Summary:
// The WorldTest class verifies the functionality of the World class in managing MapObjects and checking for overlaps.
// - testDoesObjectOverlap_True(): Ensures that the doesObjectOverlap method correctly identifies overlapping objects. Should return true for overlapping cases.
// - testDoesObjectOverlap_False(): Ensures that the doesObjectOverlap method correctly identifies non-overlapping objects. Should return false for non-overlapping cases.

package io.github.archessmn.ENG1;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.archessmn.ENG1.GameModel.Objects.MapObject;
import io.github.archessmn.ENG1.GameModel.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WorldTest {

    private World world;

    @BeforeEach
    public void setUp() {
        world = new World(100, 100); // Provide width and height for the world

        // Initialize objects with full constructor arguments
        MapObject mapObject1 = new MapObject(1, 1, 10, 10, "sprite1", "Object1", false);
        MapObject mapObject2 = new MapObject(15, 15, 10, 10, "sprite2", "Object2", false);

        // Add objects to the world
        world.addMapObject(mapObject1);
        world.addMapObject(mapObject2);
    }

    @Test
    public void testDoesObjectOverlap_True() {
        // Create an overlapping object
        MapObject overlapObject = new MapObject(5, 5, 10, 10, "sprite3", "OverlapObject", false);

        // Assert that it overlaps with an existing object
        assertTrue(world.doesObjectOverlap(overlapObject), "Overlap detection failed for overlapping object.");
    }

    @Test
    public void testDoesObjectOverlap_False() {
        // Create a non-overlapping object
        MapObject nonOverlapObject = new MapObject(30, 30, 10, 10, "sprite4", "NonOverlapObject", false);

        // Assert that it does not overlap with any existing object
        assertFalse(world.doesObjectOverlap(nonOverlapObject), "Non-overlap detection failed for non-overlapping object.");
    }
}
