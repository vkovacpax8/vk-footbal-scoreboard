package footbal.scoreboard.service;

import footbal.scoreboard.Match;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ScoreboardService {
    //We use CopyOnWriteArrayList for storing Match objects. This allows for safe iteration and modification of the list without explicit synchronization, although it does incur a performance penalty on write operations since it creates a new copy upon modification.
    private final CopyOnWriteArrayList<Match> matches = new CopyOnWriteArrayList<>();

    public void startMatch(String homeTeam, String awayTeam) {
        if (homeTeam == null || homeTeam.isEmpty() || awayTeam == null || awayTeam.isEmpty()) {
            throw new IllegalArgumentException("Team names cannot be null or empty");
        }
        Match match = new Match(homeTeam, awayTeam);
        matches.add(match);
    }

    public void updateScore(int matchIndex, int homeScore, int awayScore) {
        if (matchIndex < 0 || matchIndex >= matches.size()) {
            throw new IndexOutOfBoundsException("Match index is out of range.");
        }

        if (homeScore < 0 || awayScore < 0) {
            throw new IllegalArgumentException("Scores cannot be negative.");
        }

        Match match = matches.get(matchIndex);
        match.updateScore(homeScore, awayScore);
    }

    public void finishMatch(int index) {
        if (index >= 0 && index < matches.size()) {
            matches.remove(index);
        } else {
            throw new IndexOutOfBoundsException("Match index is out of range.");
        }
    }

    public List<String> getFormatedSummary() {
        matches.sort(Comparator.comparingInt(Match::getTotalScore)
                .reversed()
                .thenComparing(Match::getStartTime));

        List<String> summary = new ArrayList<>();
        for (int i = 0; i < matches.size(); i++) {
            summary.add((i + 1) + ". " + matches.get(i).toString());
        }

        return summary;
    }

    public List<Match> getSummary() {
        matches.sort(Comparator.comparingInt(Match::getTotalScore)
                .reversed()
                .thenComparing(Match::getStartTime));

        return new ArrayList<>(matches);
    }

    public void reset() {
        matches.clear();
    }
}
