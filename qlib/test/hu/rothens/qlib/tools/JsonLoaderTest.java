package hu.rothens.qlib.tools;

import hu.rothens.qlib.model.QuestDef;
import hu.rothens.qlib.model.QuestRequest;
import hu.rothens.qlib.model.RequestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

public class JsonLoaderTest {

    @TempDir
    Path tempDir;

    private File jsonFile;
    private HashMap<Integer, QuestDef> quests;
    private JsonLoader loader;

    @BeforeEach
    void setup() throws IOException {
        jsonFile = tempDir.resolve("test-quests.json").toFile();
        quests = new HashMap<>();
    }

    @Test
    @DisplayName("Should load valid quest definitions from JSON file")
    void testLoadValidQuests() throws IOException {
        String json = """
        [
          {
            "id": 1,
            "description": "Test Quest 1",
            "ongoing": "Ongoing text 1",
            "onfinished": "Finished text 1",
            "questgivers": [101, 102],
            "prerequisites": [],
            "pre_dialog_lines": [
              "Line 1",
              "Line 2"
            ],
            "required": [
              {
                "id": 201,
                "type": 0,
                "count": 5
              }
            ]
          },
          {
            "id": 2,
            "description": "Test Quest 2",
            "ongoing": "Ongoing text 2",
            "onfinished": "Finished text 2",
            "questgivers": [103],
            "prerequisites": [1],
            "pre_dialog_lines": [],
            "required": [
              {
                "id": 301,
                "type": 1,
                "count": 10
              }
            ]
          }
        ]
        """;

        try (FileWriter writer = new FileWriter(jsonFile)) {
            writer.write(json);
        }

        loader = new JsonLoader(jsonFile.getAbsolutePath());
        loader.load(quests);

        assertEquals(2, quests.size(), "Should load 2 quest definitions");

        QuestDef quest1 = quests.get(1);
        assertNotNull(quest1, "Quest 1 should be loaded");
        assertEquals("Test Quest 1", quest1.getDescription());
        assertEquals(2, quest1.getQuestGivers().size());
        assertTrue(quest1.getQuestGivers().contains(101));
        assertTrue(quest1.getQuestGivers().contains(102));
        assertTrue(quest1.getPrerequisites().isEmpty());
        assertEquals(quest1.getPreDialogLines(), new ArrayList<String>("Line1", "Line2"));

        HashSet<QuestRequest> requirements1 = quest1.getQuestRequest();
        assertEquals(1, requirements1.size(), "Quest 1 should have 1 requirement");
        QuestRequest req1 = requirements1.iterator().next();
        assertEquals(201, req1.getSubjectId().intValue());
        assertEquals(RequestType.KILL, req1.getRequestType());
        assertEquals(5, req1.getCount());


        QuestDef quest2 = quests.get(2);
        assertNotNull(quest2, "Quest 2 should be loaded");
        assertEquals("Test Quest 2", quest2.getDescription());
        assertEquals(1, quest2.getQuestGivers().size());
        assertTrue(quest2.getQuestGivers().contains(103));
        assertEquals(1, quest2.getPrerequisites().size());
        assertTrue(quest2.getPrerequisites().contains(1));

        HashSet<QuestRequest> requirements2 = quest2.getQuestRequest();
        assertEquals(1, requirements2.size(), "Quest 2 should have 1 requirement");
        QuestRequest req2 = requirements2.iterator().next();
        assertEquals(301, req2.getSubjectId().intValue());
        assertEquals(RequestType.GATHER, req2.getRequestType());
        assertEquals(10, req2.getCount());
        assertTrue(quest2.getPreDialogLines().isEmpty());
    }

    @Test
    @DisplayName("Should handle JSON with missing required fields")
    void testHandleMissingFields() throws IOException {
        String json = """
        [
          {
            "id": 3,
            "description": "Test Quest 3",
            "ongoing": "Ongoing text 3",
            "onfinished": "Finished text 3",
            "questgivers": [101]
            // Missing "prerequisites" and "required" fields
          }
        ]
        """;

        try (FileWriter writer = new FileWriter(jsonFile)) {
            writer.write(json);
        }

        loader = new JsonLoader(jsonFile.getAbsolutePath());
        loader.load(quests);

        assertEquals(0, quests.size(), "Should not load quests with missing required fields");
    }

    @Test
    @DisplayName("Should handle invalid JSON file")
    void testHandleInvalidJson() throws IOException {
        String invalidJson = "This is not valid JSON";

        try (FileWriter writer = new FileWriter(jsonFile)) {
            writer.write(invalidJson);
        }

        loader = new JsonLoader(jsonFile.getAbsolutePath());
        loader.load(quests);

        assertEquals(0, quests.size(), "Should not load quests from invalid JSON");
    }

    @Test
    @DisplayName("Should handle non-existent file")
    void testHandleNonExistentFile() {
        String nonExistentPath = tempDir.resolve("non-existent.json").toString();

        loader = new JsonLoader(nonExistentPath);
        loader.load(quests);

        assertEquals(0, quests.size(), "Should not load quests from non-existent file");
    }
}
