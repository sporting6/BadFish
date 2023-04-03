package com.dragonboi;

import ch.astorm.jchess.JChessGame;
import ch.astorm.jchess.core.*;
import ch.astorm.jchess.core.entities.*;
import ch.astorm.jchess.core.rules.RuleManager;
import ch.astorm.jchess.io.MoveParser;
import ch.astorm.jchess.util.ASCIIPositionRenderer;

import java.util.*;

public class Main {
    public static final int DEPTH = 3;
    public static final Random random = new Random();


    public static void main(String[] args) {

        JChessGame game = JChessGame.newGame();


//        ASCIIPositionRenderer.render(System.out, game.getPosition());


        while (game.getStatus().isPlayAllowed()) {


            Position pos = game.getPosition();



            game.play(bestMove(game.getPosition(), Color.WHITE));

            System.gc();


            game.play(game.getPosition().getLegalMoves().get(0));



            System.out.println(Position.getCount());
            System.out.println(Position.getGcs());
        }

    }

    public static void getMove(JChessGame game) {

        Scanner sc = new Scanner(System.in);
        String token = sc.next();

        try {
            game.play(token);
        } catch (java.lang.IllegalArgumentException e) {
            System.out.println("Illegal Move, try again.");
            getMove(game);
        } catch (MoveParser.InvalidMoveException e) {
            System.out.println("Illegal Move, try again.");
            getMove(game);

        } catch (java.lang.StringIndexOutOfBoundsException e) {
            System.out.println("Illegal Move, try again.");
            getMove(game);
        } catch (java.lang.NullPointerException e) {
            System.out.println("Illegal Move, try again.");
            getMove(game);
        } finally {
            return;
        }
    }


    public static double aveMaterial(Position pos, int curDepth) {
        if (curDepth >= DEPTH) {
            return evalBoard(pos);
        }

        List<Move> moves = pos.getLegalMoves();
        double[] evals = new double[moves.size()];


        if (curDepth < DEPTH) {
            for (int i = 0; i < moves.size(); i++) {
                evals[i] = aveMaterial(pos.apply(moves.get(i)), curDepth + 1);
            }
        }
        if (evals.length == 0) {
            return 0;
        }



        return Arrays.stream(evals).sum() / evals.length;
    }

    public static Move bestMove(Position pos, Color color) {
        double bestAdvantage = 0;

        List<Move> moves = pos.getLegalMoves();
        double[] evals = new double[moves.size()];

        Iterator<Move> iter = moves.iterator();

        if (color == Color.WHITE) {
            int i = 0;
            while (iter.hasNext()) {
                evals[i] = aveMaterial(pos.apply(iter.next()), 1);
                if (evals[i] > bestAdvantage) {
                    bestAdvantage = evals[i];
                }
                i++;
            }
        }
        if (color == Color.BLACK) {
            int i = 0;
            while (iter.hasNext()) {
                evals[i] = aveMaterial(pos.apply(iter.next()), 1);
                if (evals[i] < bestAdvantage) {
                    bestAdvantage = evals[i];
                }
                i++;
            }
        }


        List<Move> bestMoves = new ArrayList<Move>();

        for (int j = 0; j < moves.size(); j++) {
            if (evals[j] == bestAdvantage) {
                bestMoves.add(moves.get(j));
            }
        }
        if (bestMoves.size() == 0) {
            return moves.get(random.nextInt(moves.size()));
        }
        Move finalMove = bestMoves.get(random.nextInt(bestMoves.size()));

        if(finalMove.getDisplacement().getMoveable() instanceof Pawn){
            if((color == Color.WHITE ? finalMove.getDisplacement().getOldLocation().getRow() == 6 : finalMove.getDisplacement().getOldLocation().getRow() == 1)){
                finalMove.setPromotion(new Queen(color));

            }
        }

        System.gc();

        return finalMove;
    }

    public static double evalBoard(Position position) {
        double toReturn = 0;

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                if (position.get(x, y) != null) {
                    toReturn = toReturn + pieceValue(position.get(x, y), new Coordinate(x, y), position);
                }
            }
        }

        return toReturn;
    }


    private static double pieceValue(Moveable moveable, Coordinate coordinate, Position position) {
        Color color = moveable.getColor();


        switch (moveable.getClass().getSimpleName()) {
            case "King":
                if(position.getRuleManager().getEndgameStatus(position) == (color == Color.WHITE ? JChessGame.Status.WIN_WHITE : JChessGame.Status.WIN_BLACK)){
                    return color == Color.WHITE ? 500 : -500;
                }
                return color == Color.WHITE ? 300 : -300;
            case "Queen":
                return color == Color.WHITE ? 9 : -9;
            case "Rook":
                return color == Color.WHITE ? 5 : -5;
            case "Bishop":
            case "Knight":
                return color == Color.WHITE ? 3 : -3;
            case "Pawn":
                double toReturn = 1;
                boolean doubled = doubled(position,coordinate);
                boolean isolated = isolated(position,coordinate);


                if(doubled || isolated){
                    toReturn = .5;
                }
                if(doubled && isolated){
                    toReturn = .3;
                }

                return color == Color.WHITE ? -toReturn : -toReturn;
            default:
                return 0;
        }
    }

    public static boolean isolated(Position position, Coordinate coordinate){
        for(int i = 0; i < 8; i ++){
            boolean left = false;
            boolean right = false;

            if(coordinate.getColumn() !=0)
                left = position.get(new Coordinate(coordinate.getColumn() -1, i)) instanceof Pawn;
            if(coordinate.getColumn() !=7)
                right = position.get(new Coordinate(coordinate.getColumn() +1, i)) instanceof Pawn;
            if(left || right){
                return false;
            }
        }
        return true;
    }
    public static boolean doubled(Position position, Coordinate coordinate){
        for(int i = 0; i < 8; i ++){
            boolean check = position.get(new Coordinate(coordinate.getColumn(), i)) instanceof Pawn && new Coordinate(coordinate.getColumn(), i) != coordinate;
            if(check){
                return true;
            }
        }
        return false;
    }



}