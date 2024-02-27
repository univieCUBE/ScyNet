package org.scynet;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

import java.util.HashMap;

public class ContextualizeWithFluxDataTaskFactory extends AbstractNetworkViewTaskFactory {
	private CyApplicationManager cyApplicationManager;
	public ContextualizeWithFluxDataTaskFactory(CyApplicationManager cyApplicationManager) {
		this.cyApplicationManager = cyApplicationManager;
	}

	public TaskIterator createTaskIterator(CyNetworkView networkView){
		FileChoosing newChooser = new FileChoosing();
		HashMap<String, Double> tsvMap = newChooser.makeMap();
		return new TaskIterator(new ContextualizeWithFluxDataTask(networkView, cyApplicationManager, tsvMap) );
	}
}
