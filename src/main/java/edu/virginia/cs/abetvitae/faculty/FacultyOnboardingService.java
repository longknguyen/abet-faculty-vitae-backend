
package edu.virginia.cs.abetvitae.faculty;

import edu.virginia.cs.abetvitae.account.model.RoleName;
import edu.virginia.cs.abetvitae.account.model.UserAccount;
import edu.virginia.cs.abetvitae.account.repository.UserAccountRepository;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUser;
import edu.virginia.cs.abetvitae.faculty.dto.FacultyOnboardingRequest;
import edu.virginia.cs.abetvitae.faculty.dto.FacultyOnboardingResponse;
import edu.virginia.cs.abetvitae.faculty.model.Professor;
import edu.virginia.cs.abetvitae.faculty.repository.ProfessorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FacultyOnboardingService {

    private final UserAccountRepository userAccountRepository;
    private final ProfessorRepository professorRepository;

    public record Result(FacultyOnboardingResponse response, boolean created) {}

    @Transactional
    public Result onboard(AuthenticatedUser user, FacultyOnboardingRequest request) {

        if (user == null || !user.hasRole(RoleName.FACULTY)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "A faculty role is required to create a faculty profile");
        }

        UserAccount account = userAccountRepository
                .findWithLockById(user.accountId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "The authenticated account cannot be found"));

        if (!account.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The application account is disabled");
        }

        // Retry: already linked, change nothing
        Optional<Professor> existing =
                professorRepository.findByUserAccountId(account.getId());

        if (existing.isPresent()) {
            return new Result(
                    FacultyOnboardingResponse.from(existing.get()),
                    false);
        }

        // Claim a curated record with the same email
        List<Professor> matches =
                professorRepository.findAllByEmailIgnoreCase(account.getEmail());

        if (matches.size() > 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "More than one faculty record matches this account");
        }

        if (matches.size() == 1) {
            Professor matched = matches.getFirst();

            if (matched.getUserAccount() != null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "The matching faculty record is already linked to another account");
            }

            matched.setUserAccount(account);

            return new Result(
                    FacultyOnboardingResponse.from(
                            professorRepository.save(matched)),
                    false);
        }

        // No record exists: create from the names the user typed
        if (request == null
                || request.givenName() == null
                || request.givenName().isBlank()
                || request.familyName() == null
                || request.familyName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Given name and family name are required to create a faculty profile");
        }

        return new Result(
                FacultyOnboardingResponse.from(
                        professorRepository.save(Professor.createForAccount(
                                account,
                                request.givenName().strip(),
                                blankToNull(request.middleName()),
                                request.familyName().strip(),
                                blankToNull(request.suffix())))),
                true);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
