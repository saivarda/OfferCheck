package offercheck.service;

import offercheck.dto.OfferAnalysis;
import offercheck.dto.OfferRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class OfferAnalysisService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OfferAnalysis analyzeOffer(OfferRequest request) {
        double totalComp = request.getBaseSalary() + request.getBonus() + (request.getEquity() / 4);

        double[] market = getMarketData(request);
        double p25 = market[0];
        double median = market[1];
        double p75 = market[2];

        String verdict;
        String verdictEmoji;
        String verdictMessage;
        int percentile;

        if (totalComp < p25) {
            verdict = "LOWBALLED";
            verdictEmoji = "🚨";
            verdictMessage = "You're being lowballed. You deserve better.";
            percentile = (int) ((totalComp / p25) * 25);
        } else if (totalComp > p75) {
            verdict = "ABOVE_MARKET";
            verdictEmoji = "🚀";
            verdictMessage = "Great offer! You're above market rate.";
            percentile = (int) (75 + ((totalComp - p75) / p75) * 25);
            percentile = Math.min(percentile, 99);
        } else {
            verdict = "FAIR";
            verdictEmoji = "✅";
            verdictMessage = "Fair offer. There's still room to negotiate.";
            percentile = (int) (25 + ((totalComp - p25) / (p75 - p25)) * 50);
        }

        String breakdown = String.format(
            "Base: $%.0f | Bonus: $%.0f | Equity (annual): $%.0f | Total: $%.0f",
            request.getBaseSalary(), request.getBonus(), request.getEquity() / 4, totalComp
        );

        String prompt = buildPrompt(request, verdict, totalComp, median, p25, p75);
        String negotiationScript = callGroq(prompt);

        return OfferAnalysis.builder()
            .totalComp(totalComp)
            .marketMedian(median)
            .marketP25(p25)
            .marketP75(p75)
            .verdict(verdict)
            .verdictEmoji(verdictEmoji)
            .verdictMessage(verdictMessage)
            .percentile(percentile)
            .negotiationScript(negotiationScript)
            .breakdown(breakdown)
            .build();
    }

    private double[] getMarketData(OfferRequest request) {
        double baseP25, baseMedian, baseP75;
        int yoe = request.getYearsOfExperience();

        if (yoe <= 2) {
            baseP25 = 80000; baseMedian = 95000; baseP75 = 115000;
        } else if (yoe <= 5) {
            baseP25 = 120000; baseMedian = 140000; baseP75 = 165000;
        } else if (yoe <= 9) {
            baseP25 = 160000; baseMedian = 185000; baseP75 = 220000;
        } else {
            baseP25 = 200000; baseMedian = 240000; baseP75 = 290000;
        }

        double multiplier = 1.0;
        String loc = request.getLocation().toLowerCase();
        if (loc.contains("san francisco") || loc.contains("seattle") || loc.contains("new york") || loc.contains("nyc")) {
            multiplier = 1.3;
        } else if (loc.contains("austin") || loc.contains("denver") || loc.contains("boston")) {
            multiplier = 1.1;
        } else if (loc.contains("remote")) {
            multiplier = 1.0;
        } else {
            multiplier = 0.95;
        }

        return new double[]{baseP25 * multiplier, baseMedian * multiplier, baseP75 * multiplier};
    }

    private String buildPrompt(OfferRequest request, String verdict, double totalComp, double median, double p25, double p75) {
        return String.format(
            "You are a tech salary negotiation expert. A %s with %d years of experience received a %s offer from %s in %s.\n\n" +
            "Offer details:\n- Base: $%.0f\n- Bonus: $%.0f\n- Equity: $%.0f\n- Total comp: $%.0f\n\n" +
            "Market data:\n- P25: $%.0f\n- Median: $%.0f\n- P75: $%.0f\n\n" +
            "Verdict: %s\n\n" +
            "Write a professional, confident negotiation email/script they can use to negotiate a better offer. " +
            "Be specific with numbers. Keep it under 200 words. Sound human, not robotic.",
            request.getLevel() + " " + request.getRole(), request.getYearsOfExperience(),
            verdict.toLowerCase(), request.getCompany(), request.getLocation(),
            request.getBaseSalary(), request.getBonus(), request.getEquity(), totalComp,
            p25, median, p75, verdict
        );
    }

    private String callGroq(String prompt) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", "llama-3.1-8b-instant");
            body.put("max_tokens", 1000);
            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode message = objectMapper.createObjectNode();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            body.set("messages", messages);

            String response = restClient.post()
                .uri("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer " + groqApiKey)
                .header("Content-Type", "application/json")
                .body(body.toString())
                .retrieve()
                .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            return "Unable to generate negotiation script. Please try again.";
        }
    }
}
