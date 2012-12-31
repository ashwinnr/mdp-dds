package add;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import util.UnorderedPair;
import dd.DDOperation;

public class PrintADDOper extends DDOperation<ADDNode, ADDRNode, ADDINode, ADDLeaf, ADDManager>{

	ArrayList<String> _ordering;
	ADDManager _man;
	
	public PrintADDOper(ArrayList<String> ord, ADDManager man){
		_ordering = ord;
		_man = man;
	}
	
	@Override
	public boolean recurse(ADDRNode node) {
		
		ADDNode n = node.getNode();
		if( n instanceof ADDLeaf ){
			return false;
		}
		return true;
		
	}

	@Override
	public ADDRNode finishUpLeaf(ADDRNode node) {
		
		System.out.println(node);
		
		return null;
		
	}

	@Override
	public boolean recurse(ADDRNode node1, ADDRNode node2) {

		ADDNode n1 = node1.getNode();
		ADDNode n2 = node2.getNode();
		
		if( n1 instanceof ADDLeaf && n2 instanceof ADDLeaf ){
			return false;
		}
		
		return true;
		
	}
	
	@Override
	public UnorderedPair<UnorderedPair<String, Boolean>, UnorderedPair<Iterator<ADDRNode>, Iterator<ADDRNode>>> split(
			ADDRNode node1, ADDRNode node2) {

		//must be INode here
		ADDINode n1 = (ADDINode) node1.getNode();
		ADDINode n2 = (ADDINode) node2.getNode();
		
		String i1  = n1.getTestVariable();
		String i2 = n2.getTestVariable();
		
		System.out.println(i1 + " ::: " + i2 );
		
		int ind1 = _ordering.indexOf(i1);
		int ind2 = _ordering.indexOf(i2);
		
		if( ind1 <= ind2 ){
			
			UnorderedPair<String, Boolean> p1 = new UnorderedPair<String,Boolean>(i1, node1.isNegated());
			UnorderedPair<Iterator<ADDRNode>, Iterator<ADDRNode>> p2 = 
						new UnorderedPair< Iterator<ADDRNode>, Iterator<ADDRNode> >( 
								n1.getChildren().iterator(), UnorderedPair.makeTwoIterator(node2) 
								);
			
			return new UnorderedPair< UnorderedPair<String, Boolean>,
					UnorderedPair<Iterator<ADDRNode>, Iterator<ADDRNode>>>(p1, p2);
			
		}else{
			
			UnorderedPair<String, Boolean> p1 = new UnorderedPair<String,Boolean>(i2, node2.isNegated());
			UnorderedPair<Iterator<ADDRNode>, Iterator<ADDRNode>> p2 = 
						new UnorderedPair< Iterator<ADDRNode>, Iterator<ADDRNode>>( 
								UnorderedPair.makeTwoIterator(node1), n2.getChildren().iterator());
			
			return new UnorderedPair< UnorderedPair<String, Boolean>,
					UnorderedPair<Iterator<ADDRNode>, Iterator<ADDRNode>>>(p1, p2);
			
		}
		
	}

	@Override
	public ADDRNode merge(String _o1, boolean negate, List<ADDRNode> newlist) {

			//newlist would be empty since the recursive calls on the split 
			return null;
			
	}

	@Override
	public ADDRNode finishUpLeaf(ADDRNode node1, ADDRNode node2) {

		System.out.println(node1 + " ::: " + node2 );
		
		return null;
		
	}

}
