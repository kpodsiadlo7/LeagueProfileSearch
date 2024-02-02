package com.lol.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChampMatchDto {
    private String matchChampName;
    private int championId;
    private int assists;
    private int kda;
    private int deaths;
    private int kills;
    private String lane;
    private int dealtDamage;
    private String win;
    private String winColor;
}