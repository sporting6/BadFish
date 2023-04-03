package ch.astorm.jchess;

import ch.astorm.jchess.core.*;
import ch.astorm.jchess.core.entities.*;
import ch.astorm.jchess.core.rules.RuleManager;
import ch.astorm.jchess.io.MoveParser;
import ch.astorm.jchess.io.MoveParser.InvalidMoveException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main entry point to create a chess game.
 */
public class JChessGame {
    private RuleManager ruleManager;
    private Position position;
    private Status status;
    private Map<String, String> metadata;
    private MoveParser moveParser;

    /**
     * Final status of the game.
     */
    public static enum Status {
        /**
         * Game is not finished yet.
         */
        NOT_FINISHED(true),

        /**
         * White has won.
         */
        WIN_WHITE(false),

        /**
         * Black has won.
         */
        WIN_BLACK(false),

        /**
         * Draw (not enough material to mate).
         */
        DRAW(true),

        /**
         * Stalemate.
         */
        DRAW_STALEMATE(false),

        /**
         * Repetition of the same position.
         */
        DRAW_REPETITION(false),

        /**
         * No capture nor any pawn move in the last 75 moves.
         */
        DRAW_NOCAPTURE(false);

        private boolean allowPlay;

        private Status(boolean play) {
            this.allowPlay = play;
        }

        /**
         * Returns true if the game is allowed to play.
         */
        public boolean isPlayAllowed() {
            return allowPlay;
        }

        /**
         * Returns true if the game is finished.
         */
        public boolean isFinished() {
            return this != NOT_FINISHED;
        }
    }

    /**
     * Creates a new chess game with the specified initial position.
     *
     * @param ruleManager  The rule manager.
     * @param initPosition The initial position.
     * @param status       The status of the game.
     */
    public JChessGame(RuleManager ruleManager, Position initPosition, Status status) {
        this.ruleManager = ruleManager;
        this.position = initPosition;
        this.status = status;
        this.metadata = new HashMap<>();
        this.moveParser = new MoveParser(this);
    }


    /**
     * Creates a new {@code JChessGame} with an uninitialized {@link Position} (empty
     * board).
     */
    public static JChessGame newEmptyGame(Color colorToPlay) {
        Board board = new Board();
        RuleManager chessRules = new RuleManager();
        Position position = new Position(board, chessRules, colorToPlay);
        return new JChessGame(chessRules, position, Status.NOT_FINISHED);
    }

    /**
     * Creates a new {@code JChessGame} with the standard initial position and {@link Color#WHITE}
     * on the move.
     */
    public static JChessGame newGame() {
        JChessGame game = newEmptyGame(Color.WHITE);
        setInitialPosition(game.getPosition());
        return game;
    }

    private static void setInitialPosition(Position position) {
        Board board = position.getBoard();

        position.put(0, 0, new Rook(Color.WHITE));
        position.put(0, 1, new Knight(Color.WHITE));
        position.put(0, 2, new Bishop(Color.WHITE));
        position.put(0, 3, new Queen(Color.WHITE));
        position.put(0, 4, new King(Color.WHITE));
        position.put(0, 5, new Bishop(Color.WHITE));
        position.put(0, 6, new Knight(Color.WHITE));
        position.put(0, 7, new Rook(Color.WHITE));

        for (int i = 0; i < board.getColumnsCount(); ++i) {
            position.put(1, i, new Pawn(Color.WHITE));
        }

        int topRow = board.getRowsCount() - 1;
        position.put(topRow, 0, new Rook(Color.BLACK));
        position.put(topRow, 1, new Knight(Color.BLACK));
        position.put(topRow, 2, new Bishop(Color.BLACK));
        position.put(topRow, 3, new Queen(Color.BLACK));
        position.put(topRow, 4, new King(Color.BLACK));
        position.put(topRow, 5, new Bishop(Color.BLACK));
        position.put(topRow, 6, new Knight(Color.BLACK));
        position.put(topRow, 7, new Rook(Color.BLACK));

        for (int i = 0; i < board.getColumnsCount(); ++i) {
            position.put(topRow - 1, i, new Pawn(Color.BLACK));
        }
    }

    /**
     * Returns the metadata of the game.
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Returns the underlying {@link MoveParser}.
     */
    public MoveParser getMoveParser() {
        return moveParser;
    }

    /**
     * Returns the rule manager.
     */
    public RuleManager getRuleManager() {
        return ruleManager;
    }

    /**
     * Returns the current position.
     */
    public Position getPosition() {
        return position;
    }

    /**
     * Returns the current game status.
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Returns the available legal moves in the position.
     */
    public List<Move> getAvailableMoves() {
        return position.getLegalMoves();
    }

    /**
     * Returns the available legal moves for the given {@code moveable}.
     */
    public List<Move> getAvailableMoves(Moveable moveable) {
        return getAvailableMoves().stream().
                filter(m -> m.getDisplacement().getMoveable() == moveable).
                collect(Collectors.toList());
    }

    /**
     * Returns the available legal moves for the entity at the given {@code algebraicCoordinate}.
     * If there is no entity at this location, null will be returned.
     *
     * @param algebraicCoordinate The location (eg 'e5').
     * @return The list of legal moves or null if there is no entity at this location.
     */
    public List<Move> getAvailableMoves(String algebraicCoordinate) {
        Moveable movable = position.get(algebraicCoordinate);
        if (movable == null) {
            return null;
        }
        return getAvailableMoves(movable);
    }

    /**
     * Returns the {@link Move} corresponding to the specified {@code algebraicNotation}.
     *
     * @param algebraicNotation
     * @return The {@link Move}.
     * @throws InvalidMoveException If the {@code algebraicNotation} does not map to a legal move.
     */
    public Move getMove(String algebraicNotation) {
        return moveParser.getMove(algebraicNotation);
    }

    /**
     * Executes the specified {@code move}.
     *
     * @param move The move.
     * @return The game status after the move.
     */
    public Status play(Move move) {
        if (!status.isPlayAllowed()) {
            throw new IllegalStateException("Game is " + status);
        }
        return apply(move);
    }

    /**
     * Do the move specified by the {@code algebraicNotation} for the current color
     * on move.
     *
     * @param algebraicNotation The (first) move (eg 'Nxb5').
     * @param otherMoves        The other move to play (by alternating colors) in algebraic notation.
     * @return The game status after the move.
     * @throws InvalidMoveException If one of the move does not map to a legal move.
     */
    public Status play(String algebraicNotation, String... otherMoves) {
        if (!status.isPlayAllowed()) {
            throw new IllegalStateException("Game is " + status);
        }

        apply(getMove(algebraicNotation));
        for (String otherMove : otherMoves) {
            Move om = moveParser.getMove(otherMove);
            apply(getMove(otherMove));
        }

        return status;
    }

    /**
     * Applies the given {@code move} to the position and updates the game status.
     *
     * @param move The move to apply.
     * @return The new game status.
     */
    protected Status apply(Move move) {
        position = position.apply(move);
        status = ruleManager.getEndgameStatus(position);
        return status;
    }

    /**
     * Returns back to the previous position and resets the game status accordingly.
     * If there is no previous position available, this method returns false.
     *
     * @return The dropped move or null.
     */
    public Move back() {
        List<Move> dropped = back(1);
        return dropped.size() == 1 ? dropped.get(0) : null;
    }

    /**
     * Returns back to the {@code nbMoves}-th position. If there is not enough move,
     * the function just stops when there is no move to rollback anymore.
     * <p>The returned list is ordered. The first move of the list will be the one to
     * play in order to get back to the position before this method was called.</p>
     *
     * @param nbMoves The number of moves to rollback.
     * @return The dropped moves or an empty list.
     */
    public List<Move> back(int nbMoves) {
        if (nbMoves <= 0) {
            return Collections.EMPTY_LIST;
        }

        int counter = 0;

        List<Move> dropped = new ArrayList<>(Math.min(nbMoves, position.getMoveHistory().size()));
        while (counter < nbMoves) {
            Position previous = position.getPreviousPosition();
            if (previous == null) {
                return dropped;
            }

            Move last = position.getLastMove();
            position = previous;
            status = ruleManager.getEndgameStatus(position);
            ++counter;

            dropped.add(0, last);
        }

        return dropped;
    }

    /**
     * Returns the {@link Color} that has the move.
     */
    public Color getColorOnMove() {
        return position.getColorOnMove();
    }

    /**
     * Switches the {@link Color} that has the move and returns the new one having
     * the move.
     */
    public Color switchColorOnMove() {
        return position.switchColorOnMove();
    }

    /**
     * Mark the game as a draw.
     */
    public void draw() {
        status = Status.DRAW;
    }

    /**
     * Makes the specified {@code resignedColor} to resign. The opposite color
     * will be marked as the winner.
     */
    public void resign(Color resignedColor) {
        status = resignedColor == Color.WHITE ? Status.WIN_BLACK : Status.WIN_WHITE;
    }

    /**
     * Returns the {@link Moveable} entity at the specified {@code algebraicCoordinate}.
     *
     * @param algebraicCoordinate The coordinate (eg 'e5').
     * @return The {@link Moveable} entity or null if there is none.
     */
    public Moveable get(String algebraicCoordinate) {
        return position.get(algebraicCoordinate);
    }

    /**
     * Puts the given {@code moveable} entity at the specified {@code algebraicCoordinate}.
     * If there was already an entity at this location, the latter is returned.
     *
     * @param algebraicCoordinate The coordinate (eg 'e5').
     * @param moveable            The entity.
     * @return The previous entity or null if there was none.
     */
    public Moveable put(String algebraicCoordinate, Moveable moveable) {
        return position.put(algebraicCoordinate, moveable);
    }
}
