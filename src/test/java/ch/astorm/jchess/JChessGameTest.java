package ch.astorm.jchess;

import ch.astorm.jchess.JChessGame.Status;
import ch.astorm.jchess.core.Color;
import ch.astorm.jchess.core.Coordinate;
import ch.astorm.jchess.core.Move;
import ch.astorm.jchess.core.Moveable;
import ch.astorm.jchess.core.entities.*;
import ch.astorm.jchess.core.rules.RuleManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JChessGameTest {
    @Test
    public void testInternalObjectAccess() {
        JChessGame game = JChessGame.newGame();
        assertNotNull(game.getMetadata());
        assertNotNull(game.getMoveParser());
        assertNotNull(game.getPosition());
        assertNotNull(game.getRuleManager());
    }

    @Test
    public void testInitialPosition() {
        JChessGame game = JChessGame.newGame();
        assertNull(game.get("e5"));
        assertNotNull(game.get("a1"));

        assertEquals(20, game.getAvailableMoves().size());

        Coordinate kingLocation = game.getPosition().findLocation(King.class, Color.WHITE);
        Moveable king = game.getPosition().get(kingLocation);
        assertEquals(0, game.getAvailableMoves(king).size());

        assertNull(game.getAvailableMoves("e5"));
        assertTrue(game.getAvailableMoves("e1").isEmpty());

        game.play(game.getAvailableMoves("e2").get(0));
        game.resign(Color.WHITE);

        assertThrows(IllegalStateException.class, () -> game.play(null));
    }

    @Test
    public void testStartPositionInversed() {
        JChessGame game = JChessGame.newGame();
        assertEquals(Color.BLACK, game.switchColorOnMove());
        game.play("e5", "d4");

        Move captureMove = game.getMove("exd4");
        assertEquals("exd4", captureMove.toString());
    }

    @Test
    public void testDrawByInsufficiantMaterialKingKing() {
        JChessGame game = JChessGame.newEmptyGame(Color.WHITE);
        game.put("a1", new King(Color.WHITE));
        game.put("h8", new King(Color.BLACK));
        assertFalse(game.getStatus().isFinished());

        assertNull(game.back());
        assertTrue(game.back(2).isEmpty());
        assertTrue(game.back(-5).isEmpty());

        assertEquals(JChessGame.Status.DRAW, game.play("Ka2"));
        assertTrue(game.getStatus().isFinished());
    }

    @Test
    public void testDrawByInsufficiantMaterialKingBishop() {
        {
            JChessGame game = JChessGame.newEmptyGame(Color.WHITE);
            game.put("a1", new King(Color.WHITE));
            game.put("h8", new King(Color.BLACK));
            game.put("g7", new Bishop(Color.BLACK));
            assertEquals(JChessGame.Status.DRAW, game.play("Ka2"));
        }

        {
            JChessGame game = JChessGame.newEmptyGame(Color.WHITE);
            game.put("a1", new King(Color.BLACK));
            game.put("h8", new King(Color.WHITE));
            game.put("g7", new Bishop(Color.WHITE));
            assertEquals(JChessGame.Status.DRAW, game.play("Kg8"));
        }
    }

    @Test
    public void testDrawByInsufficiantMaterialKingKnight() {
        {
            JChessGame game = JChessGame.newEmptyGame(Color.WHITE);
            game.put("a1", new King(Color.WHITE));
            game.put("h8", new King(Color.BLACK));
            game.put("g7", new Knight(Color.BLACK));
            assertEquals(JChessGame.Status.DRAW, game.play("Ka2"));
        }

        {
            JChessGame game = JChessGame.newEmptyGame(Color.WHITE);
            game.put("a1", new King(Color.BLACK));
            game.put("h8", new King(Color.WHITE));
            game.put("g7", new Knight(Color.WHITE));
            assertEquals(JChessGame.Status.DRAW, game.play("Kg8"));
        }
    }

    @Test
    public void testNoDrawSufficiantMaterial() {
        {
            JChessGame game = JChessGame.newEmptyGame(Color.WHITE);
            game.put("a1", new King(Color.WHITE));
            game.put("h8", new King(Color.BLACK));
            game.put("g7", new Rook(Color.BLACK));
            assertEquals(JChessGame.Status.NOT_FINISHED, game.play("Ka2"));
        }

        {
            JChessGame game = JChessGame.newEmptyGame(Color.WHITE);
            game.put("a1", new King(Color.BLACK));
            game.put("h8", new King(Color.WHITE));
            game.put("g7", new Rook(Color.WHITE));
            assertEquals(JChessGame.Status.NOT_FINISHED, game.play("Kg8"));
        }
    }

    @Test
    public void testDrawByNoCapture() {
        JChessGame game = JChessGame.newEmptyGame(Color.WHITE);
        game.put("a1", new King(Color.WHITE));
        game.put("h8", new King(Color.BLACK));
        game.put("g7", new Rook(Color.BLACK));

        int incr = 0;
        while (game.getStatus() == Status.NOT_FINISHED) {
            List<Move> available = game.getAvailableMoves();

            Move move = available.get(incr % available.size());
            while (move.getCapturedEntity() != null) {
                ++incr;
                move = available.get(incr % available.size());
            }

            game.apply(move);
            ++incr;
        }

        assertEquals(Status.DRAW_NOCAPTURE, game.getStatus());
        assertEquals(RuleManager.FORCED_DRAW_MOVE_LIMIT * 2, game.getPosition().getMoveHistory().size());
    }

    @Test
    public void testDrawByRepetition() {
        JChessGame game = JChessGame.newEmptyGame(Color.WHITE);
        game.put("a1", new King(Color.WHITE));
        game.put("h8", new King(Color.BLACK));
        game.put("g7", new Rook(Color.BLACK));

        while (game.getStatus() == Status.NOT_FINISHED) {
            game.play("Ka2");
            game.play("Kg8");
            game.play("Ka1");
            game.play("Kh8");
        }

        assertEquals(Status.DRAW_REPETITION, game.getStatus());

        Move m1 = game.back();
        assertNotNull(m1);

        List<Move> m2 = game.back(5);
        assertEquals(5, m2.size());

        List<Move> m3 = game.back(999);
        assertEquals(14, m3.size());

        assertNotEquals(Status.DRAW_REPETITION, game.getStatus());

        m3.forEach(m -> game.apply(m));
        m2.forEach(m -> game.apply(m));
        game.apply(m1);

        assertEquals(Status.DRAW_REPETITION, game.getStatus());
    }

    @Test
    public void testDrawByStalemate() {
        JChessGame game = JChessGame.newEmptyGame(Color.WHITE);
        game.put("a1", new King(Color.WHITE));
        game.put("b1", new Queen(Color.WHITE));
        game.put("h8", new King(Color.BLACK));

        assertEquals(Status.DRAW_STALEMATE, game.play("Qg6"));
        assertThrows(IllegalStateException.class, () -> game.play("Qg5"));
    }
}
