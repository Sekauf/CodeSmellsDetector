package org.example.baseline;

import java.util.Collections;
import java.util.List;

public class BaselineAnalysisResult {
    private final List<CandidateDTO> candidates;
    private final AnalysisMetadata metadata;

    public BaselineAnalysisResult(List<CandidateDTO> candidates, AnalysisMetadata metadata) {
        this.candidates = Collections.unmodifiableList(candidates);
        this.metadata = metadata;
    }

    public List<CandidateDTO> getCandidates() {
        return candidates;
    }

    public AnalysisMetadata getMetadata() {
        return metadata;
    }
}
