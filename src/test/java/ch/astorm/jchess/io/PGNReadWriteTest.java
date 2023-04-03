package ch.astorm.jchess.io;

import ch.astorm.jchess.JChessGame;
import ch.astorm.jchess.JChessGame.Status;
import ch.astorm.jchess.io.PGNReader.PGNReaderException;
import ch.astorm.jchess.util.ASCIIPositionRenderer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/*
A lot of PGN can be found here: https://www.pgnmentor.com/files.html
*/
public class PGNReadWriteTest {
    @Test
    public void testAnandPGN() throws IOException {
        testPGN("Anand.zip");
    }

    @Test
    public void testNakamuraPGN() throws IOException {
        testPGN("Nakamura.zip");
    }

    private void testPGN(String resource) throws IOException {
        String pgn;
        try (InputStream input = PGNReaderTest.class.getResourceAsStream(resource);
             ZipInputStream zis = new ZipInputStream(input)) {
            ZipEntry entry = zis.getNextEntry();
            pgn = IOUtils.toString(zis, StandardCharsets.UTF_8);
            zis.closeEntry();
        }

        int counter = 1;
        try (PGNReader parser = new PGNReader(new StringReader(pgn))) {
            System.out.print("Parsing game " + counter + "...");
            JChessGame game = parser.readGame();
            while (game != null) {
                System.out.println(" done. (" + game.getPosition().getMoveHistory().size() + " moves)");
                assertNotEquals(Status.NOT_FINISHED, game.getStatus());

                writeReadGame(game);

                ++counter;
                System.out.print("Parsing game " + counter + "...");
                game = parser.readGame();
            }
            System.out.println(" complete.");
        } catch (PGNReaderException pe) {
            System.err.println();
            System.err.println("Failure of game " + counter + " for " + pe.getFailedMove() + " in " + resource);
            System.err.println("------------------------------------------");
            System.err.println("Game metadata:");
            pe.getGame().getMetadata().forEach((k, v) -> System.out.println("- " + k + ": " + v));
            System.err.println("------------------------------------------");
            System.err.println("Position before the move:\n");
            ASCIIPositionRenderer.render(System.err, pe.getGame().getPosition());
            System.err.println("==========================================");
            fail("Unable to parse PGN game", pe);
        }
    }

    private void writeReadGame(JChessGame parsed) throws IOException {
        StringWriter sw = new StringWriter();
        try (PGNWriter writer = new PGNWriter(sw)) {
            writer.writeGame(parsed);
        }

        JChessGame parsed2;
        try (PGNReader reader = new PGNReader(new StringReader(sw.toString()))) {
            parsed2 = reader.readGame();
        }

        assertTrue(parsed.getPosition().equals(parsed2.getPosition()), "Resulting positions are not the same");
        assertEquals(parsed.getStatus(), parsed2.getStatus());
        assertTrue(parsed.getMetadata().equals(parsed2.getMetadata()));
    }
}
