package offercheck.controller;

import offercheck.dto.OfferAnalysis;
import offercheck.dto.OfferRequest;
import offercheck.service.OfferAnalysisService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class OfferController {

    private final OfferAnalysisService offerAnalysisService;

    public OfferController(OfferAnalysisService offerAnalysisService) {
        this.offerAnalysisService = offerAnalysisService;
    }

    @PostMapping("/analyze")
    public OfferAnalysis analyzeOffer(@RequestBody OfferRequest request) {
        return offerAnalysisService.analyzeOffer(request);
    }
}
