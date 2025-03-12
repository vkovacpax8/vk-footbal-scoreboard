package footbal.scoreboard.service;

import footbal.scoreboard.Match;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ScoreboardService {
    public static final String MATCH_INDEX_IS_OUT_OF_RANGE = "Match index is out of range.";
    public static final String CANNOT_BE_NULL_OR_EMPTY = "Team names cannot be null or empty";
    public static final String CANNOT_BE_NEGATIVE = "Scores cannot be negative.";
    public static final String ALREADY_EXISTS = "A match with one of the teams already exists.";

    //CopyOnWriteArrayList allows for safe iteration and modification of the list without explicit synchronization,
    //although it does incur a performance penalty on write operations since it creates a new copy upon modification.
    private final CopyOnWriteArrayList<Match> matches = new CopyOnWriteArrayList<>();

    public List<Match> getMatches() {
        return new ArrayList<>(matches);
    }

    public void startMatch(String homeTeam, String awayTeam) {
        if (!StringUtils.hasText(homeTeam) || !StringUtils.hasText(awayTeam)) {
            throw new IllegalArgumentException(CANNOT_BE_NULL_OR_EMPTY);
        }

        if (matches.stream().anyMatch(match -> match.getHomeTeam().equals(homeTeam) || match.getAwayTeam().equals(awayTeam))) {
            throw new IllegalArgumentException(ALREADY_EXISTS);
        }

        matches.add(new Match(homeTeam, awayTeam));
    }

    public void updateScore(int matchIndex, int homeScore, int awayScore) {
        validateMatchIndex(matchIndex);

        if (homeScore < 0 || awayScore < 0) {
            throw new IllegalArgumentException(CANNOT_BE_NEGATIVE);
        }

        matches.get(matchIndex).updateScore(homeScore, awayScore);
    }

    public void finishMatch(int index) {
        validateMatchIndex(index);
        matches.remove(index);
    }

    private void validateMatchIndex(int index) {
        if (index < 0 || index >= matches.size()) {
            throw new IndexOutOfBoundsException(MATCH_INDEX_IS_OUT_OF_RANGE);
        }
    }

    public List<String> getFormatedSortedSummary() {
        return IntStream.range(0, matches.size())
                .mapToObj(i -> (i + 1) + ". " + sortMatches().get(i).toString())
                .collect(Collectors.toList());
    }

    public List<Match> getSortedMatches() {
        return sortMatches();
    }

    private List<Match> sortMatches() {
        return matches.stream()
                .sorted(Comparator.comparingInt(Match::getTotalScore)
                        .reversed()
                        .thenComparing(Comparator.comparing(Match::getStartTime).reversed()))
                .collect(Collectors.toList());
    }

    public void reset() {
        matches.clear();
    }
}
