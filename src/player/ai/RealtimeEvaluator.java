package player.ai;

import game.BoardHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.lang.Math.*;

import static game.BoardHelper.getAllPossibleMoves;

public class RealtimeEvaluator implements Evaluator {

    //public static final int WEIGHT_SET_SIZE = 6;
    int[][] weightSetForDiscCount;
    public static ArrayList<ArrayList<Integer>> moveCounts = new ArrayList<ArrayList<Integer>>(2);
   
 
    public int eval(int[][] board , int player){
        int score = 0;

        int[] weights = weightSetForDiscCount[BoardHelper.getTotalStoneCount(board)];

        if(weights[0] != 0) {
            score += weights[0] * mobility(board,player);
        }
        if(weights[1] != 0) {
            score += weights[1] * frontier(board,player);
        }
        if(weights[2] != 0) {
            score += weights[2] * pieces(board,player);
        }
        if(weights[3] != 0) {
            score += weights[3] * placement(board,player);
        }
        if(weights[4] != 0) {
            score += weights[4] * stability(board,player);
        }
        if(weights[5] != 0) {
            score += weights[5] * cornerGrab(board,player);
        }

        //return score;
        //return Math.min(cornerGrab(board,player),placement(board,player));
        return placement(board,player) + mobility(board,player) + frontier(board,player) + cornerGrab(board.player);
        //return  mobility(board,player) + frontier(board,player);
   }


    public RealtimeEvaluator(int[][] weightSet , int[] timingSet){

   
        weightSetForDiscCount = new int[65][weightSet[0].length];
        
        moveCounts.add(new ArrayList<Integer>());
        moveCounts.add(new ArrayList<Integer>());
 

        //dc : Disk Count
        for(int dc = 0; dc <= 64; dc++) {
            // determine which set of weights to use
            int w = 0;
            for(int i = 0; i < timingSet.length; i++) {
                if(dc <= timingSet[i]) {
                    w = i;
                    break;
                }
            }

            // first set of weights: just return them
            if(w == 0) {
                weightSetForDiscCount[dc] = weightSet[0];
                continue;
            }

            // linearly interpolate between the set of weights given for the
            // current number of moves and the previous set of weights
            double factor = ((double)dc - timingSet[w - 1]) / (timingSet[w] - timingSet[w - 1]);
            for(int i = 0; i < weightSet[w].length; i++) {
                weightSetForDiscCount[dc][i] = (int)Math.rint(factor * weightSet[w][i] + (1 - factor) * weightSet[w - 1][i]);
            }
        }
    
    }

    public static int pieces(int[][] board , int player){
        int oplayer = (player==1) ? 2 : 1;

        int mySC = BoardHelper.getPlayerStoneCount(board,player);
        int opSC = BoardHelper.getPlayerStoneCount(board,oplayer);

        return 100 * (mySC - opSC) / (mySC + opSC + 1);
    }

    static int[][] SQUARE_SCORE = {
            {100 , -10 , 8  ,  6 ,  6 , 8  , -10 ,  100},
            {-10 , -25 ,  -4, -4 , -4 , -4 , -25 , -10 },
            {8   ,  -4 ,   6,   4,   4,   6,  -4 ,  8  },
            {6   ,  -4 ,   4,   0,   0,   4,  -4 ,  6  },
            {6   ,  -4 ,   4,   0,   0,   4,  -4 ,  6  },
            {8   ,  -4 ,   6,   4,   4,   6,  -4 ,  8  },
            {-10 , -25 ,  -4, -4 , -4 , -4 , -25 , -10 },
            {100 , -10 , 8  ,  6 ,  6 , 8  , -10 ,  100}};

    public static int placement(int[][] board , int player){
        int oplayer = (player==1) ? 2 : 1;

        int myW = 0;
        int opW = 0;

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if(board[i][j]==player) myW += SQUARE_SCORE[i][j];
                if(board[i][j]==oplayer) opW += SQUARE_SCORE[i][j];
            }
        }

        return myW - opW;
    }

    public static int stability(int[][] board , int player){
        int oplayer = (player==1) ? 2 : 1;

        int myS = 0;
        int opS = 0;

        if(board[0][0] == player) myS += BoardHelper.getStableDisks(board,player,0,0).size();
        if(board[0][7] == player) myS += BoardHelper.getStableDisks(board,player,0,7).size();
        if(board[7][0] == player) myS += BoardHelper.getStableDisks(board,player,7,0).size();
        if(board[7][7] == player) myS += BoardHelper.getStableDisks(board,player,7,7).size();

        if(board[0][0] == oplayer) opS += BoardHelper.getStableDisks(board,oplayer,0,0).size();
        if(board[0][7] == oplayer) opS += BoardHelper.getStableDisks(board,oplayer,0,7).size();
        if(board[7][0] == oplayer) opS += BoardHelper.getStableDisks(board,oplayer,7,0).size();
        if(board[7][7] == oplayer) opS += BoardHelper.getStableDisks(board,oplayer,7,7).size();

        return 100 * (myS - opS) / (myS + opS + 1);
    }

    public static void writeMobility(int[][] board){

        ArrayList<Integer> listMyPlayer= new ArrayList<Integer>();
        ArrayList<Integer> listOpPlayer = new ArrayList<Integer>();
        listMyPlayer = moveCounts.get(0);
        System.out.println("Player 1:"+listMyPlayer.toString());
        listOpPlayer = moveCounts.get(1);
        System.out.println("Player 2:"+listOpPlayer.toString());

        listMyPlayer.add(getAllPossibleMoves(board,1).size());
        listOpPlayer.add(getAllPossibleMoves(board,2).size());
        
        moveCounts.set(0,listMyPlayer);
        moveCounts.set(1,listOpPlayer);


    }

   public static int mobility(int[][] board, int player) {
    int oplayer = (player == 1) ? 2 : 1;

    List<Point> myMoves = getAllPossibleMoves(board, player);
    List<Point> opMoves = getAllPossibleMoves(board, oplayer);

    int myMoveCount = myMoves.size();
    int opMoveCount = opMoves.size();

    // Calculate the number of opponent pieces that can be captured
    int captures = countPotentialCaptures(board, player);

    // Adjust the mobility score based on capturing potential
    int adjustedMobility = 100 * (myMoveCount + captures - opMoveCount) / (myMoveCount + opMoveCount + 1);

    return adjustedMobility;
}

// Helper method to count potential captures for the given player
private static int countPotentialCaptures(int[][] board, int player) {
    int oplayer = (player == 1) ? 2 : 1;
    int captures = 0;

    List<Point> myMoves = getAllPossibleMoves(board, player);

    for (Point move : myMoves) {
        int i = move.i;
        int j = move.j;

        // Check if the move results in capturing an opponent piece
        if (BoardHelper.isValidMove(board, player, i, j)) {
            int[][] tempBoard = BoardHelper.copyBoard(board);
            BoardHelper.makeMove(tempBoard, player, i, j);
            int captured = BoardHelper.getCapturedCount(tempBoard, oplayer);
            captures += captured;
        }
    }

    return captures;
}


        return 100 * (myMoveCount - opMoveCount) / (myMoveCount + opMoveCount + 1);
    }

    public static int frontier(int[][] board , int player){
        int oplayer = (player==1) ? 2 : 1;

        int myF = BoardHelper.getFrontierSquares(board,player).size();
        int opF = BoardHelper.getFrontierSquares(board,oplayer).size();

        return 100 * (myF - opF) / (myF + opF + 1);
    }

    public static int cornerGrab(int[][] board , int player){
        ArrayList<Point> moves = BoardHelper.getAllPossibleMoves(board,player);

        for(Point m : moves){
            //if player have corner move return 1
            if(m.x == 0 && m.y == 0) return 100;
            if(m.x == 7 && m.y == 0) return 100;
            if(m.x == 0 && m.y == 7) return 100;
            if(m.x == 7 && m.y == 7) return 100;
        }

        return 0;
    }

}
