package edu.virginia.cs.abetvitae.accredidation.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AbetTemplateTest {

    @Test
    void newTemplateHasSafeDraftDefaults() {
        AbetTemplate template = new AbetTemplate();

        assertThat(template.getStatus()).isEqualTo(AbetTemplateStatus.DRAFT);
        assertThat(template.getRequiredSections().isArray()).isTrue();
        assertThat(template.getRequiredSections()).isEmpty();
        assertThat(template.getFormattingRules().isObject()).isTrue();
        assertThat(template.getFormattingRules()).isEmpty();
    }
}
