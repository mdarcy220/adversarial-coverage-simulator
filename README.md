# adversarial-coverage-simulator
An app that simulates robots covering a grid.

## Setup

The procedure varies based on exactly what you want to do with it, but a minimal setup requires Java 1.8 or higher. It should be possible to import the code directly into Eclipse, and `adsim.SimulatorMain` is the entry point.

## Running

There are many run configurations that have been tacked on over time (one of the TODO items is to improve the organization of these things; contributions are welcome). However, the primary use is for simulating the adversarial coverage problem (defined in ["Robotic Adversarial Coverage"](https://pdfs.semanticscholar.org/a61e/c64e0640c517793fec070196115208f8a648.pdf) by Yehoshua and Agmon).

Adversarial coverage is the default run configuration, so by running the main class you will see a GUI with the grid (`5 x 5` by default) and the robot (green ellipse). Many options can be found in the drop-downs, particularly in the `Settings` sub-menu. Playing with these values to see what happens is the best tutorial for the time being. Note that when values under `Settings` are changed, many require a restart to take effect. This can be done with the `New` menu option.

All of the options are available as CLI commands as well. No prompt is displayed because it would just get buried in log output (when running, various information is logged to standard output), but typing `:run` in the console and pressing `Enter` will start running the simulation. `:new`, `:pause`, `:step`, `:setdisplay (gui|headless)`, `:get` and `:set` are a few more that are worth trying out.

### Adversarial Coverage Algorithms

The default algorithm for adversarial coverage is `GSACGC` (found in the `adsim.algorithm_name` setting), which uses the Greedy Safest Adversarial Coverage (GSAC) algorithm developed by Yehoshua, Agmon, and Kaminka (["Safest Path Adversarial Coverage"](https://pdfs.semanticscholar.org/da66/8d90ae3dd3bff8b552c531db01b117acb191.pdf) in IROS 2014). It can be changed to `Random`, `DQL`, or `ExternalDQL`. The `Random` mode, as the name implies, moves the robot randomly. The `DQL` mode uses a deep q-learning algorithm coded in Java as part of the simulator. The `ExternalDQL` mode reads and writes simulator information to FIFO files to communicate with a separate process (not included in this repository) for the learning task. Documentation is not yet available for `ExternalDQL`. Also note that `DQL` can use an external neural network implementation for the deep q-network (it is just the DQL main loop that is in the simulator) if desired.

It is recommended to use the external network over the built-in implementation, because the built-in implementation (1) was my very first deep learning project back when I new nothing about neural nets, (2) was abandoned as soon as I learned Torch was an option, so therefore (3) is likely to have bugs and (4) lacks customizability (only very basic feedforward nets are supported, and they are very slow).

### License

Copyright (C) Mike D'Arcy 2017

All rights reserved for now, until I have more time to pick an open source license and go through all the files to add license statements. In the meantime, if you want to use the code in your project please email me ([m.m.darcy@vikes.csuohio.edu](mailto:m.m.darcy@vikes.csuohio.edu)) and we can work something out.

