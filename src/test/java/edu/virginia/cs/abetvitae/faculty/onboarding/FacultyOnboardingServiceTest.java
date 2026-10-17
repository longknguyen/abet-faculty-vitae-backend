package edu.virginia.cs.abetvitae.faculty.onboarding;

import edu.virginia.cs.abetvitae.account.model.RoleName;
import edu.virginia.cs.abetvitae.account.model.UserAccount;
import edu.virginia.cs.abetvitae.account.repository.UserAccountRepository;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUser;
import edu.virginia.cs.abetvitae.faculty.FacultyOnboardingService;
import edu.virginia.cs.abetvitae.faculty.model.Professor;
import edu.virginia.cs.abetvitae.faculty.repository.ProfessorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacultyOnboardingServiceTest {

    private static final UUID ACCOUNT_ID =
            UUID.fromString("0199f80f-3bf3-7ed1-b5f8-060591413962");

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private ProfessorRepository professorRepository;

    private FacultyOnboardingService service;

    @BeforeEach
    void setUp() {
        service = new FacultyOnboardingService(
                userAccountRepository,
                professorRepository);
    }

    @Test
    void createsProfessorWhenNoMatchingRecordExists() {
        AuthenticatedUser user = facultyUser();
        UserAccount account = mock(UserAccount.class);

        when(userAccountRepository.findWithLockById(ACCOUNT_ID))
                .thenReturn(Optional.of(account));
        when(account.isEnabled()).thenReturn(true);
        when(account.getId()).thenReturn(ACCOUNT_ID);
        when(account.getEmail()).thenReturn("alex.morgan@virginia.edu");

        when(professorRepository.findByUserAccountId(ACCOUNT_ID))
                .thenReturn(Optional.empty());
        when(professorRepository.findAllByEmailIgnoreCase(
                "alex.morgan@virginia.edu"))
                .thenReturn(List.of());

        when(professorRepository.save(any(Professor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var request = new edu.virginia.cs.abetvitae.faculty.dto
                .FacultyOnboardingRequest(
                "Alex", null, "Morgan", null);

        FacultyOnboardingService.Result result =
                service.onboard(user, request);

        assertThat(result.created()).isTrue();
        assertThat(result.response()).isNotNull();

        verify(professorRepository).save(any(Professor.class));
    }

    @Test
    void returnsExistingLinkedProfessorWithoutCreatingAnother() {
        AuthenticatedUser user = facultyUser();
        UserAccount account = mock(UserAccount.class);
        Professor professor = mock(Professor.class);

        when(userAccountRepository.findWithLockById(ACCOUNT_ID))
                .thenReturn(Optional.of(account));
        when(account.isEnabled()).thenReturn(true);
        when(account.getId()).thenReturn(ACCOUNT_ID);

        when(professorRepository.findByUserAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(professor));

        FacultyOnboardingService.Result result =
                service.onboard(user, null);

        assertThat(result.created()).isFalse();
        assertThat(result.response()).isNotNull();

        verify(professorRepository, never()).save(any());
        verify(professorRepository, never())
                .findAllByEmailIgnoreCase(any());
    }

    @Test
    void rejectsUserWithoutFacultyRole() {
        AuthenticatedUser user = new AuthenticatedUser(
                ACCOUNT_ID,
                "Alex Morgan",
                "Morgan",
                Set.of());

        assertThatThrownBy(() -> service.onboard(user, null))
                .isInstanceOf(ResponseStatusException.class);

        verifyNoInteractions(userAccountRepository);
        verifyNoInteractions(professorRepository);
    }

    @Test
    void rejectsWhenMultipleProfessorRecordsMatchEmail() {
        AuthenticatedUser user = facultyUser();
        UserAccount account = mock(UserAccount.class);
        Professor first = mock(Professor.class);
        Professor second = mock(Professor.class);

        when(userAccountRepository.findWithLockById(ACCOUNT_ID))
                .thenReturn(Optional.of(account));
        when(account.isEnabled()).thenReturn(true);
        when(account.getId()).thenReturn(ACCOUNT_ID);
        when(account.getEmail()).thenReturn("alex.morgan@virginia.edu");

        when(professorRepository.findByUserAccountId(ACCOUNT_ID))
                .thenReturn(Optional.empty());
        when(professorRepository.findAllByEmailIgnoreCase(
                "alex.morgan@virginia.edu"))
                .thenReturn(List.of(first, second));

        assertThatThrownBy(() -> service.onboard(
                user,
                new edu.virginia.cs.abetvitae.faculty.dto
                        .FacultyOnboardingRequest(
                        "Alex", null, "Morgan", null)))
                .isInstanceOf(ResponseStatusException.class);

        verify(professorRepository, never()).save(any());
    }

    private static AuthenticatedUser facultyUser() {
        return new AuthenticatedUser(
                ACCOUNT_ID,
                "Alex Morgan",
                "Morgan",
                Set.of(RoleName.FACULTY));
    }
}
