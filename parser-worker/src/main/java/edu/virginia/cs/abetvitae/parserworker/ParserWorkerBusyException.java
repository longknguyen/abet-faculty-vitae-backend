package edu.virginia.cs.abetvitae.parserworker;

final class ParserWorkerBusyException extends RuntimeException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    ParserWorkerBusyException() {
        super("The parser worker is already processing a document");
    }
}
