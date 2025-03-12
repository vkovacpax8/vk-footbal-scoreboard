package footbal.scoreboard.service;

import footbal.scoreboard.Match;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static footbal.scoreboard.service.ScoreboardService.*;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;

public class ScoreboardServiceTest {
    public static final String TEAM_A = "Team A";
    public static final String TEAM_B = "Team B";
    public static final String TEAM_C = "Team C";
    public static final String TEAM_D = "Team D";
    public static final String TEAM_E = "Team E";
    public static final String TEAM_F = "Team F";
    public static final String TEAM_G = "Team G";
    public static final String TEAM_H = "Team H";
    public static final String TEAM_I = "Team I";
    public static final String TEAM_J = "Team J";
    public static final String UPDATED_CORRECTLY = "Score should be updated correctly";
    private ScoreboardService scoreboardService;

    @BeforeEach
    public void setUp() {
        scoreboardService = new ScoreboardService();
    }

    @Test
    public void testGetMatches() {
        //Start and assert two matches
        startAndAssertTwoMatches();
    }

    @Test
    public void testGetMatchesAfterSorting() {
        //Start and assert two matches
        startAndAssertTwoMatches();

        //Update the scores of the matches
        scoreboardService.updateScore(0, 3, 2);
        scoreboardService.updateScore(1, 6, 4);

        // Get the sorted summary and check the correct order
        List<String> summary = scoreboardService.getFormatedSortedSummary();
        assertEquals(List.of("1. " + TEAM_C + " 6 - 4 " + TEAM_D, "2. " + TEAM_A + " 3 - 2 " + TEAM_B), summary, UPDATED_CORRECTLY);

        // Get the list of matches and check the correct order
        assertTwoMatches(scoreboardService.getMatches());
    }

    @Test
    public void testStartMatchAndExisting() {
        //Start a match
        scoreboardService.startMatch(TEAM_A, TEAM_B);
        //Get the list of matches
        List<String> summary = scoreboardService.getFormatedSortedSummary();
        //Check that the list contains one match
        assertEquals(List.of("1. " + TEAM_A + " 0 - 0 " + TEAM_B), summary, "Match should be started successfully");
        //Start a match with the same team
        Exception exception = assertThrows(IllegalArgumentException.class, () -> scoreboardService.startMatch(TEAM_A, TEAM_B));
        //Check that the exception message is correct
        assertEquals(ALREADY_EXISTS, exception.getMessage());
    }

    @Test
    public void testConcurrentStartMatches() throws InterruptedException {
        // Number of threads to start matches concurrently
        final int NUM_THREADS = 10;
        // CountDownLatch to synchronize the start of all threads
        final CountDownLatch startLatch = new CountDownLatch(1);
        // CountDownLatch to synchronize the end of all threads
        final CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);
        // ExecutorService to manage the threads
        try (ExecutorService executor = newFixedThreadPool(NUM_THREADS)) {

            // Submit concurrent tasks to start matches
            for (int i = 0; i < NUM_THREADS; i++) {
                // Start a match between Team i and Team i+1
                int finalI = i;
                // Submit a task to start a match
                executor.submit(() -> {
                    try {
                        // Wait for the main thread to start
                        startLatch.await();
                        // Start the match
                        scoreboardService.startMatch("Team " + finalI, "Team " + (finalI + 1));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } finally {
                        // Decrement the count when done
                        doneLatch.countDown();
                    }
                });
            }

            // Allow all tasks to start
            startLatch.countDown();
            // Wait for all tasks to finish
            doneLatch.await();

            // After all threads have finished, verify that the number of matches equals the number of threads
            assertEquals(NUM_THREADS, scoreboardService.getSortedMatches().size(), "Number of matches should be equal to number of threads.");

            executor.shutdown();
        }
    }

    @Test
    public void testStartMatchWithNullNames() {
        //Start a match with null names
        Exception exception = assertThrows(IllegalArgumentException.class, () -> scoreboardService.startMatch(null, TEAM_B));
        //Check that the exception message is correct
        assertEquals(CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());

        //Start a match with null names
        exception = assertThrows(IllegalArgumentException.class, () -> scoreboardService.startMatch(TEAM_A, null));
        //Check that the exception message is correct
        assertEquals(CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());

        //Start a match with null names
        exception = assertThrows(IllegalArgumentException.class, () -> scoreboardService.startMatch(null, null));
        //Check that the exception message is correct
        assertEquals(CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
    }

    @Test
    public void testStartMatchWithEmptyNames() {
        //Start a match with empty names
        Exception exception = assertThrows(IllegalArgumentException.class, () -> scoreboardService.startMatch("", TEAM_B));
        //Check that the exception message is correct
        assertEquals(CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());

        //Start a match with empty names
        exception = assertThrows(IllegalArgumentException.class, () -> scoreboardService.startMatch(TEAM_A, ""));
        //Check that the exception message is correct
        assertEquals(CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());

        //Start a match with empty names
        exception = assertThrows(IllegalArgumentException.class, () -> scoreboardService.startMatch("", ""));
        //Check that the exception message is correct
        assertEquals(CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
    }

    @Test
    public void testUpdateScoreAndLowering() {
        //Start a match
        scoreboardService.startMatch(TEAM_A, TEAM_B);
        //Update the score of the match
        scoreboardService.updateScore(0, 10, 2);
        //Get the list of matches
        assertEquals("1. " + TEAM_A + " 10 - 2 " + TEAM_B, scoreboardService.getFormatedSortedSummary().getFirst(), UPDATED_CORRECTLY);
        //Update the score of the match - Lowering
        scoreboardService.updateScore(0, 8, 1);
        //Get the list of matches
        assertEquals("1. " + TEAM_A + " 8 - 1 " + TEAM_B, scoreboardService.getFormatedSortedSummary().getFirst(), UPDATED_CORRECTLY);
    }

    @Test
    public void testUpdateScoreInvalidIndex() {
        //Update score of a match
        Exception exception = assertThrows(IndexOutOfBoundsException.class, () -> scoreboardService.updateScore(999, 1, 1));
        //Check that the exception message is correct
        assertEquals(MATCH_INDEX_IS_OUT_OF_RANGE, exception.getMessage());
    }

    @Test
    public void testUpdateScoreNegativeValue() {
        //Start a match
        scoreboardService.startMatch(TEAM_A, TEAM_B);

        //Update the score of the match with negative values
        Exception exception = assertThrows(IllegalArgumentException.class, () -> scoreboardService.updateScore(0, -1, 1));
        //Check that the exception message is correct
        assertEquals(CANNOT_BE_NEGATIVE, exception.getMessage());

        //Update the score of the match with negative values
        exception = assertThrows(IllegalArgumentException.class, () -> scoreboardService.updateScore(0, -1, -1));
        //Check that the exception message is correct
        assertEquals(CANNOT_BE_NEGATIVE, exception.getMessage());

        //Update the score of the match with negative values
        exception = assertThrows(IllegalArgumentException.class, () -> scoreboardService.updateScore(0, 1, -1));
        //Check that the exception message is correct
        assertEquals(CANNOT_BE_NEGATIVE, exception.getMessage());
    }

    @Test
    public void testConcurrentScoreUpdates() throws InterruptedException {
        // Number of threads to update scores concurrently
        final int NUM_THREADS = 10;
        // CountDownLatch to synchronize the start of all threads
        final CountDownLatch startLatch = new CountDownLatch(1);
        // CountDownLatch to synchronize the end of all threads
        final CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);
        // ExecutorService to manage the threads
        try (ExecutorService executor = newFixedThreadPool(NUM_THREADS)) {

            // Start one match
            scoreboardService.startMatch(TEAM_A, TEAM_B);

            // Submit concurrent tasks to update the score of the same match
            for (int i = 1; i <= NUM_THREADS; i++) {
                // Home team score
                final int scoreA = i;
                // Away team score
                final int scoreB = i + 1;
                // Submit a task to update the score
                executor.submit(() -> {
                    try {
                        // Wait for the main thread to start
                        startLatch.await();
                        // Update the score of the match
                        scoreboardService.updateScore(0, scoreA, scoreB);
                    } catch (InterruptedException e) {
                        // Handle the exception
                        Thread.currentThread().interrupt();
                    } finally {
                        // Decrement the count when done
                        doneLatch.countDown();
                    }
                });
            }

            // Allow all tasks to start
            startLatch.countDown();
            // Wait for all tasks to finish
            doneLatch.await();

            // The last score update's home score
            int expectedFinalScoreA = NUM_THREADS - 1;

            assertEquals(expectedFinalScoreA, scoreboardService.getSortedMatches().getFirst().getHomeScore(), "Home score is incorrect.");
            assertEquals(NUM_THREADS, scoreboardService.getSortedMatches().getFirst().getAwayScore(), "Away score is incorrect.");

            executor.shutdown();
        }
    }

    @Test
    public void testFinishMatch() {
        //Start and assert two matches
        startAndAssertTwoMatches();
        //Finish the first match
        scoreboardService.finishMatch(0);
        //Get the list of matches
        assertEquals(List.of("1. " + TEAM_C + " 0 - 0 " + TEAM_D), scoreboardService.getFormatedSortedSummary(), "Match should be finished and removed from the list");
    }

    @Test
    public void testFinishMatchInvalidIndex() {
        //Finish a match with an invalid index
        Exception exception = assertThrows(IndexOutOfBoundsException.class, () -> scoreboardService.finishMatch(999));
        //Check that the exception message is correct
        assertEquals(MATCH_INDEX_IS_OUT_OF_RANGE, exception.getMessage());
    }

    @Test
    public void testFinishMatchConcurrently() throws InterruptedException {
        // Number of threads to finish matches concurrently
        final int NUM_THREADS = 10;
        // CountDownLatch to synchronize the start of all threads
        final CountDownLatch startLatch = new CountDownLatch(1);
        // CountDownLatch to synchronize the end of all threads
        final CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);
        // ExecutorService to manage the threads
        try (ExecutorService executor = newFixedThreadPool(NUM_THREADS)) {

            // Start a match
            scoreboardService.startMatch(TEAM_A, TEAM_B);

            // Submit concurrent tasks to finish the same match
            for (int i = 0; i < NUM_THREADS; i++) {
                // Submit a task to finish the match
                executor.submit(() -> {
                    try {
                        // Wait for the main thread to start
                        startLatch.await();
                        // Finish the match
                        scoreboardService.finishMatch(0);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        // Decrement the count when done
                        doneLatch.countDown();
                    }
                });
            }

            // Allow all tasks to start
            startLatch.countDown();
            // Wait for all tasks to finish
            doneLatch.await();

            // After all threads have finished, ensure the match is fully removed
            assertEquals(0, scoreboardService.getSortedMatches().size(), "Match should be removed after finishing.");

            executor.shutdown();
        }
    }

    @Test
    public void testGetSortedMatchesEmpty() {
        //Get the list of matches
        assertTrue(scoreboardService.getFormatedSortedSummary().isEmpty(), "Summary should be empty when no matches have been played");
    }

    @Test
    public void testGetSortedMatchesOrdered() {
        //Start five matches
        scoreboardService.startMatch(TEAM_A, TEAM_B);
        //Update the scores of the matches
        scoreboardService.updateScore(0, 6, 6);

        scoreboardService.startMatch(TEAM_C, TEAM_D);
        scoreboardService.updateScore(1, 10, 2);

        scoreboardService.startMatch(TEAM_E, TEAM_F);
        scoreboardService.updateScore(2, 8, 6);

        scoreboardService.startMatch(TEAM_G, TEAM_H);
        scoreboardService.updateScore(3, 3, 1);

        scoreboardService.startMatch(TEAM_I, TEAM_J);
        scoreboardService.updateScore(4, 2, 2);

        // Get the sorted summary and check the correct order
        List<String> summary = scoreboardService.getFormatedSortedSummary();
        // Check that the list contains 5 matches
        assertEquals(5, summary.size(), "Summary should contain 5 matches");
        // Check that the matches are sorted in descending order of total score
        assertEquals("1. " + TEAM_E + " 8 - 6 " + TEAM_F, summary.get(0));
        assertEquals("2. " + TEAM_C + " 10 - 2 " + TEAM_D, summary.get(1));
        assertEquals("3. " + TEAM_A + " 6 - 6 " + TEAM_B, summary.get(2));
        assertEquals("4. " + TEAM_I + " 2 - 2 " + TEAM_J, summary.get(3));
        assertEquals("5. " + TEAM_G + " 3 - 1 " + TEAM_H, summary.get(4));
    }

    @Test
    public void testReset() {
        //Start a match
        scoreboardService.startMatch(TEAM_A, TEAM_B);
        //Get the list of matches
        assertFalse(scoreboardService.getFormatedSortedSummary().isEmpty(), "Summary should not be empty before reset");

        //Reset the scoreboard
        scoreboardService.reset();
        //Get the list of matches
        assertTrue(scoreboardService.getFormatedSortedSummary().isEmpty(), "Summary should be empty after reset");
    }

    private void startAndAssertTwoMatches() {
        //Start two matches
        scoreboardService.startMatch(TEAM_A, TEAM_B);
        scoreboardService.startMatch(TEAM_C, TEAM_D);

        //Get the list of matches
        List<Match> matches = scoreboardService.getMatches();
        assertTwoMatches(matches);
    }

    private static void assertTwoMatches(List<Match> matches) {
        //Check that the list contains two matches
        assertEquals(2, matches.size(), "There should be 2 matches in the list");
        //Check that the first match has the correct home and away teams
        assertEquals(TEAM_A, matches.get(0).getHomeTeam(), "First match home team should be 'Team A'");
        assertEquals(TEAM_B, matches.get(0).getAwayTeam(), "First match away team should be 'Team B'");
        //Check that the second match has the correct home and away teams
        assertEquals(TEAM_C, matches.get(1).getHomeTeam(), "Second match home team should be 'Team C'");
        assertEquals(TEAM_D, matches.get(1).getAwayTeam(), "Second match away team should be 'Team D'");
    }
}
