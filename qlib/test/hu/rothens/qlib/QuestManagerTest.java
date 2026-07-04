package hu.rothens.qlib;

import hu.rothens.qlib.model.*;
import hu.rothens.qlib.tools.QDBLoader;
import hu.rothens.qlib.tools.UDBManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QuestManagerTest {

    private QuestManager questManager;

    @Mock
    private QDBLoader mockLoader;

    @Mock
    private UDBManager mockDbManager;

    @Mock
    private QuestSubject mockSubject;

    private QuestDef createQuestDef(int id, String desc) {
        ArrayList<Integer> givers = new ArrayList<>();
        givers.add(100 + id);

        ArrayList<Integer> prereqs = new ArrayList<>();
        if (id > 1) {
            prereqs.add(id - 1);
        }

        HashSet<QuestRequest> reqs = new HashSet<>();
        reqs.add(new QuestRequest(200 + id, RequestType.KILL, 5));

        return new QuestDef(id, desc, "Ongoing " + id, "Finished " + id, givers, reqs, prereqs,
                new ArrayList<String>(), new ArrayList<Integer>(), null);
    }

    @BeforeEach
    void setUp() {
        questManager = new QuestManager();

        doAnswer(invocation -> {
            HashMap<Integer, QuestDef> quests = invocation.getArgument(0);

            for (int i = 1; i <= 3; i++) {
                QuestDef qd = createQuestDef(i, "Test Quest " + i);
                quests.put(qd.getId(), qd);
            }

            return null;
        }).when(mockLoader).load(any());

        questManager.loadDefs(mockLoader);

        when(mockDbManager.getAllUserData()).thenReturn(new ArrayList<>());
        when(mockDbManager.newUser(anyInt())).thenAnswer(invocation -> {
            int userId = invocation.getArgument(0);
            return new QuestUser(userId, questManager);
        });

        questManager.loadProgress(mockDbManager);
    }

    @Test
    @DisplayName("getQuestUser should create a new user if not exists")
    void testGetQuestUserCreatesNewUser() {
        int userId = 42;

        QuestUser user = questManager.getQuestUser(userId);

        assertNotNull(user, "Should create a new QuestUser");
        assertEquals(userId, user.getId(), "New user should have the correct ID");

        assertFalse(user.getAvailable().isEmpty(), "New user should have starting quests available");

        verify(mockDbManager).newUser(userId);
    }

    @Test
    @DisplayName("acceptQuest should handle quest acceptance correctly")
    void testAcceptQuest() {
        int userId = 123;
        QuestUser user = questManager.getQuestUser(userId);

        QuestDef firstQuest = questManager.getDef(1);
        assertTrue(user.getAvailable().contains(firstQuest), "First quest should be available");

        questManager.acceptQuest(userId, 1);

        assertFalse(user.getAvailable().contains(firstQuest), "Quest should no longer be available");
        assertTrue(user.getInProgressQuests().containsKey(1), "Quest should be in progress");

        ArgumentCaptor<Quest> questCaptor = ArgumentCaptor.forClass(Quest.class);
        verify(mockDbManager).acceptQuest(eq(user), questCaptor.capture());
        assertEquals(1, questCaptor.getValue().getDef().getId(), "AcceptQuest should be called with correct quest");
    }

    @Test
    @DisplayName("notify should update quest progress correctly")
    void testNotify() {
        int userId = 456;
        QuestUser user = questManager.getQuestUser(userId);
        questManager.acceptQuest(userId, 1);

        when(mockSubject.getSubjectId()).thenReturn(201); // Same as in createQuestDef

        questManager.notify(userId, mockSubject, RequestType.KILL, 5);

        assertTrue(user.getFinishedQuests().contains(1), "Quest should be marked as finished");
        assertFalse(user.getInProgressQuests().containsKey(1), "Quest should no longer be in progress");

        QuestDef nextQuest = questManager.getDef(2);
        assertTrue(user.getAvailable().contains(nextQuest), "Next quest should become available");

        verify(mockDbManager, atLeastOnce()).updateProgress(eq(user), any(Quest.class));

        ArgumentCaptor<Quest> finishedCaptor = ArgumentCaptor.forClass(Quest.class);
        verify(mockDbManager).finishQuest(eq(user), finishedCaptor.capture());
        assertEquals(1, finishedCaptor.getValue().getDef().getId(), "FinishQuest should be called with correct quest");

        verify(mockDbManager).setAvailable(eq(user), eq(2));
    }

    @Test
    @DisplayName("notify should handle partial progress correctly")
    void testNotifyPartialProgress() {
        int userId = 789;
        QuestUser user = questManager.getQuestUser(userId);
        questManager.acceptQuest(userId, 1);

        when(mockSubject.getSubjectId()).thenReturn(201);

        questManager.notify(userId, mockSubject, RequestType.KILL, 2);

        assertFalse(user.getFinishedQuests().contains(1), "Quest should not be finished yet");
        assertTrue(user.getInProgressQuests().containsKey(1), "Quest should still be in progress");

        QuestDef nextQuest = questManager.getDef(2);
        assertFalse(user.getAvailable().contains(nextQuest), "Next quest should not be available yet");

        verify(mockDbManager).updateProgress(eq(user), any(Quest.class));
        verify(mockDbManager, never()).finishQuest(any(), any());
        verify(mockDbManager, never()).setAvailable(any(), anyInt());
    }
}
