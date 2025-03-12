package footbal.controller;

import footbal.scoreboard.Match;
import footbal.scoreboard.service.ScoreboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static footbal.scoreboard.service.ScoreboardService.*;

@RestController
@RequestMapping("/vk/scoreboard")
public class ScoreboardController {
    public static final String INVALID_MATCH_INDEX = "Invalid match index: ";
    public static final String UPDATED_FOR_MATCH_AT_INDEX = "Score updated for match at index ";
    public static final String FINISHED_AT_INDEX = "Match finished at index: ";
    public static final String SCOREBOARD_HAS_BEEN_RESET = "Scoreboard has been reset.";

    private final ScoreboardService scoreboardService = new ScoreboardService();

    @GetMapping("/matches")
    public List<Match> getMatches() {
        return scoreboardService.getMatches();
    }

    @PostMapping("/matches")
    public ResponseEntity<String> startMatch(@RequestParam("homeTeam") String homeTeam,
                                             @RequestParam("awayTeam") String awayTeam) {
        if (!StringUtils.hasText(homeTeam) || !StringUtils.hasText(awayTeam)) {
            return ResponseEntity.badRequest().body(CANNOT_BE_NULL_OR_EMPTY);
        }
        try {
            scoreboardService.startMatch(homeTeam, awayTeam);
            return ResponseEntity.ok("Match started: " + homeTeam + " vs " + awayTeam);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ALREADY_EXISTS);
        }
    }

    @PutMapping("/matches/{index}/score")
    public ResponseEntity<String> updateScore(@PathVariable("index") int index,
                                              @RequestParam("homeScore") int homeScore,
                                              @RequestParam("awayScore") int awayScore) {
        try {
            scoreboardService.updateScore(index, homeScore, awayScore);
            return ResponseEntity.ok(UPDATED_FOR_MATCH_AT_INDEX + index);
        } catch (IndexOutOfBoundsException e) {
            return ResponseEntity.badRequest().body(INVALID_MATCH_INDEX + index);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(CANNOT_BE_NEGATIVE);
        }
    }

    @DeleteMapping("/matches/{index}")
    public ResponseEntity<String> finishMatch(@PathVariable("index") int index) {
        try {
            scoreboardService.finishMatch(index);
            return ResponseEntity.ok(FINISHED_AT_INDEX + index);
        } catch (IndexOutOfBoundsException e) {
            return ResponseEntity.badRequest().body(INVALID_MATCH_INDEX + index);
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<List<String>> getSummary() {
        return ResponseEntity.ok(scoreboardService.getFormatedSortedSummary());
    }

    @PostMapping("/reset")
    public ResponseEntity<String> resetScoreboard() {
        scoreboardService.reset();
        return ResponseEntity.ok(SCOREBOARD_HAS_BEEN_RESET);
    }
}
