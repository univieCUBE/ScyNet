package org.scynet;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

public class ToggleEdgeSizeBasedOnFluxTaskFactory extends AbstractNetworkViewTaskFactory {
	private CyApplicationManager cyApplicationManager;
	public ToggleEdgeSizeBasedOnFluxTaskFactory(CyApplicationManager cyApplicationManager) {
		this.cyApplicationManager = cyApplicationManager;
	}

	public TaskIterator createTaskIterator(CyNetworkView networkView){
		return new TaskIterator(new ToggleEdgeSizeBasedOnFluxTask(networkView, cyApplicationManager) );
	}
}
