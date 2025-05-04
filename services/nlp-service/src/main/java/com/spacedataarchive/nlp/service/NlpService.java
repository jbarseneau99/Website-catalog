package com.spacedataarchive.nlp.service;

import com.spacedataarchive.common.model.SpaceData;
import com.spacedataarchive.common.model.ValidationStatus;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class NlpService {
    private final StanfordCoreNLP pipeline;

    public NlpService() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
        this.pipeline = new StanfordCoreNLP(props);
    }

    public SpaceData analyzeContent(SpaceData spaceData) {
        if (spaceData.getDescription() != null) {
            CoreDocument document = new CoreDocument(spaceData.getDescription());
            pipeline.annotate(document);

            List<String> keywords = new ArrayList<>();
            for (CoreSentence sentence : document.sentences()) {
                keywords.addAll(extractKeywords(sentence));
            }

            spaceData.setKeywords(keywords);
            spaceData.setLastProcessedAt(LocalDateTime.now());
            spaceData.setStatus(ValidationStatus.SUCCESS);
        }
        return spaceData;
    }

    private List<String> extractKeywords(CoreSentence sentence) {
        Set<String> keywords = new HashSet<>();
        sentence.tokens().forEach(token -> {
            String ner = token.ner();
            if (ner != null && !ner.equals("O")) {
                keywords.add(token.word());
            }
        });
        return new ArrayList<>(keywords);
    }
} 