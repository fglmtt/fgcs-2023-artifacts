package it.dt.function.augmentation;

import it.dt.function.exception.CommandLineException;

import java.util.List;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 01/10/2020 - 09:04
 */
public interface CommandLineExecutor {

    public CommandLineResult executeCommand(String command) throws CommandLineException;

    public CommandLineResult executeCommand(String command, List<String> parameterList, String targetDirectory) throws CommandLineException;

}
