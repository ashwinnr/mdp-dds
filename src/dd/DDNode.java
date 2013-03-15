package dd;

import graph.Graph;

import java.lang.ref.SoftReference;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import util.MySoftReference;


/* Interface for a DDNode
 * 
 */

public interface DDNode{
	
		public final static Logger LOGGER = Logger.getLogger(DDNode.class.getName());
		
		//returns root node of graph
		//must bne comparable to check for identical nodes
		public String toGraph(Graph g);
		
		public <T extends DDNode> T getNullDD();
		
//		public DDNode reduce();
		
		public int hashCode();
		
		public boolean equals(Object arg);
		
//		public  void nullify();
		
		public abstract void updateMinMax();
		
		public abstract double getMax();
		
		public abstract double getMin();
		
		
}
