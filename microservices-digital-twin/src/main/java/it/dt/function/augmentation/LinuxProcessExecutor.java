package it.dt.function.augmentation;

import it.dt.function.exception.CommandLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 30/09/2020 - 21:34
 */
public class LinuxProcessExecutor implements CommandLineExecutor {

    private static final Logger logger = LoggerFactory.getLogger(LinuxProcessExecutor.class);

    @Override
    public CommandLineResult executeCommand(String command) throws CommandLineException {
        return null;
    }

    public CommandLineResult executeCommand(String command, List<String> parameterList, String targetDirectory) throws CommandLineException {

        try{

            ArrayList<String> commandList = null;

            if(parameterList != null) {
                commandList = new java.util.ArrayList<>(List.copyOf(parameterList));
                commandList.add(0, command);
            }
            else{
                commandList = new ArrayList<>();
                commandList.add(command);
            }

            logger.info("Executing command: {} with parameters: {}", command, commandList);

            ProcessBuilder processBuilder = new ProcessBuilder(commandList);

            if(targetDirectory != null){
                logger.info("Setting target directory -> {}", targetDirectory);
                processBuilder.directory(new File(targetDirectory));
            }

            Process process = processBuilder.start();

            process.waitFor();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorBufferReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line = null;
            String errorLine = null;

            StringBuilder consoleLog = new StringBuilder();
            StringBuilder errorLog = new StringBuilder();

            while((line = bufferedReader.readLine()) != null || (errorLine = errorBufferReader.readLine()) != null) {
                if(line != null) consoleLog.append(line).append('\n');
                if(errorLine != null) errorLog.append(errorLine).append('\n');
            }

            return new CommandLineResult(process.exitValue(), consoleLog.toString(), errorLog.toString());

        }catch (Exception e){
            throw new CommandLineException(String.format("Error Executing Command: %s Parameters: %s Error: %s",
                    command,
                    parameterList,
                    e.getLocalizedMessage()));
        }

    }
}
