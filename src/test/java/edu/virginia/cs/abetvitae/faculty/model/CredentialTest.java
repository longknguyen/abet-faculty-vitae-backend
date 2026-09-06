package edu.virginia.cs.abetvitae.faculty.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CredentialTest {

    @Test
    void credentialRetainsIssuanceAndExpirationDetails() {
        Professor professor = new Professor();
        LocalDate issueDate = LocalDate.of(2024, 1, 10);
        LocalDate expirationDate = LocalDate.of(2027, 1, 10);
        Credential credential = new Credential();
        credential.setProfessor(professor);
        credential.setCredentialType(CredentialType.CERTIFICATION);
        credential.setName("Professional Engineering Certification");
        credential.setIssuingOrganisation("Certification Board");
        credential.setCredentialNumber("CERT-1234");
        credential.setIssueDate(issueDate);
        credential.setExpirationDate(expirationDate);
        credential.setDisplayOrder(1);

        assertThat(credential.getProfessor()).isSameAs(professor);
        assertThat(credential.getCredentialType()).isEqualTo(CredentialType.CERTIFICATION);
        assertThat(credential.getName()).isEqualTo("Professional Engineering Certification");
        assertThat(credential.getIssuingOrganisation()).isEqualTo("Certification Board");
        assertThat(credential.getCredentialNumber()).isEqualTo("CERT-1234");
        assertThat(credential.getIssueDate()).isEqualTo(issueDate);
        assertThat(credential.getExpirationDate()).isEqualTo(expirationDate);
        assertThat(credential.getDisplayOrder()).isEqualTo(1);
    }
}
