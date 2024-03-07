# ScyNet
ScyNet is a Cytoscape app for visualizing community metabolic models. ScyNet reduces the visualized network to community members and their exchange metabolites.
## Features ##
 - ScyNet drastically reduces community network size (>90% for genome scale metabolic models expected)
 - The resulting networks allow for better overview and creation of figures
 - ScyNet allows for contextualizing the reduced networks with flux data (e.g. from FBA or FVA results)
 - The network styling uses a color-blind friendly palette
 - The custom layout algorithm provides a good overview of the community members and their exchange metabolites 
## Requirements ##
Please make sure that the following requirements are installed
 - Cytoscape version 3.9.0 or later
 - [cy3sbml](https://apps.cytoscape.org/apps/cy3sbml) (for importing metabolic models)
## Installation ##
ScyNet is available in the [Cytoscape App Store](https://apps.cytoscape.org/apps/scynet). It can be installed within Cytoscape by going to `Apps -> App Manager` and then `search` for `ScyNet`. Once found, select ScyNet and click `install`.
## Basic usage ##
For more detailed information and examples, visit the [ScyNet wiki](https://github.com/univieCUBE/ScyNet/wiki)!
### Loading a Model ###
Import the input community metabolic model with the [cy3sbml](https://apps.cytoscape.org/apps/cy3sbml) app (see requirements). This can be done by going to `File -> Import -> Network from file` and then selecting the community metabolic model SBML file. Please be patient when loading models, especially larger models (>10 members). This process can take a couple of minutes, depending on your hardware.
### Model Specifications ###
ScyNet requires some information in the community metabolic model SBML file to be in a specific format. This is required to correctly attribute metabolites to either the community members or the shared exchange compartment. SBML files need to fulfill the following:
1. There must be a shared exchange compartment containing all exchange metabolites and their respective boundary reactions, as well as transfer reactions to the respective member compartments. To detect this compartment there needs to be **either**
   1. A shared exchange compartment named `medium`.
   2. A parameter `shared_compartment_id` which is set to the name of the shared exchange compartment.
2. Each metabolite ID must be prefixed with the identifier of the community member they are associated with followed by an underscore: `memberId_metaboliteId`
3. Each compartment ID that is part of a community member must be prefixed with the identifier of the community member they are associated with, followed by an underscore: `memberId_compartmentId`

A metabolic model with this format can be generated from member models using the [PyCoMo package](https://github.com/univieCUBE/PyCoMo).
### Creating a Reduced Network ###
After importing the community metabolic network with cy3sbml, 3 networks should be available: `All`, `Base`, and `Kinetic`. Select either the `All` or `Kinetic` network and run the network simplification via `Apps -> ScyNet -> Create Simplified Community Network`.
### Layout and Styling ###
ScyNet offers several options for changing the network layout, all of which can be found under `Apps -> ScyNet`. To run them, a network created by ScyNet needs to be selected first.
 - **Contextualize with Flux Data** (see below)
 - **Apply ScyNet Layout** Places all nodes into concentric circles based on node type and connection to community member nodes. This layout is automatically applied when creating a simplified community network.
 - **Toggle Non-Cross-Fed Metabolite Visibility** Hides all metabolite nodes that are not cross-fed. If all non-cross-fed metabolite nodes are hidden, it reveals them instead. Only works if flux data is available.
 - **Toggle Edge Width Relative to Flux** Sets edge widths relative to the corresponding flux values. Running this again will set all edge widths to the default width. Only works if flux data is available.
 - **Toggle Zero Flux Edge Visibility** Hides all edges with a flux value of 0. If all edges with 0 flux are hidden, it reveals them instead. Only works if flux data is available.
### Contextualization with flux data ###
ScyNet can contextualize the edges of the community network with flux data. This can be either single value fluxes (such as from FBA) or flux ranges (such as from FVA). To read the flux values with ScyNet, they need to be supplied as tab separated files (further requirements below).
#### FBA Flux File ####
The flux vector of a single state can be visualized by providing the vector in a tab separated file. This file needs to contain two columns, called `reaction_id` and `flux`.
#### FVA Flux File ####
Also flux ranges can be visualized with ScyNet. For doing so, a tab separated file with three columns needs to be provided: `reaction_id`, `min_flux`, `max_flux`.


## Citing ScyNet ##
At the present moment we are still working on the final stages of the manuscript. Once it is made public, a citation note will be included at this place.
