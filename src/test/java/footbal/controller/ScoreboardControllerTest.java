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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ScoreboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final String BASE_URL = "/vk/scoreboard";

    @BeforeEach
    public void setUp() throws Exception {
        mockMvc.perform(post(BASE_URL + "/reset"))
                .andExpect(status().isOk())
                .andExpect(content().string("Scoreboard has been reset."));
    }

    @Test
    public void testStartMatch() throws Exception {
        mockMvc.perform(post(BASE_URL + "/matches?homeTeam=Mexico&awayTeam=Canada"))
                .andExpect(status().isOk())
                .andExpect(content().string("Match started: Mexico vs Canada"));
    }

    @Test
    public void testStartMatchWithEmptyTeamNames() throws Exception {
        mockMvc.perform(post(BASE_URL + "/matches?homeTeam=&awayTeam="))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testConcurrentStartMatches() throws Exception {
        final int NUM_THREADS = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);
        try (ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS)) {

            for (int i = 0; i < NUM_THREADS; i++) {
                final Match match = new Match("Team " + i, "Team " + (i + 1)); // Create matching objects
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Wait for the main thread to signal
                        mockMvc.perform(post(BASE_URL + "/matches?homeTeam=" + match.getHomeTeam() + "&awayTeam=" + match.getAwayTeam()))
                                .andExpect(status().isOk());

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        doneLatch.countDown(); // Decrement when done
                    }
                });
            }

            startLatch.countDown(); // Allow all to start
            doneLatch.await(); // Wait for completion

            // Optionally, verify results
            mockMvc.perform(get(BASE_URL + "/summary"))
                    .andExpect(status().isOk());

            executor.shutdown();
        }
    }

    @Test
    public void testUpdateScore() throws Exception {
        mockMvc.perform(post(BASE_URL + "/matches?homeTeam=Spain&awayTeam=Brazil"));
        mockMvc.perform(put(BASE_URL + "/matches/0/score?homeScore=10&awayScore=2"))
                .andExpect(status().isOk())
                .andExpect(content().string("Score updated for match at index 0"));
    }

    @Test
    public void testUpdateScoreForNonExistentMatch() throws Exception {
        mockMvc.perform(put(BASE_URL + "/matches/999/score?homeScore=5&awayScore=3"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid match index: 999"));
    }

    @Test
    public void testUpdateScoreWithNegativeValue() throws Exception {
        mockMvc.perform(post(BASE_URL + "/matches?homeTeam=Spain&awayTeam=Brazil"));

        mockMvc.perform(put(BASE_URL + "/matches/0/score?homeScore=-5&awayScore=3"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Scores cannot be negative."));

        mockMvc.perform(put(BASE_URL + "/matches/0/score?homeScore=-5&awayScore=-3"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Scores cannot be negative."));

        mockMvc.perform(put(BASE_URL + "/matches/0/score?homeScore=5&awayScore=-3"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Scores cannot be negative."));
    }

    @Test
    public void testConcurrentUpdates() throws Exception {
        // First start a match
        Match match = new Match("Team A", "Team B");

        mockMvc.perform(post(BASE_URL + "/matches?homeTeam=" + match.getHomeTeam() + "&awayTeam=" + match.getAwayTeam()))
                .andExpect(status().isOk());

        final int NUM_THREADS = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);
        try (ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS)) {

            for (int i = 0; i < NUM_THREADS; i++) {
                final int scoreHome = i; // Incremental home score
                final int scoreAway = i + 1; // Incremental away score
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Wait for the main thread to signal
                        mockMvc.perform(put(BASE_URL + "/matches/0/score?homeScore=" + scoreHome + "&awayScore=" + scoreAway))
                                .andExpect(status().isOk());
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        doneLatch.countDown(); // Decrement when done
                    }
                });
            }

            startLatch.countDown(); // Allow all to start
            doneLatch.await(); // Wait for completion

            // Optionally, verify the scoring. You would check the state of the match accordingly
            mockMvc.perform(get(BASE_URL + "/summary"))
                    .andExpect(status().isOk());

            executor.shutdown();
        }
    }

    @Test
    public void testFinishMatch() throws Exception {
        mockMvc.perform(post(BASE_URL + "/matches?homeTeam=Germany&awayTeam=France"));
        mockMvc.perform(delete(BASE_URL + "/matches/0"))
                .andExpect(status().isOk())
                .andExpect(content().string("Match at index 0 finished."));
    }

    @Test
    public void testFinishNonExistentMatch() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/matches/999"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid match index: 999"));
    }

    @Test
    public void testConcurrentFinishMatches() throws Exception {
        // Start a match
        Match match = new Match("Team A", "Team B");
        mockMvc.perform(post(BASE_URL + "/matches?homeTeam=" + match.getHomeTeam() + "&awayTeam=" + match.getAwayTeam()))
                .andExpect(status().isOk());

        final int NUM_THREADS = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);
        try (ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS)) {

            for (int i = 0; i < NUM_THREADS; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Wait for the main thread to signal
                        mockMvc.perform(delete(BASE_URL + "/matches/0"))
                                .andExpect(status().isNoContent());
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        doneLatch.countDown(); // Decrement when done
                    }
                });
            }

            startLatch.countDown(); // Allow all to start
            doneLatch.await(); // Wait for completion

            // Verify the match has been finished
            mockMvc.perform(get(BASE_URL + "/summary"))
                    .andExpect(status().isOk());

            executor.shutdown();
        }
    }

    @Test
    public void testGetSummary() throws Exception {
        mockMvc.perform(post(BASE_URL + "/matches?homeTeam=Uruguay&awayTeam=Italy"));
        mockMvc.perform(post(BASE_URL + "/matches?homeTeam=Spain&awayTeam=Brazil"));
        mockMvc.perform(post(BASE_URL + "/matches?homeTeam=Mexico&awayTeam=Canada"));

        mockMvc.perform(get(BASE_URL + "/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[\"1. Uruguay 0 - 0 Italy\", \"2. Spain 0 - 0 Brazil\", \"3. Mexico 0 - 0 Canada\"]"));
    }

    @Test
    public void testGetSummaryWhenNoMatches() throws Exception {
        mockMvc.perform(get(BASE_URL + "/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]")); // Expecting an empty list
    }

    @Test
    public void testGetSummaryAfterVariousMatches() throws Exception {
        // Start several matches
        mockMvc.perform(post(BASE_URL + "/matches?homeTeam=Uruguay&awayTeam=Italy"));
        mockMvc.perform(post(BASE_URL + "/matches?homeTeam=Spain&awayTeam=Brazil"));

        // Update scores
        mockMvc.perform(put(BASE_URL + "/matches/0/score?homeScore=3&awayScore=0"));
        mockMvc.perform(put(BASE_URL + "/matches/1/score?homeScore=1&awayScore=2"));

        // Get summary
        mockMvc.perform(get(BASE_URL + "/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[\"1. Uruguay 3 - 0 Italy\", \"2. Spain 1 - 2 Brazil\"]"));
    }

    @Test
    public void testResetScoreboard() throws Exception {
        mockMvc.perform(post(BASE_URL + "/matches?homeTeam=Italy&awayTeam=France"))
                .andExpect(status().isOk())
                .andExpect(content().string("Match started: Italy vs France"));

        mockMvc.perform(post(BASE_URL + "/reset"))
                .andExpect(status().isOk())
                .andExpect(content().string("Scoreboard has been reset."));

        mockMvc.perform(get(BASE_URL + "/summary"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]")); // Expecting an empty list after reset
    }
}
