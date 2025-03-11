package footbal.controller;

import footbal.scoreboard.service.ScoreboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vk/scoreboard")
public class ScoreboardController {

    private final ScoreboardService scoreboardService = new ScoreboardService();

    @PostMapping("/matches")
    public ResponseEntity<String> startMatch(@RequestParam("homeTeam") String homeTeam, @RequestParam("awayTeam") String awayTeam) {
        if (!StringUtils.hasText(homeTeam) || !StringUtils.hasText(awayTeam)) {
            return ResponseEntity.badRequest().body("Team names must not be empty");
        }
        scoreboardService.startMatch(homeTeam, awayTeam);
        return ResponseEntity.ok("Match started: " + homeTeam + " vs " + awayTeam);
    }

    @PutMapping("/matches/{index}/score")
    public ResponseEntity<String> updateScore(@PathVariable("index") int index, @RequestParam("homeScore") int homeScore, @RequestParam("awayScore") int awayScore) {
        try {
            scoreboardService.updateScore(index, homeScore, awayScore);
            return ResponseEntity.ok("Score updated for match at index " + index);
        } catch (IndexOutOfBoundsException e) {
            return ResponseEntity.badRequest().body("Invalid match index: " + index);
        }catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Scores cannot be negative.");
        }
    }

    @DeleteMapping("/matches/{index}")
    public ResponseEntity<String> finishMatch(@PathVariable("index") int index) {
        try {
            scoreboardService.finishMatch(index);
            return ResponseEntity.ok("Match at index " + index + " finished.");
        } catch (IndexOutOfBoundsException e) {
            return ResponseEntity.badRequest().body("Invalid match index: " + index);
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<List<String>> getSummary() {
        List<String> summary = scoreboardService.getFormatedSummary();
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/reset")
    public ResponseEntity<String> resetScoreboard() {
        scoreboardService.reset();
        return ResponseEntity.ok("Scoreboard has been reset.");
    }
}
