# PDE Tests

## PDE Tester

### What it does

This test application starts two service provider systems, each providing two services, and a consumer system. The consumer system communicates with the PDE, the Service registry, and the Orchestrator, and verifies that these core systems are working together as intended.

### Usage

- Run an Arrowhead cloud containing at least a Service Registry, an Orchestrator, and a Plant Description Engine.
- Build the project: In the root directory, run

        mvn package

- Update the files `config/pde-tester.properties`, `config/dual-provider-1.properties`, and `config/dual-provider-2.properties`.
- In the root directory, run:

       ./run-test.sh

## Temperature demo

### What it does

This demo consists of three Arrowhead systems: Two thermometer applications which provide a `temperature` service, and one fan system which consumes this service.

### Usage

- Run an Arrowhead cloud containing at least a Service Registry, an Orchestrator, and a Plant Description Engone _or_ an Authorization system.
- Build the project: In the root directory, run

        mvn package

- Update the files `config/fan.properties`, `config/temperature-provider-1.properties`, and `config/temperature-provider-2.properties`.
- In the root directory, run:

       ./temperature-demo.sh

- Add orchestration rules (and, in the case of non-flexible orchestration, authorization rules) allowing the fan to consume the provided `temperature` services.
