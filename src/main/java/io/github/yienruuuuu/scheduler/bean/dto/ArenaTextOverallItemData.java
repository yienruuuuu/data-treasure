package io.github.yienruuuuu.scheduler.bean.dto;

import java.math.BigDecimal;

public record ArenaTextOverallItemData(
        int rank,
        Integer rankSpreadMin,
        Integer rankSpreadMax,
        String modelName,
        String providerName,
        String licenseType,
        int score,
        Integer scoreCi,
        Integer modelVotes,
        BigDecimal inputPricePerM,
        BigDecimal outputPricePerM,
        String contextLengthText,
        boolean preliminary,
        String modelUrl
) {
}
