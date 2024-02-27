package org.scynet;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

import java.util.HashMap;

public class ContextualizeWithFluxDataTaskFactory extends AbstractNetworkViewTaskFactory {
	private CyNetworkManager cyNetworkManager;
	private CyApplicationManager cyApplicationManager;
	/**
	 * The naming service for networks in Cytoscape
	 */
	private final CyNetworkNaming cyNetworkNaming;
	public ContextualizeWithFluxDataTaskFactory(CyApplicationManager cyApplicationManager, CyNetworkManager cyNetworkManager, CyNetworkNaming cyNetworkNaming) {
		this.cyApplicationManager = cyApplicationManager;
		this.cyNetworkManager = cyNetworkManager;
		this.cyNetworkNaming = cyNetworkNaming;
	}

	public TaskIterator createTaskIterator(CyNetworkView networkView){
		FileChoosing newChooser = new FileChoosing();
		HashMap<String, Double> tsvMap = newChooser.makeMap();
		return new TaskIterator(new ContextualizeWithFluxDataTask(networkView, cyApplicationManager, tsvMap, newChooser.isFva, this.cyNetworkManager, this.cyNetworkNaming) );
	}
}
