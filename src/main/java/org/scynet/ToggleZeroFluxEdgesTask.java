package org.scynet;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.task.AbstractNetworkViewTask;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.TaskMonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.cytoscape.application.CyUserLog;
import org.apache.log4j.Logger;

public class ToggleZeroFluxEdgesTask extends AbstractNetworkViewTask {

	private final Logger logger;
	private CyApplicationManager cyApplicationManager;

	public ToggleZeroFluxEdgesTask(CyNetworkView view, CyApplicationManager cyApplicationManager){
		super(view);
		this.logger = Logger.getLogger(CyUserLog.NAME);
		this.cyApplicationManager = cyApplicationManager;
	}
	
	@Override
	public void run(final TaskMonitor taskMonitor) {
		if (cyApplicationManager.getCurrentNetwork() == null) {
			logger.warn("No network selected. Nothing to do.");
			return;
		}

		if (view == null) {
			logger.warn("No network view available for selected network. Nothing to do.");
			return;
		}

		//Check if sbml id and flux columns exist
		CyNetwork currentNetwork = cyApplicationManager.getCurrentNetwork();
		Set<String> columnNames = CyTableUtil.getColumnNames(currentNetwork.getDefaultEdgeTable());

		if (columnNames.contains("sbml id") && columnNames.contains("flux")) {
			// Check for case no flux values assigned
			Boolean allHidden = true;

			// Check if any zero flux edge is not hidden
			for (CyEdge newEdge : currentNetwork.getEdgeList()) {
				Double edgeFlux = currentNetwork.getDefaultEdgeTable().getRow(newEdge.getSUID()).get("flux", Double.class);
				View<CyEdge> edgeView = view.getEdgeView(newEdge);
				if (edgeView == null || edgeFlux == null) {
					continue;
				}
				if (edgeFlux == 0.0d && edgeView.getVisualProperty(BasicVisualLexicon.EDGE_VISIBLE)) {
					allHidden = false;
					break;
				}
			}

			if (allHidden) {
				// Task: make 0 flux edges visible (toggle off)
				logger.info("Making all edges with flux 0 visible.");
				for (CyEdge newEdge : currentNetwork.getEdgeList()) {
					Double edgeFlux = currentNetwork.getDefaultEdgeTable().getRow(newEdge.getSUID()).get("flux", Double.class);
					View<CyEdge> edgeView = view.getEdgeView(newEdge);
					if (edgeView == null || edgeFlux == null) {
						continue;
					}
					if (edgeFlux == 0.0d) {
						edgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);

						// Make connected nodes visible as well
						View<CyNode> sourceView = view.getNodeView(newEdge.getSource());
						View<CyNode> targetView = view.getNodeView(newEdge.getTarget());
						if (sourceView != null) {
							sourceView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
						}
						if (targetView != null) {
							targetView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
						}

					}
				}
			} else {
				// Task: hide 0 flux edges
				logger.info("Hiding all edges with flux 0.");
				for (CyEdge newEdge : currentNetwork.getEdgeList()) {
					Double edgeFlux = currentNetwork.getDefaultEdgeTable().getRow(newEdge.getSUID()).get("flux", Double.class);
					View<CyEdge> edgeView = view.getEdgeView(newEdge);
					if (edgeView == null || edgeFlux == null) {
						continue;
					}
					if (edgeFlux == 0.0d) {
						edgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
					}
				}
			}

			hideSingletons(currentNetwork);

		}
		else {
			logger.error("The selected network is not in ScyNet format.");
		}
	}

	private void hideSingletons(CyNetwork currentNetwork) {
		for (CyNode node : currentNetwork.getNodeList()) {
			View<CyNode> nodeView = view.getNodeView(node);
			if (!nodeView.getVisualProperty(BasicVisualLexicon.NODE_VISIBLE)) {
				continue;
			} else if (Objects.equals("exchange metabolite", currentNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("type", String.class))) {
				// Check for visible edges
				List<CyEdge> edges = currentNetwork.getAdjacentEdgeList(node, CyEdge.Type.ANY);
				List<CyEdge> visibleEdges = new ArrayList<>();

				for (CyEdge edge : edges) {
					View<CyEdge> edgeView = view.getEdgeView(edge);
					if (edgeView.getVisualProperty(BasicVisualLexicon.EDGE_VISIBLE)) {
						visibleEdges.add(edge);
						break;
					}
				}

				// Hide nodes without visible edges
				if (visibleEdges.isEmpty()) {
					nodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, false);
				}
			}
		}
	}
}
