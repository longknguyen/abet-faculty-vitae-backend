package edu.virginia.cs.abetvitae.account.dto;

/** Minimal authenticated-session information exposed to the client. */
public record AuthenticatedSessionResponse(boolean authenticated, String subject) {
}
