package camml.core.search;

/** Interface class for a function to clean TOMs */
public interface TOMCleaner
{
	public static class StandardTOMCleaner implements TOMCleaner
	{		
		public static StandardTOMCleaner tomCleaner = new StandardTOMCleaner();
		private StandardTOMCleaner() {}
		public void cleanTOM(TOM tom)
		{
			// loop through nodes cleaning each in turn.
			for ( int i = 0; i < tom.getNumNodes(); i++ ) {
				int nodeI = tom.nodeAt(i);
				int[] dirtyParent = tom.node[nodeI].parent;
				
				double oldCost = tom.caseInfo.nodeCache.getMMLCost( tom.node[nodeI] );
				for (int j = dirtyParent.length-1; j >= 0; j--) {
					int nodeJ = dirtyParent[j];
					double structureDiff = tom.caseInfo.tomCoster.costToToggleArc(tom,nodeI,nodeJ);				
					tom.removeArc(nodeI,nodeJ);
					double newCost = tom.caseInfo.nodeCache.getMMLCost(tom.node[nodeI]);
					if ( newCost > oldCost - structureDiff) {
						tom.addArc(nodeI,nodeJ);
					}
					else {
						oldCost = newCost;
					}
				}
			}			
		}
	}

	/** Don't perform any cleaning  */
	public static class NoCleanTOMCleaner implements TOMCleaner
	{		
		public static NoCleanTOMCleaner tomCleaner = new NoCleanTOMCleaner();
		private NoCleanTOMCleaner() {}
		public void cleanTOM(TOM tom) {}
	}

	/** remove all arcs which are not a parent of the specified target node */
	public static class TargetOnlyTOMCleaner implements TOMCleaner
	{		
		int target;
		public TargetOnlyTOMCleaner(int target) {this.target = target;}
		public void cleanTOM(TOM tom) {
			// loop through nodes cleaning each in turn.
			for ( int nodeI = 0; nodeI < tom.getNumNodes(); nodeI++ ) {
				if (nodeI != target) {				
					int[] dirtyParent = tom.node[nodeI].parent;					
								
					for (int j = 0; j < dirtyParent.length; j++) {
						int nodeJ = dirtyParent[j];
						tom.removeArc(nodeI,nodeJ);
					}
				}
			}			
		}
	}

	/** Clean away all nodes not in the markov blanket of the specified variable */
	public static class MarkovBlanketTOMCleaner implements TOMCleaner
	{		
		int target;
		public MarkovBlanketTOMCleaner(int target) {this.target = target;}
		public void cleanTOM(TOM tom) {
			// loop through nodes cleaning each in turn.
			for ( int nodeI = 0; nodeI < tom.getNumNodes(); nodeI++ ) {
				if (nodeI != target) {				
					int[] dirtyParent = tom.node[nodeI].parent;					
					boolean childOfTarget = false;
					for (int j = 0; j < dirtyParent.length; j++) {
						if (dirtyParent[j] == target) {
							childOfTarget = true;
							break;
						}
					}
					if (childOfTarget) {continue;}
					
					for (int j = 0; j < dirtyParent.length; j++) {
						int nodeJ = dirtyParent[j];
						tom.removeArc(nodeI,nodeJ);
					}
				}
			}			
		}
	}

	void cleanTOM(TOM tom);
}