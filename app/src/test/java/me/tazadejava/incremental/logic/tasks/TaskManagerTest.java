package me.tazadejava.incremental.logic.tasks;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;

import me.tazadejava.incremental.logic.taskmodifiers.TimePeriod;

import static org.junit.Assert.*;

public class TaskManagerTest {

    @Test
    public void testCreation() {
        TaskManager taskManager = new TaskManager();

        taskManager.addNewPersistentGroup("My Group");

        assertEquals(taskManager.getAllCurrentGroupNames(), new ArrayList<>(Arrays.asList("My Group")));

        taskManager.addNewTimePeriod("My Time Period", LocalDate.of(2020, 8, 1), LocalDate.of(2020, 8, 31));

        TimePeriod timePeriod = taskManager.getCurrentTimePeriod();

        assertNotNull(timePeriod);

        assertEquals(timePeriod.getName(), "My Time Period");
    }
}