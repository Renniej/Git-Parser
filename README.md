# Git Parser Project

## Overview

This project was developed as part of the Hyperskill Kotlin Developer course. The main goal of this project is to parse and analyze Git objects to understand the underlying logic of Git operations. By delving into the mechanics of Git, this project aims to demystify the inner workings of one of the most widely used version control systems in the industry.

## Features

- **Parsing Git Objects**: Efficiently read and interpret various Git objects including blobs, commits, and trees.
- **Analyzing Commit Histories**: Understand the structure and data stored in commit objects.
- **Error Handling**: Diagnose and handle common issues that arise when using Git.
- **User Interface**: Simple CLI for interacting with the parsed data.
- **Branch and Log Information**: Print out local branches and logs for a given branch.
- **Full File Tree Display**: Print out the full file tree for a given commit.

## Skills Developed

### Kotlin Programming
- **Basic Kotlin**: Developed foundational skills in Kotlin, including variables, data types, and control structures.
- **Advanced Kotlin**: Utilized functional programming with lambdas and higher-order functions, managed exceptions, and explored Kotlin's standard library.
- **OOP Principles**: Applied object-oriented programming concepts, including classes, inheritance, and polymorphism.

### Git Knowledge
- **Git Object Types**: Learned about different Git object types (blobs, trees, commits) and their internal structures.
- **Git Commands**: Gained proficiency in basic and advanced Git commands.
- **Error Diagnosis**: Improved ability to diagnose and troubleshoot Git-related issues.

### Software Development Practices
- **Version Control**: Practiced version control techniques using Git and GitHub for managing and tracking changes in the codebase.
- **Code Documentation**: Developed skills in writing clear and concise documentation.
- **Unit Testing**: Implemented unit tests to ensure the correctness of the parsing logic.
- **Build Tools**: Used Gradle for dependency management and project builds.

## Project Structure

The project is organized into the following modules:

- **Parser**: Contains classes and functions for reading and interpreting Git objects.
- **Analyzer**: Includes logic for analyzing parsed data and extracting meaningful information.
- **CLI**: Provides a command-line interface for users to interact with the application.
- **Tests**: Unit tests to verify the functionality and reliability of the code.

## Installation and Usage

### Prerequisites

- Java 8 or higher
- Kotlin 1.4 or higher

### Clone the Repository

```sh
git clone https://github.com/yourusername/git-parser.git
cd git-parser
