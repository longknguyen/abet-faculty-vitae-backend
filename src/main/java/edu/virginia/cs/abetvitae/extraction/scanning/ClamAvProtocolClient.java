package edu.virginia.cs.abetvitae.extraction.scanning;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/** Implements the bounded ClamAV INSTREAM network protocol. */
final class ClamAvProtocolClient {

    private static final byte[] INSTREAM_COMMAND = "zINSTREAM\0"
            .getBytes(StandardCharsets.US_ASCII);
    private static final int STREAM_CHUNK_SIZE = 8_192;
    private static final int MAXIMUM_RESPONSE_BYTES = 4_096;

    private final String host;
    private final int port;
    private final int connectTimeoutMilliseconds;
    private final int readTimeoutMilliseconds;
    private final long maximumFileSizeBytes;

    ClamAvProtocolClient(
            String host,
            int port,
            int connectTimeoutMilliseconds,
            int readTimeoutMilliseconds,
            long maximumFileSizeBytes) {
        this.host = host;
        this.port = port;
        this.connectTimeoutMilliseconds = connectTimeoutMilliseconds;
        this.readTimeoutMilliseconds = readTimeoutMilliseconds;
        this.maximumFileSizeBytes = maximumFileSizeBytes;
    }

    String scan(Path stagedFile) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMilliseconds);
            socket.setSoTimeout(readTimeoutMilliseconds);
            sendFile(socket, stagedFile);
            return readResponse(socket);
        }
    }

    private void sendFile(Socket socket, Path stagedFile) throws IOException {
        try (InputStream input = Files.newInputStream(stagedFile, LinkOption.NOFOLLOW_LINKS)) {
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            output.write(INSTREAM_COMMAND);

            byte[] buffer = new byte[STREAM_CHUNK_SIZE];
            long sent = 0L;
            int read;
            while ((read = input.read(buffer)) != -1) {
                sent += read;
                if (sent > maximumFileSizeBytes) {
                    throw new ClamAvFileLimitExceededException();
                }
                output.writeInt(read);
                output.write(buffer, 0, read);
            }
            output.writeInt(0);
            output.flush();
            socket.shutdownOutput();
        }
    }

    private static String readResponse(Socket socket) throws IOException {
        InputStream input = socket.getInputStream();
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        for (int index = 0; index < MAXIMUM_RESPONSE_BYTES; index++) {
            int value = input.read();
            if (value == 0) {
                return response.toString(StandardCharsets.US_ASCII);
            }
            if (value == -1) {
                break;
            }
            response.write(value);
        }
        throw new IOException("ClamAV returned an invalid response");
    }
}
