package offercheck.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OfferAnalysis {
    private double totalComp;
    private double marketMedian;
    private double marketP25;
    private double marketP75;
    private String verdict;
    private String verdictEmoji;
    private String verdictMessage;
    private int percentile;
    private String negotiationScript;
    private String breakdown;
}
