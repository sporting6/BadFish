package com.dragonboi;

import ch.astorm.jchess.JChessGame;
import ch.astorm.jchess.core.*;
import ch.astorm.jchess.core.entities.*;
import ch.astorm.jchess.io.MoveParser;
import ch.astorm.jchess.util.ASCIIPositionRenderer;

import java.util.*;
import java.util.concurrent.BlockingDeque;

public class Main {
    public static final int DEPTH = 4;
    public static final Random random = new Random();

//        game.play("d4", "Nf6", "c4", "g6", "Nc3", "Bg7","e4", "d6", "Nf3", "O-O", "Be2", "e5");

    public static void main(String[] args) {

        Scanner in = new Scanner(System.in);

        System.out.println("What Should I do?");
        System.out.println("1. Play Myself");
        System.out.println("2. Play You");

        int a = in.nextInt();

        if(a == 1) {
            playSelf();
        } else if(a == 2){
            System.out.println("What Color Do You Want To Play?");
            System.out.println("1. Black");
            System.out.println("2. White");
            int b = in.nextInt();
            if(b == 1){
                playOther(Color.BLACK);
            } else if(b == 2){
                playOther(Color.WHITE);
            }
            else {
                System.out.println("Invalid Setting, Try Again.");
                return;
            }
        } else {
            System.out.println("Invalid Setting, Try Again.");
            return;
        }
    }
    public static void playOther(Color color){
        JChessGame game = JChessGame.newGame();

        Scanner in = new Scanner(System.in);

        ASCIIPositionRenderer.render(System.out, game.getPosition());

        JChessGame.Status status = game.getStatus();

        if(color == Color.WHITE){
            while (status == JChessGame.Status.NOT_FINISHED) {

                getMove(game);

                ASCIIPositionRenderer.render(System.out, game.getPosition());

                play(game, Color.BLACK);
                System.gc();
            }
        }

        if(color == Color.BLACK){
            while (status == JChessGame.Status.NOT_FINISHED) {
                play(game, Color.WHITE);

                getMove(game);

                ASCIIPositionRenderer.render(System.out, game.getPosition());

                System.gc();
            }
        }
        else return;
    }




    public static void playSelf(){
        JChessGame game = JChessGame.newGame();


        ASCIIPositionRenderer.render(System.out, game.getPosition());

        JChessGame.Status status = game.getStatus();


        while (status == JChessGame.Status.NOT_FINISHED) {

            play(game, Color.WHITE);

            play(game, Color.BLACK);


            System.gc();
        }
    }

    public static JChessGame.Status play(JChessGame game, Color color){
        JChessGame.Status status;
        Position pos = game.getPosition();
        Move bestMove = bestMove(pos, color);

        try {
            status = game.play(bestMove);
        } catch (IllegalStateException e){
            bestMove.setPromotion(new Queen(color));
            status = game.play(bestMove);
            System.out.println("Piece Promoted");
        }

        ASCIIPositionRenderer.render(System.out, game.getPosition());

        System.out.println(evalBoard(pos));
        pos.clearCache();
        return status;
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
                Position nextPos = pos.apply(moves.get(i));
                evals[i] = aveMaterial(nextPos, curDepth + 1);
                nextPos.clearCache();
            }
        }
        if (evals.length == 0) {
            return 0;
        }
        moves = null;
        double maxAdvantage = 0;
        double minAdvantage = 0;


        for (double i: evals) {
            if (i > maxAdvantage) {
                maxAdvantage = i;
            }
            if (i < minAdvantage) {
                maxAdvantage = i;
            }
        }

        return curDepth % 2 == 0 ? maxAdvantage : minAdvantage;
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
        else {
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

                return color == Color.WHITE ? toReturn : -toReturn;
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