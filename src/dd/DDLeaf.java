package dd;

import graph.Graph;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

import add.ADDLeaf;

public abstract class DDLeaf<N> implements DDNode {
	
		
		public N getLeafValues() {
			return leafValues;
		}

		private static int LEAF_SIZE = 5;

		protected N leafValues;
		
		public int hashCode(){
			return leafValues.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			
			if( obj instanceof DDLeaf ){
				return this.leafValues.equals(((DDLeaf)obj).leafValues);
			}
			
			return false;
			
		}
	
		public String toGraph(Graph g){
			
			String str = "leaf " + leafValues.hashCode();
			
			g.addNode(str , LEAF_SIZE);
			
			g.addNodeLabel(str, leafValues.toString());
			
			g.addNodeShape(str, "square");
			
			g.addNodeColor(str,  "yellow");

			return str;
			
		}
		
		public abstract DDLeaf<N> plugIn( final N leafVals );

//		@Override
//		public void nullify() {
//			this.leafValues = null;
//		}
		
		@Override
		public String toString() {
			return this.leafValues.toString();
		}
		
}
