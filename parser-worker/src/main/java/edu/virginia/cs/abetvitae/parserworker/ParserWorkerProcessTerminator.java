package edu.virginia.cs.abetvitae.parserworker;

import org.springframework.stereotype.Component;

/** Terminates only the dedicated worker process when a parser cannot be stopped safely. */
@Component
class ParserWorkerProcessTerminator {

    static final int PARSING_TIMEOUT_EXIT_CODE = 124;

    void terminate() {
        Runtime.getRuntime().halt(PARSING_TIMEOUT_EXIT_CODE);
    }
}
