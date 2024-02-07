package mnkgame;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Arrays;

public class ProprioLuiPlayer implements MNKPlayer {
	private Random rand;
	private MNKBoard B;
	private MNKGameState myWin;
	private MNKGameState yourWin;
	private int TIMEOUT;

	//default constructor
	public ProprioLuiPlayer(){}


	public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
		// New random seed for each game
		rand    = new Random(System.currentTimeMillis()); 
		B       = new MNKBoard(M,N,K);
		myWin   = first ? MNKGameState.WINP1 : MNKGameState.WINP2; 
		yourWin = first ? MNKGameState.WINP2 : MNKGameState.WINP1;
		TIMEOUT = timeout_in_secs;	
	}

	public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC) {
		MNKCell out = new MNKCell(-1, -1);
        MNKCell outc = FC[0];
		long start = System.currentTimeMillis();

        //System.out.println("Mossa iniziata!\n");  /* DEBUGGING */

		if(MC.length > 0) {     //Se abbiamo già una mossa fatta
			MNKCell c = MC[MC.length-1]; // Recover the last move from MC
			B.markCell(c.i,c.j);         // Save the last move in the local MNKBoard 'B'
		}
        else{                   // SE SIAMO PRIMI
            outc = new MNKCell(B.M/2, B.N/2, MNKCellState.P1);  //marchiamo il centro
            B.markCell(B.M/2, B.N/2);
            return outc;
        }

        // If there is just one possible move, return immediately
		if(FC.length == 1)  return outc;
		
		else{   //ALGORITMO DI ITERATIVE DEEPENING
            int alpha = -20;
            int beta= 20;
            int i = 1;
            int depth = B.M*B.N;
            MC = B.getMarkedCells();
            FC = FCRadiux(B,MC);
            while (i < depth){  //affinchè non si arriva alla profondità max
                if((System.currentTimeMillis()-start)/1000.0 < TIMEOUT*(97.0/100.0)){ //se c'è ancora tempo
                    out = MiniMaxChoice(B, FC, MC, start, i, alpha, beta); //chiamata minimax
                    if((System.currentTimeMillis()-start)/1000.0 < TIMEOUT*(97.0/100.0)){
                        outc = out;
                        i=i+2;  //partiamo con profondità 1, e ripetiamo aumentando di 2 alla volta
                    }
                }
                else{
                    i=depth;    //break se manca tempo
                }
            }
		}

        B.markCell(outc.i, outc.j);
		return outc;
	}


    public MNKCell MiniMaxChoice(MNKBoard B, MNKCell[] FC, MNKCell[] MC, long start, int depth, int alpha, int beta){
        MNKCell bestCell = new MNKCell(0, 0);
        int bestCelleval = -100;

        for(int k = FC.length-1;k>=0; k--){
            if((System.currentTimeMillis()-start)/1000.0 < TIMEOUT*(97.0/100.0)){
                B.markCell(FC[k].i, FC[k].j);
                int outRec = MiniMax(B, FC, MC, false, start, depth-1, alpha, beta);  //ricorsione
                B.unmarkCell();

                if(outRec > bestCelleval && outRec!=-19){    //se la ricorsione è migliore e FC[k] è stata conclusa
                    bestCell = FC[k];
                    bestCelleval = outRec;
                }
                //Alpha-Beta Pruning
                alpha = Math.max(alpha, bestCelleval);
                if (beta <= alpha)  return bestCell;
            }
            else
            {
                //System.out.println(("Choice - time end")); /* DEBUGGING */
                return bestCell;
            }
        }

        return bestCell;
    }

	//ALGORITMO DI MINIMAX CON ALPHA-BETA PRUNING
    public int MiniMax(MNKBoard B, MNKCell[] FC, MNKCell[] MC, boolean isMax, long start, int depth, int alpha, int beta){
        int bestCell = 0;
        int outRec;
        if ((B.gameState() != MNKGameState.OPEN) || depth == 0){    //Se stiamo in un gamestate finale OR siamo a maxdepth
            return evaluate(B, start, MC, isMax);
        }
        FC = FCAdder(B, FC, B.MC.peekLast()); //Aggiunge alla lista di celle libere le celle attorno l'ultima piazzata

        if (isMax){                 //MAX PLAYER
            bestCell = -100;
            for(int k = FC.length-1;k>=0; k--) {  //Cerchiamo dalle ultime celle caricate nell'array
                if((System.currentTimeMillis()-start)/1000.0 < TIMEOUT*(97.0/100.0)){
                    B.markCell(FC[k].i, FC[k].j);
                    outRec = MiniMax(B, FC, MC, false, start, depth-1, alpha, beta);  //ricorsione
                    B.unmarkCell();

                    if(outRec > bestCell && outRec!=-19)
                        bestCell = outRec;
                    
                    //Alpha-Beta Pruning
                    alpha = Math.max(alpha, bestCell);
                    if (beta <= alpha)  return bestCell;
                }
                else
                {
                    //System.out.println(("Maximizing - time end")); /* DEBUGGING */
                    return bestCell;
                }
            }
        }
        else if (!isMax){       //MIN PLAYER
            bestCell = 100;
            for(int k = FC.length-1; k>=0; k--) {  //Cerchiamo dalle ultime celle caricate nell'array
                if((System.currentTimeMillis()-start)/1000.0 < TIMEOUT*(97.0/100.0)){
                    B.markCell(FC[k].i, FC[k].j);
                    outRec = MiniMax(B, FC, MC, true, start, depth-1, alpha, beta);   //ricorsione
                    B.unmarkCell();

                    if(outRec < bestCell && outRec!=-19)
                        bestCell = outRec;
                    
                    //Alpha-Beta Pruning
                    beta = Math.min(beta, bestCell);
                    if (beta <= alpha)  return bestCell;
                }
                else
                {
                    //System.out.println(("Minimizing - time end")); /* DEBUGGING */
                    return bestCell;
                }
            }
        }

        return bestCell;
    }

    //FUNZIONE EVALUATE, ASSEGNA UN PESO ALLE CELLE
    public int evaluate(MNKBoard B, long start, MNKCell[] MC, boolean isMax){
        if((System.currentTimeMillis()-start)/1000.0 >= TIMEOUT*(97.0/100.0)){
            return -19;                                  //caso time finish
        }
        else if(B.gameState()==myWin) {         //caso nostra win
            return 20;
        }
        else if(B.gameState()==yourWin) {       //caso nostra loss
            return -20;
        }
        else if(B.gameState()==MNKGameState.DRAW)
        {
            return 19;  //caso draw (preferibile ad uno stato 'open' ignoto)
        }
        return isMax ? -celleVicine(B, MC[MC.length-1]) : celleVicine(B, MC[MC.length-1]);  //Caso OPEN
    }

    //funzione valore di celle vicine - cambia il valore di una cella open in base al numero di celle circostanti
    public int celleVicine(MNKBoard B, MNKCell d){
        int si, sj, ei, ej, val = 0;
        si = Math.max(0, d.i - 1);  
        sj = Math.max(0, d.j - 1);  
        ei = Math.min(B.M-1, d.i + 1);     
        ej = Math.min(B.N-1, d.j + 1);    //consideriamo le celle attorno a d

        for(int i=si; i<=ei; i++){
            for(int j=sj; j<=ej; j++){
                if (B.cellState(i, j) == d.state){  //Se tale cella è stata posizionata dal corrispettivo giocatore
                    val++;  //incrementiamo val per ogni cella occupata circostante
                }
            }
        }
        return val;
    }
        
    public MNKCell[] FCAdder(MNKBoard B, MNKCell[] FCC, MNKCell z){
        HashSet<MNKCell> FCCompatto = new HashSet<MNKCell>(Arrays.asList(FCC));  //Trasformiamo l'array in HashSet
        int si, sj, ei, ej;
        si = Math.max(0, z.i - 1);  
        sj = Math.max(0, z.j - 1);    
        ei = Math.min(B.M-1, z.i + 1);  
        ej = Math.min(B.N-1, z.j + 1);

        for(int i=si; i<=ei; i++){
            for(int j=sj; j<=ej; j++){
                MNKCell c = new MNKCell(i, j, B.cellState(i, j));
                if ((c.state == MNKCellState.FREE)){
                    FCCompatto.add(c);  //aggiungiamo le celle libere 
                }
            }
        }
        FCCompatto.remove(new MNKCell(z.i, z.j, MNKCellState.FREE));  //Rimuoviamo l'ultima cella marcata inserita
        return FCCompatto.toArray(new MNKCell[FCCompatto.size()]);
    }
    
    public MNKCell[] FCRadiux(MNKBoard B, MNKCell[] MC){
        HashSet<MNKCell> FCCompatto = new HashSet<MNKCell>((int) Math.ceil((B.M*B.N)));
        int si, sj, ei, ej;
        for (int k = MC.length-1; k>=0; k--) {  //per ogni cella marcata che trovi nella board...
            //prendiamo le celle libere attorno ad essa
            si = Math.max(0, MC[k].i - 1);  
            sj = Math.max(0, MC[k].j - 1);   
            ei = Math.min(B.M-1, MC[k].i + 1);  
            ej = Math.min(B.N-1, MC[k].j + 1);    

            for(int i=si; i<=ei; i++){
                for(int j=sj; j<=ej; j++){
                    MNKCell c = new MNKCell(i, j, B.cellState(i, j));
                    if ((c.state == MNKCellState.FREE)){
                        FCCompatto.add(c);
                    }
                }
            }
        }
        return FCCompatto.toArray(new MNKCell[FCCompatto.size()]);
    }

	public String playerName() {
		return "ゴKing Crimsonゴ";
	}
}
