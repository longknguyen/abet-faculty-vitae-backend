package edu.virginia.cs.abetvitae.extraction.mapping;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.virginia.cs.abetvitae.account.model.UserAccount;
import edu.virginia.cs.abetvitae.accreditation.model.FacultyCycleRecord;
import edu.virginia.cs.abetvitae.extraction.model.ExtractionRun;
import edu.virginia.cs.abetvitae.review.model.ExtractionReview;
import edu.virginia.cs.abetvitae.review.model.ExtractionReviewStatus;
import edu.virginia.cs.abetvitae.review.model.ReviewSectionType;
import edu.virginia.cs.abetvitae.review.repository.ExtractionReviewItemRepository;
import edu.virginia.cs.abetvitae.review.repository.ExtractionReviewRepository;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StructuredDraftPersistenceServiceTest {

    @Test
    void refusesToCreateASecondActiveDraftForFacultyCycle() {
        ExtractionReviewRepository reviewRepository =
                mock(ExtractionReviewRepository.class);
        ExtractionReviewItemRepository itemRepository =
                mock(ExtractionReviewItemRepository.class);
        FacultyCycleRecord facultyCycleRecord = mock(FacultyCycleRecord.class);
        UUID facultyCycleRecordId = UUID.randomUUID();
        when(facultyCycleRecord.getId()).thenReturn(facultyCycleRecordId);
        when(reviewRepository.existsByFacultyCycleRecordIdAndStatus(
                facultyCycleRecordId,
                ExtractionReviewStatus.DRAFT
        )).thenReturn(true);
        StructuredDraftPersistenceService service =
                new StructuredDraftPersistenceService(
                        new StructuredCvDraftValidator(),
                        reviewRepository,
                        itemRepository
                );

        assertThatThrownBy(() -> service.createDraft(
                mock(ExtractionRun.class),
                facultyCycleRecord,
                mock(UserAccount.class),
                completeDraft()
        )).isInstanceOf(CvMappingException.class)
                .hasMessage(
                        "A draft review already exists for this faculty cycle"
                );

        verify(reviewRepository, never()).save(any());
        verify(itemRepository, never()).saveAll(any());
    }

    @Test
    void persistsValidatedSuggestionsAsUnselectedDraftItems() {
        ExtractionReviewRepository reviewRepository =
                mock(ExtractionReviewRepository.class);
        ExtractionReviewItemRepository itemRepository =
                mock(ExtractionReviewItemRepository.class);
        FacultyCycleRecord facultyCycleRecord = mock(FacultyCycleRecord.class);
        when(facultyCycleRecord.getId()).thenReturn(UUID.randomUUID());
        when(reviewRepository.save(any())).thenAnswer(invocation ->
                invocation.getArgument(0, ExtractionReview.class));
        StructuredDraftPersistenceService service =
                new StructuredDraftPersistenceService(
                        new StructuredCvDraftValidator(),
                        reviewRepository,
                        itemRepository
                );

        service.createDraft(
                mock(ExtractionRun.class),
                facultyCycleRecord,
                mock(UserAccount.class),
                completeDraft()
        );

        verify(itemRepository).saveAll(org.mockito.ArgumentMatchers.argThat(
                items -> ((List<?>) items).size()
                        == ReviewSectionType.values().length
        ));
    }

    private static StructuredCvDraft completeDraft() {
        return new StructuredCvDraft(
                Arrays.stream(ReviewSectionType.values())
                        .map(type -> new MappedReviewSection(
                                type,
                                "source",
                                List.of("entry"),
                                type.ordinal()
                        ))
                        .toList(),
                new MappingProvenance("fixture", "1", null, null)
        );
    }
}
