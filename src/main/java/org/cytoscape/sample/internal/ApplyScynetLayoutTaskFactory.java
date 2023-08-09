package org.cytoscape.sample.internal;

import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TunableSetter;

import java.util.HashMap;
import java.util.Map;

/**
 * Applies CustomLayout to the given CyNetworkView with specific Tunable
 * parameters.
 */
public class ApplyScynetLayoutTaskFactory implements NetworkViewTaskFactory {
	private CyLayoutAlgorithmManager layoutManager;

	public ApplyScynetLayoutTaskFactory(CyLayoutAlgorithmManager layoutManager) {
		this.layoutManager = layoutManager;
	}
	
	public TaskIterator createTaskIterator(CyNetworkView view) {
		// Get an instance of the layout.  We usually won't know at runtime
		// what the implementation class is so we won't cast it to anything.
		CyLayoutAlgorithm layout = layoutManager.getLayout("scynetLayout");
		
		// Create a new context for the layout so we can configure the settings
		// without changing the user's defaults.
		Object context = layout.createLayoutContext();

		String layoutAttribute = null;
		return layout.createTaskIterator(view, context, CyLayoutAlgorithm.ALL_NODE_VIEWS, layoutAttribute);
	}
	
	public boolean isReady(CyNetworkView view) {
		return view != null;
	};
}
