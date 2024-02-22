package org.cytoscape.sample.internal;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

public class ToggleZeroFluxEdgesTaskFactory extends AbstractNetworkViewTaskFactory {
	private CyApplicationManager cyApplicationManager;
	public ToggleZeroFluxEdgesTaskFactory(CyApplicationManager cyApplicationManager) {
		this.cyApplicationManager = cyApplicationManager;
	}

	public TaskIterator createTaskIterator(CyNetworkView networkView){
		return new TaskIterator(new ToggleZeroFluxEdgesTask(networkView, cyApplicationManager) );
	}
}
