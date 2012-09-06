package dd;

public abstract class NodeOperation<D extends DDNode> {

	public abstract D doUnaryOp(D node);
	
	public abstract D doBinaryOp(D node1, D node2);
	
}
