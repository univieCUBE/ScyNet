package org.cytoscape.sample.internal;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

public class ToggleShowOnlyCfNodesTaskFactory extends AbstractNetworkViewTaskFactory {
	private CyApplicationManager cyApplicationManager;
	public ToggleShowOnlyCfNodesTaskFactory(CyApplicationManager cyApplicationManager) {
		this.cyApplicationManager = cyApplicationManager;
	}

	public TaskIterator createTaskIterator(CyNetworkView networkView){
		return new TaskIterator(new ToggleShowOnlyCfNodesTask(networkView, cyApplicationManager) );
	}
}
