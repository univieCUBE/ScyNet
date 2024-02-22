package org.scynet;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.task.AbstractNetworkViewTask;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.TaskMonitor;

import java.util.Objects;
import java.util.Set;

import static java.lang.Math.abs;

public class ToggleEdgeSizeBasedOnFluxTask extends AbstractNetworkViewTask {

	private CyApplicationManager cyApplicationManager;

	public ToggleEdgeSizeBasedOnFluxTask(CyNetworkView view, CyApplicationManager cyApplicationManager){
		super(view);
		this.cyApplicationManager = cyApplicationManager;
	}
	
	@Override
	public void run(final TaskMonitor taskMonitor) {
		if (cyApplicationManager.getCurrentNetwork() == null){			
			return;
		}

		if(view == null){
			return;
		}

		//Check if type and cross-fed columns exist
		CyNetwork currentNetwork = cyApplicationManager.getCurrentNetwork();
		Set<String> columnNames = CyTableUtil.getColumnNames(currentNetwork.getDefaultEdgeTable());

		Double defaultEdgeWidth = 10.0d;
		Double minEdgeWidth = 1.0d;
		Double maxEdgeWidth = 50.0d;

		if (columnNames.contains("sbml id") && columnNames.contains("flux")) {
			// Check for case no flux values assigned
			Boolean allFluxNull = true;
			Boolean allWidthDefault = true;
			Double maxFlux = 0.0d;
			for (CyEdge newEdge : currentNetwork.getEdgeList()) {
				Double edgeFlux = currentNetwork.getDefaultEdgeTable().getRow(newEdge.getSUID()).get("flux", Double.class);
				View<CyEdge> edgeView = view.getEdgeView(newEdge);
				if (edgeView == null) {
					continue;
				}
				if (edgeFlux != null) {
					allFluxNull = false;
					maxFlux = Math.max(maxFlux, edgeFlux);
					Double edgeWidth = edgeView.getVisualProperty(BasicVisualLexicon.EDGE_WIDTH);
					if (!Objects.equals(edgeWidth, defaultEdgeWidth)) {
						allWidthDefault = false;
					}
				}

			}

			// No flux values given: nothing to do
			if (allFluxNull) {return;}

			if (allWidthDefault) {
				// Task: set edge width relative to flux
				Double scalingFactor = (maxEdgeWidth - minEdgeWidth) / maxFlux;
				for (CyEdge newEdge : currentNetwork.getEdgeList()) {
					Double edgeFlux = currentNetwork.getDefaultEdgeTable().getRow(newEdge.getSUID()).get("flux", Double.class);
					View<CyEdge> edgeView = view.getEdgeView(newEdge);
					if (edgeView == null) {
						continue;
					}
					if ((edgeFlux != 0.0d) && (edgeFlux != null)) {
						Double edgeWidth = abs(edgeFlux) * scalingFactor + minEdgeWidth;
						if (edgeWidth > maxEdgeWidth) {
							edgeWidth = maxEdgeWidth;
						}
						edgeView.setLockedValue(BasicVisualLexicon.EDGE_WIDTH, edgeWidth);
					} else if (edgeFlux == 0.0d) {
						Double edgeWidth = 0.0d;
						edgeView.setLockedValue(BasicVisualLexicon.EDGE_WIDTH, edgeWidth);
					}
				}
			}
			else {
				// Task: set edge width to default (toggle off)
				for (CyEdge newEdge : currentNetwork.getEdgeList()) {
					View<CyEdge> edgeView = view.getEdgeView(newEdge);
					if (edgeView == null) {
						continue;
					}
					edgeView.setLockedValue(BasicVisualLexicon.EDGE_WIDTH, defaultEdgeWidth);
				}
			}
		}

//
	}
}
