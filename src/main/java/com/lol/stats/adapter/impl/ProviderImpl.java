package com.lol.stats.adapter.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.lol.stats.adapter.mapper.ChampionMapper;
import com.lol.stats.adapter.mapper.LeagueMapper;
import com.lol.stats.adapter.mapper.RankMapper;
import com.lol.stats.adapter.mapper.SummonerMapper;
import com.lol.stats.domain.*;
import com.lol.stats.model.Champion;
import com.lol.stats.model.LeagueInfo;
import com.lol.stats.model.Rank;
import com.lol.stats.model.SummonerInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderImpl implements Provider {

    private final SummonerClient summonerClient;
    private final SummonerMapper summonerMapper;
    private final ChampionMapper championMapper;
    private final RankMapper rankMapper;
    private final LeagueMapper leagueMapper;
    private final ClientLoLVersion clientLoLVersion;
    private final MatchClient matchClient;
    private final AllChampionClient allChampionClient;

    @Override
    public String provideKey() {
        try {
            String apiKeyFilePath = "x:/key.txt";
            Path path = Paths.get(apiKeyFilePath);
            byte[] apiKeyBytes = Files.readAllBytes(path);
            return new String(apiKeyBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "INVALID API_KEY";
    }

    @Override
    public SummonerInfo getSummonerInfo(final String summonerName) {
        return summonerMapper.fromSummonerInfoDto(summonerClient.getSummonerByName(summonerName, provideKey()));
    }

    @Override
    public String getLatestLoLVersion() {
        return clientLoLVersion.getLolVersions()[0];
    }

    @Override
    public List<Rank> getLeagueV4Info(final String summonerId) {
        return rankMapper.mapToRankListFromRankDtoList(summonerClient.getLeagueV4(summonerId, provideKey()));
    }

    @Override
    public List<Champion> getChampionsByPuuId(final String puuid) {
        return championMapper.mapToChampionListFromChampionDtoList(summonerClient.getChampions(puuid, provideKey()));
    }

    @Override
    public List<String> getMatchesByPuuIdAndCount(final String puuid, final int count) {
        return matchClient.getMatchesByPuuIdAndCount(puuid, count, provideKey());
    }

    @Override
    public List<LeagueInfo> getLeagueInfoListBySummonerId(String summonerId) {
        return leagueMapper.mapToLeagueInfoListFromLeagueInfoDtoList(summonerClient.getLeagueInfoBySummonerId(summonerId, provideKey()));
    }

    @Override
    public JsonNode getExampleSummonerNameFromExistingGame() {
        return summonerClient.getExampleSummonerNameFromRandomExistingGame(provideKey());
    }

    @Override
    public JsonNode getAllChampionsDependsOnLoLVersion(String latestLoLVersion) {
        return allChampionClient.getChampionById(latestLoLVersion);
    }

    @Override
    public JsonNode getInfoAboutMatchById(String matchId) {
        return matchClient.getInfoAboutMatchById(matchId, provideKey());
    }

    @Override
    public JsonNode getMatchInfoBySummonerId(String id) {
        return summonerClient.getMatchInfoBySummonerId(id, provideKey());
    }

    @Override
    public JsonNode getSummonerSpells(String latestLoLVersion) {
        return allChampionClient.getSummonerSpells(latestLoLVersion);
    }
}