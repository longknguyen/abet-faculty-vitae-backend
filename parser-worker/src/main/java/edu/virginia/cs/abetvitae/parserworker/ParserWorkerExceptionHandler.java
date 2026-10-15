package edu.virginia.cs.abetvitae.parserworker;

import edu.virginia.cs.abetvitae.extraction.parsing.CvTextExtractionException;
import edu.virginia.cs.abetvitae.extraction.parsing.CvTextExtractionFailure;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Translates internal failures into the parser contract without exposing implementation details. */
@RestControllerAdvice(assignableTypes = ParserWorkerController.class)
final class ParserWorkerExceptionHandler {

    @ExceptionHandler(CvTextExtractionException.class)
    ResponseEntity<WorkerError> extractionFailure(CvTextExtractionException exception) {
        HttpStatus status = exception.getFailure() == CvTextExtractionFailure.FILE_TOO_LARGE
                ? HttpStatus.CONTENT_TOO_LARGE
                : HttpStatus.UNPROCESSABLE_CONTENT;
        return ResponseEntity.status(status).body(new WorkerError(
                exception.getFailure().name(), exception.getMessage()));
    }

    @ExceptionHandler(ParserWorkerBusyException.class)
    ResponseEntity<WorkerError> workerBusy(ParserWorkerBusyException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new WorkerError(
                CvTextExtractionFailure.WORKER_UNAVAILABLE.name(), exception.getMessage()));
    }

    @ExceptionHandler(InvalidWorkerRequestException.class)
    ResponseEntity<WorkerError> invalidRequest(InvalidWorkerRequestException exception) {
        return ResponseEntity.badRequest().body(new WorkerError(
                exception.failure().name(), exception.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<WorkerError> unexpectedFailure() {
        return ResponseEntity.internalServerError().body(new WorkerError(
                CvTextExtractionFailure.WORKER_UNAVAILABLE.name(),
                "The document could not be processed safely"));
    }

    record WorkerError(String failure, String message) {}
}
