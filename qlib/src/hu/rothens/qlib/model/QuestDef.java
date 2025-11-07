package hu.rothens.qlib.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This class is a flyweight for prototyping a quest.
 * Created by Rothens on 2015.03.31..
 */
@Getter
@EqualsAndHashCode(of = {"id"})
@ToString(of = {"id", "description", "ongoing", "onfinished"})
public class QuestDef {
    private final int id;
    private final String description;
    private final String ongoing;
    private final String onfinished;
    private final ArrayList<Integer> prerequisites;
    private final ArrayList<String> preDialogueLines;
    /**
     * -- GETTER --
     *  Returns the ids of the quests this quest is prerequisite of.
     *
     * @return an ArrayList containing the ids
     */
    private final ArrayList<Integer> touch;
    private final ArrayList<Integer> questGivers;
    private final HashSet<QuestRequest> questRequest;

    public QuestDef(int id, String description, String ongoing, String onfinished, ArrayList<Integer> questGivers, HashSet<QuestRequest> questRequest, ArrayList<Integer> prerequisites, ArrayList<String> preDialogueLines) {
        this.id = id;
        this.description = description;
        this.ongoing = ongoing;
        this.onfinished = onfinished;
        this.questGivers = questGivers;
        this.questRequest = questRequest;
        this.prerequisites = prerequisites;
        this.preDialogueLines = preDialogueLines;
        touch = new ArrayList<Integer>();
    }

    public void addTouch(int i){
        touch.add(i);
    }

    public Quest createQuest(){
        return new Quest(this);
    }
}
