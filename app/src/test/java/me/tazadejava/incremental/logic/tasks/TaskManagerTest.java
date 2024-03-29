package me.tazadejava.incremental.logic.tasks;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Arrays;

import me.tazadejava.incremental.logic.LogicalUtils;
import me.tazadejava.incremental.logic.taskmodifiers.Group;

import static org.junit.Assert.*;

public class TaskManagerTest {

    /*
    Unit tests TODO:
     */

    private TaskManager getTaskManager() {
        TaskManager taskManager = new TaskManager("");
        taskManager.addNewPersistentGroup("My Group");
        taskManager.addNewTimePeriod("My Time Period", LocalDate.now().minusDays(60), LocalDate.now().plusDays(60));

        return taskManager;
    }

    @Test
    public void testCreation() {
        TaskManager taskManager = getTaskManager();

        assertEquals(taskManager.getAllCurrentGroupNames(), new ArrayList<>(Arrays.asList("My Group")));

        TimePeriod timePeriod = taskManager.getCurrentTimePeriod();

        assertNotNull(timePeriod);
        assertEquals(timePeriod.getName(), "My Time Period");
        assertTrue(taskManager.isCurrentTimePeriodActive());
    }

    @Test
    public void testEarlyTaskLogic() {
        TaskManager taskManager = getTaskManager();
        TimePeriod timePeriod = taskManager.getCurrentTimePeriod();
        Group group = taskManager.getPersistentGroupByName("My Group");

        NonrepeatingTask taskGenerator;
        taskManager.addNewGeneratedTask(taskGenerator = new NonrepeatingTask(taskManager, LocalDate.now().plusDays(1), timePeriod, "Task 1", LocalDateTime.now().plusDays(2),
                group, null, 40));

        Task task = taskGenerator.getAllTasks()[0];

        assertEquals("Task 1", task.getName());

        assertEquals(task.getTotalLoggedMinutesOfWork(), 0);
        assertEquals(task.getTodaysMinutesLeft(), 0);
        assertEquals(task.getTotalMinutesLeftOfWork(), 40);

        //work now a bit early
        task.startWorkingOnTask();
        task.logMinutes(10, "", false);

        assertEquals(task.getTotalLoggedMinutesOfWork(), 10);
        assertEquals(task.getTodaysMinutesLeft(), 0);
        assertEquals(task.getTotalMinutesLeftOfWork(), 30);

        //work finish early
        task.startWorkingOnTask();
        task.logMinutes(30, "", true);

        assertEquals(task.getTotalLoggedMinutesOfWork(), 40);
        assertEquals(task.getTodaysMinutesLeft(), 0);
        assertEquals(task.getTotalMinutesLeftOfWork(), 0);
        assertEquals(task.isTaskComplete(), true);
        assertEquals(task.isTaskCurrentlyWorkedOn(), false);
    }

    @Test
    public void testOverdueTaskLogic() {
        TaskManager taskManager = getTaskManager();
        TimePeriod timePeriod = taskManager.getCurrentTimePeriod();
        Group group = taskManager.getPersistentGroupByName("My Group");

        NonrepeatingTask taskGenerator;
        taskManager.addNewGeneratedTask(taskGenerator = new NonrepeatingTask(taskManager, LocalDate.now().minusDays(2), timePeriod, "Task 1", LocalDateTime.now().minusDays(1),
                group, null, 40));

        Task task = taskGenerator.getAllTasks()[0];

        assertEquals(task.getName(), "Task 1");
        assertEquals(task.isOverdue(), true);

        assertEquals(task.getTotalLoggedMinutesOfWork(), 0);
        assertEquals(task.getTodaysMinutesLeft(), 40);
        assertEquals(task.getTotalMinutesLeftOfWork(), 40);

        //work now a bit early
        task.startWorkingOnTask();
        task.logMinutes(10, "", false);

        assertEquals(task.getTotalLoggedMinutesOfWork(), 10);
        assertEquals(task.getTodaysMinutesLeft(), 30);
        assertEquals(task.getTotalMinutesLeftOfWork(), 30);

        //work finish early
        task.startWorkingOnTask();
        task.logMinutes(30, "", true);

        assertEquals(task.getTotalLoggedMinutesOfWork(), 40);
        assertEquals(task.getTodaysMinutesLeft(), 0);
        assertEquals(task.getTotalMinutesLeftOfWork(), 0);
        assertEquals(task.isTaskComplete(), true);
        assertEquals(task.isTaskCurrentlyWorkedOn(), false);
    }

    @Test
    public void testUpcomingTaskMinutesLogic() {
        TaskManager taskManager = getTaskManager();
        TimePeriod timePeriod = taskManager.getCurrentTimePeriod();
        Group group = taskManager.getPersistentGroupByName("My Group");

        NonrepeatingTask taskGenerator;
        taskManager.addNewGeneratedTask(taskGenerator = new NonrepeatingTask(taskManager, LocalDate.now().plusDays(2), timePeriod, "Upcoming Task", LocalDateTime.now().plusDays(3),
                group, null, 40));

        Task task = taskGenerator.getAllTasks()[0];

        assertEquals(task.isTaskCurrentlyWorkedOn(), false);
        assertEquals(taskGenerator.getNextUpcomingTask(), task);
        assertArrayEquals(taskGenerator.getPendingTasks(), new Task[0]);
    }
}