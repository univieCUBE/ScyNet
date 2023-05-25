package org.cytoscape.sample.internal;

import org.cytoscape.io.datasource.DataSourceManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

/**
 * A task factory to create a task that creates a new network view in Cytoscape based on the current network and a specified data source.
 */
public class CreateNetworkViewTaskFactory extends AbstractTaskFactory {

	/**
	 * The factory for creating networks
	 */
	private final CyNetworkFactory cnf;
	/**
	 * The factory for creating network views
	 */
	private final CyNetworkViewFactory cnvf;
	/**
	 * The manager for network views in Cytoscape
	 */
	private final CyNetworkViewManager networkViewManager;
	/**
	 * The manager for networks in Cytoscape
	 */
	private final CyNetworkManager networkManager;
	/**
	 * The naming service for networks in Cytoscape
	 */
	private final CyNetworkNaming cyNetworkNaming;
	/**
	 * The manager for data sources in Cytoscape
	 */
	private final DataSourceManager dataSourceManager;
	/**
	 * The current network in Cytoscape
	 */
	private final CyNetwork currentNetwork;
	/**
	 * The boolean defined by the 'crossfeeding' toggle-button
	 */
	private boolean showOnlyCrossfeeding;
	/**
	 * The toggle-button for 'crossfeeding'
	 */
	private final JToggleButton myButton;
	private final CyApplicationManager cyApplicationManager;

	/**
	 * Constructs a new CreateNetworkViewTaskFactory with the given parameters.
	 * @param cyNetworkNaming the naming service for networks in Cytoscape
	 * @param cnf the factory for creating networks
	 * @param networkManager the manager for networks in Cytoscape
	 * @param cnvf the factory for creating network views
	 * @param networkViewManager the manager for network views in Cytoscape
	 * @param dataSourceManager the manager for data sources in Cytoscape
	 * @param cyApplicationManager the manager for applications in Cytoscape
	 * @param myButton the toggle button to display the network view in the app
	 */
	public CreateNetworkViewTaskFactory(CyNetworkNaming cyNetworkNaming, CyNetworkFactory cnf, CyNetworkManager networkManager,
										CyNetworkViewFactory cnvf, CyNetworkViewManager networkViewManager, DataSourceManager dataSourceManager,
										CyApplicationManager cyApplicationManager, JToggleButton myButton) {
		this.cnf = cnf;
		this.cnvf = cnvf;
		this.networkViewManager = networkViewManager;
		this.networkManager = networkManager;
		this.cyNetworkNaming = cyNetworkNaming;
		this.dataSourceManager = dataSourceManager;
		this.currentNetwork = cyApplicationManager.getCurrentNetwork();
		this.cyApplicationManager = cyApplicationManager;
		this.showOnlyCrossfeeding = false;
		this.myButton = myButton;

		ActionListener listener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JToggleButton toggleButton = (JToggleButton) e.getSource();
				// Handle the "selected" and "not selected" outcome
				showOnlyCrossfeeding = toggleButton.isSelected();
			}
		};
		this.myButton.addActionListener(listener);
	}

	/**
	 * Creates a new task iterator for creating a new network view based on the current network and specified data source.
	 *
	 * @return a task iterator for the CreateNetworkViewTask
	 */
	public TaskIterator createTaskIterator() {
		FileChoosing newChooser = new FileChoosing();
		HashMap<String, Double> tsvMap = newChooser.makeMap();
		return new TaskIterator(new CreateNetworkViewTask(cyNetworkNaming, cnf, networkManager, cnvf, networkViewManager, dataSourceManager, currentNetwork, tsvMap, showOnlyCrossfeeding, cyApplicationManager, newChooser.isFva));
	}

	/**
	 * Returns the toggle button used to display the network view in the app.
	 *
	 * @return the toggle button
	 */
	public JToggleButton getButton() {
		return this.myButton;
	}
}