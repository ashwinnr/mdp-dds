/**
 * RDDL: A simple graphics display for the Sidewalk domain. 
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/10/10
 *
 **/

package rddl.viz;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import rddl.EvalException;
import rddl.State;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVARIABLE_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.TYPE_NAME;

public class TrafficDisplay extends StateViz {

	private int _nRows;
	private int _nCols;
	private int _wingSize;
	private int _blockSize;
	
	private static String LEFT_ARROW = "\u2190";
	private static String RIGHT_ARROW = "\u2192";
	private static String UP_ARROW = "\u2191";
	private static String DOWN_ARROW = "\u2193";
	
	public TrafficDisplay(int time_delay_per_frame, int nrows, int ncols, int blockSize, int wingSize ) {
		_nTimeDelay = time_delay_per_frame; // in milliseconds
		_nRows = nrows;
		_nCols = ncols;
		_wingSize = wingSize;
		_blockSize = blockSize;
	}
	
	public boolean _bSuppressNonFluents = true;
	public BlockDisplay _bd = null;
	public int _nTimeDelay = 0;
	private int _xsize;
	private int _ysize;
	
	public void display(State s, int time) {
		System.out.println("TIME = " + time + ": " + getStateDescription(s));
		
	}

	//////////////////////////////////////////////////////////////////////

	public String getStateDescription(State s) {
		
//		if( _bd != null ){
//			
//			_bd.setCell(0, 0, Color.black, "O");
//			
//			_bd.setCell(1, 1, Color.black, "1");
//			
//			_bd.setCell(2, 2, Color.black, "2");
//			
//		}

//		System.out.println( s.toString() );
		
		StringBuilder sb = new StringBuilder();

		TYPE_NAME cell_type = new TYPE_NAME("cell");
		ArrayList<LCONST> cells = s._hmObject2Consts.get(cell_type);

		TYPE_NAME intersection_type = new TYPE_NAME("intersection");
		ArrayList<LCONST> intersections = s._hmObject2Consts.get(intersection_type);

		PVAR_NAME occupied = new PVAR_NAME("occupied");
		PVAR_NAME direction = new PVAR_NAME("direction");
		
		PVAR_NAME light_signal1 = new PVAR_NAME("light-signal1");
		PVAR_NAME light_signal2 = new PVAR_NAME("light-signal2");
		PVAR_NAME turn_at = new PVAR_NAME("turn-at");
		
		PVAR_NAME input = new PVAR_NAME("PERIMETER-INPUT-RATE");
		
		PVAR_NAME rate = new PVAR_NAME("PERIMETER-INPUT-RATE");
		
		PVAR_NAME exit = new PVAR_NAME("PERIMETER-EXIT-CELL");
		
		PVAR_NAME street_ew = new PVAR_NAME("STREET-EW");
		
		PVAR_NAME street_we = new PVAR_NAME("STREET-WE");
		
		PVAR_NAME street_ns = new PVAR_NAME("STREET-NS");
		
		PVAR_NAME street_sn = new PVAR_NAME("STREET_SN");
		
		PVAR_NAME xion_at = new PVAR_NAME("INTERSECTION-AT");
		
		if( _bd == null ){
			
			_xsize = _nRows + ( _nRows - 1 ) * _blockSize + 2*_wingSize;
//			_xsize *= 2;
			
			_ysize = _nCols + ( _nCols - 1 ) * _blockSize + 2*_wingSize;
			
//			_ysize *= 2;
			
			_bd = new BlockDisplay("Traffic simulation", "" + _xsize + "" + _ysize, _xsize, _ysize);
			_bd.repaint();
			
		}
		
		for( LCONST cell : cells ){
			
			int i, j;
			if( cell._sConstValue.startsWith("c") ){
				
				int index = cell._sConstValue.indexOf("_");
				i = Integer.parseInt(cell._sConstValue.substring(1,index) );
				j = Integer.parseInt( cell._sConstValue.substring(index+1) );
				
				ArrayList<LCONST> thisCell = new ArrayList<LCONST>();
				thisCell.add(cell);
				
				boolean val = (Boolean)s.getPVariableAssign(occupied, thisCell);
				
				boolean dirxn = (Boolean)s.getPVariableAssign(direction, thisCell);
				
				Color col = ( val ) ? Color.blue : Color.black ;
				
				boolean ew_dirxn = (Boolean)s.getPVariableAssign(street_ew, thisCell);
				
				boolean we_dirxn = (Boolean)s.getPVariableAssign(street_we, thisCell);
				
				boolean sn_dirxn = (Boolean)s.getPVariableAssign(street_sn, thisCell);
				
				boolean ns_dirxn = (Boolean)s.getPVariableAssign(street_ns, thisCell);
				
				String text = ( dirxn ) ? ( ( ew_dirxn ) ? LEFT_ARROW : ( we_dirxn ? RIGHT_ARROW : null ) ) : 
									( (sn_dirxn ) ? UP_ARROW : ( ( ns_dirxn ? DOWN_ARROW : null ) ) ) ;
				
				_bd.setCell(_ysize-j-1,i , col, text);
				
				System.out.println( "Set " + i + " " + ( _ysize-j-1 ) + " to " + val + 
						" color " + col.toString() + " text " + text );
				
			}
			
		}
		
		for( LCONST xion : intersections ){
			
			int i, j;
			
			int index = xion._sConstValue.indexOf("_");
			i = Integer.parseInt(xion._sConstValue.substring(1,index) );
			j = Integer.parseInt( xion._sConstValue.substring(index+1) );
			
			ArrayList<LCONST> thisCell = new ArrayList<LCONST>();
			thisCell.add(xion);
			
			boolean l1 = (Boolean)s.getPVariableAssign(light_signal1, thisCell);
			
			boolean l2 = (Boolean)s.getPVariableAssign(light_signal2, thisCell);
			
			Color col = ( !l1 && !l2 || !l1 && l2 ) ? Color.GREEN : ( ( l1 && l2 ) ? Color.black : Color.RED );
			
			boolean turn = (Boolean)s.getPVariableAssign(turn_at, thisCell);

			String text = null;
			
			for( LCONST cell : cells ){
				
				ArrayList<LCONST> blah = new ArrayList<LCONST>();
				
				blah.add(xion);
				
				blah.add(cell);
				
				boolean this_xion = (Boolean) s.getPVariableAssign( xion_at, blah );
				
				if( this_xion ){
					
					ArrayList<LCONST> arg = new ArrayList<LCONST>();
					arg.add(cell);
			
					boolean ew_dirxn = (Boolean)s.getPVariableAssign(street_ew, arg);
					
					boolean we_dirxn = (Boolean)s.getPVariableAssign(street_we, arg);
					
					boolean sn_dirxn = (Boolean)s.getPVariableAssign(street_sn, arg);
					
					boolean ns_dirxn = (Boolean)s.getPVariableAssign(street_ns, arg);
			
					boolean val = (Boolean)s.getPVariableAssign(occupied, arg);
					
					if( l1 &&  !l2 ){
						text = ( ( ew_dirxn ) ? LEFT_ARROW : ( we_dirxn ? RIGHT_ARROW : null ) );
					}else if( !l1 && l2 ){
						text = ( (sn_dirxn ) ? UP_ARROW : ( ( ns_dirxn ? DOWN_ARROW : null ) ) );
					}else if( l1 && l2 ){
						text = "X";
					}
					
					if( val ){
						col = Color.yellow;
					}
					
					break;
					
				}
				
			}
			
			_bd.setCell( _ysize-j-1, i, col, text);
			
			System.out.println( "Set xion " + (_ysize-j-1) + " " + i + " to  color " + col.toString() + " " + text );
			
		}
		
		_bd.repaint();
		
//		 Sleep so the animation can be viewed at a frame rate of 1000/_nTimeDelay per second
	    try {
			Thread.currentThread().sleep(_nTimeDelay);
		} catch (InterruptedException e) {
			System.err.println(e);
			e.printStackTrace(System.err);
		}
		
		
		
//		try {
//			System.in.read();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		
		
		return sb.toString();
	}
	
	public void close() {
		_bd.close();
	}
}

