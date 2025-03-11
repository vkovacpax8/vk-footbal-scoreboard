package footbal.scoreboard;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class Match {
    private final String homeTeam;
    private final String awayTeam;
    //The Match class uses AtomicInteger for the homeScore and awayScore. This allows safe increment and read operations without the need for synchronization. You can update scores concurrently without running into race conditions.
    private final AtomicInteger homeScore = new AtomicInteger(0);
    private final AtomicInteger awayScore = new AtomicInteger(0);
    private final LocalDateTime startTime;

    public Match(String homeTeam, String awayTeam) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.startTime = LocalDateTime.now();
    }

    public String getHomeTeam() {
        return homeTeam;
    }

    public String getAwayTeam() {
        return awayTeam;
    }

    private void setHomeScore(int score) {
        homeScore.set(score);
    }

    private void setAwayScore(int score) {
        awayScore.set(score);
    }

    public int getHomeScore() {
        return homeScore.get();
    }

    public int getAwayScore() {
        return awayScore.get();
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void updateScore(int homeScore, int awayScore) {
        setHomeScore(homeScore);
        setAwayScore(awayScore);
    }

    public int getTotalScore() {
        return getHomeScore() + getAwayScore();
    }

    @Override
    public String toString() {
        return homeTeam + " " + homeScore + " - " + awayScore + " " + awayTeam;
    }
}
