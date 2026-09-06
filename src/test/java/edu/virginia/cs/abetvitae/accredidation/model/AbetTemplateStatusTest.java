package edu.virginia.cs.abetvitae.accredidation.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AbetTemplateStatusTest {

    @Test
    void valuesMatchPersistenceContract() {
        assertThat(AbetTemplateStatus.values()).containsExactly(
                AbetTemplateStatus.DRAFT,
                AbetTemplateStatus.PUBLISHED,
                AbetTemplateStatus.RETIRED
        );
    }
}
