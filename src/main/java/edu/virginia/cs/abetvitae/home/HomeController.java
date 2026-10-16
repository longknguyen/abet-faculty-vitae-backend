package edu.virginia.cs.abetvitae.home;

import edu.virginia.cs.abetvitae.account.security.AuthenticatedUser;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUserService;
import edu.virginia.cs.abetvitae.home.dto.HomeResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class HomeController {

    private final AuthenticatedUserService authenticatedUserService;
    private final HomeService homeService;

    @GetMapping
    public HomeResponse home(Authentication authentication) {
        AuthenticatedUser user = authenticatedUserService.requireUser(authentication);
        return homeService.loadHome(user);
    }
}
