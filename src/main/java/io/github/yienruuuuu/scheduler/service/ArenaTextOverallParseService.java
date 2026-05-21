package io.github.yienruuuuu.scheduler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yienruuuuu.scheduler.bean.dto.ArenaTextOverallItemData;
import io.github.yienruuuuu.scheduler.bean.dto.ArenaTextOverallSnapshotData;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses text-overall metadata and ranking rows from arena leaderboard HTML.
 */
@Slf4j
@Service
public class ArenaTextOverallParseService {

    private static final String LEADERBOARD_KEY = "text_overall";
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b([A-Z][a-z]+ \\d{1,2}, \\d{4})\\b");
    private static final Pattern VOTES_PATTERN = Pattern.compile("\\b([\\d,]+)\\s+votes\\b");
    private static final Pattern MODELS_PATTERN = Pattern.compile("\\b([\\d,]+)\\s+models\\b");
    private static final Pattern EMBEDDED_JSON_PATTERN = Pattern.compile("(?s)(\\{.*\\}|\\[.*\\])");

    private final ObjectMapper objectMapper;

    public ArenaTextOverallParseService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ArenaTextOverallSnapshotData parse(String sourceUrl, String html) {
        Document doc = Jsoup.parse(html, sourceUrl);
        String pageText = doc.text();

        LocalDate updatedDate = extractUpdatedDate(pageText)
                .orElseThrow(() -> new IllegalStateException("Failed to parse updated date from arena leaderboard"));
        long totalVotes = extractLong(VOTES_PATTERN, pageText)
                .orElseThrow(() -> new IllegalStateException("Failed to parse total votes from arena leaderboard"));
        int declaredModels = extractLong(MODELS_PATTERN, pageText)
                .map(Long::intValue)
                .orElseThrow(() -> new IllegalStateException("Failed to parse declared model count from arena leaderboard"));

        List<ArenaTextOverallItemData> items = parseItemsFromScripts(doc).orElseGet(List::of);
        if (items.isEmpty()) {
            items = parseItemsFromTable(doc);
        }
        if (items.isEmpty()) {
            items = parseItemsFromText(doc.wholeText(), doc);
        }

        log.debug("Parsed arena leaderboard snapshot. sourceUrl={}, updatedDate={}, declaredModels={}, fetchedItems={}",
                sourceUrl, updatedDate, declaredModels, items.size());

        return new ArenaTextOverallSnapshotData(
                LEADERBOARD_KEY,
                sourceUrl,
                updatedDate,
                totalVotes,
                declaredModels,
                items
        );
    }

    private Optional<List<ArenaTextOverallItemData>> parseItemsFromScripts(Document doc) {
        Optional<List<ArenaTextOverallItemData>> fromNextData = parseNextDataScript(doc);
        if (fromNextData.isPresent()) {
            return fromNextData;
        }

        for (Element script : doc.select("script")) {
            String raw = script.data();
            if (raw == null || raw.isBlank()) {
                raw = script.html();
            }
            if (raw == null || raw.isBlank()) {
                continue;
            }
            Optional<List<ArenaTextOverallItemData>> fromScript = tryParseItemsFromScriptContent(raw);
            if (fromScript.isPresent()) {
                return fromScript;
            }
        }
        return Optional.empty();
    }

    private Optional<List<ArenaTextOverallItemData>> parseNextDataScript(Document doc) {
        Element nextDataScript = doc.selectFirst("script#__NEXT_DATA__");
        if (nextDataScript == null) {
            return Optional.empty();
        }
        return tryParseItemsFromScriptContent(nextDataScript.html());
    }

    private Optional<List<ArenaTextOverallItemData>> tryParseItemsFromScriptContent(String rawScript) {
        String candidate = rawScript == null ? null : rawScript.trim();
        if (candidate == null || candidate.isBlank()) {
            return Optional.empty();
        }

        List<String> jsonCandidates = new ArrayList<>();
        if (candidate.startsWith("{") || candidate.startsWith("[")) {
            jsonCandidates.add(candidate);
        } else {
            Matcher matcher = EMBEDDED_JSON_PATTERN.matcher(candidate);
            while (matcher.find()) {
                jsonCandidates.add(matcher.group(1));
            }
        }

        for (String jsonCandidate : jsonCandidates) {
            try {
                JsonNode root = objectMapper.readTree(jsonCandidate);
                List<ArenaTextOverallItemData> items = new ArrayList<>();
                collectItemNodes(root, items);
                if (!items.isEmpty()) {
                    items.sort((a, b) -> Integer.compare(a.rank(), b.rank()));
                    return Optional.of(items);
                }
            } catch (Exception ignored) {
                // Try next candidate.
            }
        }
        return Optional.empty();
    }

    private void collectItemNodes(JsonNode node, List<ArenaTextOverallItemData> items) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isObject() && maybeItemNode(node)) {
            toItem(node).ifPresent(items::add);
        }

        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                collectItemNodes(entry.getValue(), items);
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                collectItemNodes(child, items);
            }
        }
    }

    private boolean maybeItemNode(JsonNode node) {
        boolean hasRank = node.has("rank");
        boolean hasScore = node.has("score") || node.has("rating");
        boolean hasModel = node.has("model") || node.has("model_name") || node.has("name");
        return hasRank && hasScore && hasModel;
    }

    private Optional<ArenaTextOverallItemData> toItem(JsonNode node) {
        Integer rank = readInt(node, "rank");
        Integer score = readInt(node, "score");
        if (score == null) {
            score = readInt(node, "rating");
        }
        String modelName = readString(node, "model");
        if (modelName == null) {
            modelName = readString(node, "model_name");
        }
        if (modelName == null) {
            modelName = readString(node, "name");
        }
        if (rank == null || score == null || modelName == null || modelName.isBlank()) {
            return Optional.empty();
        }

        String provider = readString(node, "provider");
        if (provider == null) {
            provider = readString(node, "organization");
        }

        return Optional.of(new ArenaTextOverallItemData(
                rank,
                readInt(node, "rank_spread_min"),
                readInt(node, "rank_spread_max"),
                modelName,
                provider,
                readString(node, "license_type"),
                score,
                readInt(node, "score_ci"),
                readInt(node, "votes"),
                readDecimal(node, "input_price_per_m"),
                readDecimal(node, "output_price_per_m"),
                readString(node, "context_length"),
                node.path("is_preliminary").asBoolean(false),
                readString(node, "model_url")
        ));
    }

    private List<ArenaTextOverallItemData> parseItemsFromText(String wholeText, Document doc) {
        List<String> lines = wholeText.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();

        List<ArenaTextOverallItemData> items = new ArrayList<>();
        Map<String, String> modelUrls = doc.select("a[href]").stream()
                .collect(java.util.stream.Collectors.toMap(
                        Element::text,
                        element -> element.absUrl("href"),
                        (first, second) -> first
                ));

        for (int i = 0; i < lines.size(); i++) {
            if (!isInteger(lines.get(i))) {
                continue;
            }
            int rank = Integer.parseInt(lines.get(i));
            if (rank < 1 || rank > 1000) {
                continue;
            }
            if (i + 6 >= lines.size()) {
                break;
            }

            String spreadLine = lines.get(i + 1);
            Matcher spreadMatcher = Pattern.compile("(\\d+)\\D+(\\d+)").matcher(spreadLine);
            if (!spreadMatcher.find()) {
                continue;
            }

            String modelName = lines.get(i + 2);
            if (modelName.length() < 3 || isInteger(modelName)) {
                continue;
            }

            String providerLicense = lines.get(i + 3);
            String scoreLine = lines.get(i + 4);
            Matcher scoreMatcher = Pattern.compile("(\\d+)\\s*[±\\u00B1]\\s*(\\d+)").matcher(scoreLine);
            if (!scoreMatcher.find()) {
                continue;
            }

            boolean preliminary = "Preliminary".equalsIgnoreCase(lines.get(i + 5));
            String votesAndPrice = preliminary ? safeGet(lines, i + 6) : lines.get(i + 5);
            String context = preliminary ? safeGet(lines, i + 7) : safeGet(lines, i + 6);

            Integer votes = parseVotesPrefix(votesAndPrice);
            BigDecimal inputPrice = parsePrice(votesAndPrice, 1);
            BigDecimal outputPrice = parsePrice(votesAndPrice, 2);

            String provider = providerLicense;
            String license = null;
            int dotIndex = providerLicense.indexOf("·");
            if (dotIndex >= 0) {
                provider = providerLicense.substring(0, dotIndex).trim();
                license = providerLicense.substring(dotIndex + 1).trim();
            }

            items.add(new ArenaTextOverallItemData(
                    rank,
                    Integer.parseInt(spreadMatcher.group(1)),
                    Integer.parseInt(spreadMatcher.group(2)),
                    modelName,
                    provider,
                    license,
                    Integer.parseInt(scoreMatcher.group(1)),
                    Integer.parseInt(scoreMatcher.group(2)),
                    votes,
                    inputPrice,
                    outputPrice,
                    context,
                    preliminary,
                    modelUrls.get(modelName)
            ));
        }

        return items.stream()
                .distinct()
                .sorted((a, b) -> Integer.compare(a.rank(), b.rank()))
                .toList();
    }

    private List<ArenaTextOverallItemData> parseItemsFromTable(Document doc) {
        List<ArenaTextOverallItemData> items = new ArrayList<>();
        for (Element row : doc.select("tr")) {
            List<Element> cells = row.select("> td");
            if (cells.size() < 7) {
                continue;
            }

            Integer rank = parseInteger(cells.get(0).text());
            if (rank == null || rank < 1 || rank > 1000) {
                continue;
            }

            List<Integer> spreadValues = extractIntegers(cells.get(1).text());
            Integer spreadMin = spreadValues.size() >= 1 ? spreadValues.get(0) : null;
            Integer spreadMax = spreadValues.size() >= 2 ? spreadValues.get(1) : null;

            Element modelAnchor = cells.get(2).selectFirst("a[href]");
            String modelName = modelAnchor != null
                    ? firstNonBlank(modelAnchor.attr("title"), modelAnchor.text())
                    : null;
            if (modelName == null || modelName.isBlank()) {
                continue;
            }

            String providerAndLicense = cells.get(2).select("span").stream()
                    .map(Element::text)
                    .filter(text -> text.contains("·"))
                    .findFirst()
                    .orElse(null);

            String provider = providerAndLicense;
            String license = null;
            if (providerAndLicense != null) {
                int split = providerAndLicense.indexOf("·");
                provider = providerAndLicense.substring(0, split).trim();
                license = providerAndLicense.substring(split + 1).trim();
            }

            List<Integer> scoreValues = extractIntegers(cells.get(3).text());
            Integer score = scoreValues.size() >= 1 ? scoreValues.get(0) : null;
            Integer scoreCi = scoreValues.size() >= 2 ? scoreValues.get(1) : null;
            if (score == null) {
                continue;
            }

            Integer votes = parseInteger(cells.get(4).text());
            String priceText = cells.get(5).text();
            BigDecimal inputPrice = parsePrice(priceText, 1);
            BigDecimal outputPrice = parsePrice(priceText, 2);
            String contextLength = normalizeText(cells.get(6).text());

            items.add(new ArenaTextOverallItemData(
                    rank,
                    spreadMin,
                    spreadMax,
                    modelName,
                    provider,
                    license,
                    score,
                    scoreCi,
                    votes,
                    inputPrice,
                    outputPrice,
                    contextLength,
                    "Preliminary".equalsIgnoreCase(cells.get(2).text()),
                    modelAnchor == null ? null : modelAnchor.absUrl("href")
            ));
        }

        return items.stream()
                .distinct()
                .sorted((a, b) -> Integer.compare(a.rank(), b.rank()))
                .toList();
    }

    private Optional<LocalDate> extractUpdatedDate(String pageText) {
        Matcher matcher = DATE_PATTERN.matcher(pageText);
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(matcher.group(1), DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)));
        } catch (DateTimeParseException exception) {
            return Optional.empty();
        }
    }

    private Optional<Long> extractLong(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(matcher.group(1).replace(",", "")));
    }

    private boolean isInteger(String value) {
        return value != null && value.matches("\\d+");
    }

    private String safeGet(List<String> lines, int index) {
        return index >= 0 && index < lines.size() ? lines.get(index) : "N/A";
    }

    private Integer parseVotesPrefix(String text) {
        Matcher matcher = Pattern.compile("([\\d,]+)").matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1).replace(",", ""));
    }

    private Integer parseInteger(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.replace(",", "").trim();
        if (!normalized.matches("-?\\d+")) {
            return null;
        }
        return Integer.parseInt(normalized);
    }

    private List<Integer> extractIntegers(String text) {
        List<Integer> values = new ArrayList<>();
        Matcher matcher = Pattern.compile("-?\\d+").matcher(text == null ? "" : text.replace(",", ""));
        while (matcher.find()) {
            values.add(Integer.parseInt(matcher.group()));
        }
        return values;
    }

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }

    private BigDecimal parsePrice(String text, int position) {
        Matcher matcher = Pattern.compile("\\$([\\d.]+)|N/A").matcher(text);
        List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        if (matches.size() < position) {
            return null;
        }
        String raw = matches.get(position - 1);
        if ("N/A".equals(raw)) {
            return null;
        }
        return new BigDecimal(raw.replace("$", ""));
    }

    private Integer readInt(JsonNode node, String key) {
        JsonNode value = node.get(key);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isInt() || value.isLong()) {
            return value.asInt();
        }
        String text = value.asText().replace(",", "").trim();
        if (text.isEmpty() || !text.matches("-?\\d+")) {
            return null;
        }
        return Integer.parseInt(text);
    }

    private BigDecimal readDecimal(JsonNode node, String key) {
        String value = readString(node, key);
        if (value == null || value.isBlank() || "N/A".equalsIgnoreCase(value)) {
            return null;
        }
        String normalized = value.replace("$", "").trim();
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String readString(JsonNode node, String key) {
        JsonNode value = node.get(key);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text.trim();
    }
}
