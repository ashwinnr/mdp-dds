/**
 * RDDL: A simple graphics display for the Sidewalk domain. 
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/10/10
 *
 **/

package rddl.viz;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Map;

import rddl.EvalException;
import rddl.State;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVARIABLE_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.TYPE_NAME;

public class CrossingTrafficDisplay extends StateViz {

	public CrossingTrafficDisplay() {
		_nTimeDelay = 200; // in milliseconds
	}

	public CrossingTrafficDisplay(int time_delay_per_frame) {
		_nTimeDelay = time_delay_per_frame; // in milliseconds
	}
	
	public boolean _bSuppressNonFluents = false;
	public BlockDisplay _bd = null;
	public int _nTimeDelay = 0;
	
	public void display(State s, int time) {
		System.out.println("TIME = " + time + ": " + getStateDescription(s));
	}

	//////////////////////////////////////////////////////////////////////

	public String getStateDescription(State s) {
		StringBuilder sb = new StringBuilder();

		TYPE_NAME xpos_type = new TYPE_NAME("xpos");
		ArrayList<LCONST> list_xpos = s._hmObject2Consts.get(xpos_type);

		TYPE_NAME ypos_type = new TYPE_NAME("ypos");
		ArrayList<LCONST> list_ypos = s._hmObject2Consts.get(ypos_type);
		
		PVAR_NAME GOAL = new PVAR_NAME("GOAL");
		
		PVAR_NAME robot_at_x = new PVAR_NAME("robot-at-x");
		PVAR_NAME robot_at_y = new PVAR_NAME("robot-at-y");
		
		PVAR_NAME obstacle_at = new PVAR_NAME("obstacle-at");
		
		PVAR_NAME coll_x = new PVAR_NAME("collision-x");
		PVAR_NAME coll_y = new PVAR_NAME("collision-y");

		int max_row = list_ypos.size() - 1;
		int max_col = list_xpos.size() - 1;
		//
		
		if (_bd == null) {
			_bd= new BlockDisplay("RDDL Crossing Traffic Simulation", "RDDL Crossing Traffic Simulation", max_row + 2, max_col + 2);	
		}
		//rows : 0 -> max_row+1
		//cols : 0 -> max_col+1
		//x1 -> col 1
		//xn -> col n
		//y1 -> row max_row
		//yn -> row 1
		
		// Set up an arity-1 parameter list
		ArrayList<LCONST> params = new ArrayList<LCONST>(2);
		params.add(null);
		params.add(null);
		
		ArrayList<LCONST> param_x = new ArrayList<LCONST>(1);
		param_x.add(null);
		

		ArrayList<LCONST> param_y = new ArrayList<LCONST>(1);
		param_y.add(null);


		_bd.clearAllCells();
		_bd.clearAllLines();
		for (LCONST xpos : list_xpos) {
			for (LCONST ypos : list_ypos) {
				int col = Integer.parseInt(xpos.toString().substring(1, xpos.toString().length()));
				int row = max_row + 1 - Integer.parseInt(ypos.toString().substring(1, ypos.toString().length()));
				//ypos = 1 => row = max_row
				//ypos = 1 => row = 2
				
				params.set(0, xpos);
				params.set(1, ypos);
				
				param_x.set(0, xpos );
				param_y.set(0, ypos );
				
				boolean is_goal  = (Boolean)s.getPVariableAssign(GOAL, params);
				boolean robot    = (Boolean)s.getPVariableAssign(robot_at_x, param_x)
						&& (Boolean)s.getPVariableAssign(robot_at_y, param_y);
				boolean obstacle = (Boolean)s.getPVariableAssign(obstacle_at, params);
				boolean colli_x =  (Boolean)s.getPVariableAssign(coll_x, param_x);
				boolean colli_y =  (Boolean)s.getPVariableAssign(coll_y, param_y);
				
//				System.out.println(xpos + " " + ypos + " " + robot );
				
				if (robot && is_goal)
					_bd.setCell(row, col, Color.green, "G!");
				else if (is_goal) 
					_bd.setCell(row, col, Color.cyan, "G");
				else if ((robot && obstacle) || colli_x || colli_y )
					_bd.setCell(row, col, Color.red, "X");
				else if (robot && !obstacle)
					_bd.setCell(row, col, Color.orange, "R");
				else if (obstacle)
					_bd.setCell(row, col, Color.black, null);
			}
		}
			
		_bd.repaint();
		
		// Sleep so the animation can be viewed at a frame rate of 1000/_nTimeDelay per second
	    try {
			Thread.currentThread().sleep(_nTimeDelay);
		} catch (InterruptedException e) {
			System.err.println(e);
			e.printStackTrace(System.err);
		}
				
		return sb.toString();
	}
	
	public void close() {
		_bd.close();
	}
}

