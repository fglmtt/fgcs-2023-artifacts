# Function Driven WLDT Based Digital Twin

This Java project, named WLDT Function Driven Basic Digital Twin, 
encompasses several packages designed to manage functions within the context of a digital twin. 
Below is a brief description of each package:

## Adapter Package

- `FunctionDigitalAdapter`: Manages functions received on the digital interface.
- `FunctionPhysicalAdapter`: Handles functions to be executed on the physical interface.

## Augmentation Package

Contains classes related to executing Python functions within the Java engine using the command line. 

## Config Package

Holds configurations for the function-driven digital twin, specifying MQTT broker connections and topics.

## Orchestrator Package

- `DemoScenarioOrchestrator`: Demonstrates orchestration functionalities in a demo scenario.
- `SimpleOrchestrator`: Allows testing of basic orchestration for the execution of digital twins with functions.

## Shadowing Package

- `FunctionDrivenShadowing`: Implements function-driven shadowing, triggering functions upon receiving events from the physical world.

## Test Package

Includes test classes and functions to generate data for the digital-physical interface. It incorporates images on the physical adapter and functions to be updated and executed on the MQTT digital adapter.

## Main Classes

- `DigitalTwinProcess`: Launches the digital twin.
- `MqttLifecycleListener`: Monitors the digital twin's progress.

## Additional Project Components:

- `Dockerfile`: Facilitates project management using Docker.
- `Configuration file`: Manages project settings.
