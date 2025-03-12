# Vladimir Kovac's Live Football World Cup Scoreboard Library/REST Application

## Overview
This library provides an in-memory scoreboard for managing ongoing football matches, allowing you to start matches, update scores, finish matches, and retrieve a summary of matches ordered by score.

## Features

**ScoreboardService Class Features:**

- **Start a new match**: Allows starting a new football match by specifying the home and away teams.
- **Update scores**: Enables updating the scores of an ongoing match by specifying the match index and the new scores for the home and away teams.
- **Finish a match**: Allows finishing an ongoing match by specifying the match index, which removes the match from the scoreboard.
- **Get a summary of matches ordered by score**: Provides a summary of all ongoing matches, ordered by their total score and start time.
- **Reset the scoreboard**: Resets the scoreboard, removing all ongoing matches.

**Controller Class Features:**

- **Start a new match**: Endpoint to start a new match by providing the home and away teams.
- **Update scores**: Endpoint to update the scores of an ongoing match by providing the match index and new scores.
- **Finish a match**: Endpoint to finish an ongoing match by providing the match index.
- **Get a summary of matches ordered by score**: Endpoint to retrieve a summary of all ongoing matches, ordered by their total score and start time.
- **Reset the scoreboard**: Endpoint to reset the scoreboard, removing all ongoing matches.

These features align with the methods and functionalities provided in the ScoreboardService class and are typically exposed through corresponding **REST endpoints** in the controller class.

## Usage Example

### Using the ScoreboardService Class

1. Create a `ScoreboardService` instance.
2. Use `startMatch(homeTeam, awayTeam)` to add a match.
3. Use `updateScore(index, homeScore, awayScore)` to update the score.
4. Use `finishMatch(index)` to remove a match.
5. Use `getFormatedSortedSummary()` to get the current list of matches.

### Using the Controller Class

1. Start a new match by sending a POST request to `/matches` with the home and away teams in the request body: `curl -X POST http://localhost:8081/matches -H "Content-Type: application/json" -d '{"homeTeam": "Team A", "awayTeam": "Team B"}'`


2. Update the scores of an ongoing match by sending a PUT request to `/matches/{index}/score` with the new scores in the request body: `curl -X PUT http://localhost:8081/matches/0/score -H "Content-Type: application/json" -d '{"homeScore": 1, "awayScore": 2}'`


3. Finish an ongoing match by sending a DELETE request to `/matches/{index}`: `curl -X DELETE http://localhost:8081/matches/0`


4. Retrieve a summary of all ongoing matches by sending a GET request to `/matches/summary`: `curl -X GET http://localhost:8081/matches/summary`


5. Reset the scoreboard by sending a POST request to `/matches/reset`: `curl -X POST http://localhost:8081/matches/reset`

## Notes
- This implementation uses an in\-memory store.
- The matches are sorted by total score and then by the start time.
- The application is built using Spring Boot and Maven.

## TDD Approach
The implementation was guided by test-driven development practices with unit tests covering all major functionalities.

The TDD (Test-Driven Development) approach described in the code involves the following steps:

- **Write a Test**: Before writing any functional code, a unit test is written for the desired functionality. This test initially fails because the functionality is not yet implemented.
- **Implement the Code**: Write the minimum amount of code necessary to make the test pass. This code is often simple and straightforward.
- **Refactor**: Once the test passes, the code is refactored to improve its structure and readability while ensuring that all tests still pass.
- **Repeat**: This cycle is repeated for each new feature or functionality.
- In the provided code, unit tests cover all major functionalities of the **ScoreboardService class**, ensuring that each feature works as expected before moving on to the next.
