package ch.astorm.jchess.core;

import ch.astorm.jchess.JChessGame;
import ch.astorm.jchess.JChessGame.Status;
import ch.astorm.jchess.core.entities.King;
import ch.astorm.jchess.core.entities.Knight;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PositionTest {

    @Test
    public void testNoPieceOnBoard() {
        JChessGame game = JChessGame.newEmptyGame(Color.WHITE);
        List<Move> moves = game.getAvailableMoves();
        assertEquals(0, moves.size());
    }

    @Test
    public void testSingleMinorPieceOnBoard() {
        JChessGame game = JChessGame.newEmptyGame(Color.WHITE);
        Position position = game.getPosition();
        position.put("e5", new Knight(Color.WHITE));

        List<Move> moves = game.getAvailableMoves();
        assertEquals(8, moves.size());

        assertEquals(Status.NOT_FINISHED, game.play("Ng6"));
    }

    @Test
    public void testInvalidPositionMultipleKings() {
        JChessGame game = JChessGame.newEmptyGame(Color.WHITE);
        Position position = game.getPosition();
        position.put(0, 0, new King(Color.WHITE));
        position.put(5, 5, new King(Color.WHITE));
        position.put(7, 7, new King(Color.BLACK));

        assertThrows(IllegalStateException.class, () -> game.getAvailableMoves());
    }

    @Test
    public void testInvalidCoordinate() {
        JChessGame game = JChessGame.newEmptyGame(Color.WHITE);
        assertThrows(IllegalArgumentException.class, () -> game.getPosition().get(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> game.getPosition().get(0, -1));
        assertThrows(IllegalArgumentException.class, () -> game.getPosition().get(10, 0));
        assertThrows(IllegalArgumentException.class, () -> game.getPosition().get(0, 10));
    }

    @Test
    public void testLocationCoordinate() {
        JChessGame game = JChessGame.newEmptyGame(Color.WHITE);
        assertNull(game.getPosition().findLocation(King.class, Color.WHITE));
    }

    @Test
    public void testEquals() {
        JChessGame game1 = JChessGame.newEmptyGame(Color.WHITE);
        Position pos1 = game1.getPosition();

        assertFalse(pos1.equals(null));
        assertFalse(pos1.equals(new Object()));
        assertTrue(pos1.equals(pos1));

        JChessGame game2 = JChessGame.newEmptyGame(Color.WHITE);
        Position pos2 = game2.getPosition();

        assertTrue(pos1.equals(pos2));
        assertTrue(pos2.equals(pos1));
        assertEquals(pos1.hashCode(), pos2.hashCode());
    }
}
