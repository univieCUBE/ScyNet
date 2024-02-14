package org.cytoscape.sample.internal;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.io.datasource.DataSourceManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TunableSetter;
import org.cytoscape.work.undo.UndoSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.cytoscape.application.CyApplicationConfiguration;

import javax.swing.*;
import java.util.Properties;
import java.io.File;
import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 The CyActivator class is responsible for activating the SCyNet application in Cytoscape.
 This class extends the AbstractCyActivator class and overrides the start() method to register the necessary
 services, tasks, and factories for the application to work properly.
 */
public class CyActivator extends AbstractCyActivator {
	private static Logger logger;

	/**
	 * Default constructor for the CyActivator class.
	 */
	public CyActivator() {
		super();
	}

	/**
	 * Overrides the start() method in the AbstractCyActivator class to register the necessary services, tasks,
	 * and factories for the application to work properly.
	 @param bc The bundle context used to register the services.
	 */
	public void start(BundleContext bc) {
		// Initialize the log file
		Bundle bundle = bc.getBundle();
		String name = bundle.getSymbolicName();
		String version = bundle.getVersion().toString();

		logger = LoggerFactory.getLogger(CyActivator.class);
		logger.info("Starting " + name + " version " + version);

		// Get the necessary services
		DataSourceManager dataSourceManager = getService(bc, DataSourceManager.class);
		CyNetworkNaming cyNetworkNamingServiceRef = getService(bc,CyNetworkNaming.class);
		CyNetworkFactory cyNetworkFactoryServiceRef = getService(bc,CyNetworkFactory.class);
		CyNetworkManager cyNetworkManagerServiceRef = getService(bc,CyNetworkManager.class);
		CyApplicationManager cyApplicationManager = getService(bc, CyApplicationManager.class);
		CyNetworkViewFactory cyNetworkViewFactoryServiceRef = getService(bc,CyNetworkViewFactory.class);
		CyNetworkViewManager cyNetworkViewManagerServiceRef = getService(bc,CyNetworkViewManager.class);
		UndoSupport undo = getService(bc, UndoSupport.class);
		CyLayoutAlgorithmManager layoutManager = getService(bc, CyLayoutAlgorithmManager.class);

		// Set properties for creating a network view task factory
		Properties createNetworkViewTaskFactoryProps = new Properties();
		createNetworkViewTaskFactoryProps.setProperty("preferredMenu","Apps.SCyNet");

		// Create a JFrame and JToggleButton for the new menu item
		JToggleButton myButton = new JToggleButton("Toggle only 'crossfeeding' Nodes");

		// Get all the networks and create a network view task factory for each network

		CreateNetworkViewTaskFactory createNetworkViewTaskFactory = new CreateNetworkViewTaskFactory(cyNetworkNamingServiceRef, cyNetworkFactoryServiceRef, cyNetworkManagerServiceRef, cyNetworkViewFactoryServiceRef, cyNetworkViewManagerServiceRef, layoutManager, dataSourceManager, cyApplicationManager, myButton);
		createNetworkViewTaskFactoryProps.setProperty("title", "Create Simplified Community Network");
		registerService(bc, createNetworkViewTaskFactory, TaskFactory.class, createNetworkViewTaskFactoryProps);

		ToggleShowOnlyCfNodesTaskFactory toggleShowOnlyCfNodes = new ToggleShowOnlyCfNodesTaskFactory(cyApplicationManager);
		Properties toggleShowOnlyCfNodesProperties = new Properties();
		toggleShowOnlyCfNodesProperties.setProperty("preferredMenu","Apps.SCyNet");
		toggleShowOnlyCfNodesProperties.setProperty("title", "Toggle Nodes");
		registerService(bc,toggleShowOnlyCfNodes, NetworkViewTaskFactory.class,toggleShowOnlyCfNodesProperties);

		ScynetLayout scynetLayout = new ScynetLayout(undo, cyApplicationManager);

		Properties customLayoutProps = new Properties();
		customLayoutProps.setProperty("preferredMenu","Custom Layouts");
		registerService(bc, scynetLayout, CyLayoutAlgorithm.class, customLayoutProps);

		// ApplyCustomLayoutTaskFactory service
		ApplyScynetLayoutTaskFactory applyLayoutTaskFactory = new ApplyScynetLayoutTaskFactory(layoutManager);

		Properties applyCustomLayoutProperties = new Properties();
		applyCustomLayoutProperties.setProperty("preferredMenu","Apps.SCyNet");
		applyCustomLayoutProperties.setProperty("title", "Apply ScyNet Layout");
		registerService(bc, applyLayoutTaskFactory, NetworkViewTaskFactory.class, applyCustomLayoutProperties);

	}
}