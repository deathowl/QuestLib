package hu.rothens.qlib.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.rothens.qlib.model.QuestDef;
import hu.rothens.qlib.model.QuestRequest;
import hu.rothens.qlib.model.RequestType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * JSON loader for quest definitions.
 */
@Slf4j
public class JsonLoader implements QDBLoader {

    private final String filePath;
    private final ObjectMapper objectMapper;

    public JsonLoader(String filePath) {
        this.filePath = filePath;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void load(HashMap<Integer, QuestDef> quests) {
        try {
            List<QuestDefinition> questDefinitions = objectMapper.readValue(
                    new File(filePath),
                    new TypeReference<List<QuestDefinition>>() {}
            );

            log.info("Loaded {} quest definitions from file", questDefinitions.size());

            for (QuestDefinition definition : questDefinitions) {
                try {
                    HashSet<QuestRequest> requirements = new HashSet<>();
                    if (definition.required != null && !definition.required.isEmpty()) {
                        for (RequirementDTO req : definition.required) {
                            requirements.add(new QuestRequest(
                                    req.id,
                                    RequestType.values()[req.type],
                                    req.count
                            ));
                        }
                    } else {
                        log.warn("No requirements for quest: {}", definition.id);
                        continue;
                    }

                    QuestDef questDef = new QuestDef(
                            definition.id,
                            definition.description,
                            definition.ongoing,
                            definition.onfinished,
                            definition.questgivers != null ? definition.questgivers : new ArrayList<>(),
                            requirements,
                            definition.prerequisites != null ? definition.prerequisites : new ArrayList<>(),
                            definition.pre_dialog_lines != null ? definition.pre_dialog_lines : new ArrayList<>()
                    );

                    if (quests.containsKey(questDef.getId())) {
                        log.error("Quest ID collision! Quest {} collides with {}",
                                questDef, quests.get(questDef.getId()));
                    } else {
                        quests.put(questDef.getId(), questDef);
                    }
                } catch (Exception e) {
                    log.error("Error processing quest {}: {}", definition.id, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Failed to load quest definitions from {}: {}", filePath, e.getMessage());
        }
    }

    private static class QuestDefinition {
        @JsonProperty("id")
        private int id;

        @JsonProperty("description")
        private String description;

        @JsonProperty("ongoing")
        private String ongoing;

        @JsonProperty("onfinished")
        private String onfinished;

        @JsonProperty("questgivers")
        private ArrayList<Integer> questgivers;

        @JsonProperty("prerequisites")
        private ArrayList<Integer> prerequisites;

        @JsonProperty("required")
        private List<RequirementDTO> required;

        @JsonProperty("pre_dialog_lines")
        private List<String> preDialogueLines;
    }

    private static class RequirementDTO {
        @JsonProperty("id")
        private int id;

        @JsonProperty("type")
        private int type;

        @JsonProperty("count")
        private int count;
    }
}
