# ScyNet
ScyNet is a Cytoscape app for visualizing community metabolic models. ScyNet reduces the visualized network to community members and their exchange metabolites.
## Requirements ##
Cytoscape version 3.9.0 or later
Importing the metabolic model is handled by cy3sbml. (apps.cytoscape.org/apps/cy3sbml)
## Installation ##
In the current state, ScyNet needs to be installed from file. This can be done in Cytoscape via Apps -> App Manager -> Install from file
## Basic usage ##
### Loading a Model ###
The input community metabolic model needs to be stored in SBML format. The SBML file then needs to be imported with the cy3sbml app (see requirements). Please be patient when loading models, especially larger models (>10 members). This process can take a couple of minutes (less than 5 in all tested cases).
### Model Specifications ###
For ScyNet to attribute metabolites to either the community members or the common exchange space correctly, three format requirements exist:
1. There must be a common exchange compartment named "exchg" containing all exchange metabolites and their respective boundary reactions, as well as transfer reactions to the respective member compartments.
2. Each metabolite ID must be prefixed with the identifier of the community member they are associated with followed by an underscore: MEMBER_ID_METABOLITE_ID
3. Each compartment ID that is part of a community member must be prefixed with the identifier of the community member they are associated with followed by an underscore: MEMBER_ID_COMPARTMENT_ID
A metabolic model with this format can be generated from member models using the PyCoMo package.
### Creating a Reduced Network ###
A reduced network visualization can be produced by selecting the network loaded by cy3sbml and then selecting Apps -> SCyNet -> Create Simplified Community Network
A new window will open, prompting the selection of a file. In this step, you can either select a flux vector file to visualize it in the network, or cancel the file selection and produce a reduced network without flux data.
#### FBA Flux File ####
The flux vector of a single state can be visualized by providing the vector in a tab separated file. This file needs to contain two columns, called reaction_id and flux.
#### FVA Flux File ####
Also flux ranges can be visualized with ScyNet. For doing so, a tab separated file with three columns needs to be provided: reaction_id, min_flux, max_flux.
### Layout and Styling ###
ScyNet offers two additional options for structuring the visualisation. The first is Toggle Nodes, which hides or unhides all metabolites and reactions that are not part of cross-feeding. The second is Apply ScyNet Layout, which positions the nodes of the network in 4 circles. Going inwards, the circles contain metabolites connected to a single member, the community member nodes, metabolites connected to exactly two members and lastly metabolites connected to 3 or more community members.
Both options can be found under Apps -> SCyNet
