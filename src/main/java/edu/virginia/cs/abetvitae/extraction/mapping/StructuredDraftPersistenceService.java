package edu.virginia.cs.abetvitae.extraction.mapping;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.virginia.cs.abetvitae.account.model.UserAccount;
import edu.virginia.cs.abetvitae.accreditation.model.FacultyCycleRecord;
import edu.virginia.cs.abetvitae.extraction.model.ExtractionRun;
import edu.virginia.cs.abetvitae.review.model.ExtractionReview;
import edu.virginia.cs.abetvitae.review.model.ExtractionReviewItem;
import edu.virginia.cs.abetvitae.review.model.ExtractionReviewStatus;
import edu.virginia.cs.abetvitae.review.repository.ExtractionReviewItemRepository;
import edu.virginia.cs.abetvitae.review.repository.ExtractionReviewRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Persists only validated, unconfirmed suggestions inside the processing transaction. */
@Service
@RequiredArgsConstructor
public class StructuredDraftPersistenceService {

    private final StructuredCvDraftValidator validator;
    private final ExtractionReviewRepository reviewRepository;
    private final ExtractionReviewItemRepository reviewItemRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void createDraft(
            ExtractionRun extractionRun,
            FacultyCycleRecord facultyCycleRecord,
            UserAccount reviewedByUser,
            StructuredCvDraft draft
    ) {
        validator.validate(draft);
        if (reviewRepository.existsByFacultyCycleRecordIdAndStatus(
                facultyCycleRecord.getId(),
                ExtractionReviewStatus.DRAFT
        )) {
            throw new CvMappingException(
                    "A draft review already exists for this faculty cycle"
            );
        }

        ExtractionReview review = reviewRepository.save(
                ExtractionReview.createDraft(
                        extractionRun,
                        facultyCycleRecord,
                        reviewedByUser
                )
        );
        List<ExtractionReviewItem> items = draft.sections().stream()
                .map(section -> ExtractionReviewItem.createSuggestion(
                        review,
                        section.sectionType(),
                        section.sourceText(),
                        suggestedValue(section),
                        section.displayOrder()
                ))
                .toList();
        reviewItemRepository.saveAll(items);
    }

    private static ObjectNode suggestedValue(MappedReviewSection section) {
        ObjectNode value = JsonNodeFactory.instance.objectNode();
        ArrayNode entries = value.putArray("entries");
        section.entries().forEach(entries::add);
        return value;
    }
}
