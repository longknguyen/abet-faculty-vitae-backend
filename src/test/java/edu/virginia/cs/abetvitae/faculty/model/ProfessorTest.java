package edu.virginia.cs.abetvitae.faculty.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessorTest {

    @Test
    void displayNameUsesExplicitOverride() {
        Professor professor = new Professor();
        professor.setGivenName("Augusta");
        professor.setFamilyName("King");
        professor.setDisplayNameOverride("Ada Lovelace");

        assertThat(professor.getDisplayName()).isEqualTo("Ada Lovelace");
    }

    @Test
    void displayNameCombinesOnlyNonBlankNameParts() {
        Professor professor = new Professor();
        professor.setGivenName("Paul");
        professor.setMiddleName("   ");
        professor.setFamilyName("Reynolds");
        professor.setSuffix("Jr.");

        assertThat(professor.getDisplayName()).isEqualTo("Paul Reynolds Jr.");
    }

    @Test
    void blankOverrideFallsBackToNameParts() {
        Professor professor = new Professor();
        professor.setGivenName("Nada");
        professor.setFamilyName("Basit");
        professor.setDisplayNameOverride(" ");

        assertThat(professor.getDisplayName()).isEqualTo("Nada Basit");
    }

    @Test
    void displayNameIsEmptyWhenEveryNamePartIsAbsent() {
        Professor professor = new Professor();

        assertThat(professor.getDisplayName()).isEmpty();
    }
}
