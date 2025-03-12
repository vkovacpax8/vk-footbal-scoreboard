package footbal.controller;

import footbal.scoreboard.Match;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static footbal.controller.ScoreboardController.INVALID_MATCH_INDEX;
import static footbal.controller.ScoreboardController.SCOREBOARD_HAS_BEEN_RESET;
import static footbal.scoreboard.service.ScoreboardService.*;
import static footbal.scoreboard.service.ScoreboardServiceTest.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ScoreboardControllerTest {

    public static final String HOME_TEAM = "homeTeam";
    public static final String AWAY_TEAM = "awayTeam";
    public static final String HOME_SCORE = "homeScore";
    public static final String AWAY_SCORE = "awayScore";
    public static final String TEAM = "Team ";
    @Autowired
    private MockMvc mockMvc;

    private final String BASE_URL = "/vk/scoreboard";
    private final String MATCHES_URL = BASE_URL + "/matches";
    private final String SUMMARY_URL = BASE_URL + "/summary";

    @BeforeEach
    public void setUp() throws Exception {
        //Reset the scoreboard before each test
        resetScoreboard();
    }

    @Test
    public void testGetMatches() throws Exception {
        // Get the list of matches
        startAndGetTwoMatches();
    }

    @Test
    public void testGetMatchesAfterSorting() throws Exception {
        // Get the list of matches
        startAndGetTwoMatches();

        updateScore(0, 10, 2);
        updateScore(1, 12, 4);

        getSummary("[\"1. " + TEAM_C + " 12 - 4 " + TEAM_D + "\", \"2. " + TEAM_A + " 10 - 2 " + TEAM_B + "\"]");

        // Get the list of matches
        getTwoMatches();
    }

    @Test
    public void testStartMatchAndExisting() throws Exception {
        // Start a match
        startMatch(TEAM_A, TEAM_B);
        // Start a match with the same teams
        startMatchBadRequest(TEAM_A, TEAM_B, ALREADY_EXISTS);
    }

    @Test
    public void testStartMatchWithEmptyTeamNames() throws Exception {
        // Start a match with empty team names
        startMatchBadRequest("", "", CANNOT_BE_NULL_OR_EMPTY);
    }

    @Test
    public void testConcurrentStartMatches() throws Exception {
        // Start a match
        final int NUM_THREADS = 10;
        // Create a latch to synchronize the threads
        final CountDownLatch startLatch = new CountDownLatch(1);
        // Create a latch to signal when all threads are done
        final CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);
        // Create a thread pool
        try (ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS)) {

            // Start multiple matches concurrently
            for (int i = 0; i < NUM_THREADS; i++) {
                // Create matching objects
                final Match match = new Match(TEAM + i, TEAM + (i + 1));
                // Submit a task to the executor
                executor.submit(() -> {
                    try {
                        // Wait for the main thread to signal
                        startLatch.await();
                        // Start a match
                        startMatch(match.getHomeTeam(), match.getAwayTeam());
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        // Decrement when done
                        doneLatch.countDown();
                    }
                });
            }

            // Allow all to start
            startLatch.countDown();
            // Wait for completion
            doneLatch.await();

            // Optionally, verify results
            verifyScoring();

            executor.shutdown();
        }
    }

    @Test
    public void testUpdateScoreAndLowering() throws Exception {
        // Start a match
        startMatch(TEAM_A, TEAM_B);
        // Update the score for a match
        updateScore(0, 10, 2);
        // Get the list of matches
        getSummary("[\"1. " + TEAM_A + " 10 - 2 " + TEAM_B + "\"]");

        // Update the score for a match
        updateScore(0, 8, 1);
        // Get the list of matches
        getSummary("[\"1. " + TEAM_A + " 8 - 1 " + TEAM_B + "\"]");
    }

    @Test
    public void testUpdateScoreForNonExistentMatch() throws Exception {
        // Update the score for a non-existent match
        updateScoreBadRequest(999, 5, 3, INVALID_MATCH_INDEX + "999");
    }

    @Test
    public void testUpdateScoreWithNegativeValue() throws Exception {
        // Start a match
        startMatch(TEAM_A, TEAM_B);
        // Update the score for a match
        updateScoreBadRequest(0, -5, 3, CANNOT_BE_NEGATIVE);
        // Update the score for a match
        updateScoreBadRequest(0, -5, -3, CANNOT_BE_NEGATIVE);
        // Update the score for a match
        updateScoreBadRequest(0, 5, -3, CANNOT_BE_NEGATIVE);
    }

    @Test
    public void testConcurrentUpdates() throws Exception {
        // Start a match
        startMatch(TEAM_A, TEAM_B);

        // Update the score for a match
        final int NUM_THREADS = 10;
        // Create a latch to synchronize the threads
        final CountDownLatch startLatch = new CountDownLatch(1);
        // Create a latch to signal when all threads are done
        final CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);
        // Create a thread pool
        try (ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS)) {

            // Update the score concurrently
            for (int i = 0; i < NUM_THREADS; i++) {
                // Incremental home score
                final int scoreHome = i;
                // Incremental away score
                final int scoreAway = i + 1;
                // Submit a task to the executor
                executor.submit(() -> {
                    try {
                        // Wait for the main thread to signal
                        startLatch.await();
                        // Update the score for a match
                        updateScore(0, scoreHome, scoreAway);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        // Decrement when done
                        doneLatch.countDown();
                    }
                });
            }

            // Allow all to start
            startLatch.countDown();
            // Wait for completion
            doneLatch.await();

            // Optionally, verify the scoring. You would check the state of the match accordingly
            verifyScoring();

            executor.shutdown();
        }
    }

    @Test
    public void testFinishMatch() throws Exception {
        // Start a match
        startMatch(TEAM_A, TEAM_B);
        // Finish a match
        finishMatch();
    }

    @Test
    public void testFinishNonExistentMatch() throws Exception {
        // Finish a non-existent match
        finishMatchBadRequest();
    }

    @Test
    public void testConcurrentFinishMatches() throws Exception {
        // Start a match
        startMatch(TEAM_A, TEAM_B);

        // Finish multiple matches concurrently
        final int NUM_THREADS = 10;
        // Create a latch to synchronize the threads
        final CountDownLatch startLatch = new CountDownLatch(1);
        // Create a latch to signal when all threads are done
        final CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);
        // Create a thread pool
        try (ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS)) {

            // Finish multiple matches concurrently
            for (int i = 0; i < NUM_THREADS; i++) {
                // Submit a task to the executor
                executor.submit(() -> {
                    try {
                        // Wait for the main thread to signal
                        startLatch.await();
                        // Finish a match
                        finishMatch();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        // Decrement when done
                        doneLatch.countDown();
                    }
                });
            }

            // Allow all to start
            startLatch.countDown();
            // Wait for completion
            doneLatch.await();

            // Verify the match has been finished
            verifyScoring();

            executor.shutdown();
        }
    }

    @Test
    public void testGetSummary() throws Exception {
        // Start several matches
        startMatch(TEAM_A, TEAM_B);
        startMatch(TEAM_C, TEAM_D);
        startMatch(TEAM_E, TEAM_F);

        // Get the list of matches
        getSummary("[\"1. " + TEAM_E + " 0 - 0 " + TEAM_F + "\", \"2. " + TEAM_C + " 0 - 0 " + TEAM_D + "\", \"3. " + TEAM_A + " 0 - 0 " + TEAM_B + "\"]");
    }

    @Test
    public void testGetSummaryWhenNoMatches() throws Exception {
        // Get the list of matches
        getSummary("[]");
    }

    @Test
    public void testGetSummaryAfterVariousMatches() throws Exception {
        // Start several matches
        startMatch(TEAM_A, TEAM_B);
        startMatch(TEAM_C, TEAM_D);

        // Update scores
        updateScore(0, 3, 0);
        updateScore(1, 1, 2);

        // Get summary
        getSummary("[\"1. " + TEAM_C + " 1 - 2 " + TEAM_D + "\", \"2. " + TEAM_A + " 3 - 0 " + TEAM_B + "\"]");
    }

    @Test
    public void testResetScoreboard() throws Exception {
        startMatch(TEAM_A, TEAM_B);
        resetScoreboard();
        getSummary("[]");
    }

    private void startAndGetTwoMatches() throws Exception {
        // Start a few matches
        startMatch(TEAM_A, TEAM_B);
        startMatch(TEAM_C, TEAM_D);

        getTwoMatches();
    }

    private void startMatch(String homeTeam, String awayTeam) throws Exception {
        // Start a match
        mockMvc.perform(post(MATCHES_URL)
                        .param(HOME_TEAM, homeTeam)
                        .param(AWAY_TEAM, awayTeam))
                .andExpect(status().isOk());
    }

    private void startMatchBadRequest(String homeTeam, String awayTeam, String message) throws Exception {
        // Start a match
        mockMvc.perform(post(MATCHES_URL)
                        .param(HOME_TEAM, homeTeam)
                        .param(AWAY_TEAM, awayTeam))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(message));
    }

    private void getTwoMatches() throws Exception {
        // Get the list of matches
        mockMvc.perform(get(MATCHES_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                        "[{\"homeTeam\":\"" + TEAM_A +
                                "\",\"awayTeam\":\"" + TEAM_B +
                                "\"},{\"homeTeam\":\"" + TEAM_C +
                                "\",\"awayTeam\":\"" + TEAM_D +
                                "\"}]"));
    }

    private void resetScoreboard() throws Exception {
        // Reset the scoreboard
        mockMvc.perform(post(BASE_URL + "/reset"))
                .andExpect(status().isOk())
                .andExpect(content().string(SCOREBOARD_HAS_BEEN_RESET));
    }

    private void updateScore(int matchIndex, int homeScore, int awayScore) throws Exception {
        // Update the score for a match
        mockMvc.perform(put(MATCHES_URL + "/" + matchIndex + "/score")
                        .param(HOME_SCORE, String.valueOf(homeScore))
                        .param(AWAY_SCORE, String.valueOf(awayScore)))
                .andExpect(status().isOk())
                .andExpect(content().string("Score updated for match at index " + matchIndex));
    }

    private void updateScoreBadRequest(int matchIndex, int homeScore, int awayScore, String message) throws Exception {
        // Update the score for a match
        mockMvc.perform(put(MATCHES_URL + "/" + matchIndex + "/score")
                        .param(HOME_SCORE, String.valueOf(homeScore))
                        .param(AWAY_SCORE, String.valueOf(awayScore)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(message));
    }

    private void getSummary(String expectedResult) throws Exception {
        mockMvc.perform(get(SUMMARY_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedResult));
    }

    private void verifyScoring() throws Exception {
        mockMvc.perform(get(SUMMARY_URL))
                .andExpect(status().isOk());
    }

    private void finishMatch() throws Exception {
        mockMvc.perform(delete(MATCHES_URL + "/" + 0))
                .andExpect(status().isOk())
                .andExpect(content().string("Match finished at index: " + 0));
    }

    private void finishMatchBadRequest() throws Exception {
        mockMvc.perform(delete(MATCHES_URL + "/" + 999))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid match index: 999"));
    }
}
