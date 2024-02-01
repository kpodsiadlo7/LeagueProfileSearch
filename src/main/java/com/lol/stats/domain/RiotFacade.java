package com.lol.stats.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.lol.stats.adapter.MatchMapper;
import com.lol.stats.adapter.SummonerMapper;
import com.lol.stats.dto.MatchDto;
import com.lol.stats.dto.SummonerDto;
import com.lol.stats.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiotFacade {

    private final AllChampionClient allChampionClient;
    private final SummonerMapper summonerMapper;
    private final MatchMapper matchMapper;
    private final MatchClient matchClient;
    private final Provider provider;

    public SummonerDto getSummonerInfoByName(final String summonerName) throws InterruptedException {
        SummonerInfo summonerInfo = getSummonerInfo(summonerName);

        List<Rank> ranks = getSummonerRank(summonerInfo.getId());
        Champion champion = getMainChampion(summonerInfo.getPuuid());
        String latestLoLVersion = getLatestLoLVersion();

        return summonerMapper.toSummonerDto(bakeSummoner(summonerInfo, ranks, champion, latestLoLVersion));
    }

    //TODO ZAMIENIONE METODY
    private Summoner bakeSummoner(final SummonerInfo summonerInfo, final List<Rank> ranks, final Champion champion, final String latestLoLVersion) {
        Summoner summoner = setRanksForSoloAndFlex(ranks);
        String mainChampName = getChampionById(champion.getChampionId(), latestLoLVersion);

        return Summoner.builder()
                .id(summonerInfo.getId())
                .accountId(summonerInfo.getAccountId())
                .puuid(summonerInfo.getPuuid())
                .name(summonerInfo.getName())
                .profileIconId(summonerInfo.getProfileIconId())
                .summonerLevel(summonerInfo.getSummonerLevel())
                .ranks(ranks)
                .rankFlexColor(summoner.getRankFlexColor())
                .rankSoloColor(summoner.getRankSoloColor())
                .mainChamp(mainChampName)
                .versionLoL(latestLoLVersion)
                .build();
    }

    private Champion getMainChampion(final String puuid) {
        List<Champion> champions = provider.getChampionsByPuuId(puuid);
        if (champions != null && !champions.isEmpty()) {
            return champions.get(0);
        }
        return new Champion();
    }

    private Summoner setRanksForSoloAndFlex(List<Rank> ranks) {
        String flexRankColor = "";
        String soloRankColor = "";
        if (ranks != null && !ranks.isEmpty()) {
            for (var rank : ranks) {
                if (rank.getQueueType().equals("RANKED_FLEX_SR")) {
                    flexRankColor = setRankColorDependsOnTier(rank.getTier());

                } else if (rank.getQueueType().equals("RANKED_SOLO_5x5")) {
                    soloRankColor = setRankColorDependsOnTier(rank.getTier());
                }
            }
        }
        return Summoner.builder().rankSoloColor(soloRankColor).rankFlexColor(flexRankColor).build();
    }


    private String getLatestLoLVersion() {
        return provider.getLatestLoLVersion();
    }

    private SummonerInfo getSummonerInfo(String summonerName) {
        return provider.getSummonerInfo(summonerName);
    }

    private List<Rank> getSummonerRank(final String summonerId) {
        return provider.getLeagueV4Info(summonerId);
    }


    String getChampionById(final int championId, final String latestLoLVersion) {
        if (championId == -1) {
            return "brak";
        }
        String championName = getChampionByKey(championId, allChampionClient.getChampionById(latestLoLVersion).get("data")).get("name").asText();
        return championName != null ? championName.replaceAll("[\\s'.]+", "") : "Brak takiego championka";
    }

    private static JsonNode getChampionByKey(int key, JsonNode champions) {
        for (JsonNode championNode : champions) {
            int championKey = Integer.parseInt(championNode.get("key").asText());

            if (championKey == key) {
                return championNode;
            }
        }
        return null;
    }

    private String setRankColorDependsOnTier(final String rank) {
        String lowerCaseRank = rank.toLowerCase();
        return switch (lowerCaseRank) {
            case "gold" -> "#FFD700";
            case "silver" -> "#C0C0C0";
            case "platinum" -> "#A9A9A9";
            case "emerald" -> "#2ecc71";
            case "diamond" -> "#00CED1";
            case "bronze" -> "#964B00";
            case "grandmaster" -> "#000080";
            case "master" -> "#800080";
            default -> "#363949";
        };
    }
    //TODO ZAMIENIONE METODY

    List<String> getSummonerMatchesByNameAndCount(String summonerName, int count) {
        String puuId = getSummonerInfo(summonerName).getPuuid();
        return provider.getMatchesByPuuIdAndCount(puuId, count);
    }

    JsonNode getInfoAboutMatchById(String matchId) {
        return matchClient.getInfoAboutMatchById(matchId, provider.provideKey()).get("info");
    }
/*
    JsonNode getInfoAboutAllSummonerInActiveGame(String summonerName) {
        JsonNode summonerInfo = getSummonerInfo(summonerName);
        JsonNode matchInfo = summonerClient.getMatchInfoBySummonerId(summonerInfo.get("id").asText(), provider.provideKey());
        ObjectNode allInfoAboutMatch = JsonNodeFactory.instance.objectNode();
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        ArrayNode bannedChampionsArray = JsonNodeFactory.instance.arrayNode();

        if (!matchInfo.isEmpty() && !matchInfo.get("participants").isEmpty()) {
            String userTeam = null;
            for (JsonNode s : matchInfo.get("participants")) {
                ObjectNode summoner = (ObjectNode) s;
                ArrayNode ranks = getSummonerRank(s.get("summonerId").asText());
                setRankedSoloRank(ranks, summoner);
                summoner.put("champName", getChampionById(s.get("championId").asText()));
                summoner.put("1spellName", getSpellNameBySpellId(s.get("spell1Id").asText()));
                summoner.put("2spellName", getSpellNameBySpellId(s.get("spell2Id").asText()));
                arrayNode.add(summoner);

                if (s.get("summonerId").asText().equals(summonerInfo.get("id").asText()))
                    userTeam = summoner.get("teamId").asText();
            }
            allInfoAboutMatch.put("summoners", arrayNode);

            for (JsonNode champ : matchInfo.get("bannedChampions")) {
                bannedChampionsArray.add(champ.get("championId").asText());
            }

            allInfoAboutMatch.put("bannedChampions", bannedChampionsArray);
            allInfoAboutMatch.put("userTeam", userTeam);
            allInfoAboutMatch.put("gameMode", matchInfo.get("gameMode"));
        }
        return allInfoAboutMatch;
    }

 */

    private Match setRankedSoloRank(List<Rank> ranks) {
        String rank = "BRAK RANGI";
        String rankColor = "#363949";
        if (ranks != null && !ranks.isEmpty()) {
            for (Rank r : ranks) {
                if (r.getQueueType().equals("RANKED_SOLO_5x5")) {
                    rank = r.getTier();
                    rankColor = setRankColorDependsOnTier(rank);
                    log.warn("Powinien ustalić rangę {} i kolor {}", rank, rankColor);
                    return Match.builder().rank(rank).rankColor(rankColor).build();
                }
            }
        }
        return Match.builder().rank(rank).rankColor(rankColor).build();
    }

    private String getSpellNameBySpellId(String spellId) {
        JsonNode summonerSpells = allChampionClient.getSummonerSpells(getLatestLoLVersion());

        for (JsonNode n : summonerSpells.get("data")) {
            if (n.get("key").asText().equals(spellId)) {
                return n.get("name").asText();
            }
        }
        return "";
    }

    private LeagueInfo getLeagueInfo(String summonerId) {
        List<LeagueInfo> leagueInfoList = provider.getLeagueInfoListBySummonerId(summonerId);

        if (leagueInfoList != null && !leagueInfoList.isEmpty()) {
            for (LeagueInfo league : leagueInfoList) {
                if (league.getQueueType().equals("RANKED_SOLO_5x5")) {
                    return league;
                }
            }
        }
        return null;
    }

    public String getRandomSummonerNameFromExistingGame() {
        JsonNode exampleMatch = provider.getExampleSummonerNameFromExistingGame();
        if (exampleMatch != null && !exampleMatch.get("gameList").isEmpty()) {
            String summonerNameFromExistingGame = exampleMatch.get("gameList").get(0).get("participants").get(0).get("summonerName").asText();
            return summonerNameFromExistingGame != null ? summonerNameFromExistingGame : "Brak listy gier. Spróbuj ponownie za chwilę";
        }
        return "Brak listy gier. Spróbuj ponownie za chwilę";
    }


    public MatchDto getLast3MatchesBySummonerName(String summonerName) throws InterruptedException {
        List<String> matchesIdList = getSummonerMatchesByNameAndCount(summonerName, 20);
        Match leagueInfo = getLeagueInfoFromMatchesList(summonerName);
        return matchMapper.toDto(getLastRankedMatchesDependsOnCount(leagueInfo, matchesIdList, 3));
    }

    public MatchDto getLast20MatchesBySummonerName(String summonerName) throws InterruptedException {
        List<String> matchesIdList = getSummonerMatchesByNameAndCount(summonerName, 50);
        Match leagueInfo = getLeagueInfoFromMatchesList(summonerName);
        return matchMapper.toDto(getLastRankedMatchesDependsOnCount(leagueInfo, matchesIdList, 20));
    }

    private Match getLastRankedMatchesDependsOnCount(Match leagueInfo, List<String> matchesIdList, int count) throws InterruptedException {
        int matches = 0;
        int questions = 0;
        int wins = 0;
        int losses = 0;

        for (var singleMatch : matchesIdList) {
            JsonNode matchJN = getInfoAboutMatchById(singleMatch);
            if (matchJN.get("gameMode").asText().equals("CLASSIC")) {
                for (JsonNode m : matchJN.get("participants")) {
                    ChampMatch champMatch = new ChampMatch();
                    if (m.get("puuid").asText().equals(leagueInfo.getPuuid())) {
                        champMatch.setMatchChampName(m.get("championName").asText());
                        champMatch.setChampionId(m.get("championId").asInt());
                        champMatch.setAssists(m.get("assists").asInt());
                        champMatch.setKda(m.get("challenges").get("kda").asInt());
                        champMatch.setDeaths(m.get("deaths").asInt());
                        champMatch.setKills(m.get("kills").asInt());
                        champMatch.setLane(m.get("teamPosition").asText());
                        champMatch.setDealtDamage(m.get("totalDamageDealtToChampions").asInt());
                        switch (m.get("win").asText()) {
                            case "true" -> {
                                champMatch.setWin("WYGRANA");
                                champMatch.setWinColor("green");
                                wins++;
                            }

                            case "false" -> {
                                champMatch.setWin("PRZEGRANA");
                                champMatch.setWinColor("red");
                                losses++;
                            }
                        }
                        leagueInfo.getMatches().add(champMatch);
                        matches++;
                        if (matches >= count) {
                            leagueInfo.setWins(wins);
                            leagueInfo.setLosses(losses);
                            return leagueInfo;
                        }
                        break;
                    }
                }
            }
            questions++;
            if (questions >= 15) {
                sleep(1000);
                questions = 0;
            }
        }
        return null;
    }

    private Match getLeagueInfoFromMatchesList(String summonerName) {
        SummonerInfo summonerInfo = getSummonerInfo(summonerName);
        String summonerId = summonerInfo.getId();
        LeagueInfo leagueInfo = getLeagueInfo(summonerId);
        Match match = setRankedSoloRank(getSummonerRank(summonerId));

        return bakeMatch(summonerInfo, leagueInfo, match);
    }

    private Match bakeMatch(final SummonerInfo summonerInfo, final LeagueInfo leagueInfo, final Match match) {
        return Match.builder()
                .id(summonerInfo.getId())
                .accountId(summonerInfo.getAccountId())
                .puuid(summonerInfo.getPuuid())
                .name(summonerInfo.getName())
                .profileIconId(summonerInfo.getProfileIconId())
                .summonerLevel(summonerInfo.getSummonerLevel())
                .leagueInfo(leagueInfo)
                .rank(match.getRank())
                .rankColor(match.getRankColor())
                .matches(new ArrayList<>()).build();
    }
}
