package footbal.scoreboard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;

public class ScoreboardServiceTest {
    private ScoreboardService scoreboardService;

    @BeforeEach
    public void setUp() {
        scoreboardService = new ScoreboardService();
    }

    @Test
    public void testStartMatch() {
        scoreboardService.startMatch("Mexico", "Canada");
        List<String> summary = scoreboardService.getFormatedSummary();
        assertEquals(1, summary.size(), "Match should be started successfully");
        assertEquals("1. Mexico 0 - 0 Canada", summary.getFirst());
    }

    @Test
    public void testConcurrentStartMatches() throws InterruptedException {
        final int NUM_THREADS = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);
        try (ExecutorService executor = newFixedThreadPool(NUM_THREADS)) {

            // Submit concurrent tasks to start new matches
            for (int i = 0; i < NUM_THREADS; i++) {
                final int matchIndex = i;
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Wait for the main thread to start
                        scoreboardService.startMatch("Team " + matchIndex, "Team " + (matchIndex + 1));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } finally {
                        doneLatch.countDown(); // Decrement the count when done
                    }
                });
            }

            startLatch.countDown(); // Allow all tasks to start
            doneLatch.await(); // Wait for all tasks to finish

            // After all threads have finished, verify that the number of matches equals the number of threads
            assertEquals(NUM_THREADS, scoreboardService.getSummary().size(), "Number of matches should be equal to number of threads.");

            executor.shutdown();
        }
    }

    @Test
    public void testStartMatchWithNullNames() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> scoreboardService.startMatch(null, "Team B"));
        assertEquals("Team names cannot be null or empty", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> scoreboardService.startMatch("Team A", null));
        assertEquals("Team names cannot be null or empty", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> scoreboardService.startMatch(null, null));
        assertEquals("Team names cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testStartMatchWithEmptyNames() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> scoreboardService.startMatch("", "Team B"));
        assertEquals("Team names cannot be null or empty", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> scoreboardService.startMatch("Team A", ""));
        assertEquals("Team names cannot be null or empty", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> scoreboardService.startMatch("", ""));
        assertEquals("Team names cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testUpdateScore() {
        scoreboardService.startMatch("Spain", "Brazil");
        scoreboardService.updateScore(0, 10, 2);
        List<String> summary = scoreboardService.getFormatedSummary();
        assertEquals("1. Spain 10 - 2 Brazil", summary.getFirst(), "Score should be updated correctly");
    }

    @Test
    public void testUpdateScoreInvalidIndex() {
        Exception exception = assertThrows(IndexOutOfBoundsException.class, () -> scoreboardService.updateScore(999, 1, 1));
        assertEquals("Match index is out of range.", exception.getMessage());
    }

    @Test
    public void testUpdateScoreNegativeValue() {
        scoreboardService.startMatch("Spain", "Brazil");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> scoreboardService.updateScore(0, -1, 1));
        assertEquals("Scores cannot be negative.", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> scoreboardService.updateScore(0, -1, -1));
        assertEquals("Scores cannot be negative.", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> scoreboardService.updateScore(0, 1, -1));
        assertEquals("Scores cannot be negative.", exception.getMessage());
    }

    @Test
    public void testConcurrentScoreUpdates() throws InterruptedException {
        final int NUM_THREADS = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);
        try (ExecutorService executor = newFixedThreadPool(NUM_THREADS)) {

            // Start a match
            String homeTeam = "Team A";
            String awayTeam = "Team B";
            scoreboardService.startMatch(homeTeam, awayTeam);

            // Submit concurrent tasks to update the score of the same match
            for (int i = 1; i <= NUM_THREADS; i++) {
                final int scoreA = i; // Home team score
                final int scoreB = i + 1; // Away team score
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Wait for the main thread to start
                        scoreboardService.updateScore(0, scoreA, scoreB); // Update match 0 (first one)
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown(); // Decrement the count when done
                    }
                });
            }

            startLatch.countDown(); // Allow all tasks to start
            doneLatch.await(); // Wait for all tasks to finish

            // Check the final score - expecting the last score update to win out
            int expectedFinalScoreA = NUM_THREADS - 1; // The last score update's home score
            // The last score update's away score

            assertEquals(expectedFinalScoreA, scoreboardService.getSummary().getFirst().getHomeScore(), "Home score is incorrect.");
            assertEquals(NUM_THREADS, scoreboardService.getSummary().getFirst().getAwayScore(), "Away score is incorrect.");

            executor.shutdown();
        }
    }

    @Test
    public void testFinishMatch() {
        scoreboardService.startMatch("Germany", "France");
        scoreboardService.startMatch("Argentina", "Australia");
        scoreboardService.finishMatch(0);
        List<String> summary = scoreboardService.getFormatedSummary();
        assertEquals(1, summary.size());
        assertEquals("1. Argentina 0 - 0 Australia", summary.getFirst(), "Match should be finished and removed from the list");
    }

    @Test
    public void testFinishMatchInvalidIndex() {
        Exception exception = assertThrows(IndexOutOfBoundsException.class, () -> scoreboardService.finishMatch(999));
        assertEquals("Match index is out of range.", exception.getMessage());
    }

    @Test
    public void testFinishMatchConcurrently() throws InterruptedException {
        final int NUM_THREADS = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);
        try (ExecutorService executor = newFixedThreadPool(NUM_THREADS)) {

            // Start one match
            scoreboardService.startMatch("Team A", "Team B");

            // Submit concurrent tasks to finish the same match
            for (int i = 0; i < NUM_THREADS; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Wait for the main thread to start
                        scoreboardService.finishMatch(0); // Finish the match at index 0
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown(); // Decrement the count when done
                    }
                });
            }

            startLatch.countDown(); // Allow all tasks to start
            doneLatch.await(); // Wait for all tasks to finish

            // After all threads have finished, ensure the match is fully removed
            assertEquals(0, scoreboardService.getSummary().size(), "Match should be removed after finishing.");

            executor.shutdown();
        }
    }

    @Test
    public void testGetSummaryEmpty() {
        assertTrue(scoreboardService.getFormatedSummary().isEmpty(), "Summary should be empty when no matches have been played");
    }

    @Test
    public void testGetSummaryOrdered() {
        scoreboardService.startMatch("Uruguay", "Italy");
        scoreboardService.updateScore(0, 6, 6);

        scoreboardService.startMatch("Spain", "Brazil");
        scoreboardService.updateScore(1, 10, 2);

        scoreboardService.startMatch("Mexico", "Canada");
        scoreboardService.updateScore(2, 0, 5);

        scoreboardService.startMatch("Argentina", "Australia");
        scoreboardService.updateScore(3, 3, 1);

        scoreboardService.startMatch("Germany", "France");
        scoreboardService.updateScore(4, 2, 2);

        List<String> summary = scoreboardService.getFormatedSummary();
        assertEquals("1. Uruguay 6 - 6 Italy", summary.get(0));
        assertEquals("2. Spain 10 - 2 Brazil", summary.get(1));
        assertEquals("3. Mexico 0 - 5 Canada", summary.get(2));
        assertEquals("4. Argentina 3 - 1 Australia", summary.get(3));
        assertEquals("5. Germany 2 - 2 France", summary.get(4));
    }

    @Test
    public void testReset() {
        scoreboardService.startMatch("Team K", "Team L");
        assertFalse(scoreboardService.getFormatedSummary().isEmpty(), "Summary should not be empty before reset");

        scoreboardService.reset();
        assertTrue(scoreboardService.getFormatedSummary().isEmpty(), "Summary should be empty after reset");
    }
}
