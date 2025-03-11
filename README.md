# Live Football World Cup Scoreboard Library

## Overview
This library provides an in-memory scoreboard for ongoing football matches during the World Cup.

## Features
- Start a new match
- Update scores
- Finish a match
- Get a summary of matches ordered by score

## Usage Example
1. Create a `Scoreboard` instance.
2. Use `startMatch(homeTeam, awayTeam)` to add a match.
3. Use `updateScore(index, homeScore, awayScore)` to update the score.
4. Use `finishMatch(index)` to remove a match.
5. Use `getSummary()` to get the current list of matches.

## Notes
- This implementation uses an in-memory store.
- The matches are sorted by total score and then by the start time.

## TDD Approach
The implementation was guided by test-driven development practices with unit tests covering all major functionalities.
